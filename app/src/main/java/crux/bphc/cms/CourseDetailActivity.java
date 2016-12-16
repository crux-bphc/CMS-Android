package crux.bphc.cms;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import helper.MoodleServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;

import static app.Constants.API_URL;
import static app.Constants.TOKEN;

public class CourseDetailActivity extends AppCompatActivity {
    Course course;
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        course = getIntent().getParcelableExtra("data");
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        setTitle(course.getShortname());
        sendRequest(course.getCourseId());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendRequest(int courseId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(TOKEN, courseId);

        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {
                List<CourseSection> sectionList = response.body();
                if (sectionList == null) {
                    //todo not registered, ask to register

                    return;
                }

                for (CourseSection section : sectionList) {
                    addSection(section);
                }
            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                //no internet connection
                //todo show offline available data
                Toast.makeText(CourseDetailActivity.this, "Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSection(CourseSection section) {
        View v = LayoutInflater.from(this).inflate(R.layout.row_course_section, linearLayout, false);
        ((TextView) v.findViewById(R.id.sectionName)).setText(section.getName());
        linearLayout.addView(v);
    }

}
