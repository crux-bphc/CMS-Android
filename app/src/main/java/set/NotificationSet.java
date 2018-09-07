package set;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import set.forum.Discussion;

/**
 * Created by harsu on 18-01-2017.
 */

@RealmClass
public class NotificationSet implements RealmModel {

    @PrimaryKey
    private int uniqueId;
    private int bundleID;
    private String notifSummary;
    private String notifTitle;
    private String notifContext;

    public NotificationSet() {
    }

    public NotificationSet(int courseID, String courseName, int modId, String moduleName) {
        this.bundleID = courseID;
        this.notifSummary = courseName;
        this.uniqueId = modId;
        this.notifContext = moduleName;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public int getBundleID() {
        return bundleID;
    }


    public void setBundleID(int bundleID) {
        this.bundleID = bundleID;
    }

    public String getNotifSummary() {
        return notifSummary;
    }

    public void setNotifSummary(String notifSummary) {
        this.notifSummary = notifSummary;
    }

    public String getNotifTitle() {
        return notifTitle;
    }

    public void setNotifTitle(String notifTitle) {
        this.notifTitle = notifTitle;
    }

    public String getNotifContext() {
        return notifContext;
    }

    public void setNotifContext(String notifContext) {
        this.notifContext = notifContext;
    }

    public NotificationSet(Course course, CourseSection section, Module module) {
        this.bundleID = course.getId();
        this.notifSummary = course.getShortname();
        this.notifTitle = section.getName();
        this.uniqueId = module.getId();
        this.notifContext = module.getName();
    }

    public NotificationSet(Course course, Module module, Discussion discussion) {
        this.bundleID = course.getId();
        this.notifSummary = course.getShortname();
        this.notifTitle = module.getName();
        this.notifContext = discussion.getName();
        this.uniqueId = discussion.getId();
    }


    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }


    public String getTitle() {
        return notifSummary;
    }

    public String getGroupKey() {
        return notifSummary;

    }

    public String getContentText() {
        return notifContext;
    }
}
