package crux.bphc.cms.models.search;

import java.util.List;

/**
 * Created by siddhant on 12/17/16.
 */

public class CourseSearch {

    private int total;
    private List<Course> courses;

    public CourseSearch(int total, List<Course> courses) {
        this.total = total;
        this.courses = courses;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public int getTotal() {
        return total;
    }
}
