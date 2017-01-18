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
    private Course course;
    private Module module;

    public NotificationSet() {
    }

    public NotificationSet(Course course, Module module) {
        this.course = course;
        this.module = module;
        this.modId = module.getId();
    }

    public void setModId(int modId) {
        this.modId = modId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
        modId=module.getId();
    }

    public String getTitle() {
        return course.getShortname();
    }

    public String getGroupKey() {
        return course.getShortname();

    }

    public String getContentText() {
        return module.getName();
    }
}
