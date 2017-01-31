package set;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by harsu on 18-01-2017.
 */

@RealmClass
public class NotificationSet implements RealmModel {

    @PrimaryKey
    private int modId;
    private int courseID;
    private String courseName;
    private String moduleName;

    public NotificationSet() {
    }

    public NotificationSet(int courseID, String courseName, int modId, String moduleName) {
        this.courseID = courseID;
        this.courseName = courseName;
        this.modId = modId;
        this.moduleName = moduleName;
    }

    public int getModId() {
        return modId;
    }

    public int getCourseID() {
        return courseID;
    }

    public void setCourseID(int courseID) {
        this.courseID = courseID;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public NotificationSet(Course course, Module module) {
        this.courseID = course.getId();
        this.courseName = course.getShortname();
        this.modId = module.getId();
        this.moduleName = module.getName();
    }


    public void setModId(int modId) {
        this.modId = modId;
    }


    public String getTitle() {
        return courseName;
    }

    public String getGroupKey() {
        return courseName;

    }

    public String getContentText() {
        return moduleName;
    }
}
