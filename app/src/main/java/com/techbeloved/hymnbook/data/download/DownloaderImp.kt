package com.techbeloved.hymnbook.data.download

import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageReference
import com.techbeloved.hymnbook.data.model.DOWNLOAD_FAILED
import com.techbeloved.hymnbook.data.model.READY
import com.techbeloved.hymnbook.data.repo.HymnsRepository
import com.techbeloved.hymnbook.data.repo.OnlineRepo
import com.techbeloved.hymnbook.utils.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService

class DownloaderImp(private val storage: FirebaseStorage,
                    private val cacheDir: File,
                    private val hymnsRepository: HymnsRepository,
                    private val onlineRepo: OnlineRepo,
                    private val schedulerProvider: SchedulerProvider,
                    private val executor: ExecutorService) : Downloader {

    private val currentDownloads: MutableSet<Int> = mutableSetOf()

    private val disposables = CompositeDisposable()

    override fun enqueueDownloadForHymn(hymnId: Int) {
        // Only proceed if this item is not being downloaded already
        if (currentDownloads.contains(hymnId)) return
        currentDownloads += hymnId

        onlineRepo.getHymnById(hymnId)
                .doOnNext { Timber.i(it.sheetMusicUrl) }
                .subscribeOn(schedulerProvider.io())
                .flatMap { hymn ->
                    // The public url is of the form https://storage.googleapis.com/hymnbook-50b7e.appspot.com/catalogs/wccrm/pdf/2019-05-17_01.16.16hymn_114.pdf
                    val subPath = hymn.sheetMusicUrl.substring(hymn.sheetMusicUrl.lastIndexOf("catalog"))
                    val storageRef = storage.getReference(subPath)
                    Timber.i(storageRef.toString())
                    val destinationDir = File(cacheDir, "sheet_music")
                    if (!destinationDir.exists()) destinationDir.mkdir()
                    val destinationFile = File(destinationDir, "hymn_$hymnId.pdf")
                    getFile(storageRef, destinationFile, hymn.sheetMusicUrl)
                }
                .observeOn(schedulerProvider.io())
                .subscribe({ download ->

                    when (download) {
                        is FileDownload.Success -> {
                            val progress = 100
                            Timber.i("Download Success should update status $download")
                            hymnsRepository.updateHymnDownloadStatus(hymnId, progress, READY, download.remoteUrl, download.destinationUrl)
                            currentDownloads.remove(hymnId)
                        }
                        is FileDownload.Canceled -> {
                            hymnsRepository.updateHymnDownloadProgress(hymnId, 0, DOWNLOAD_FAILED)
                            currentDownloads.remove(hymnId)
                        }
                        is FileDownload.Progress -> {
                            Timber.i("Id: $hymnId, Progress: ${download.progress}")
                            hymnsRepository.updateHymnDownloadProgress(hymnId, download.progress)
                        }
                        is FileDownload.Failure -> {
                            hymnsRepository.updateHymnDownloadProgress(hymnId, 0, DOWNLOAD_FAILED)
                            currentDownloads.remove(hymnId)
                        }
                    }

                }, { throwable ->
                    Timber.w(throwable)
                    hymnsRepository.updateHymnDownloadProgress(hymnId, 0, DOWNLOAD_FAILED)
                    currentDownloads.remove(hymnId)
                }).run { disposables.add(this) }
    }

    override fun clear() {
        if (!disposables.isDisposed) disposables.dispose()
        currentDownloads.clear()
    }

    private fun getFile(storageRef: StorageReference, destination: File, sheetMusicUrl: String): Observable<FileDownload> {
        return Observable.create { emitter ->
            val downloadTask = storageRef.getFile(destination)
                    .addOnProgressListener(executor, OnProgressListener { taskSnapshot: FileDownloadTask.TaskSnapshot ->
                        val progress: Int = (taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount.toFloat() * 100).toInt()

                        emitter.onNext(FileDownload.Progress(progress))
                    })
                    .addOnSuccessListener(executor, OnSuccessListener {
                        Timber.i("success")
                        val progress: Int = (it.bytesTransferred / it.totalByteCount.toFloat() * 100).toInt()
                        if (progress == 100) {
                            //emitter.onNext(FileDownload.Progress(progress))
                        }
                        emitter.onNext(FileDownload.Success(sheetMusicUrl, destination.absolutePath))
                    })
                    .addOnFailureListener(executor, OnFailureListener { exception ->
                        Timber.w(exception)
                        emitter.onNext(FileDownload.Failure(exception))
                    })
                    .addOnCanceledListener(executor, OnCanceledListener {
                        Timber.i("cancelled")
                        emitter.onNext(FileDownload.Canceled)
                    })
            emitter.setCancellable { downloadTask.cancel() }
        }
    }

}

sealed class FileDownload {
    data class Progress(val progress: Int) : FileDownload()
    data class Success(val remoteUrl: String, val destinationUrl: String) : FileDownload()
    object Canceled : FileDownload()
    data class Failure(val error: Exception) : FileDownload()
}