package crux.bphc.cms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.List;

import app.MyApplication;
import crux.bphc.cms.R;
import crux.bphc.cms.fragments.CourseEnrolFragment;
import crux.bphc.cms.fragments.CourseSectionFragment;
import crux.bphc.cms.fragments.DiscussionFragment;
import crux.bphc.cms.fragments.ForumFragment;
import io.realm.Realm;
import set.Course;
import set.forum.Discussion;
import set.search.Contact;

import static app.Constants.COURSE_PARCEL_INTENT_KEY;
import static app.Constants.TOKEN;

public class CourseDetailActivity extends AppCompatActivity {

    public static final String COURSE_ENROL_FRAG_TRANSACTION_KEY = "course_enrol_frag";
    public List<Contact> contacts;
    Course course;
    Realm realm;
    private FrameLayout mCourseEnrolContainer;
    private FragmentManager fragmentManager;
    private CourseEnrolFragment mCourseEnrolFragment;
    private set.search.Course mEnrolCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        realm = MyApplication.getInstance().getRealmInstance();
        fragmentManager = getSupportFragmentManager();
        mCourseEnrolContainer = findViewById(R.id.course_section_enrol_container);

        Intent intent = getIntent();

        mEnrolCourse = intent.getParcelableExtra(COURSE_PARCEL_INTENT_KEY);
        int courseId = intent.getIntExtra("courseId", -1);
        int forumId = intent.getIntExtra("forumId", -1);
        int discussionId = intent.getIntExtra("discussionId", -1);

        if (courseId == -1 && mEnrolCourse == null) {
            finish();
            return;
        } else if (courseId == -1 && mEnrolCourse != null) {
            courseId = mEnrolCourse.getId();
        }

        course = realm
                .where(Course.class)
                .equalTo("id", courseId)
                .findFirst();

        // check if enrolled
        if (course == null) {
            contacts = mEnrolCourse.getContacts();
            setCourseEnrol();
            setTitle(mEnrolCourse.getShortname());

        } else {
            setTitle(course.getShortname());
            if (forumId == -1 && discussionId == -1) {
                setCourseSection();
            } else if (forumId != -1 && discussionId == -1) {
                setForumFragment(forumId);
            } else if (forumId == -1 && discussionId != -1) {
                // We need to find the forumId first
                forumId = realm.where(Discussion.class).equalTo("id", discussionId)
                        .findFirst().getForumid();
                setDiscussionFragment(forumId, discussionId);
            }
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setCourseEnrol() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mCourseEnrolFragment = CourseEnrolFragment.newInstance(TOKEN, mEnrolCourse);
        fragmentTransaction.replace(
                R.id.course_section_enrol_container,
                mCourseEnrolFragment,
                COURSE_ENROL_FRAG_TRANSACTION_KEY);
        fragmentTransaction.commit();
    }

    private void setCourseSection() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CourseSectionFragment courseSectionFragment = CourseSectionFragment.newInstance(
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
        Fragment forumFragment = ForumFragment.newInstance(TOKEN, forumId, course.getShortname());
        fragmentTransaction.addToBackStack(null)
                .replace(R.id.course_section_enrol_container, forumFragment, "Announcements");
        fragmentTransaction.commit();
    }

    private void setDiscussionFragment(int forumId, int discussionId) {
        setForumFragment(forumId);
        fragmentManager.executePendingTransactions();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment discussionFragment = DiscussionFragment.newInstance(discussionId, course.getShortname());
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
