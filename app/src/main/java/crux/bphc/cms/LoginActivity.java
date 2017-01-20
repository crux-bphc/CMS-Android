package crux.bphc.cms;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import app.MyApplication;
import helper.MoodleServices;
import helper.UserAccount;
import helper.UserDetail;
import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import set.Course;
import set.CourseSection;
import set.Module;

import static app.Constants.API_URL;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {


    UserAccount userAccount;
    Realm realm;
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userAccount = new UserAccount(this);
        checkLoggedIn();

        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        ImageView bitsLogo, background;
        background = (ImageView) findViewById(R.id.background);
        bitsLogo = (ImageView) findViewById(R.id.bitsLogo);
        Picasso.with(this).load(R.drawable.bitslogo).into(bitsLogo);
        Picasso.with(this).load(R.drawable.intro_bg).into(background);


    }

    private void checkLoggedIn() {
        if (userAccount.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            if (getIntent().getParcelableExtra("path") != null)
                intent.putExtra("path", getIntent().getParcelableExtra("path"));
            startActivity(intent);
            finish();
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true, "Logging in...");
            UserLoginTask mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute();
        }
    }

    private boolean isEmailValid(String email) {

        return email.startsWith("f20");
    }

    private boolean isPasswordValid(String password) {

        return password.length() >= 4;
    }


    private void showProgress(final boolean show, String message) {
        if (show)
            progressDialog.show();
        else
            progressDialog.hide();
        progressDialog.setMessage(message);
        // mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void getUserCourses() {
        showProgress(true, "Fetching courses list");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        realm = MyApplication.getInstance().getRealmInstance();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<ResponseBody> courseCall = moodleServices.getCourses(userAccount.getToken(), userAccount.getUserID());
        System.out.println(courseCall.request().url().toString());

        courseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseString = response.body().string();
                    if (responseString.contains("Invalid token")) {
                        return;
                    }
                    Gson gson = new Gson();
                    final List<Course> coursesList = gson
                            .fromJson(responseString, new TypeToken<List<Course>>() {
                            }.getType());


                    final RealmResults<Course> results = realm.where(Course.class).findAll();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            results.deleteAllFromRealm();
                            realm.copyToRealm(coursesList);
                            downloadCourseContents();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this,"Unable to fetch course list",Toast.LENGTH_SHORT).show();
                checkLoggedIn();


            }
        });

    }

    private int toDownload=0;


    private void downloadCourseContents() {
        showProgress(true,"Fetching course contents");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        List<Course> courses = realm.copyFromRealm(realm.where(Course.class).findAll());

        toDownload=courses.size();
        if(toDownload==0){
            checkLoggedIn();
            return;
        }
        for (final Course course : courses) {
            Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(userAccount.getToken(), course.getId());
            courseCall.enqueue(new Callback<List<CourseSection>>() {
                @Override
                public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {
                    List<CourseSection> sectionList =  response.body();
                    if (sectionList == null) {
                        return;
                    }
                    for (CourseSection section : sectionList) {
                        section.setCourseID(course.getId());
                        realm.beginTransaction();
                        realm.where(CourseSection.class)
                                .equalTo("id", section.getId())
                                .findAll().deleteAllFromRealm();
                        realm.copyToRealmOrUpdate(section);
                        realm.commitTransaction();
                    }
                    toDownload--;
                    if(toDownload==0){
                        checkLoggedIn();
                    }
                }

                @Override
                public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                    userAccount.waitForInternetConnection(true);
                    toDownload--;
                    if(toDownload==0){
                        checkLoggedIn();
                    }
                }
            });
        }
    }


    private interface MoodleLogin {
        @GET("login/token.php?service=moodle_mobile_app")
        Call<LoginDetail> login(@Query("username") String username, @Query("password") String password);

        @GET("webservice/rest/server.php?wsfunction=core_webservice_get_site_info&moodlewsrestformat=json")
        Call<UserDetail> fetchUserDetail(@Query("wstoken") String token);

    }

    private class UserLoginTask {
        String email, password;

        UserLoginTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        void execute() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            final MoodleLogin moodleLogin = retrofit.create(MoodleLogin.class);

            Call<LoginDetail> call = moodleLogin.login(email, password);
            call.enqueue(new Callback<LoginDetail>() {
                @Override
                public void onResponse(Call<LoginDetail> call, Response<LoginDetail> response) {
                    final LoginDetail loginDetail = response.body();
                    Log.d("Login: ", loginDetail.error + " " + loginDetail.token);

                    if (loginDetail.error != null) {
                        if (loginDetail.error.contains("Web services must be enabled")) {

                            Toast.makeText(LoginActivity.this, "Please contact network administrator to enable mobile services", Toast.LENGTH_LONG).show();
                            showProgress(false, "");
                            return;
                        }

                        //check if password is correct
                        else {
                            Toast.makeText(LoginActivity.this, "The Username or Password is incorrect", Toast.LENGTH_SHORT).show();
                            showProgress(false, "");
                            return;
                        }
                    }

                    showProgress(true, "Fetching user details");
                    Call<UserDetail> userDetailCall = moodleLogin.fetchUserDetail(loginDetail.token);
                    userDetailCall.enqueue(new Callback<UserDetail>() {
                        @Override
                        public void onResponse(Call<UserDetail> call, Response<UserDetail> response) {
                            UserDetail userDetail = response.body();

                            if (userDetail.errorcode != null) {
                                Toast.makeText(LoginActivity.this, "Unknown error occurred. Please retry.", Toast.LENGTH_LONG).show();
                                showProgress(false, "");
                                return;
                            }

                            userDetail.setUsername(email);
                            userDetail.setToken(loginDetail.token);
                            userDetail.setPassword(password);
                            userAccount.setUser(userDetail);
                            getUserCourses();

                        }

                        @Override
                        public void onFailure(Call<UserDetail> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, "Please check your Internet Connection", Toast.LENGTH_SHORT).show();
                            showProgress(false, "");
                        }
                    });


                }

                @Override
                public void onFailure(Call<LoginDetail> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Please check your Internet Connection", Toast.LENGTH_SHORT).show();
                    showProgress(false, "");
                }
            });


        }
    }

    private class LoginDetail {

        public String token;
        public String error;

        public LoginDetail(String error, String token) {
            this.token = token;
            this.error = error;
        }
    }

}

