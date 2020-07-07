package crux.bphc.cms.network;

import java.util.List;

import crux.bphc.cms.models.core.UserDetail;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.enrol.SelfEnrol;
import crux.bphc.cms.models.forum.ForumData;
import crux.bphc.cms.models.enrol.CourseSearch;

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
}
