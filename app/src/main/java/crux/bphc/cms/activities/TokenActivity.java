package crux.bphc.cms.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import crux.bphc.cms.R;
import crux.bphc.cms.app.Constants;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.helper.CourseDataHandler;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.models.UserAccount;
import crux.bphc.cms.models.core.UserDetail;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import crux.bphc.cms.network.APIClient;
import crux.bphc.cms.network.MoodleServices;
import crux.bphc.cms.utils.UserUtils;
import crux.bphc.cms.utils.Utils;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TokenActivity extends AppCompatActivity {

    private static final String TAG = TokenActivity.class.getName();

    private ProgressDialog progressDialog;

    private MoodleServices moodleServices;
    private UserAccount userAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // reverse condition because SplashTheme is default dark
        if (!MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        progressDialog = new ProgressDialog(this);

        Retrofit retrofit = APIClient.getRetrofitInstance();
        moodleServices = retrofit.create(MoodleServices.class);

        userAccount = new UserAccount(this);

        findViewById(R.id.google_login).setOnClickListener(v -> onLogin());
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri data = intent.getData();
            if (data != null) {
                String scheme = data.getScheme();
                if (scheme != null && !scheme.equals(Constants.SSO_URL_SCHEME)) {
                    Toast.makeText(this, "Invalid token URI Schema.",
                            Toast.LENGTH_SHORT).show();
                    Utils.showBadTokenDialog(this);
                    return;
                }
                final String host_scheme = "token=";
                String host = data.getHost();

                if (host != null) {

                    if (!host.contains(host_scheme)) {
                        Toast.makeText(this, "Invalid token URI Schema.",
                                Toast.LENGTH_SHORT).show();
                        Utils.showBadTokenDialog(this);
                        return;
                    }

                    // Clean up the host so that we can extract the token
                    host = host.replace(host_scheme, "");
                    host = host.replaceAll("/?#?$", "");

                    host = new String(Base64.decode(host, Base64.DEFAULT));

                    String[] parts = host.split(":::");
                    if (parts.length != 2) {
                        Toast.makeText(this, "Invalid token signature",
                                Toast.LENGTH_SHORT).show();
                        Utils.showBadTokenDialog(this);
                        return;
                    }

                    String digest = parts[0].toUpperCase(Locale.ROOT);
                    String token = parts[1];

                    HashMap<String, String> launchData = MyApplication.getInstance().getLoginLaunchData();
                    String signature = launchData.get("SITE_URL") + launchData.get("PASSPORT");

                    try {
                        if (!(Utils.bytesToHex(MessageDigest.getInstance("md5")
                                .digest(signature.getBytes(StandardCharsets.US_ASCII)))
                                .equals(digest))) {
                            Toast.makeText(this, "Invalid token signature",
                                    Toast.LENGTH_SHORT).show();
                            Utils.showBadTokenDialog(this);
                            return;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        Toast.makeText(this, "MD5 not a valid MessageDigest algorithm! :o",
                                Toast.LENGTH_SHORT).show();
                    }
                    loginUsingToken(token);
                }
            }
        }
        checkLoggedIn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void loginUsingToken(String token) {
        showProgress(true, "Fetching your details");
        Call<UserDetail> call = moodleServices.fetchUserDetail(token);
        call.enqueue(new Callback<UserDetail>() {
            @Override
            public void onResponse(@NotNull Call<UserDetail> call, @NotNull Response<UserDetail> response) {
                if (response.isSuccessful()) {
                    UserDetail userDetail;
                    if ((userDetail = response.body()) != null) {
                        if (userDetail.getUsername() == null) {
                            // No `username` field i.e token invalid
                            String responseString = response.toString();

                            if (responseString.contains("Invalid token")
                                || responseString.contains("accessexception")) {
                                showProgress(false, "");
                                Utils.showBadTokenDialog(TokenActivity.this);
                                return;
                            }
                        }
                        userDetail.setToken(token);
                        userAccount.setUser(userDetail);
                        fetchUserData();
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<UserDetail> call, @NotNull Throwable t) {
                Log.wtf(TAG, t);
                showProgress(false, "");
            }
        });
    }

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
        HashMap<String, String> data = new HashMap<>();
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

    private void fetchUserData() {
        new CourseDataRetriever(this).execute();
    }

    private void checkLoggedIn() {
        if (userAccount.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            dismissProgress();
            startActivity(intent);
            finish();
        }
    }

    static class CourseDataRetriever extends AsyncTask<Void, Integer, Boolean> {

        private static final int PROGRESS_COURSE_LIST = 1;
        private static final int PROGRESS_COURSE_CONTENT = 2;
        private static final int PROGRESS_SITE_NEWS = 3;

        private final WeakReference<TokenActivity> activityRef;
        private final CourseDataHandler courseDataHandler;
        private final CourseRequestHandler courseRequestHandler;

        CourseDataRetriever(TokenActivity activity) {
            activityRef = new WeakReference<>(activity);
            courseDataHandler = new CourseDataHandler(activityRef.get(), null);
            courseRequestHandler = new CourseRequestHandler(activityRef.get());
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // We set the realm instance for this worker thread
            courseDataHandler.setRealmInstance(Realm.getDefaultInstance());

            /* Fetch User's Course List */
            publishProgress(PROGRESS_COURSE_LIST);
            List<Course> courseList = courseRequestHandler.getCourseList(activityRef.get());
            if (courseList == null) {
                if (!UserUtils.isValidToken(new UserAccount(activityRef.get()).getToken())) {
                    UserUtils.logout(activityRef.get());
                    UserUtils.clearBackStackAndLaunchTokenActivity(activityRef.get());
                }
                return false;
            }
            courseDataHandler.replaceCourses(courseList);

            /* Fetch Course Content */
            publishProgress(PROGRESS_COURSE_CONTENT);
            List<Course> courses = courseDataHandler.getCourseList();

            for (final Course course : courses) {
                List<CourseSection> courseSections = courseRequestHandler.getCourseData(course);
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
                courseDataHandler.replaceCourseData(course.getId(), courseSections);
            }

            /* Fetch Site News */
            publishProgress(PROGRESS_SITE_NEWS);
            List<Discussion> discussions = courseRequestHandler.getForumDiscussions(1); // 1 is always site news
            if (discussions != null) {
                for (Discussion d : discussions) {
                    d.setForumId(1);
                }
                courseDataHandler.setForumDiscussions(1, discussions);
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length > 0) {
                switch (values[0]) {
                    case PROGRESS_COURSE_LIST:
                        activityRef.get().showProgress(true, "Fetching your course list");
                        break;
                    case PROGRESS_COURSE_CONTENT:
                        activityRef.get().showProgress(true, "Fetching your courses' contents");
                        break;
                    case PROGRESS_SITE_NEWS:
                        activityRef.get().showProgress(true, "Fetching site news");
                        break;
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            activityRef.get().checkLoggedIn();
        }
    }
}
