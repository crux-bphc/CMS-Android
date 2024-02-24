package crux.bphc.cms.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import crux.bphc.cms.R
import crux.bphc.cms.core.FileManager
import crux.bphc.cms.databinding.FragmentFolderModuleBinding
import crux.bphc.cms.databinding.RowFolderModuleFileBinding
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.course.Content
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.utils.FileUtils
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import java.util.*

class FolderModuleFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var mFileManager: FileManager
    private lateinit var moreOptionsViewModel: OptionsViewModel
    private lateinit var mAdapter: FolderModuleAdapter
    private lateinit var binding: FragmentFolderModuleBinding

    private var moduleInstance = 0
    private var courseName: String = ""
    private var module: Module? = null
    private var contents: List<Content> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentFolderModuleBinding.inflate(layoutInflater)
        moduleInstance = requireArguments().getInt(MODULE_ID_KEY)
        courseName = requireArguments().getString(COURSE_NAME_KEY) ?: ""

        realm = Realm.getDefaultInstance()
        mFileManager = FileManager(requireActivity(), courseName) { fileName: String ->
            onDownloadComplete(fileName)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moreOptionsViewModel = ViewModelProvider(requireActivity())[OptionsViewModel::class.java]

        val mClickListener = ClickListener { `object`: Any, _: Int ->
            val content = `object` as Content
            downloadOrOpenFile(content, false)
            true
        }

        val layoutManager = LinearLayoutManager(context)

        mAdapter = FolderModuleAdapter(mClickListener, ArrayList())
        binding.files.layoutManager = layoutManager
        binding.files.adapter = mAdapter

        module = realm.where(Module::class.java).equalTo("instance", moduleInstance).findFirst()
        contents = module?.contents ?: emptyList()
        updateContents()
    }

    private fun updateContents() {
        mAdapter.setContents(contents)
    }

    private fun downloadOrOpenFile(content: Content, forceDownload: Boolean) {
        if (forceDownload || !mFileManager.isModuleContentDownloaded(content)) {
            Toast.makeText(activity, "Downloading file - " + content.fileName, Toast.LENGTH_SHORT)
                .show()
            mFileManager.downloadModuleContent(content, module!!)
        } else {
            mFileManager.openModuleContent(content)
        }
    }

    private fun onDownloadComplete(fileName: String) {
        mAdapter.notifyDataSetChanged()
        val content = contents.stream().filter{ it.fileName == fileName }.findAny().orElse(null)
        content?.apply { mFileManager.openModuleContent(this) }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    private inner class FolderModuleAdapter(
        private val mClickListener: ClickListener,
        private val contents: MutableList<Content>
    ) : RecyclerView.Adapter<FolderModuleAdapter.FolderModuleViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        fun setContents(contents: List<Content>) {
            this.contents.addAll(contents)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderModuleViewHolder {
            val itemBinding = RowFolderModuleFileBinding.inflate(inflater, parent, false)
            return FolderModuleViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: FolderModuleViewHolder, position: Int) {
            holder.bind(contents[position])
        }

        override fun getItemCount(): Int {
            return contents.size
        }

        inner class FolderModuleViewHolder(val itemBinding: RowFolderModuleFileBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(content: Content) {
                itemBinding.name.text = content.fileName

                val icon = FileUtils.getDrawableIconFromFileName(content.fileName)
                itemBinding.icon.setImageResource(icon)

                val downloaded = mFileManager.isModuleContentDownloaded(content)
                if (downloaded) {
                    itemBinding.download.setImageResource(R.drawable.eye)
                }

                itemBinding.clickWrapper.setOnClickListener {
                    mClickListener.onClick(contents[layoutPosition], layoutPosition)
                }

                itemBinding.more.setOnClickListener {
                    val options = ArrayList<MoreOptionsFragment.Option>()
                    val observer: Observer<MoreOptionsFragment.Option?> // to handle the selection
                    if (downloaded) {
                        options.addAll(
                            listOf(
                                MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                                MoreOptionsFragment.Option(1, "Re-Download", R.drawable.outline_file_download_24),
                                MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                                MoreOptionsFragment.Option(3, "Properties", R.drawable.ic_info)
                            )
                        )
                        observer = Observer { option: MoreOptionsFragment.Option? ->
                            if (option == null) return@Observer
                            when (option.id) {
                                0 -> downloadOrOpenFile(content, false)
                                1 -> downloadOrOpenFile(content, true)
                                2 -> mFileManager.shareModuleContent(content)
                                3 -> PropertiesAlertDialog(requireActivity(), content).show()
                            }
                            moreOptionsViewModel.selection.removeObservers(requireActivity())
                            moreOptionsViewModel.clearSelection()
                        }
                    } else {
                        options.addAll(
                            listOf(
                                MoreOptionsFragment.Option(0, "Download", R.drawable.outline_file_download_24),
                                MoreOptionsFragment.Option(1, "Properties", R.drawable.ic_info)
                            )
                        )
                        observer = Observer { option: MoreOptionsFragment.Option? ->
                            if (option == null) return@Observer
                            when (option.id) {
                                0 -> downloadOrOpenFile(content, false)
                                1 -> PropertiesAlertDialog(requireContext(), content).show()
                            }
                        }
                        moreOptionsViewModel.selection.removeObservers(requireActivity())
                        moreOptionsViewModel.clearSelection()
                    }

                    val fragment = MoreOptionsFragment.newInstance(content.fileName, options)
                    fragment.show(requireActivity().supportFragmentManager, fragment.tag)
                    moreOptionsViewModel.selection.observe(requireActivity(), observer)
                }
            }
        }
    }

    companion object {
        private const val MODULE_ID_KEY = "moduleID"
        private const val COURSE_NAME_KEY = "courseName"

        fun newInstance(moduleId: Int, courseName: String): FolderModuleFragment {
            val args = Bundle()
            args.putInt(MODULE_ID_KEY, moduleId)
            args.putString(COURSE_NAME_KEY, courseName)

            val fragment = FolderModuleFragment()
            fragment.arguments = args
            return fragment
        }
    }
}