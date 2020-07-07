package crux.bphc.cms.models.enrol;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import crux.bphc.cms.network.MoodleServices;

/**
 * Model class to represent response from
 * {@link MoodleServices#searchCourses}.
 *
 * @author Siddhant Kumar Patel (17-Dec-2016)
 */

public class CourseSearch {

    @SerializedName("total") private int total;
    @SerializedName("courses") private List<SearchedCourseDetail> courses;

    @SuppressWarnings("unused")
    public CourseSearch() {
    }

    public List<SearchedCourseDetail> getCourses() {
        return courses;
    }

    public int getTotal() {
        return total;
    }
}
