package crux.bphc.cms;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import helper.APIClient;
import helper.CourseDataHandler;
import helper.CourseRequestHandler;
import helper.MoodleServices;
import helper.UserAccount;
import helper.UserDetail;
import helper.UserUtils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import set.Course;
import set.CourseSection;

public class TokenActivity extends AppCompatActivity {

    @BindView(R.id.token) EditText tokenEditText;

    private ProgressDialog progressDialog;
    private Toast toast = null;

    private MoodleServices moodleServices;
    private UserAccount userAccount;
    private CourseDataHandler courseDataHandler;
    private CourseRequestHandler courseRequestHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);
        ButterKnife.bind(this);

        userAccount = new UserAccount(this);
        checkLoggedIn();

        Retrofit retrofit = APIClient.getRetrofitInstance();
        moodleServices = retrofit.create(MoodleServices.class);

        progressDialog = new ProgressDialog(this);

        courseDataHandler = new CourseDataHandler(this);
        courseRequestHandler = new CourseRequestHandler(this);
    }

    @OnClick(R.id.instructions)
    void onClickInstructions() {
        String preferencesUrl = "http://id.bits-hyderabad.ac.in/moodle/user/preferences.php";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(preferencesUrl));
        startActivity(intent);
    }

    @OnClick(R.id.login)
    void onClickLogin() {
        String token = tokenEditText.getText().toString();
        if (token.length() < 25) {
            if (toast != null) toast.cancel();
            toast = Toast.makeText(this, "Please provide a valid token", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        showProgress(true, "Fetching user details.");
        Call<ResponseBody> call = moodleServices.fetchUserDetail(token);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String responseString = "";
                try {
                    responseString = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (responseString.contains("Invalid token")) {
                    Toast.makeText(
                            TokenActivity.this,
                            "Invalid token provided!",
                            Toast.LENGTH_SHORT).show();
                    showProgress(false, "");
                    return;
                }

                if (responseString.length() > 0) {
                    UserDetail userDetail = new Gson().fromJson(responseString, UserDetail.class);
                    userDetail.setToken(token);
                    userDetail.setPassword(""); // because SSO login

                    userAccount.setUser(userDetail);

                    // now get users courses
                    getUserCourses();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                showProgress(false, "");
            }
        });
    }

    private void showProgress(final boolean show, String message) {
        if (show)
            progressDialog.show();
        else
            progressDialog.hide();
        progressDialog.setMessage(message);
    }

    private void getUserCourses() {
        new CourseDataRetriever(this, courseRequestHandler, courseDataHandler).execute();
    }

    private void checkLoggedIn() {
        if (userAccount.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            if (getIntent().getParcelableExtra("path") != null) {
                intent.putExtra(
                        "path",
                        getIntent().getParcelableExtra("path").toString()
                );
            }
            startActivity(intent);
            finish();
        }
    }

    class CourseDataRetriever extends AsyncTask<Void,Integer,Boolean> {

        private Context context;
        private CourseDataHandler courseDataHandler;
        private CourseRequestHandler courseRequests;

        public CourseDataRetriever(
                Context context,
                CourseRequestHandler courseRequestHandler,
                CourseDataHandler courseDataHandler) {
            this.context = context;
            this.courseRequests = courseRequestHandler;
            this.courseDataHandler = courseDataHandler;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length > 0) {
                if (values[values.length - 1] == 1) {
                    showProgress(true, "Fetching courses list");
                } else if (values[values.length - 1] == 2) {
                    showProgress(true, "Fetching course contents");
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            publishProgress(1);
            List<Course> courseList = courseRequests.getCourseList();
            if (courseList == null) {
                UserUtils.checkTokenValidity(context);
                return null;
            }
            courseDataHandler.setCourseList(courseList);
            publishProgress(2);
            List<Course> courses = courseDataHandler.getCourseList();

            for (final Course course : courses) {
                List<CourseSection> courseSections = courseRequests.getCourseData(course);
                if (courseSections == null) {
                    continue;
                }
                courseDataHandler.setCourseData(course.getCourseId(), courseSections);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            checkLoggedIn();
        }
    }
}
