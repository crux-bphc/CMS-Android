package crux.bphc.cms.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import crux.bphc.cms.models.UserAccount;
import crux.bphc.cms.network.MoodleServices;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import crux.bphc.cms.models.forum.ForumData;

import static crux.bphc.cms.app.Constants.API_URL;

/**
 * Class responsible for making API requests
 *
 * @author Harshit Agarwal
 */

public class CourseRequestHandler {

    public static final String TAG = CourseRequestHandler.class.getName();
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String NETWORK_ERROR = "Network error";
    public static final String ACCESS_EXCEPTION = "accessexception";

    final UserAccount userAccount;
    final MoodleServices moodleServices;

    public CourseRequestHandler(Context context) {
        userAccount = new UserAccount(context);
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
        Call<ResponseBody> courseCall = moodleServices.fetchCourses(userAccount.getToken(), userAccount.getUserID());

        courseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
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
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
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

        Call<ResponseBody> courseListCall = moodleServices.fetchCourses(userAccount.getToken(), userAccount.getUserID());

        try {
            Response<ResponseBody> courseListResp = courseListCall.execute(); // sync call
            if (courseListResp.code() != 200) {
                return null;
            }
            if (courseListResp.body() == null) return null;
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

        Call<List<CourseSection>> courseCall = moodleServices.fetchCourseContent(userAccount.getToken(), course.getId());
        try {
            Response<List<CourseSection>> response = courseCall.execute();
            if (response.code() != 200) {
                return null;
            }
            List<CourseSection> responseCourseSections = response.body();
            if (responseCourseSections == null) return new ArrayList<>();
            return resolve(responseCourseSections);
        } catch (IOException e) {
            Log.w(TAG, "IOException while getting course data", e);
        }
        return null;
    }

    public void getCourseData(int courseId, @Nullable final CallBack<List<CourseSection>> callBack) {

        Call<List<CourseSection>> courseCall = moodleServices.fetchCourseContent(userAccount.getToken(), courseId);
        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(@NonNull Call<List<CourseSection>> call, @NonNull Response<List<CourseSection>> response) {
                List<CourseSection> responseCourseSections = response.body();
                if (responseCourseSections == null) return;
                List<CourseSection> sectionList = resolve(responseCourseSections);
                if (callBack != null) {
                    callBack.onResponse(sectionList);
                }
            }

            @Override
            public void onFailure(@NotNull Call<List<CourseSection>> call, @NotNull Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(t.getMessage(), t);
                }
            }
        });
    }


    public List<Discussion> getForumDiscussions(int moduleId) {
        Call<ForumData> callForumData = moodleServices.getForumDiscussions(userAccount.getToken(), moduleId, 0, 0);
        try {
            Response<ForumData> response = callForumData.execute();
            if (response.code() != 200) {
                return null;
            }
            ForumData forumData = response.body();
            if (forumData == null) return null;
            return forumData.getDiscussions();
        } catch (IOException e) {
            Log.w(TAG, "IOException while getting Forum Discussions", e);
        }
        return null;
    }


    public void getForumDiscussions(int moduleId, @Nullable final CallBack<List<Discussion>> callBack) {
        Call<ForumData> call = moodleServices.getForumDiscussions(userAccount.getToken(), moduleId, 0, 0);
        call.enqueue(new Callback<ForumData>() {
            @Override
            public void onResponse(@NotNull Call<ForumData> call, @NotNull Response<ForumData> response) {
                if (response.body() == null) return;
                List<Discussion> discussions = response.body().getDiscussions();
                if (callBack != null) {
                    callBack.onResponse(discussions);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ForumData> call, @NotNull Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(NETWORK_ERROR, t);
                }
            }
        });
    }

    //This method resolves the names of files with same names
    private List<CourseSection> resolve(List<CourseSection> courseSections) {
        List<Content> contents = new ArrayList<>();
        for (CourseSection courseSection : courseSections) {
            for (Module module : courseSection.getModules()) {
                List<Content> currContents = module.getContents();
                    contents.addAll(currContents);
            }
        }

        Set<Content> set = new TreeSet<>((c1, c2) -> c1.getFileName().compareTo(c2.getFileName()));
        for (Content c : contents) {
            while (!set.add(c)) {
                changeName(c);
            }
        }

        return courseSections;
    }

    private void changeName(Content content) {
        String fileName = content.getFileName();
        String newFileName = fileName;

        // new file name will be of the format <original>(count)[.ext]
        int lastIndex = fileName.lastIndexOf('(');
        boolean countUpdated = false;
        if (lastIndex != -1) {
            String fileNum = fileName.substring(lastIndex + 1, fileName.lastIndexOf(')'));
            try {
                int count = Integer.parseInt(fileNum);
                newFileName = fileName.substring(0, lastIndex + 1)
                        + ++count
                        + fileName.substring(fileName.lastIndexOf(')'));
                countUpdated = true;
            } catch (NumberFormatException e) {
            }
        }

        if (!countUpdated) {
            int extension = fileName.lastIndexOf('.');
            if (extension != -1) {
                newFileName = fileName.substring(0, extension) + "(1)" +
                        fileName.substring(extension);
            } else {
                newFileName = fileName + "(1)";
            }
        }
        content.setFileName(newFileName);
    }

    public interface CallBack<T> {

        void onResponse(T responseObject);

        void onFailure(String message, Throwable t);
    }
}
