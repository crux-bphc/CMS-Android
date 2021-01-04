package crux.bphc.cms.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import crux.bphc.cms.R
import crux.bphc.cms.app.Constants
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.fragments.CourseContentFragment
import crux.bphc.cms.fragments.CourseEnrolFragment
import crux.bphc.cms.fragments.DiscussionFragment
import crux.bphc.cms.fragments.ForumFragment
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.enrol.SearchedCourseDetail
import crux.bphc.cms.models.forum.Discussion
import io.realm.Realm

class CourseDetailActivity : AppCompatActivity() {
    lateinit var course: Course
    lateinit var realm: Realm

    private var mEnrolCourse: SearchedCourseDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyApplication.getInstance().isDarkModeEnabled) {
            setTheme(R.style.AppTheme_Dark)
        }
        setContentView(R.layout.activity_course_detail)

        realm = Realm.getDefaultInstance()

        mEnrolCourse = intent.getParcelableExtra(Constants.COURSE_PARCEL_INTENT_KEY) as SearchedCourseDetail?
        var courseId = intent.getIntExtra(INTENT_COURSE_ID_KEY, -1)
        val forumId = intent.getIntExtra(INTENT_FORUM_ID_KEY, -1)
        val discussionId = intent.getIntExtra(INTENT_DISCUSSION_ID_KEY, -1)

        if (courseId == -1) {
            if (mEnrolCourse == null) {
                finish()
                return
            } else {
                courseId = mEnrolCourse!!.id  // mEnrolCourse should never be null, fail if it is
                // TODO: Probably show a box to the user and finish activity here instead of NPE
            }
        }

        val queryCourse = realm
                .where(Course::class.java)
                .equalTo("id", courseId)
                .findFirst()

        // check if enrolled
        if (queryCourse == null) {
            setCourseEnrol()
            title = mEnrolCourse!!.shortName
        } else {
            course = queryCourse
            title = course.shortName
            if (discussionId == -1) {
                if (forumId == -1) {
                    setCourseSection()
                } else {
                    setForumFragment(forumId)
                }
            } else {
                // Show discussion, regardless of forumId
                val discussion = realm.where(Discussion::class.java).equalTo("id", discussionId).findFirst()
                if (discussion != null) {
                    setDiscussionFragment(discussion.forumId, discussionId)
                }
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setCourseEnrol() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val mCourseEnrolFragment = CourseEnrolFragment.newInstance(UserAccount.token, mEnrolCourse)
        fragmentTransaction.replace(
                R.id.course_section_enrol_container,
                mCourseEnrolFragment,
                COURSE_ENROL_FRAG_TRANSACTION_KEY)
        fragmentTransaction.commit()
    }

    private fun setCourseSection() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val courseSectionFragment = CourseContentFragment.newInstance(
                UserAccount.token,
                course.id)
        fragmentTransaction.replace(
                R.id.course_section_enrol_container,
                courseSectionFragment,
                "course_section_frag"
        ).commit()
    }

    private fun setForumFragment(forumId: Int) {
        // We first add and commit courseSection
        setCourseSection()
        supportFragmentManager.executePendingTransactions()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val forumFragment: Fragment = ForumFragment.newInstance(course.id, forumId, course.shortName)
        fragmentTransaction.addToBackStack(null)
                .replace(R.id.course_section_enrol_container, forumFragment, "Announcements")
        fragmentTransaction.commit()
    }

    private fun setDiscussionFragment(forumId: Int, discussionId: Int) {
        setForumFragment(forumId)
        supportFragmentManager.executePendingTransactions()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val discussionFragment: Fragment = DiscussionFragment.newInstance(course.id, discussionId,
                course.shortName)
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
        const val INTENT_COURSE_ID_KEY = "courseId"
        const val INTENT_MOD_ID_KEY = "modId"
        const val INTENT_FORUM_ID_KEY = "forumId"
        const val INTENT_DISCUSSION_ID_KEY = "discussionId"
    }
}