package crux.bphc.cms.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.R
import crux.bphc.cms.activities.CourseDetailActivity
import crux.bphc.cms.databinding.FragmentMyCoursesBinding
import crux.bphc.cms.databinding.RowCourseBinding
import crux.bphc.cms.exceptions.InvalidTokenException
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseDownloader
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.utils.UserUtils
import io.realm.Realm
import kotlinx.coroutines.*
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class MyCoursesFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var courseDataHandler: CourseDataHandler
    private lateinit var binding: FragmentMyCoursesBinding

    private var coursesUpdated = 0
    private var courses: MutableList<Course> = ArrayList()
    private lateinit var mAdapter: Adapter

    // Activity result launchers
    private val courseDetailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mAdapter.filterCoursesByName(courses, binding.searchCourseET.text.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMyCoursesBinding.inflate(layoutInflater)

        setHasOptionsMenu(true)
    }

    private var badge: BadgeDrawable? = null

    override fun onStart() {
        super.onStart()
        requireActivity().title = "My Courses"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        realm = Realm.getDefaultInstance()

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.my_courses_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mark_all_as_read -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val realm = Realm.getDefaultInstance()
                    val courseDataHandler = CourseDataHandler(realm)
                    courseDataHandler.markAllAsRead()
                    realm.close()

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(requireActivity(), "Marked all as read", Toast.LENGTH_SHORT).show()
                        mAdapter.courses = this@MyCoursesFragment.courseDataHandler.courseList
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseDataHandler = CourseDataHandler(realm)
        courses = courseDataHandler.courseList

        // Set up the adapter
        mAdapter = Adapter(requireActivity(), courses)
        mAdapter.courses = courses
        mAdapter.clickListener = ClickListener { `object`: Any, position: Int ->
            val course = `object` as Course
            val intent = Intent(activity, CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.INTENT_COURSE_ID_KEY, course.id)
            courseDetailActivityLauncher.launch(intent)
            return@ClickListener true
        }

        mAdapter.downloadClickListener = ClickListener { `object`: Any, position: Int ->
            val course = `object` as Course
            if (course.downloadStatus != -1) return@ClickListener false
            course.downloadStatus = 0
            mAdapter.notifyItemChanged(position)
            val courseDownloader = CourseDownloader(activity, courseDataHandler.getCourseName(course.id))
            courseDownloader.setDownloadCallback(object : CourseDownloader.DownloadCallback {
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
            courseDownloader.downloadCourseData(course.id)
            return@ClickListener true
        }

        binding.recyclerView.adapter = mAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.searchCourseET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val searchText = s.toString().lowercase(Locale.ROOT).trim { it <= ' ' }
                mAdapter.filterCoursesByName(courses, searchText)
                if (searchText.isNotEmpty()) {
                    binding.searchIcon.setImageResource(R.drawable.ic_cancel_black_24dp)
                    binding.searchIcon.setOnClickListener {
                        binding.searchCourseET.setText("")
                        binding.searchIcon.setImageResource(R.drawable.ic_search)
                        binding.searchIcon.setOnClickListener(null)
                        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                                as? InputMethodManager
                        val currentFocus = requireActivity().currentFocus
                        if (currentFocus != null) {
                            inputManager?.hideSoftInputFromWindow(currentFocus.windowToken,
                                InputMethodManager.HIDE_NOT_ALWAYS)
                        }
                    }
                } else {
                    binding.searchIcon.setImageResource(R.drawable.ic_search)
                    binding.searchIcon.setOnClickListener(null)
                }
            }
        })

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            refreshCourses()
        }

        checkEmpty()
    }

    private fun checkEmpty() {
        if (courses.isEmpty()) {
            binding.empty?.visibility = View.VISIBLE
        } else {
            binding.empty?.visibility = View.GONE
        }
    }

    private fun refreshCourses() {
        lifecycleScope.launch {
            launch(Dispatchers.IO) { // lifecycle scope allows cancellation of this scope
                val courseRequestHandler = CourseRequestHandler()
                try {
                    val courseList = courseRequestHandler.fetchCourseListSync()
                    courses.clear()
                    courses.addAll(courseList)
                    Log.i(TAG, "${courseList.size} courses")
                    val realm = Realm.getDefaultInstance() // tie a realm instance to this thread
                    val courseDataHandler = CourseDataHandler(realm)
                    courseDataHandler.replaceCourses(courseList)
                    realm.close()
                    withContext(Dispatchers.Main) {
                        checkEmpty()
                    }
                    updateCourseContent()
                } catch (e: Exception) {
                    Log.e(TAG, "", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        if (e is InvalidTokenException) {
                            UserUtils.logout()
                            UserUtils.clearBackStackAndLaunchTokenActivity(requireActivity())
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout?.isRefreshing = false
                    }
                }
            }
        }
    }

    private suspend fun updateCourseContent() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Fetching course contents")
            val courseRequestHandler = CourseRequestHandler()
            val promises = courses.map map@{
                async innerAsync@{
                    val sections: MutableList<CourseSection>
                    try {
                        sections = courseRequestHandler.getCourseDataSync(it.id)
                    } catch (e: IOException) {
                        Log.e(TAG, "IOException when syncing course: ${it.id}}", e)
                        return@innerAsync false
                    }

                    val realm = Realm.getDefaultInstance() // tie a realm instance to this thread
                    val courseDataHandler = CourseDataHandler(realm)
                    val newPartsInSections = courseDataHandler.isolateNewCourseData(it.id, sections)
                    courseDataHandler.replaceCourseData(it.id, sections)

                    realm.close() // let's not forget to do this
                    if (newPartsInSections.isNotEmpty()) {
                        return@innerAsync true
                    }
                    return@innerAsync false
                }
            }

            coursesUpdated = promises.awaitAll().fold(0) { i, x -> if (x) i + 1 else i }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout?.isRefreshing = false
                mAdapter.filterCoursesByName(courses, binding.searchCourseET.text.toString())
                val message: String = if (coursesUpdated == 0) {
                    getString(R.string.upToDate)
                } else {
                    resources.getQuantityString(R.plurals.noOfCoursesUpdated, coursesUpdated,
                        coursesUpdated)
                }
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
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
            val itemBinding = RowCourseBinding.inflate(inflater, parent, false)
            return MyViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(courses[position])
        }

        override fun getItemCount(): Int {
            return courses.size
        }

        private fun sortCourses(courseList: List<Course>): List<Course> {
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
                    if (course.shortName.lowercase(Locale.ROOT).contains(courseName)) {
                        filteredCourses.add(course)
                    }
                }
            } else {
                filteredCourses = courseList as MutableList<Course>
            }
            courses = filteredCourses
        }

        inner class MyViewHolder(val itemBinding: RowCourseBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(course: Course) {
                val name = course.courseName[1] + " " + course.courseName[2]
                val count = courseDataHandler.getUnreadCount(course.id)
                itemBinding.courseNumber.text = course.courseName[0]
                itemBinding.courseName.text = name
                itemBinding.unreadCount.text = DecimalFormat.getIntegerInstance().format(count.toLong())
                itemBinding.unreadCount.isVisible = count != 0
                itemBinding.markAsReadButton.isVisible = count != 0
                itemBinding.favoriteButton.setImageResource(if (course.isFavorite) R.drawable.ic_fav_filled else R.drawable.ic_fav_outlined)
            }

            fun confirmDownloadCourse() {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Confirm Download")
                    .setMessage("Are you sure you want to all the contents of this course?")
                    .setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                        if (downloadClickListener != null) {
                            val pos = layoutPosition
                            if (!downloadClickListener!!.onClick(courses[pos], pos)) {
                                Toast.makeText(activity, "Download already in progress",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            fun markCourseAsRead() {
                val courseId = courses[layoutPosition].id
                courseDataHandler.markCourseAsRead(courseId)
                notifyItemChanged(layoutPosition)

                Toast.makeText(activity, "Marked all as read", Toast.LENGTH_SHORT).show()
            }

            fun setFavoriteStatus(position: Int, isFavourite: Boolean) {
                val course = courses[position]
                courseDataHandler.setFavoriteStatus(course.id, isFavourite)
                course.isFavorite = isFavourite
                courses = sortCourses(courses)
                notifyDataSetChanged()
                val toast = if (isFavourite) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites)
                Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show()
            }

            init {
                itemBinding.courseCard.setOnClickListener {
                    if (clickListener != null) {
                        val pos = layoutPosition
                        clickListener!!.onClick(courses[pos], pos)
                    }
                }

                itemBinding.markAsReadButton.setOnClickListener { markCourseAsRead() }
                itemBinding.favoriteButton.setOnClickListener { setFavoriteStatus(layoutPosition, !courses[layoutPosition].isFavorite) }
                itemBinding.downloadImage.setOnClickListener { confirmDownloadCourse() }
            }
        }

        init {
            this.courses = courseList
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MyCoursesFragment()

        @JvmStatic
        val TAG = "MyCoursesFragment"
    }
}
