package crux.bphc.cms.models;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import crux.bphc.cms.models.forum.Discussion;

/**
 * Created by harsu on 18-01-2017.
 */

@RealmClass
public class NotificationSet implements RealmModel {

    @PrimaryKey
    private int uniqueId;
    private int bundleId;
    private String notifSummary;
    private String notifTitle;
    private String notifContent;

    public NotificationSet() {
    }

    public NotificationSet(int uniqueId, int bundleId, String notifTitle, String notifContent, String notifSummary) {
        this.uniqueId = uniqueId;
        this.bundleId = bundleId;
        this.notifTitle = notifTitle;
        this.notifContent = notifContent;
        this.notifSummary = notifSummary;
    }

    /** Helper methods to create NotificationSet objects */
    public static NotificationSet createNotificationSet(Course course, CourseSection section, Module module) {
        return new NotificationSet(module.getId(), course.getId(), section.getName(), module.getName(), course.getShortname());
    }

    public static NotificationSet createNotificationSet(Course course, Module module, Discussion discussion) {
        return new NotificationSet(discussion.getId(), course.getId(), module.getName(), discussion.getMessage(), course.getShortname());
    }

    public static NotificationSet createNotificationSet(int courseID, String courseName, int modId, String moduleName) {
        return new NotificationSet(modId, courseID, "", moduleName, courseName);
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public int getBundleID() {
        return bundleId;
    }


    public void setBundleID(int bundleID) {
        this.bundleId = bundleID;
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

    public String getNotifContent() {
        return notifContent;
    }

    public void setNotifContent(String notifContent) {
        this.notifContent = notifContent;
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

}
