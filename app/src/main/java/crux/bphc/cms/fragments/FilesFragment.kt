package crux.bphc.cms.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.R
import crux.bphc.cms.adapters.FilesAdapter
import crux.bphc.cms.core.getFormattedFileSize
import crux.bphc.cms.databinding.DownloadsFragmentBinding
import crux.bphc.cms.models.Download
import crux.bphc.cms.models.SingleLiveEvent
import crux.bphc.cms.utils.FileUtils
import crux.bphc.cms.viewmodels.FilesViewModel
import kotlinx.android.synthetic.main.fragment_my_courses.*
import java.io.File
import java.util.*


class FilesFragment : Fragment() {


    private lateinit var binding: DownloadsFragmentBinding
    private val viewModel by viewModels<FilesViewModel>()

    private val downloadsAdapter = FilesAdapter()

    override fun onStart() {
        super.onStart()
        requireActivity().title = getString(R.string.nav_bar_files)

        // Overriding the back button since when inside a folder we'd want the back button to go up one folder instead of going back to the previous fragment
        requireView().apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    // Returning false makes the system take over so goBackOneDirectory returns false when in the outermost folder
                    return@setOnKeyListener viewModel.goBackOneDirectory()
                }
                false
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DownloadsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadsAdapter.onRowClickListener = {
            if (it.isDirectory) {
                viewModel.goToDirectory(it.name)
            } else {
                openFile(it)
            }
        }

        downloadsAdapter.onDeleteClickListener = {
            showDeleteConfirmationDialog(it)
        }

        binding.downloadsRecycler.adapter = downloadsAdapter

        with(viewModel) {
            isLoading.observe(viewLifecycleOwner, isLoadingObserver)
            downloads.observe(viewLifecycleOwner, downloadsObserver)
            isListEmptyMessage.observe(viewLifecycleOwner, isListEmptyMessageObserver)
            deletedMessage.observe(viewLifecycleOwner, deletedMessageObserver)
        }
    }

    private val isLoadingObserver = Observer<Boolean> { isLoading ->
        with(binding) {
            if (isLoading) {
                filesProgress.isVisible = true
                noFilesText.isVisible = false
                downloadsRecycler.isVisible = false
            } else {
                filesProgress.isVisible = false
            }
        }
    }

    private val downloadsObserver = Observer<List<Download>> {
        binding.downloadsRecycler.isVisible = true
        downloadsAdapter.data = it
    }

    private val isListEmptyMessageObserver = Observer<Int?> { message ->
        binding.noFilesText.isVisible = message != null
        message?.let {
            binding.noFilesText.text = getString(it)
        }
    }

    private val deletedMessageObserver = Observer<SingleLiveEvent<Boolean>> { deletedSuccessfully ->
        deletedSuccessfully.getContentIfNotHandled()?.let { deleted ->
            Snackbar.make(binding.downloadsCoordinator, getString(if (deleted) R.string.delete_file_success else R.string.delete_file_failed), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File) {
        val fileUri = FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, FileUtils.getFileMimeType(file.name))
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            intent.setDataAndType(fileUri, "application/*")
            startActivity(
                    Intent.createChooser(
                            intent,
                            "No Application found to open File - ${file.name}"
                    )
            )
        }
    }

    private fun showDeleteConfirmationDialog(file: File) {
        val fileType = getString(if (file.isDirectory) R.string.folder else R.string.file)
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_file_dialog_title, fileType))
                .setMessage(getString(R.string.delete_file_dialog_message, fileType, file.getFormattedFileSize(requireContext())))
                .setNeutralButton(R.string.delete_file_dialog_neutral) { _, _ -> /* Do nothing */ }
                .setPositiveButton(R.string.delete_file_dialog_positive) { _, _ -> viewModel.deleteFile(file) }
                .show()

    }

    companion object {
        @JvmStatic
        fun newInstance(): FilesFragment = FilesFragment()
    }
}