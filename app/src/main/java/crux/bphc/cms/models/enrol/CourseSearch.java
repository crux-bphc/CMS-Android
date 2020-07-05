package crux.bphc.cms.models.enrol;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model class to represent response from
 * {@link crux.bphc.cms.helper.MoodleServices#getSearchedCourses}.
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
