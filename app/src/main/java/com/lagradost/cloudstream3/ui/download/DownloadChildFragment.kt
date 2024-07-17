package com.lagradost.cloudstream3.ui.download

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter.formatShortFileSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentChildDownloadsBinding
import com.lagradost.cloudstream3.mvvm.observe
import com.lagradost.cloudstream3.ui.download.DownloadButtonSetup.handleDownloadClick
import com.lagradost.cloudstream3.ui.result.FOCUS_SELF
import com.lagradost.cloudstream3.ui.result.setLinearListLayout
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.attachBackPressedCallback
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.detachBackPressedCallback
import com.lagradost.cloudstream3.utils.UIHelper.fixPaddingStatusbar
import com.lagradost.cloudstream3.utils.UIHelper.setAppBarNoScrollFlagsOnTV
import com.lagradost.cloudstream3.utils.VideoDownloadManager

class DownloadChildFragment : Fragment() {
    private lateinit var downloadsViewModel: DownloadViewModel

    companion object {
        fun newInstance(headerName: String, folder: String): Bundle {
            return Bundle().apply {
                putString("folder", folder)
                putString("name", headerName)
            }
        }
    }

    override fun onDestroyView() {
        unsetDownloadDeleteListener()
        detachBackPressedCallback()
        binding = null
        super.onDestroyView()
    }

    private var binding: FragmentChildDownloadsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        downloadsViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        val localBinding = FragmentChildDownloadsBinding.inflate(inflater, container, false)
        binding = localBinding
        return localBinding.root
    }

    private var downloadDeleteEventListener: ((Int) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * We never want to retain multi-delete state
         * when navigating to downloads. Setting this state
         * immediately can sometimes result in the observer
         * not being notified in time to update the UI.
         *
         * By posting to the main looper, we ensure that this
         * operation is executed after the view has been fully created
         * and all initializations are completed, allowing the
         * observer to properly receive and handle the state change.
         */
        Handler(Looper.getMainLooper()).post {
            downloadsViewModel.setIsMultiDeleteState(false)
        }

        val folder = arguments?.getString("folder")
        val name = arguments?.getString("name")
        if (folder == null) {
            activity?.onBackPressedDispatcher?.onBackPressed() // TODO FIX
            return
        }
        fixPaddingStatusbar(binding?.downloadChildRoot)

        binding?.downloadChildToolbar?.apply {
            title = name
            if (isLayout(PHONE or EMULATOR)) {
                setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                setNavigationOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            setAppBarNoScrollFlagsOnTV()
        }

        binding?.downloadDeleteAppbar?.setAppBarNoScrollFlagsOnTV()

        observe(downloadsViewModel.childCards) {
            if (it.isEmpty()) {
                activity?.onBackPressedDispatcher?.onBackPressed()
                return@observe
            }

            (binding?.downloadChildList?.adapter as? DownloadAdapter)?.submitList(it)
        }
        observe(downloadsViewModel.isMultiDeleteState) { isMultiDeleteState ->
            val adapter = binding?.downloadChildList?.adapter as? DownloadAdapter
            adapter?.setIsMultiDeleteState(isMultiDeleteState)
            binding?.downloadDeleteAppbar?.isVisible = isMultiDeleteState
            if (!isMultiDeleteState) {
                detachBackPressedCallback()
                downloadsViewModel.clearSelectedItems()
                binding?.downloadChildToolbar?.isVisible = true
            }
        }
        observe(downloadsViewModel.selectedBytes) {
            updateDeleteButton(downloadsViewModel.selectedItemIds.value?.count() ?: 0, it)
        }
        observe(downloadsViewModel.selectedItemIds) {
            handleSelectedChange(it)
            updateDeleteButton(it.count(), downloadsViewModel.selectedBytes.value ?: 0L)

            binding?.btnDelete?.isVisible = it.isNotEmpty()
            binding?.selectItemsText?.isVisible = it.isEmpty()

            val allSelected = downloadsViewModel.isAllSelected()
            if (allSelected) {
                binding?.btnToggleAll?.setText(R.string.deselect_all)
            } else binding?.btnToggleAll?.setText(R.string.select_all)
        }

        val adapter = DownloadAdapter(
            {},
            { downloadClickEvent ->
                handleDownloadClick(downloadClickEvent)
                if (downloadClickEvent.action == DOWNLOAD_ACTION_DELETE_FILE) {
                    setUpDownloadDeleteListener(folder)
                }
            },
            { itemId, isChecked ->
                if (isChecked) {
                    downloadsViewModel.addSelected(itemId)
                } else downloadsViewModel.removeSelected(itemId)
            }
        )

        binding?.downloadChildList?.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            this.adapter = adapter
            setLinearListLayout(
                isHorizontal = false,
                nextRight = FOCUS_SELF,
                nextDown = FOCUS_SELF,
            )
        }

        downloadsViewModel.updateChildList(requireContext(), folder)
    }

    private fun handleSelectedChange(selected: MutableSet<Int>) {
        if (selected.isNotEmpty()) {
            binding?.downloadDeleteAppbar?.isVisible = true
            binding?.downloadChildToolbar?.isVisible = false
            activity?.attachBackPressedCallback {
                downloadsViewModel.setIsMultiDeleteState(false)
            }

            binding?.btnDelete?.setOnClickListener {
                // We want to unset it here if we have it so
                // that we don't have to run it for every download,
                // we just do it once here.
                unsetDownloadDeleteListener()
                context?.let { ctx ->
                    downloadsViewModel.handleMultiDelete(ctx) {
                        arguments?.getString("folder")
                            ?.let { folder -> downloadsViewModel.updateChildList(ctx, folder) }
                    }
                }
            }

            binding?.btnCancel?.setOnClickListener {
                downloadsViewModel.setIsMultiDeleteState(false)
            }

            binding?.btnToggleAll?.setOnClickListener {
                val allSelected = downloadsViewModel.isAllSelected()
                val adapter = binding?.downloadChildList?.adapter as? DownloadAdapter
                if (allSelected) {
                    adapter?.notifySelectionStates()
                    downloadsViewModel.clearSelectedItems()
                } else {
                    adapter?.notifyAllSelected()
                    downloadsViewModel.selectAllItems()
                }
            }

            downloadsViewModel.setIsMultiDeleteState(true)
        }
    }

    private fun updateDeleteButton(count: Int, selectedBytes: Long) {
        val formattedSize = formatShortFileSize(context, selectedBytes)
        binding?.btnDelete?.text =
            getString(R.string.delete_format).format(count, formattedSize)
    }

    private fun setUpDownloadDeleteListener(folder: String) {
        downloadDeleteEventListener = { id: Int ->
            val list = (binding?.downloadChildList?.adapter as? DownloadAdapter)?.currentList
            if (list?.any { it.data.id == id } == true) {
                context?.let { downloadsViewModel.updateChildList(it, folder) }
            }
        }
        downloadDeleteEventListener?.let { VideoDownloadManager.downloadDeleteEvent += it }
    }

    private fun unsetDownloadDeleteListener() {
        downloadDeleteEventListener?.let {
            VideoDownloadManager.downloadDeleteEvent -= it
        }
        downloadDeleteEventListener = null
    }
}