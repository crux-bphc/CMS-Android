package crux.bphc.cms.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import crux.bphc.cms.R
import crux.bphc.cms.adapters.CourseContentAdapter
import crux.bphc.cms.app.Urls
import crux.bphc.cms.core.FileManager
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseManager
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import crux.bphc.cms.activities.CourseDetailActivity
import crux.bphc.cms.activities.MainActivity


/**
 * @author Siddhant Kumar Patel, Abhijeet Viswa
 */
class CourseContentFragment : Fragment() {
    private lateinit var fileManager: FileManager
    private lateinit var realm: Realm
    private lateinit var courseDataHandler: CourseDataHandler
    private lateinit var courseRequestHandler: CourseRequestHandler
    private lateinit var courseManager: CourseManager

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

    private val courseContents: List<CourseContent>
        get() {
            val contents = ArrayList<CourseContent>()
            courseSections.stream().filter { courseSection: CourseSection ->
                !(courseSection.modules.isEmpty()
                        && courseSection.summary.isEmpty()
                        && courseSection.name.matches(Regex("^Topic \\d*$")))
            }.forEach { courseSection: CourseSection ->
                contents.add(courseSection)
                contents.addAll(courseSection.modules)
            }
            return contents
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Show error if invalid courseId
        courseId = arguments?.getInt(COURSE_ID_KEY) ?: -1

        // Initialize realm here instead of onCreateView so that other classes can be initialized
        realm = Realm.getDefaultInstance()

        courseDataHandler = CourseDataHandler(realm)
        courseName = courseDataHandler.getCourseName(courseId)
        courseSections = courseDataHandler.getCourseData(courseId)

        courseRequestHandler = CourseRequestHandler()

        fileManager = FileManager(requireActivity(), courseName) { setCourseContentsOnAdapter() }
        fileManager.registerDownloadReceiver()

        courseManager = CourseManager(courseId, courseRequestHandler)

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = courseDataHandler.getCourseNameForActionBarTitle(courseId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsViewModel =
            ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)

        empty = view.findViewById(R.id.empty) as TextView
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recycler_view)

        courseSections = courseDataHandler.getCourseData(courseId)
        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.isRefreshing = true
            refreshContent()
        }

        adapter = CourseContentAdapter(
            requireActivity(), courseContents, fileManager,
            moduleClickWrapperClickListener, moduleMoreOptionsClickListener
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setItemViewCacheSize(10)

        val contextUrl = requireArguments().getString(CONTEXT_URL_KEY) ?: ""
        if (contextUrl.isNotEmpty()) {
            mSwipeRefreshLayout.isRefreshing = true
            refreshContent(contextUrl) // If there is a url, there may be updates
        }

        mSwipeRefreshLayout.setOnRefreshListener {
            mSwipeRefreshLayout.isRefreshing = true
            refreshContent()
        }
        empty.setOnClickListener {
            mSwipeRefreshLayout.isRefreshing = true
            refreshContent()
        }
        showSectionsOrEmpty()
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
                options.addAll(
                    listOf(
                        MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                        MoreOptionsFragment.Option(
                            1,
                            "Re-Download",
                            R.drawable.outline_file_download_24
                        ),
                        MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                        MoreOptionsFragment.Option(3, "Mark as Unread", R.drawable.eye_off)
                    )
                )
                if (module.modType === Module.Type.RESOURCE) {
                    options.add(MoreOptionsFragment.Option(4, "Properties", R.drawable.ic_info))
                }
                Observer label@{ option: MoreOptionsFragment.Option? ->
                    if (option == null) return@label
                    when (option.id) {
                        0 -> fileManager.openModuleContent(content!!)
                        1 -> {
                            if (!module.isDownloadable) {
                                return@label
                            }
                            Toast.makeText(
                                activity, "Downloading file - " + content!!.fileName,
                                Toast.LENGTH_SHORT
                            ).show()
                            fileManager.downloadModuleContent(content, module)
                        }
                        2 -> fileManager.shareModuleContent(content!!)
                        3 -> {
                            courseDataHandler.markModuleAsUnread(module);
                            adapter.notifyItemChanged(position)
                        }
                        4 -> PropertiesAlertDialog(requireActivity(), content!!)
                    }
                    moreOptionsViewModel.selection.removeObservers(requireActivity())
                    moreOptionsViewModel.clearSelection()
                }
            } else {
                options.addAll(
                    listOf(
                        MoreOptionsFragment.Option(
                            0,
                            "Download",
                            R.drawable.outline_file_download_24
                        ),
                        MoreOptionsFragment.Option(1, "Share", R.drawable.ic_share),
                        MoreOptionsFragment.Option(2, "Mark as Unread", R.drawable.eye_off)
                    )
                )
                if (module.modType === Module.Type.RESOURCE) {
                    options.add(
                        MoreOptionsFragment.Option(
                            3, "Properties", R.drawable.ic_info
                        )
                    )
                }
                Observer label@{ option: MoreOptionsFragment.Option? ->
                    if (option == null) return@label
                    val activity = activity
                    when (option.id) {
                        0 -> if (content != null) {
                            fileManager.downloadModuleContent(content, module)
                        }
                        1 -> shareModuleLinks(module)
                        2 -> {
                            courseDataHandler.markModuleAsUnread(module);
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
                moreOptionsFragment.show(
                    requireActivity().supportFragmentManager,
                    moreOptionsFragment.tag
                )
                moreOptionsViewModel.selection.observe(activity, observer)
                courseDataHandler.markModuleAsRead(module);
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
                    val fragment = if (module.modType === Module.Type.FORUM)
                        ForumFragment.newInstance(courseId, module.instance, courseName)
                    else FolderModuleFragment.newInstance(module.instance, courseName)
                    activity.supportFragmentManager
                        .beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.course_section_enrol_container, fragment, "Announcements")
                        .commit()
                }
                Module.Type.LABEL -> {
                    val desc = module.description
                    if (activity != null && desc.isNotEmpty()) {
                        val alertDialog: AlertDialog.Builder = if (UserAccount.isDarkModeEnabled) {
                            AlertDialog.Builder(activity, R.style.Theme_AppCompat_Dialog_Alert)
                        } else {
                            AlertDialog.Builder(
                                activity,
                                R.style.Theme_AppCompat_Light_Dialog_Alert
                            )
                        }
                        val htmlDescription = HtmlCompat.fromHtml(
                            module.description,
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                        val descriptionWithOutExtraSpace =
                            htmlDescription.toString().trim { it <= ' ' }
                        alertDialog.setMessage(
                            htmlDescription.subSequence(
                                0,
                                descriptionWithOutExtraSpace.length
                            )
                        )
                        alertDialog.setNegativeButton("Close", null)
                        alertDialog.show()
                    }
                }
                Module.Type.RESOURCE -> if (content != null) {
                    if (fileManager.isModuleContentDownloaded(content)) {
                        fileManager.openModuleContent(content)
                    } else {
                        Toast.makeText(
                            getActivity(), "Downloading file - " + content.fileName,
                            Toast.LENGTH_SHORT
                        ).show()
                        fileManager.downloadModuleContent(content, module)
                    }
                }
                else -> if (activity != null) {
                    Utils.openURLInBrowser(activity, module.url)
                }
            }
            courseDataHandler.markModuleAsRead(module);
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
        if (context != null) requireContext().startActivity(
            Intent.createChooser(
                sharingIntent,
                null
            )
        )
    }


    private fun findAndScrollToPosition(urlStr: String) {
        val url = Uri.parse(urlStr)
        var position = 0

        if (Urls.isCourseModuleUrl(url)) {
            val modId = Urls.getModIdFromUrl(url)
            courseDataHandler.getModuleByModId(modId) ?: return
            position = adapter.getPositionFromModId(modId)
        } else if (Urls.isCourseSectionUrl(url)) {
            val sectionNum = Urls.getSectionNumFromUrl(url)
            courseDataHandler.getSectionBySectionNum(courseId, sectionNum) ?: return
            position = adapter.getPositionFromSectionNum(sectionNum)
        }

        recyclerView.smoothScrollToPosition(position)
    }


    private fun showSectionsOrEmpty() {
        if (courseSections.stream()
                .anyMatch { section: CourseSection -> !section.modules.isEmpty() }
        ) {
            empty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun refreshContent(contextUrl: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            var sections = mutableListOf<CourseSection>()
            try {
                sections = courseRequestHandler.getCourseDataSync(courseId)
            } catch (e: IOException) {
                Log.e(TAG, "IOException when syncing course: ${courseId}}", e)
                if (courseSections.isEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        empty.text = resources.getText(R.string.failed_course_content_refresh)
                        empty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE

                        Toast.makeText(activity, "Unable to connect to server!", Toast.LENGTH_SHORT)
                            .show()
                        mSwipeRefreshLayout.isRefreshing = false
                    }
                }
            }

            val realm = Realm.getDefaultInstance() // tie a realm instance to this thread
            val courseDataHandler = CourseDataHandler(realm)

            courseDataHandler.isolateNewCourseData(courseId, sections) // This marks as unread

            for (courseSection in sections) {
                val modules = courseSection.modules
                for (module in modules) {
                    if (module.modType == Module.Type.FORUM) {
                        val discussions = courseRequestHandler
                            .getForumDicussionsSync(module.instance)
                        for (d in discussions) {
                            d.forumId = module.instance
                        }

                        val newDiscussions = courseDataHandler
                            .setForumDiscussions(module.instance, discussions)
                        if (newDiscussions.size > 0) {
                            courseDataHandler.markModuleAsUnread(module);
                        }
                    }
                }
            }
            courseDataHandler.replaceCourseData(courseId, sections)
            courseSections = sections
            CoroutineScope(Dispatchers.Main).launch {
                setCourseContentsOnAdapter()
                findAndScrollToPosition(contextUrl)
                mSwipeRefreshLayout.isRefreshing = false
                empty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    @MainThread
    private fun setCourseContentsOnAdapter() {
        fileManager.reloadFileList()
        adapter.setCourseContents(courseContents)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.mark_all_as_read) {
            courseDataHandler.markCourseAsRead(courseId)
            courseSections = courseDataHandler.getCourseData(courseId)
            setCourseContentsOnAdapter()
            Toast.makeText(activity, "Marked all as read", Toast.LENGTH_SHORT).show()
            return true
        } else if (item.itemId == R.id.action_open_in_browser) {
            Utils.openURLInBrowser(requireActivity(), Urls.getCourseUrl(courseId).toString())
            return true;
        } else {

            viewLifecycleOwner.lifecycleScope.launch {
                val retResult = courseManager.unenrolCourse(requireContext())
                withContext(Dispatchers.Main) {
                    if (retResult) {
                        val intent = Intent(requireActivity(), MainActivity::class.java)

                        intent.putExtra(INTENT_UNENROL_RESULT,true)
                              .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                        startActivity(intent)
                    }
                }
            }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.course_details_menu, menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        fileManager.unregisterDownloadReceiver()
        realm.close()
    }

    companion object {
        private const val TAG = "CourseContentFragment"

        private const val TOKEN_KEY = "token"
        private const val COURSE_ID_KEY = "id"
        private const val CONTEXT_URL_KEY = "contextUrl"
        const val INTENT_UNENROL_RESULT = "unenrolResult"

        @JvmStatic
        fun newInstance(token: String, courseId: Int, contextUrl: String): CourseContentFragment {
            val args = Bundle()
            args.putString(TOKEN_KEY, token)
            args.putInt(COURSE_ID_KEY, courseId)
            args.putString(CONTEXT_URL_KEY, contextUrl)

            val courseSectionFragment = CourseContentFragment()
            courseSectionFragment.arguments = args
            return courseSectionFragment
        }
    }
}