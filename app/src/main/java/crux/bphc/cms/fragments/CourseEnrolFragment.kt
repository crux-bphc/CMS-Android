package crux.bphc.cms.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.fragments.CourseContentFragment.Companion.newInstance
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.enrol.SearchedCourseDetail
import crux.bphc.cms.models.enrol.SelfEnrol
import crux.bphc.cms.network.MoodleServices
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CourseEnrolFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var course: SearchedCourseDetail

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        course = requireArguments().getParcelable(COURSE_KEY) ?: SearchedCourseDetail()
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        realm = Realm.getDefaultInstance()
        return inflater.inflate(R.layout.fragment_course_enrol, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mCourseDisplayName = view.findViewById<TextView>(R.id.course_enrol_course_display_name)
        val mCourseCategory = view.findViewById<TextView>(R.id.course_enrol_course_category)
        val mTeachers = view.findViewById<LinearLayout>(R.id.course_enrol_teachers)
        val teachers = course.contacts
        val noTeacherInfo = view.findViewById<TextView>(R.id.course_enrol_teacher_no_info)
        val mEnrolButton = view.findViewById<Button>(R.id.course_enrol_enrol_button)
        val courses: MutableList<Course> = ArrayList()

        lifecycleScope.launch {
            launch(Dispatchers.IO) {
                val courseRequestHandler = CourseRequestHandler()
                try {
                    val courseList = courseRequestHandler.fetchCourseListSync()
                    courses.clear()
                    courses.addAll(courseList)
                    val realm = Realm.getDefaultInstance() // tie a realm instance to this thread
                    val courseDataHandler = CourseDataHandler(realm)
                    courseDataHandler.replaceCourses(courseList)
                    realm.close()

                } catch (e: Exception) {
                    Log.e("CourseEnrolFragment", "", e)
                }
            }
        }

        mCourseDisplayName.text = course.displayName
        mCourseCategory.text = course.categoryName
        mEnrolButton.setOnClickListener { createEnrollmentConfirmationDialog().show() }

        if (teachers.isEmpty()) {
            noTeacherInfo.visibility = View.VISIBLE
            return
        }
        noTeacherInfo.visibility = View.GONE
        val layoutParams = noTeacherInfo.layoutParams as LinearLayout.LayoutParams
        layoutParams.setMargins(0, 8, 0, 0)
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(
            android.R.attr.textColorPrimary,
            typedValue,
            true
        )
        val arr = requireActivity().obtainStyledAttributes(
            typedValue.data, intArrayOf(
                android.R.attr.textColorSecondary
            )
        )
        val textColor = arr.getColor(0, -1)
        arr.recycle()
        for ((_, fullName) in teachers) {
            val teacherName = TextView(activity)
            teacherName.layoutParams = layoutParams
            teacherName.text = fullName
            teacherName.setTextColor(textColor)
            teacherName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            teacherName.setPaddingRelative(8, 0, 8, 0)
            mTeachers.addView(teacherName)
        }
    }

    private fun createEnrollmentConfirmationDialog(): AlertDialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.course_enrol_confirmation_msg)
            .setPositiveButton("OK") { _, _ -> enrolInCourse() }
            .setNegativeButton("Cancel") { _, _ -> }
        return builder.create()
    }

    private fun enrolInCourse() {
        val progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Attempting to enroll")
        progressDialog.setCancelable(false)
        progressDialog.show()
        val retrofit = Retrofit.Builder()
            .baseUrl(Urls.MOODLE_URL.toString())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val moodleServices = retrofit.create(MoodleServices::class.java)
        val call = moodleServices.selfEnrolUserInCourse(UserAccount.token, course.id)
        println(call.request().url)
        call.enqueue(object : Callback<SelfEnrol> {
            override fun onResponse(call: Call<SelfEnrol>, response: Response<SelfEnrol>) {
                progressDialog.dismiss()
                var body: SelfEnrol
                if (response.body().also { body = it!! } != null && body.status) {
                    Toast.makeText(
                        activity,
                        "Successfully enrolled in " + course.displayName,
                        Toast.LENGTH_SHORT
                    ).show()
                    val fragmentManager = requireActivity().supportFragmentManager
                    val courseSectionFragment = newInstance(UserAccount.token, course.id, "")
                    val courseSet = Course(course)
                    realm.executeTransaction { r: Realm -> r.copyToRealmOrUpdate(courseSet) }
                    fragmentManager
                        .beginTransaction()
                        .replace(R.id.course_section_enrol_container, courseSectionFragment)
                        .commit()
                } else {
                    Toast.makeText(activity, R.string.course_enrol_course_already_enrolled_msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SelfEnrol>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(activity, "Unable to connect to server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    companion object {
        private const val COURSE_KEY = "course"

        fun newInstance(course: SearchedCourseDetail): CourseEnrolFragment {
            val fragment = CourseEnrolFragment()
            val args = Bundle()
            args.putParcelable(COURSE_KEY, course)
            fragment.arguments = args
            return fragment
        }
    }
}