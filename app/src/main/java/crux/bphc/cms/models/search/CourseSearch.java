package crux.bphc.cms.models.search;

import java.util.List;

/**
 * Created by siddhant on 12/17/16.
 */

public class CourseSearch {

    private int total;
    private List<Course> courses;
    // as there is no information available for overviewfiles in api request, the data type of list
    // is kept String
    private List<String> warnings;

    public CourseSearch(int total, List<Course> courses, List<String> warnings) {
        this.total = total;
        this.courses = courses;
        this.warnings = warnings;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public int getTotal() {
        return total;
    }
}
