package crux.bphc.cms.network;

import android.annotation.TargetApi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import crux.bphc.cms.models.core.AutoLoginDetail;
import crux.bphc.cms.models.core.UserDetail;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.enrol.SelfEnrol;
import crux.bphc.cms.models.forum.ForumData;
import crux.bphc.cms.models.enrol.CourseSearch;
import retrofit2.http.Url;

/**
 * Interface of  Retrofit compatible API calls.
 *
 * @author Created by harsu on 16-12-2016.
 */
public interface MoodleServices {

    /**
     * User's data like username, first name, last name, full name, userId is obtained.
     * <p>
     * The {@link UserDetail} response object can have any and all possible objects nullable.
     * This indicates an invalid token and the response JSON will have a message as well as
     * exception code. This can be used as a convenient check to see if the token is valid
     * or not
     *
     * @param token A valid Moodle Web Service token
     */
    @GET("webservice/rest/server.php?wsfunction=core_webservice_get_site_info&moodlewsrestformat=json")
    Call<UserDetail> fetchUserDetail(@Query("wstoken") String token);

    /**
     * Fetch courses enroled by user
     *
     * @param token A valid Moodle Web Service token
     * @param userId The Id of the user, as obtained from {@link #fetchUserDetail}
     */
    @GET("webservice/rest/server.php?wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json")
    Call<ResponseBody> fetchCourses(@Query("wstoken") String token, @Query("userid") int userId);

    /**
     * Fetch the content of a course i.e the sections and the modules in those sections.
     * 
     * @param token A valid Moodle Web Service token
     * @param courseId The Id of the course, as obtained from {@link #fetchCourses}
     */
    @GET("webservice/rest/server.php?wsfunction=core_course_get_contents&moodlewsrestformat=json")
    Call<List<CourseSection>> fetchCourseContent(@Query("wstoken") String token, @Query("courseid") int courseId);

    /**
     * Get courses based on a search pattern
     *
     * @param token A valid Moodle Web Service token
     * @param courseName The search pattern
     * @param page Page number to fetch
     * @param numberOfResults Number of results per page to fetch
     */
    @GET("webservice/rest/server.php?wsfunction=core_course_search_courses&moodlewsrestformat=json&criterianame=search")
    Call<CourseSearch> searchCourses(@Query("wstoken") String token,
                                     @Query("criteriavalue") String courseName,
                                     @Query("page") int page,
                                     @Query("perpage") int numberOfResults);

    /**
     * Enrols a user to a course
     * @param token A valid Moodle Web Service token
     * @param courseId Id of the course to enrol the user to
     */
    @GET("webservice/rest/server.php?wsfunction=enrol_self_enrol_user&moodlewsrestformat=json")
    Call<SelfEnrol> selfEnrolUserInCourse(@Query("wstoken") String token, @Query("courseid") int courseId);

    /**
     * Get the discussions in a forum. The discussions are sorted in descending
     * order of the time they were last modified. However, pinned posts will
     * show up on the top.
     *
     * @param token A valid Moodle Web Service token
     * @param forumid The id of the forum. This will be the <code>instance</code>
     *               field of the corresponding module object. A value of
     *                <code>1</code> corresponds to the "Site News" forum.
     * @param page Page number to fetch
     * @param perpage Number of discussions to fetch per page
     */
    @GET("webservice/rest/server.php?wsfunction=mod_forum_get_forum_discussions_paginated&moodlewsrestformat=json" +
            "&sortby=timemodified&sortdirection=DESC")
    Call<ForumData> getForumDiscussions(@Query("wstoken") String token,
                                        @Query("forumid") int forumid,
                                        @Query("page") int page,
                                        @Query("perpage") int perpage);

    @GET("webservice/rest/server.php?wsfunction=core_user_add_user_device&moodlewsrestformat=json")
    Call<ResponseBody> registerUserDevice(@Query("wstoken") @NotNull String token,
                                    @Query("appid") @NotNull String appid,
                                    @Query("name") @NotNull String name,
                                    @Query("model") @NotNull String model,
                                    @Query("platform") @NotNull String platform,
                                    @Query("version") @NotNull String version,
                                    @Query("pushid") @NotNull String pushid,
                                    @Query("uuid") @NotNull String uuid);

    @GET("webservice/rest/server.php?wsfunction=core_user_remove_user_device&moodlewsrestformat=json")
    Call<ResponseBody> deregisterUserDevice(@Query("wstoken") @NotNull String token,
                                            @Query("uuid") @NotNull String uuid,
                                            @Query("appid") @NotNull String appId);

    /**
     * Endpoint to obtain an autologin key using private token. This endpoint
     * requires the private token to not be a GET parameter with the user agent
     * set to 'MoodleMobile'.
     */
    @FormUrlEncoded
    @Headers("User-Agent: MoodleMobile")
    @POST("webservice/rest/server.php?wsfunction=tool_mobile_get_autologin_key&moodlewsrestformat=json")
    Call<AutoLoginDetail> autoLoginGetKey(@Query("wstoken") @NotNull String token,
                                          @Field("privatetoken") @NotNull String privateToken);

    /**
     * Use an auto-login key to create a user session. The auto-login endpoint
     * is dynamic and will be returned with the auto-login key. The endpoint
     * will redirect to the wwwroot. The 'Set-Cookie' header of this redirect
     * response will contain the session token for further session auth based
     * requests.
     */
    @GET
    Call<ResponseBody> autoLoginWithKey(@Url @NotNull  String autoLoginUrl,
                                        @Query("userid") int userId,
                                        @Query("key") @NotNull String key);

    @GET("course/view.php")
    Call<ResponseBody> viewCoursePage(@Header("Cookie") String moodleSession,
                                      @Query("id") int courseId);

    /**
     * Webpage used to unenrol a user from a course they self-enroled in.
     * enrolId and sessKey should be obtained before hand using the
     * {@see MoodleServices#viewCoursePage} endpoint. sessKey is a CSRF
     * token embedded into links and forms by Moodle with each request.
     */
    @GET("enrol/self/unenrolself.php?confirm=1")
    Call<ResponseBody> selfUnenrolCourse(@Header("Cookie") String moodleSession,
                                     @Query("enrolid") String enrolId,
                                     @Query("sesskey") String sessKey);
}
