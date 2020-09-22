package crux.bphc.cms.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.R
import crux.bphc.cms.activities.CourseDetailActivity
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseDownloader
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.helper.CourseRequestHandler.CallBack
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.utils.UserUtils
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_my_courses.*
import kotlinx.android.synthetic.main.row_course.*
import kotlinx.android.synthetic.main.row_course.view.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class MyCoursesFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var courseDataHandler: CourseDataHandler

    private var coursesUpdated = 0
    private var courses: MutableList<Course> = ArrayList()
    private lateinit var mAdapter: Adapter
    private lateinit var moreOptionsViewModel: OptionsViewModel

    override fun onStart() {
        super.onStart()
        requireActivity().title = "My Courses"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        realm = Realm.getDefaultInstance()
        return inflater.inflate(R.layout.fragment_my_courses, container, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COURSE_SECTION_ACTIVITY) {
            courses = courseDataHandler.courseList
            mAdapter.filterCoursesByName(courses, searchCourseET.text.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseDataHandler = CourseDataHandler(requireContext(), realm)
        courses = courseDataHandler.courseList

        moreOptionsViewModel = ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)

        // Set up the adapter
        mAdapter = Adapter(requireActivity(), courses)
        mAdapter.courses = courses
        mAdapter.clickListener = ClickListener { `object`: Any, position: Int ->
            val course = `object` as Course
            val intent = Intent(activity, CourseDetailActivity::class.java)
            intent.putExtra("courseId", course.courseId)
            intent.putExtra("course_name", course.shortName)
            startActivityForResult(intent, COURSE_SECTION_ACTIVITY)
            return@ClickListener true
        }

        mAdapter.downloadClickListener = ClickListener { `object`: Any, position: Int ->
            val course = `object` as Course
            if (course.downloadStatus != -1) return@ClickListener false
            course.downloadStatus = 0
            mAdapter.notifyItemChanged(position)
            val courseDownloader = CourseDownloader(activity, courseDataHandler.getCourseName(course.id))
            courseDownloader.setDownloadCallback(object: CourseDownloader.DownloadCallback {
                override fun onCourseDataDownloaded() {
                    course.downloadedFiles = courseDownloader.getDownloadedContentCount(course.id)
                    course.totalFiles = courseDownloader.getTotalContentCount(course.id)
                    if (course.totalFiles == course.downloadedFiles) {
                        Toast.makeText(activity, "All files already downloaded", Toast.LENGTH_SHORT).show()
                        course.downloadStatus = -1
                    } else {
                        course.downloadStatus = 1
                    }
                    mAdapter.notifyItemChanged(position)
                }

                override fun onCourseContentDownloaded() {
                    course.downloadedFiles = course.downloadedFiles + 1
                    if (course.downloadedFiles == course.totalFiles) {
                        course.downloadStatus = -1
                        courseDownloader.unregisterReceiver()
                        //todo notification all files downloaded for this course
                    }
                    mAdapter.notifyItemChanged(position)
                }

                override fun onFailure() {
                    Toast.makeText(activity, "Check your internet connection", Toast.LENGTH_SHORT)
                            .show()
                    course.downloadStatus = -1
                    mAdapter.notifyItemChanged(position)
                    courseDownloader.unregisterReceiver()
                }
            })
            courseDownloader.downloadCourseData(course.courseId)
            return@ClickListener true
        }

        recyclerView.adapter = mAdapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        searchCourseET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val searchText = s.toString().toLowerCase(Locale.ROOT).trim { it <= ' ' }
                mAdapter.filterCoursesByName(courses, searchText)
                if (searchText.isNotEmpty()) {
                    searchIcon.setImageResource(R.drawable.ic_cancel_black_24dp)
                    searchIcon.setOnClickListener {
                        searchCourseET.setText("")
                        searchIcon.setImageResource(R.drawable.ic_search)
                        searchIcon.setOnClickListener(null)
                        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                                as? InputMethodManager
                        val currentFocus = requireActivity().currentFocus
                        if (currentFocus != null) {
                            inputManager?.hideSoftInputFromWindow(currentFocus.windowToken,
                                    InputMethodManager.HIDE_NOT_ALWAYS)
                        }
                    }
                } else {
                    searchIcon.setImageResource(R.drawable.ic_search)
                    searchIcon.setOnClickListener(null)
                }
            }
        })

      swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            makeRequest()
        }

        checkEmpty()
        if (courses.isEmpty()) {
            swipeRefreshLayout.isRefreshing = true
            makeRequest()
        }
    }

    private fun checkEmpty() {
        if (courses.isEmpty()) {
            empty!!.visibility = View.VISIBLE
        } else {
            empty!!.visibility = View.GONE
        }
    }

    private fun makeRequest() {
        val courseRequestHandler = CourseRequestHandler(activity)
        courseRequestHandler.getCourseList(object : CallBack<List<Course>> {
            override fun onResponse(courseList: List<Course>) {
                courses.clear()
                courses.addAll(courseList)
                courseDataHandler.replaceCourses(courseList)
                checkEmpty()
                updateCourseContent(courses)
                mAdapter.filterCoursesByName(courseList, searchCourseET.text.toString())
            }

            override fun onFailure(message: String, t: Throwable) {
                swipeRefreshLayout.isRefreshing = false
                if (message.contains("Invalid token")) {
                    Toast.makeText(activity, "Invalid token! Probably your token was reset.",
                            Toast.LENGTH_SHORT).show()
                    UserUtils.logout(requireActivity())
                    UserUtils.clearBackStackAndLaunchTokenActivity(requireActivity())
                    return
                }
                Toast.makeText(activity, "Unable to connect to server!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCourseContent(courses: List<Course>?) {
        courseDataHandler.replaceCourses(courses!!)
        val courseRequestHandler = CourseRequestHandler(activity)
        coursesUpdated = 0
        if (courses.isEmpty()) swipeRefreshLayout.isRefreshing = false
        for (course in courses) {
            courseRequestHandler.getCourseData(course.courseId,
                    object : CallBack<List<CourseSection?>?> {
                        override fun onResponse(sections: List<CourseSection?>?) {
                            if (sections == null) return
                            for (courseSection in sections) {
                                val modules = courseSection?.modules ?: continue
                                for (module in modules) {
                                    if (module.modType == Module.Type.FORUM) {
                                        courseRequestHandler.getForumDiscussions(module.instance, object : CallBack<List<Discussion?>?> {
                                            override fun onResponse(responseObject: List<Discussion?>?) {
                                                if (responseObject != null) {
                                                    for (d in responseObject) {
                                                        d?.setForumId(module.instance)
                                                    }
                                                }
                                                val newDiscussions = courseDataHandler.setForumDiscussions(module.instance, responseObject)
                                                if (newDiscussions.size > 0) courseDataHandler.markModuleAsReadOrUnread(module, true)
                                            }

                                            override fun onFailure(message: String, t: Throwable) {
                                                swipeRefreshLayout.isRefreshing = false
                                            }
                                        })
                                    }
                                }
                            }
                            val newPartsInSections = sections.let {
                                courseDataHandler
                                        .isolateNewCourseData(course.courseId, it)
                            }
                            sections.let { courseDataHandler.replaceCourseData(course.courseId, it) }
                            if (newPartsInSections?.isNotEmpty() == true) {
                                coursesUpdated++
                            }
                            //Refresh the recycler view for the last course
                            if (course.courseId == courses[courses.size - 1].courseId) {
                                swipeRefreshLayout.isRefreshing = false
                                mAdapter.notifyDataSetChanged()
                                val message: String = if (coursesUpdated == 0) {
                                    getString(R.string.upToDate)
                                } else {
                                    resources.getQuantityString(R.plurals.noOfCoursesUpdated, coursesUpdated, coursesUpdated)
                                }
                                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(message: String, t: Throwable) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                    })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        realm.close()
    }

    private inner class Adapter constructor(
            val context: Context,
            courseList: List<Course>
    ) : RecyclerView.Adapter<Adapter.MyViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        var clickListener: ClickListener? = null
        var downloadClickListener: ClickListener? = null
        var courses: List<Course> = ArrayList()
            set(value) {
                for (i in field.indices) {
                    field[i].downloadStatus = -1
                }
                field = sortCourses(value)
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(inflater.inflate(R.layout.row_course, parent, false))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(courses[position])
        }

        override fun getItemCount(): Int {
            return courses.size
        }

        private fun sortCourses(courseList: List<Course>) : List<Course> {
            return courseList.sortedWith { o1, o2 ->
                if (o1.isFavorite == o2.isFavorite) {
                    o1.shortName.compareTo(o2.shortName)
                } else {
                    if (!o1.isFavorite && o2.isFavorite) 1 else -1
                }
            }
        }

        fun filterCoursesByName(courseList: List<Course>, courseName: String) {
            var filteredCourses: MutableList<Course> = ArrayList()
            if (courseName.isNotEmpty()) {
                for (course in courseList) {
                    if (course.fullName.toLowerCase(Locale.ROOT).contains(courseName)) {
                        filteredCourses.add(course)
                    }
                }
            } else {
                filteredCourses = courseList as MutableList<Course>
            }
            courses = filteredCourses
        }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(course: Course) {
                val name = course.courseName[1].toString() + " " + course.courseName[2]
                val count = courseDataHandler.getUnreadCount(course.id)
                itemView.course_number.text = course.courseName[0]
                itemView.course_name.text = name
                itemView.unread_count.text = DecimalFormat.getIntegerInstance().format(count.toLong())
                itemView.unread_count.visibility = if (count == 0) View.INVISIBLE else View.VISIBLE
                itemView.favorite.visibility = if (course.isFavorite) View.VISIBLE else View.INVISIBLE
            }

            fun confirmDownloadCourse() {
                MaterialAlertDialogBuilder(context)
                        .setTitle("Confirm Download")
                        .setMessage("Are you sure you want to all the contents of this course?")
                        .setPositiveButton("Yes") { _: DialogInterface?, i: Int ->
                            if (downloadClickListener != null) {
                                val pos = layoutPosition
                                if (!downloadClickListener!!.onClick(this@MyCoursesFragment.courses[pos], pos)) {
                                    Toast.makeText(activity, "Download already in progress",
                                            Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
            }

            fun markAllAsRead(position: Int) {
                val courseId = this@MyCoursesFragment.courses[position].courseId
                val courseSections: List<CourseSection>

                courseSections = courseDataHandler.getCourseData(courseId)
                courseDataHandler.markAllAsRead(courseSections)

                val count = courseDataHandler.getUnreadCount(this@MyCoursesFragment.courses[position].id)
                unread_count.text = DecimalFormat.getIntegerInstance().format(count.toLong())
                unread_count.visibility = if (count == 0) View.INVISIBLE else View.VISIBLE
                Toast.makeText(activity, "Marked all as read", Toast.LENGTH_SHORT).show()
            }

            fun setFavoriteStatus(position: Int, isFavourite: Boolean) {
                val course = courses[position]
                courseDataHandler.setFavoriteStatus(course.courseId, isFavourite)
                course.isFavorite = isFavourite
                sortCourses(courses)
                notifyDataSetChanged()
                val toast = if (isFavourite) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites)
                Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show()
            }

            init {
                itemView.click_wrapper.setOnClickListener {
                    if (clickListener != null) {
                        val pos = layoutPosition
                        clickListener!!.onClick(courses[pos], pos)
                    }
                }

                itemView.more.setOnClickListener {
                    val observer: Observer<MoreOptionsFragment.Option> // to handle the selection
                    //Set up our options and their handlers
                    val isFavorite = courses[layoutPosition].isFavorite
                    val favoriteOption = if (isFavorite) "Remove from favorites" else "Add to favorites"
                    val options = ArrayList(listOf(
                            MoreOptionsFragment.Option(0, "Download course", R.drawable.download),
                            MoreOptionsFragment.Option(1, "Mark all as read", R.drawable.eye),
                            MoreOptionsFragment.Option(2, favoriteOption, R.drawable.star)
                    ))
                    observer = Observer { option: MoreOptionsFragment.Option? ->
                        when (option?.getId()) {
                            0 -> confirmDownloadCourse()
                            1 -> markAllAsRead(layoutPosition)
                            2 -> setFavoriteStatus(layoutPosition, !isFavorite)
                        }
                        moreOptionsViewModel.selection.removeObservers((context as AppCompatActivity))
                        moreOptionsViewModel.clearSelection()
                    }
                    val courseName = this@MyCoursesFragment.courses[layoutPosition].shortName
                    val moreOptionsFragment = MoreOptionsFragment.newInstance(courseName, options)
                    moreOptionsFragment.show((context as AppCompatActivity).supportFragmentManager, moreOptionsFragment.tag)
                    moreOptionsViewModel.selection.observe(context, observer)
                }
            }
        }

        init {
            this.courses = courseList
        }
    }

    companion object {
        private const val COURSE_SECTION_ACTIVITY = 105
        @JvmStatic
        fun newInstance(): MyCoursesFragment {
            return MyCoursesFragment()
        }
    }
}