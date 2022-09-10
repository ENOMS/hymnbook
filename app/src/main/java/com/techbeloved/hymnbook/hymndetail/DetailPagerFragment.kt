package com.techbeloved.hymnbook.hymndetail


import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.snackbar.Snackbar
import com.techbeloved.hymnbook.R
import com.techbeloved.hymnbook.sheetmusic.SheetMusicDetailFragment
import com.techbeloved.hymnbook.usecases.Lce
import com.techbeloved.hymnbook.utils.DepthPageTransformer
import com.techbeloved.hymnbook.utils.MINIMUM_VERSION_FOR_SHARE_LINK
import com.techbeloved.hymnbook.utils.WCCRM_LOGO_URL
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DetailPagerFragment : BaseDetailPagerFragment() {

    private val detailArgs by navArgs<DetailPagerFragmentArgs>()
    private val viewModel: HymnPagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore current index
        currentHymnId = savedInstanceState?.getInt(EXTRA_CURRENT_ITEM_ID, detailArgs.hymnId)
            ?: detailArgs.hymnId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewpagerHymnDetail.setPageTransformer(DepthPageTransformer())
        val detailPagerAdapter = DetailPagerAdapter()
        binding.viewpagerHymnDetail.adapter = detailPagerAdapter

        viewModel.hymnIndicesLiveData.observe(viewLifecycleOwner) { indicesLce ->
            val indexToLoad = currentHymnId
            when (indicesLce) {
                is Lce.Loading -> showProgressLoading(indicesLce.loading)
                is Lce.Content -> {
                    initializeViewPager(detailPagerAdapter, indicesLce.content, indexToLoad)
                    updatePlaybackHymnItems(indicesLce.content.map { it.index })
                }
                is Lce.Error -> showContentError(indicesLce.error)
            }
        }

        viewModel.header.observe(viewLifecycleOwner) { title ->
            binding.toolbarDetail.title = title
        }

        viewModel.shareLinkStatus.observe(viewLifecycleOwner) { shareStatus ->
            when (shareStatus) {
                ShareStatus.Loading -> showShareLoadingDialog()
                is ShareStatus.Success -> showShareOptionsChooser(shareStatus.shareLink)
                is ShareStatus.Error -> {
                    showShareError(shareStatus.error)
                }
                ShareStatus.None -> {
                    cancelProgressDialog()
                }
            }
        }
    }

    private fun showShareError(error: Throwable) {
        Timber.w(error)
        Snackbar.make(
            requireView().rootView,
            "Failure creating share content",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showShareOptionsChooser(shareLink: String) {
        ShareCompat.IntentBuilder.from(requireActivity()).apply {
            setChooserTitle(getString(R.string.share_hymn))
            setType("text/plain")
            setText(shareLink)
        }.startChooser()

    }

    private var progressDialog: ProgressDialog? = null
    private fun showShareLoadingDialog() {
        progressDialog = ProgressDialog.show(requireContext(), "Share hymn", "Working")
        progressDialog?.setCancelable(true)
    }

    private fun cancelProgressDialog() {
        progressDialog?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_CURRENT_ITEM_ID, currentHymnId)
        super.onSaveInstanceState(outState)
    }

    private fun initializeViewPager(
        detailPagerAdapter: DetailPagerAdapter,
        hymnIndices: List<HymnNumber>,
        initialIndex: Int
    ) {
        Timber.i("Initializing viewPager with index: $initialIndex")
        val oldItemCount = detailPagerAdapter.itemCount
        detailPagerAdapter.submitList(hymnIndices)

        // initialIndex represents the hymn number, where as the adapter uses a zero based index
        // Which implies that when the indices is sorted by titles, the correct detail won't be shown.
        // So we just need to find the index from the list of hymn indices

        val indexToLoad = hymnIndices.indexOfFirst { it.index == initialIndex }
        if (oldItemCount == 0) {
            // Set the initial item to load. Otherwise, this is a refresh.
            binding.viewpagerHymnDetail.currentItem = indexToLoad
        }
    }

    override fun initiateContentSharing() {
        viewModel.requestShareLink(
            currentHymnId,
            getString(R.string.about_app),
            MINIMUM_VERSION_FOR_SHARE_LINK,
            WCCRM_LOGO_URL
        )
    }


    inner class DetailPagerAdapter : FragmentStateAdapter(this) {
        private val items = mutableListOf<HymnNumber>()

        override fun createFragment(position: Int): Fragment {
            val item = items[position]
            if (item.preferSheetMusic) return SheetMusicDetailFragment().apply { init(item.index) }
            return DetailFragment().apply { init(item.index) }
        }

        override fun getItemId(position: Int): Long {
            return items[position].hashCode().toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return items.any { it.hashCode().toLong() == itemId }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        fun getItem(position: Int): HymnNumber? {
            val itemId = getItemId(position)
            return items.firstOrNull { it.hashCode().toLong() == itemId }
        }

        fun submitList(newItems: List<HymnNumber>) {
            val callback = HmnPagerDiffer(items, newItems)
            val diff = DiffUtil.calculateDiff(callback)
            this.items.clear()
            this.items.addAll(newItems)
            diff.dispatchUpdatesTo(this)
        }

    }

}
