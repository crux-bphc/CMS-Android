package helper;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;

import static app.Constants.API_URL;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class CourseRequestHandler {

    public static final String INVALID_TOKEN = "Invalid token";
    public static final String NETWORK_ERROR = "Network error";
    public static final String ACCESS_EXCEPTION = "accessexception";
    UserAccount userAccount;
    Context context;
    MoodleServices moodleServices;

    public CourseRequestHandler(Context context) {
        userAccount = new UserAccount(context);
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        moodleServices = retrofit.create(MoodleServices.class);
    }

    /**
     * Async call for getting user Courses
     */
    public void getCourseList(@Nullable final CallBack<List<Course>> callBack) {
        Call<ResponseBody> courseCall = moodleServices.getCourses(userAccount.getToken(), userAccount.getUserID());

        courseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) {
                        return;
                    }
                    String responseString = response.body().string();
                    if (responseString.contains(INVALID_TOKEN)) {
                        if (callBack != null) {
                            callBack.onFailure(INVALID_TOKEN, new Throwable(INVALID_TOKEN));
                        }
                        return;
                    }
                    if (responseString.contains(ACCESS_EXCEPTION)) {
                        if (callBack != null) {
                            callBack.onFailure(ACCESS_EXCEPTION, new Throwable(ACCESS_EXCEPTION));
                        }
                        return;
                    }
                    Gson gson = new Gson();

                    // TODO shouldn't crash for empty course list, but it did once
                    // User wasn't registered in any courses. Bug could not be replicated.
                    // Refer to Issue on Github for details.
                    final List<Course> coursesList = gson
                            .fromJson(responseString, new TypeToken<List<Course>>() {
                            }.getType());

                    if (callBack != null) {
                        callBack.onResponse(coursesList);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.onFailure(e.getMessage(), new Throwable(e.getMessage()));
                    }
                } catch (JsonSyntaxException jse) {
                    if (callBack != null) {
                        callBack.onFailure(jse.getMessage(), new Throwable(jse.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(NETWORK_ERROR, t);
                }
            }
        });
    }


    /**
     * Fetches all user enrolled courses from the Moodle server
     */
    public List<Course> getCourseList(Context context) {

        Call<ResponseBody> courseListCall = moodleServices.getCourses(userAccount.getToken(), userAccount.getUserID());

        try {
            Response<ResponseBody> courseListResp = courseListCall.execute(); // sync call
            if (courseListResp.code() != 200) {
                return null;
            }
            String responseString = courseListResp.body().string();
            if (responseString.contains("Invalid token")) {
                return null;
            }
            Gson gson = new Gson();
            return gson
                    .fromJson(responseString, new TypeToken<List<Course>>() {
                    }.getType());

        } catch (IOException e) {
            if (context != null)
                Toast.makeText(context, "Unable to connect to the Internet", Toast.LENGTH_SHORT).show();
        } catch (JsonSyntaxException jse) {
            if (context != null)
                Toast.makeText(context, "Malformed json. Maybe your token was reset!", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public List<CourseSection> getCourseData(Course course) {

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(userAccount.getToken(), course.getId());
        try {
            Response response = courseCall.execute();
            if (response.code() != 200) {
                return null;
            }
            return (List<CourseSection>) response.body();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getCourseData(int courseId, @Nullable final CallBack<List<CourseSection>> callBack) {

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(userAccount.getToken(), courseId);
        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {
                List<CourseSection> sectionList = response.body();
                if (callBack != null) {
                    callBack.onResponse(sectionList);
                }
            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(t.getMessage(), t);
                }
            }
        });
    }


    public interface CallBack<T> {

        public void onResponse(T responseObject);

        public void onFailure(String message, Throwable t);
    }
}
