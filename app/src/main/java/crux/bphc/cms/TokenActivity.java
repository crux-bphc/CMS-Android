package crux.bphc.cms;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import app.Constants;
import app.MyApplication;
import butterknife.ButterKnife;
import butterknife.OnClick;
import helper.APIClient;
import helper.CourseDataHandler;
import helper.CourseRequestHandler;
import helper.MoodleServices;
import helper.UserAccount;
import helper.UserDetail;
import helper.UserUtils;
import helper.Util;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import set.Course;
import set.CourseSection;
import set.Module;
import set.forum.Discussion;

public class TokenActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private Toast toast = null;

    private MoodleServices moodleServices;
    private UserAccount userAccount;
    private CourseDataHandler courseDataHandler;
    private CourseRequestHandler courseRequestHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // reverse condition because SplashTheme is default dark
        if (!MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);

        Retrofit retrofit = APIClient.getRetrofitInstance();
        moodleServices = retrofit.create(MoodleServices.class);

        courseDataHandler = new CourseDataHandler(this);
        courseRequestHandler = new CourseRequestHandler(this);

        userAccount = new UserAccount(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent != null && intent.getAction() == Intent.ACTION_VIEW) {
            Uri data = intent.getData();
            if (data != null) {
                if (!data.getScheme().equals(Constants.SSO_URL_SCHEME)) {
                    Toast.makeText(this, "Invalid token URI Schema.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                final String host_scheme = "token=";
                String host = data.getHost();

                if (!host.contains(host_scheme)) {
                    Toast.makeText(this, "Invalid token URI Schema.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Clean up the host so that we can extract the token
                host = host.replace(host_scheme, "");
                host = host.replaceAll("/?#?$", "");

                host = new String(Base64.decode(host, Base64.DEFAULT));

                String[] parts = host.split(":::");
                String digest = parts[0].toUpperCase();
                String token = parts[1];

                HashMap<String, String> launchData = MyApplication.getInstance().getLoginLaunchData();
                String signature = launchData.get("SITE_URL") + launchData.get("PASSPORT");

                try {
                    if (!(Util.bytesToHex(MessageDigest.getInstance("md5")
                            .digest(signature.getBytes(StandardCharsets.US_ASCII)))
                            .equals(digest))) {
                        Toast.makeText(this, "Invalid token signature",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(this, "MD5 not a valid MessageDigest algorithm! :o",
                            Toast.LENGTH_SHORT).show();
                }
                loginUsingToken(token);
            }
        }

        checkLoggedIn();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void loginUsingToken(String token) {
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

                if (responseString.contains("accessexception")) {
                    Toast.makeText(
                            TokenActivity.this,
                            "Please provide the Moodle Mobile web service Token",
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

    @OnClick(R.id.google_login)
    void onLogin() {
        /*
            We'll just create an into a specific Moodle endpoint. The Moodle website will handle the authentication,
            generate a token and redirect the browser to the Uri with specified schema. The browser will create an
            intent that'll launch this activity again.
         */

        // A random number that identifies the request
        String passport = ((Integer) new Random().nextInt(1000)).toString();
        String loginUrl = String.format(Constants.SSO_LOGIN_URL, passport, Constants.SSO_URL_SCHEME);

        // Set the launch data, we need this to verify the token obtained after SSO
        HashMap<String, String> data = new HashMap<String, String>();
        // SITE_URL must not end with trailing /
        data.put("SITE_URL", Constants.API_URL.replaceAll("/$", ""));
        data.put("PASSPORT", passport);
        MyApplication.getInstance().setLoginLaunchData(data);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(loginUrl));
        startActivity(intent);
    }

    private void showProgress(final boolean show, String message) {
        if (show)
            progressDialog.show();
        else
            progressDialog.hide();
        progressDialog.setMessage(message);
    }

    private void dismissProgress() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
            dismissProgress();
            startActivity(intent);
            finish();
        }
    }

    class CourseDataRetriever extends AsyncTask<Void, Integer, Boolean> {

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
            List<Course> courseList = courseRequests.getCourseList(context);
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
                for (CourseSection courseSection : courseSections) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == Module.Type.FORUM) {
                            List<Discussion> discussions = courseRequestHandler.getForumDiscussions(module.getInstance());
                            if (discussions == null) continue;
                            for (Discussion d : discussions) {
                                d.setForumId(module.getInstance());
                            }
                            courseDataHandler.setForumDiscussions(module.getInstance(), discussions);
                        }
                    }
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
