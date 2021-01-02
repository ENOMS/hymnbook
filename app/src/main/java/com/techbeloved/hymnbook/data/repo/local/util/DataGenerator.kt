package com.techbeloved.hymnbook.data.repo.local.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.techbeloved.hymnbook.HymnbookApp
import com.techbeloved.hymnbook.data.model.Hymn
import com.techbeloved.hymnbook.data.model.Topic
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset

object DataGenerator {

    fun generateHymns(): List<Hymn> {
        val typeOfHymnList = object : TypeToken<List<Hymn>>() {}.type

        val gson = GsonBuilder().create()
        return getHymnFiles().map { filename -> loadHymnJsonFromAsset(filename).also { Timber.d("Loaded $filename") } }
                .flatMap { hymnJson ->
                    hymnJson?.let { gson.fromJson(it, typeOfHymnList) } ?: emptyList()
                }
    }

    fun generateTopics(): List<Topic> {
        val typeOfTopicList = object : TypeToken<List<Topic>>() {}.type
        return GsonBuilder().create().fromJson(loadTopicsJsonFromAsset(), typeOfTopicList)
    }

    private fun loadHymnJsonFromAsset(filename: String): String? {
        val json: String?
        try {
            val inputStream = HymnbookApp.instance.assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.defaultCharset())
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return json
    }

    /**
     * Preloaded hymn files must be json and contain the word "hymns" and located in the assets folder
     */
    private fun getHymnFiles(): List<String> {
        return HymnbookApp.instance.assets.list("")?.filter { it.matches(".*hymns.*\\.json$".toRegex()) }
                ?: emptyList()
    }

    fun loadTopicsJsonFromAsset(): String? {
        val json: String?
        try {
            val inputStream = HymnbookApp.instance.assets.open("all_topics.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.defaultCharset())
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return json
    }
}
