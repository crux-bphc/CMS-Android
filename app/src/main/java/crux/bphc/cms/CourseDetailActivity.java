package crux.bphc.cms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import crux.bphc.cms.fragments.CourseEnrolFragment;
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

    LinearLayout linearLayout;
    private FrameLayout mCourseEnrolContainer;
    private CourseEnrolFragment mCourseEnrolFragment;

    public static final String COURSE_ENROL_FRAG_TRANSACTION_KEY = "course_enrol_frag";
    private set.search.Course mReceivedCourse;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        setContentView(R.layout.activity_course_detail);

        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        mCourseEnrolContainer = (FrameLayout) findViewById(R.id.course_enrol_container);

        mReceivedCourse = getIntent().getParcelableExtra(COURSE_PARCEL_INTENT_KEY);
        int courseId = getIntent().getIntExtra("id", -1);

        if (courseId == -1 && mReceivedCourse != null) {
            int receivedCourseId = mReceivedCourse.getId();
            System.out.println("receivedCourseId: " + receivedCourseId);
            Course courseEnrol = getFirstCourse(receivedCourseId);

            if(courseEnrol == null) {
                linearLayout.setVisibility(View.GONE);
                mCourseEnrolContainer.setVisibility(View.VISIBLE);
                contacts = mReceivedCourse.getContacts();
                setCourseEnrol();
            }
        }
        else if(mReceivedCourse == null && courseId != -1) {
            mCourseEnrolContainer.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);

            course = getFirstCourse(courseId);
            setTitle(course.getShortname());


            RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseID", courseId).findAll();
            for (CourseSection section : courseSections) {
                addSection(section);
            }

            sendRequest(course.getCourseId());

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        else {
            finish();
            return;
        }

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
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        CourseEnrolFragment oldFragment = (CourseEnrolFragment) getSupportFragmentManager()
                .findFragmentByTag(COURSE_ENROL_FRAG_TRANSACTION_KEY);
        if(oldFragment != null) {
            fragmentTransaction.remove(oldFragment);
            fragmentTransaction.commit();
        }
        mCourseEnrolFragment = CourseEnrolFragment.newInstance(TOKEN, mReceivedCourse);
        fragmentTransaction.add(
                R.id.course_enrol_container,
                mCourseEnrolFragment,
                COURSE_ENROL_FRAG_TRANSACTION_KEY);
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

    private void sendRequest(final int courseId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(TOKEN, courseId);

        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {
                final List<CourseSection> sectionList = response.body();
                if (sectionList == null) {
                    //todo not registered, ask to register, change UI, show enroll button

                    return;
                }

                final RealmResults<CourseSection> results = realm.where(CourseSection.class).equalTo("courseID", courseId).findAll();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
                        linearLayout.removeAllViews();
                        for (CourseSection section : sectionList) {
                            addSection(section);
                            section.setCourseID(courseId);
                            realm.copyToRealmOrUpdate(section);

                        }
                    }
                });


            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                //no internet connection

                //todo show offline available data
                Toast.makeText(CourseDetailActivity.this, "Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSection(final CourseSection section) {
        View v = LayoutInflater.from(this).inflate(R.layout.row_course_section, linearLayout, false);
        ((TextView) v.findViewById(R.id.sectionName)).setText(section.getName());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CourseDetailActivity.this, CourseModulesActivity.class);
                intent.putExtra("id", section.getId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        linearLayout.addView(v);
    }


}
