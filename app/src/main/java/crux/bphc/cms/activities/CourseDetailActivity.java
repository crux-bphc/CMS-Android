package crux.bphc.cms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.fragments.CourseContentFragment;
import crux.bphc.cms.fragments.CourseEnrolFragment;
import crux.bphc.cms.fragments.DiscussionFragment;
import crux.bphc.cms.fragments.ForumFragment;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.enrol.SearchedCourseDetail;
import crux.bphc.cms.models.forum.Discussion;
import io.realm.Realm;

import static crux.bphc.cms.app.Constants.COURSE_PARCEL_INTENT_KEY;
import static crux.bphc.cms.app.Constants.TOKEN;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String COURSE_ENROL_FRAG_TRANSACTION_KEY = "course_enrol_frag";
    Course course;
    Realm realm;
    private FragmentManager fragmentManager;
    private SearchedCourseDetail mEnrolCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        realm = MyApplication.getInstance().getRealmInstance();
        fragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();

        mEnrolCourse = intent.getParcelableExtra(COURSE_PARCEL_INTENT_KEY);
        int courseId = intent.getIntExtra("courseId", -1);
        int forumId = intent.getIntExtra("forumId", -1);
        int discussionId = intent.getIntExtra("discussionId", -1);

        if (courseId == -1 && mEnrolCourse == null) {
            finish();
            return;
        } else if (courseId == -1) {
            courseId = mEnrolCourse.getId();
        }

        course = realm
                .where(Course.class)
                .equalTo("id", courseId)
                .findFirst();

        // check if enrolled
        if (course == null) {
            setCourseEnrol();
            setTitle(mEnrolCourse.getShortName());

        } else {
            setTitle(course.getShortName());
            if (discussionId == -1) {
                if (forumId == -1){
                    setCourseSection();
                } else {
                    setForumFragment(forumId);
                }
            } else {
                // Show discussion, regardless of forumId
                Discussion discussion = realm.where(Discussion.class).equalTo("id", discussionId).findFirst();
                if (discussion != null) {
                    setDiscussionFragment(discussion.getForumid(), discussionId);
                }
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void setCourseEnrol() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CourseEnrolFragment mCourseEnrolFragment = CourseEnrolFragment.newInstance(TOKEN, mEnrolCourse);
        fragmentTransaction.replace(
                R.id.course_section_enrol_container,
                mCourseEnrolFragment,
                COURSE_ENROL_FRAG_TRANSACTION_KEY);
        fragmentTransaction.commit();
    }

    private void setCourseSection() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CourseContentFragment courseSectionFragment = CourseContentFragment.newInstance(
                TOKEN,
                course.getCourseId());
        fragmentTransaction.replace(
                R.id.course_section_enrol_container,
                courseSectionFragment,
                "course_section_frag"
        ).commit();
    }

    private void setForumFragment(int forumId) {
        // We first add and commit courseSection
        setCourseSection();
        fragmentManager.executePendingTransactions();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment forumFragment = ForumFragment.newInstance(forumId, course.getShortName());
        fragmentTransaction.addToBackStack(null)
                .replace(R.id.course_section_enrol_container, forumFragment, "Announcements");
        fragmentTransaction.commit();
    }

    private void setDiscussionFragment(int forumId, int discussionId) {
        setForumFragment(forumId);
        fragmentManager.executePendingTransactions();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment discussionFragment = DiscussionFragment.newInstance(discussionId, course.getShortName());
        fragmentTransaction.addToBackStack(null)
                .replace(R.id.course_section_enrol_container, discussionFragment, "Discussion");
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
