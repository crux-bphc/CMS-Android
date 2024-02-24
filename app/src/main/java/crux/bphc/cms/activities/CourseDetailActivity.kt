package crux.bphc.cms.activities

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.databinding.ActivityCourseDetailBinding
import crux.bphc.cms.fragments.CourseContentFragment
import crux.bphc.cms.fragments.CourseEnrolFragment
import crux.bphc.cms.fragments.DiscussionFragment
import crux.bphc.cms.fragments.ForumFragment
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.enrol.SearchedCourseDetail
import crux.bphc.cms.models.forum.NewPostCustomData
import io.realm.Realm

class CourseDetailActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var course: Course
    private lateinit var binding: ActivityCourseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UserAccount.isDarkModeEnabled) {
            setTheme(R.style.AppTheme_Dark)
        }
        binding = ActivityCourseDetailBinding.inflate(layoutInflater)

        realm = Realm.getDefaultInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        resolveIntent()

        setContentView(binding.root)
    }

    private fun resolveIntent() {
        val contextUrl = intent.getStringExtra(INTENT_CONTEXT_URL_KEY) ?: ""
        var courseId = intent.getIntExtra(INTENT_COURSE_ID_KEY, -1)
        val customDataStr = intent.getStringExtra(INTENT_CUSTOM_DATA_KEY) ?: ""
        val enrolCourse = intent.getParcelableExtra(INTENT_ENROL_COURSE_KEY) as SearchedCourseDetail?

        if (courseId == -1 && enrolCourse == null) {
            Toast.makeText(this, getString(R.string.invalid_course_id), Toast.LENGTH_SHORT).show();
            finish()
            return
        }

        if (courseId == -1) {
            courseId = enrolCourse!!.id
        }

        var localCourse = realm
            .where(Course::class.java)
            .equalTo("id", courseId)
            .findFirst()

        if (enrolCourse != null && localCourse == null) {
            // Not available locally, meaning most likely not enroled
            setCourseEnrol(enrolCourse)
            title = enrolCourse.shortName
            return
        }

        this.course = localCourse ?: Course(courseId)
        title = course.shortName

        val url = Uri.parse(contextUrl) ?: Uri.EMPTY
        if (Urls.isCourseSectionUrl(url) || Urls.isCourseModuleUrl(url)) {
            setCourseContentFragment(contextUrl)
        } else if (Urls.isForumDiscussionUrl(url)){
            // We parse the json here
            val customData = Gson().fromJson(customDataStr, NewPostCustomData::class.java)
            customData.apply {
                if (forumId != -1 && discussionId != -1) {
                    setDiscussionFragment(forumId, discussionId)
                }
            }
        } else {
            setCourseContentFragment("")
        }
    }

    private fun setCourseEnrol(enrolCourse: SearchedCourseDetail) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val mCourseEnrolFragment = CourseEnrolFragment.newInstance(enrolCourse)
        fragmentTransaction.replace(
            R.id.course_section_enrol_container,
            mCourseEnrolFragment,
            COURSE_ENROL_FRAG_TRANSACTION_KEY)
        fragmentTransaction.commit()
    }


    private fun setCourseContentFragment(contextUrl: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val courseSectionFragment = CourseContentFragment.newInstance(
            UserAccount.token,
            course.id,
            contextUrl,
        )
        fragmentTransaction.replace(
            R.id.course_section_enrol_container,
            courseSectionFragment,
            "course_section_frag"
        ).commit()
    }

    private fun setForumFragment(forumId: Int) {
        // We first add and commit courseSection
        setCourseContentFragment("")
        supportFragmentManager.executePendingTransactions()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val forumFragment: Fragment = ForumFragment.newInstance(course.id, forumId, course.shortName)
        fragmentTransaction.addToBackStack(null)
            .replace(R.id.course_section_enrol_container, forumFragment, "Announcements")
        fragmentTransaction.commit()
    }

    private fun setDiscussionFragment(
        forumId: Int = -1,
        discussionId: Int = -1,
    ) {
        setForumFragment(forumId)
        supportFragmentManager.executePendingTransactions()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val discussionFragment: Fragment = DiscussionFragment.newInstance(
            course.id,
            forumId,
            discussionId,
            course.shortName
        )
        fragmentTransaction.addToBackStack(null)
            .replace(R.id.course_section_enrol_container, discussionFragment, "Discussion")
        fragmentTransaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val COURSE_ENROL_FRAG_TRANSACTION_KEY = "course_enrol_frag"
        const val INTENT_CONTEXT_URL_KEY = "contextUrl"
        const val INTENT_COURSE_ID_KEY = "courseId"
        const val INTENT_CUSTOM_DATA_KEY = "customData"
        const val INTENT_ENROL_COURSE_KEY = "courseParcel"
    }
}