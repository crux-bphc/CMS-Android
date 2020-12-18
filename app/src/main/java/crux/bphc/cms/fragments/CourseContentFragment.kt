package crux.bphc.cms.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import crux.bphc.cms.R
import crux.bphc.cms.adapters.CourseContentAdapter
import crux.bphc.cms.app.Constants
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.helper.CourseRequestHandler.CallBack
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.io.FileManager
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Siddhant Kumar Patel, Abhijeet Viswa
 */
class CourseContentFragment : Fragment() {
    private lateinit var fileManager: FileManager
    private lateinit var courseDataHandler: CourseDataHandler
    private lateinit var realm: Realm

    var courseId: Int = 0
    private lateinit var courseName: String
    private lateinit var courseSections: List<CourseSection>

    private lateinit var empty: TextView
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseContentAdapter
    private lateinit var moreOptionsViewModel: OptionsViewModel

    private val moduleClickWrapperClickListener = createModuleClickWrapperClickListener()
    private val moduleMoreOptionsClickListener = createModuleMoreOptionsClickListener()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MODULE_ACTIVITY && resultCode == FileManager.DATA_DOWNLOADED) {
            setCourseContentsOnAdapter()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Show error if invalid courseId
        courseId = arguments?.getInt(COURSE_ID_KEY) ?: -1

        // Initialize realm here instead of onCreateView so that other classes can be initialized
        realm = Realm.getDefaultInstance()

        courseDataHandler = CourseDataHandler(requireActivity(), realm)
        courseName = courseDataHandler.getCourseName(courseId)
        courseSections = ArrayList()

        fileManager = FileManager(requireActivity(), courseName)
        fileManager.registerDownloadReceiver()

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        val title = courseDataHandler.getCourseNameForActionBarTitle(courseId)
        requireActivity().title = title
        super.onStart()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_course_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moreOptionsViewModel = ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)
        empty = view.findViewById(R.id.empty) as TextView
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recycler_view)
        courseSections = courseDataHandler.getCourseData(courseId)
        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.isRefreshing = true
            sendRequest(courseId)
        }
        adapter = CourseContentAdapter(requireActivity(), courseContents, fileManager,
                moduleClickWrapperClickListener, moduleMoreOptionsClickListener)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setItemViewCacheSize(5)
        fileManager.setCallback { setCourseContentsOnAdapter() }
        mSwipeRefreshLayout.setOnRefreshListener {
            mSwipeRefreshLayout.isRefreshing = true
            sendRequest(courseId)
        }
        empty.setOnClickListener {
            mSwipeRefreshLayout.isRefreshing = true
            sendRequest(courseId)
        }
        showSectionsOrEmpty()
    }

    private fun showSectionsOrEmpty() {
        if (courseSections.stream().anyMatch { section: CourseSection -> !section.modules.isEmpty() }) {
            empty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun createModuleMoreOptionsClickListener(): ClickListener {
        return ClickListener { `object`: Any, position: Int ->
            val moreOptionsViewModel = moreOptionsViewModel
            val module = `object` as Module
            val content = module.contents.first()
            val downloaded = content != null && fileManager.isModuleContentDownloaded(content)

            /* Set up our options and their handlers */
            val options = ArrayList<MoreOptionsFragment.Option>()
            val observer: Observer<MoreOptionsFragment.Option?> = if (downloaded) {
                options.addAll(listOf(
                        MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                        MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                        MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                        MoreOptionsFragment.Option(3, "Mark as Unread", R.drawable.eye_off)
                ))
                if (module.modType === Module.Type.RESOURCE) {
                    options.add(MoreOptionsFragment.Option(4, "Properties", R.drawable.ic_info))
                }
                Observer label@ { option: MoreOptionsFragment.Option? ->
                    if (option == null) return@label
                    when (option.id) {
                        0 -> fileManager.openModuleContent(content!!)
                        1 -> {
                            if (!module.isDownloadable) {
                                return@label
                            }
                            Toast.makeText(activity, "Downloading file - " + content!!.fileName,
                                    Toast.LENGTH_SHORT).show()
                            fileManager.downloadModuleContent(content, module)
                        }
                        2 -> fileManager.shareModuleContent(content)
                        3 -> {
                            courseDataHandler.markModuleAsReadOrUnread(module, true)
                            adapter.notifyItemChanged(position)
                        }
                        4 -> PropertiesAlertDialog(activity, content).show()
                    }
                    if (activity != null) {
                        moreOptionsViewModel.selection.removeObservers(requireActivity())
                    }
                    moreOptionsViewModel.clearSelection()
                }
            } else {
                options.addAll(listOf(
                        MoreOptionsFragment.Option(0, "Download", R.drawable.download),
                        MoreOptionsFragment.Option(1, "Share", R.drawable.ic_share),
                        MoreOptionsFragment.Option(2, "Mark as Unread", R.drawable.eye_off)
                ))
                if (module.modType === Module.Type.RESOURCE) {
                    options.add(MoreOptionsFragment.Option(
                            3, "Properties", R.drawable.ic_info))
                }
                Observer label@ { option: MoreOptionsFragment.Option? ->
                    if (option == null) return@label
                    val activity = activity
                    when (option.id) {
                        0 -> if (content != null) {
                            fileManager.downloadModuleContent(content, module)
                        }
                        1 -> shareModuleLinks(module)
                        2 -> {
                            courseDataHandler.markModuleAsReadOrUnread(module, true)
                            adapter.notifyItemChanged(position)
                        }
                        3 -> if (content != null && activity != null) {
                            PropertiesAlertDialog(activity, content).show()
                        }
                    }
                    if (activity != null) {
                        moreOptionsViewModel.selection.removeObservers(activity)
                    }
                    moreOptionsViewModel.clearSelection()
                }
            }

            /* Show the fragment and register the observer */
            val activity = activity
            if (activity != null) {
                val moreOptionsFragment = MoreOptionsFragment.newInstance(module.name, options)
                moreOptionsFragment.show(requireActivity().supportFragmentManager,
                        moreOptionsFragment.tag)
                moreOptionsFragment.show(activity.supportFragmentManager, moreOptionsFragment.tag)
                moreOptionsViewModel.selection.observe(activity, observer)
                courseDataHandler.markModuleAsReadOrUnread(module, false)
                adapter.notifyItemChanged(position)
            }
            true
        }
    }

    private fun createModuleClickWrapperClickListener(): ClickListener {
        return ClickListener { `object`: Any, _: Int ->
            val module = `object` as Module
            val activity = activity
            val content = if (!module.contents.isEmpty()) module.contents.first() else null
            when (module.modType) {
                Module.Type.URL -> if (activity != null && content != null) {
                    val url = content.fileUrl
                    if (url.isNotEmpty()) {
                        Utils.openURLInBrowser(activity, url)
                    }
                }
                Module.Type.FORUM, Module.Type.FOLDER -> if (activity != null) {
                    val fragment = if (module.modType === Module.Type.FORUM) ForumFragment.newInstance(module.instance, courseName) else FolderModuleFragment.newInstance(module.instance, courseName)
                    activity.supportFragmentManager
                            .beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.course_section_enrol_container, fragment, "Announcements")
                            .commit()
                }
                Module.Type.LABEL -> {
                    val desc = module.description
                    if (activity != null && desc.isNotEmpty()) {
                        val alertDialog: AlertDialog.Builder = if (MyApplication.getInstance()
                                        .isDarkModeEnabled) {
                            AlertDialog.Builder(activity, R.style.Theme_AppCompat_Dialog_Alert)
                        } else {
                            AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert)
                        }
                        val htmlDescription = HtmlCompat.fromHtml(module.description,
                                HtmlCompat.FROM_HTML_MODE_COMPACT)
                        val descriptionWithOutExtraSpace = htmlDescription.toString().trim { it <= ' ' }
                        alertDialog.setMessage(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length))
                        alertDialog.setNegativeButton("Close", null)
                        alertDialog.show()
                    }
                }
                Module.Type.RESOURCE -> if (content != null) {
                    if (fileManager.isModuleContentDownloaded(content)) {
                        fileManager.openModuleContent(content)
                    } else {
                        Toast.makeText(getActivity(), "Downloading file - " + content.fileName,
                                Toast.LENGTH_SHORT).show()
                        fileManager.downloadModuleContent(content, module)
                    }
                }
                else -> if (activity != null) {
                    Utils.openURLInBrowser(activity, module.url)
                }
            }
            courseDataHandler.markModuleAsReadOrUnread(module, false)
            true
        }
    }

    private fun shareModuleLinks(module: Module) {
        val content = (if (!module.contents.isEmpty()) module.contents.first() else null) ?: return
        val toShare = content.fileUrl.replace("/moodle", "/fileShare/moodle") +
                "&courseName=" + courseName.replace(" ", "%20") + "&courseId=" + courseId
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, toShare)
        if (context != null) requireContext().startActivity(Intent.createChooser(sharingIntent, null))
    }

    private fun sendRequest(courseId: Int) {
        val courseRequestHandler = CourseRequestHandler(activity)
        courseRequestHandler.getCourseData(courseId, object : CallBack<List<CourseSection?>?> {
            override fun onResponse(sectionList: List<CourseSection?>?) {
                empty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                if (sectionList == null) {
                    // TODO: not registered, ask to register, change UI, show enroll button
                    return
                }
                for (courseSection in sectionList.filterNotNull()) {
                    for (module in courseSection.modules) {
                        if (module.modType == Module.Type.FORUM) {
                            // TODO: Convert to coroutines
                            courseRequestHandler.getForumDiscussions(module.instance,
                                    object : CallBack<List<Discussion?>?> {
                                override fun onResponse(responseObject: List<Discussion?>?) {
                                    if (responseObject == null) return
                                    for (d in responseObject) {
                                        d?.forumId = module.instance
                                    }
                                    val newDiscussions = courseDataHandler
                                            .setForumDiscussions(module.instance, responseObject)
                                    if (newDiscussions.size > 0) {
                                        courseDataHandler.markModuleAsReadOrUnread(module, true)
                                    }
                                }

                                override fun onFailure(message: String, t: Throwable) {
                                    mSwipeRefreshLayout.isRefreshing = false
                                }
                            })
                        }
                    }
                }
                courseSections = sectionList.filterNotNull()
                courseDataHandler.replaceCourseData(courseId, sectionList)
                setCourseContentsOnAdapter()
                mSwipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(message: String, t: Throwable) {
                if (t is IllegalStateException) {
                    //course unenrolled. delete course details, open enroll screen
                    courseDataHandler.deleteCourse(courseId)
                    Toast.makeText(activity, "you have un-enrolled from the course",
                            Toast.LENGTH_SHORT).show()
                    requireActivity().setResult(COURSE_DELETED)
                    requireActivity().finish()
                    return
                }
                if (courseSections.isEmpty()) {
                    empty.text = resources.getText(R.string.failed_course_content_refresh)
                    empty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                Toast.makeText(activity, "Unable to connect to server!", Toast.LENGTH_SHORT).show()
                mSwipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun setCourseContentsOnAdapter() {
        fileManager.reloadFileList()
        adapter.setCourseContents(courseContents)
    }

    private val courseContents: List<CourseContent>
        get() {
            val contents = ArrayList<CourseContent>()
            courseSections.stream().filter { courseSection: CourseSection ->
                !(courseSection.modules.isEmpty()
                        && courseSection.summary.isEmpty()
                        && courseSection.name.matches(Regex("Topic \\d")))
            }.forEach { courseSection: CourseSection ->
                contents.add(courseSection)
                contents.addAll(courseSection.modules)
            }
            return contents
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mark_all_as_read) {
            courseDataHandler.markAllAsRead(courseSections)
            courseSections = courseDataHandler.getCourseData(courseId)
            setCourseContentsOnAdapter()
            Toast.makeText(activity, "Marked all as read", Toast.LENGTH_SHORT).show()
            return true
        }
        if (item.itemId == R.id.action_open_in_browser) {
            Utils.openURLInBrowser(requireActivity(), Constants.getCourseURL(courseId))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.course_details_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroy() {
        super.onDestroy()
        fileManager.unregisterDownloadReceiver()
        realm.close()
    }

    companion object {
        const val COURSE_DELETED = 102
        private const val MODULE_ACTIVITY = 101
        private const val TOKEN_KEY = "token"
        private const val COURSE_ID_KEY = "id"

        @JvmStatic
        fun newInstance(token: String?, courseId: Int): CourseContentFragment {
            val courseSectionFragment = CourseContentFragment()
            val args = Bundle()
            args.putString(TOKEN_KEY, token)
            args.putInt(COURSE_ID_KEY, courseId)
            courseSectionFragment.arguments = args
            return courseSectionFragment
        }
    }
}