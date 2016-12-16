package helper;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import set.Course;

/**
 * Created by harsu on 16-12-2016.
 */

public interface MoodleServices {
    @GET("webservice/rest/server.php?wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json")
    Call<List<Course>> getCourses(@Query("wstoken") String token, @Query("userid") int userID);
}
