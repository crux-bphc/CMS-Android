package crux.bphc.cms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import crux.bphc.cms.fragments.CourseEnrolFragment;
import crux.bphc.cms.fragments.CourseSectionFragment;
import helper.MoodleServices;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;
import set.search.Contact;

import static app.Constants.API_URL;
import static app.Constants.COURSE_PARCEL_INTENT_KEY;
import static app.Constants.TOKEN;

public class CourseDetailActivity extends AppCompatActivity {

    Course course;
    public List<Contact> contacts;

    private FrameLayout mCourseEnrolContainer;
    private FragmentManager fragmentManager;
    private CourseEnrolFragment mCourseEnrolFragment;

    public static final String COURSE_ENROL_FRAG_TRANSACTION_KEY = "course_enrol_frag";
    private set.search.Course mEnrolCourse;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        setContentView(R.layout.activity_course_detail);
        fragmentManager = getSupportFragmentManager();
        mCourseEnrolContainer = (FrameLayout) findViewById(R.id.course_section_enrol_container);

        Intent intent = getIntent();

        mEnrolCourse = intent.getParcelableExtra(COURSE_PARCEL_INTENT_KEY);
        int courseId = intent.getIntExtra("id", -1);

        if(courseId == -1 && mEnrolCourse == null) {
            finish();
            return;
        }
        else if(courseId == -1 && mEnrolCourse != null) {
            courseId = mEnrolCourse.getId();
        }

        course = getFirstCourse(courseId);

        if(course == null) {
            System.out.println("receivedCourseId: " + courseId);
            contacts = mEnrolCourse.getContacts();
            setCourseEnrol();

        }
        else {
            String activityTitleName = intent.getStringExtra("course_name");
            setTitle(activityTitleName);
            setCourseSection();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private Course getFirstCourse(int courseId) {
        RealmResults<Course> courses = realm
                .where(Course.class)
                .equalTo("id", courseId)
                .findAll();
        if(courses.size() == 0) {
            System.out.println("Zero courses matched");
            return null;
        }
        System.out.println("Number of courses matched: " + courses.size());
        return courses.first();
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
