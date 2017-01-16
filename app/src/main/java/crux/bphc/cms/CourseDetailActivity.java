package crux.bphc.cms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.util.List;

import app.MyApplication;
import crux.bphc.cms.fragments.CourseEnrolFragment;
import crux.bphc.cms.fragments.CourseSectionFragment;
import io.realm.Realm;
import io.realm.RealmResults;
import set.Course;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        realm = MyApplication.getInstance().getRealmInstance();
        fragmentManager = getSupportFragmentManager();
        mCourseEnrolContainer = (FrameLayout) findViewById(R.id.course_section_enrol_container);

        Intent intent = getIntent();

        mEnrolCourse = intent.getParcelableExtra(COURSE_PARCEL_INTENT_KEY);
        int courseId = intent.getIntExtra("id", -1);

        if (courseId == -1 && mEnrolCourse == null) {
            finish();
            return;
        } else if (courseId == -1 && mEnrolCourse != null) {
            courseId = mEnrolCourse.getId();
        }

        course =  realm
                .where(Course.class)
                .equalTo("id", courseId)
                .findFirst();

        if (course == null ) {
            System.out.println("receivedCourseId: " + courseId);
            contacts = mEnrolCourse.getContacts();
            setCourseEnrol();
            setTitle(mEnrolCourse.getShortname());

        } else {
            setTitle(course.getShortname());
            setCourseSection();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
