package helper;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Content;
import set.Course;
import set.CourseSection;
import set.Module;

import static app.Constants.API_URL;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class CourseRequestHandler {

    public static final String INVALID_TOKEN = "Invalid token";
    public static final String NETWORK_ERROR = "Network error";
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
                    String responseString = response.body().string();
                    if (responseString.contains(INVALID_TOKEN)) {
                        if (callBack != null) {
                            callBack.onFailure(INVALID_TOKEN,new Throwable(INVALID_TOKEN));
                        }
                        return;
                    }
                    Gson gson = new Gson();
                    final List<Course> coursesList = gson
                            .fromJson(responseString, new TypeToken<List<Course>>() {
                            }.getType());

                    if (callBack != null) {
                        callBack.onResponse(coursesList);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.onFailure(e.getMessage(),new Throwable(e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(NETWORK_ERROR,t);
                }
            }
        });
    }


    /**
     * Sync call for getting user Courses
     */
    public List<Course> getCourseList() {

        Call<ResponseBody> courseListCall = moodleServices.getCourses(userAccount.getToken(), userAccount.getUserID());

        try {
            Response<ResponseBody> courseListResp = courseListCall.execute();
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
            List<CourseSection> resolvedSameNames = resolve(response.body());
            return resolvedSameNames;
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
                List<CourseSection> sectionList = resolve(response.body());
                if (callBack != null) {
                    callBack.onResponse(sectionList);
                }
            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                if (callBack != null) {
                    callBack.onFailure(t.getMessage(),t);
                }
            }
        });
    }


    private List<CourseSection> resolve(Object response){
        List<CourseSection> courseSections = (List<CourseSection>) response;
        List<Content> contents = new ArrayList<>();
        for(CourseSection courseSection : courseSections){
            for (Module module : courseSection.getModules()){
                if(module.getContents()!=null){
                    contents.addAll(module.getContents());
                }
            }
        }

        Set<Content> set = new HashSet<>();
        for(int i =0; i<contents.size(); i++){
            while(!set.add(contents.get(i))){
                changeName(contents.get(i));
            }
        }

        return courseSections;
    }

    private void changeName(Content content){
        String fileName = content.getFilename();
        String newFileName = fileName;
        if(!(fileName.lastIndexOf('.')==-1 && fileName.lastIndexOf(')')==-1)){

            int lastIndex = fileName.lastIndexOf('(');
            if(lastIndex == -1){
                newFileName = fileName.substring(0, fileName.lastIndexOf('.')) + "(1)" + fileName.substring(fileName.lastIndexOf('.'));
            }else {
                String fileNum = fileName.substring(lastIndex+1, fileName.lastIndexOf(')'));
                try{
                    int count = Integer.parseInt(fileNum);
                    newFileName = fileName.substring(0,lastIndex+1) + ++count + fileName.substring(fileName.lastIndexOf(')'));
                }catch (Exception e){
                    newFileName = fileName;
                }
            }
        }
        content.setFilename(newFileName);
    }

    public interface CallBack<T> {

        public void onResponse(T responseObject);

        public void onFailure(String message, Throwable t);
    }
}
