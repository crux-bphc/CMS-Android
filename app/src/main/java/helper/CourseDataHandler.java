package helper;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import app.MyApplication;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;
import set.Content;
import set.Course;
import set.CourseSection;
import set.Module;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class CourseDataHandler {

    Context context;
    UserAccount userAccount;

    public CourseDataHandler(Context context) {
        this.context = context;
        userAccount = new UserAccount(context);
    }

    public static String getCourseName(int courseId) {
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        Course course = realm.where(Course.class).equalTo("id", courseId).findFirst();
        String name = course.getShortname();
        realm.close();
        return name;
    }

    private static <T> T deepCopy(T object, Class<T> type) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(object, type), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param courseList the list of new courses to be replaced in db.
     * @return new Courses as compared from previously stored database.
     */
    public List<Course> setCourseList(List<Course> courseList) {
        if (!userAccount.isLoggedIn()) {
            return null;
        }
        List<Course> newCourses = new ArrayList<>();
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());


        for (Course course : courseList) {
            if (realm.where(Course.class).equalTo("id", course.getId()).findFirst() == null) {
                newCourses.add(course);
            }
        }
        realm.beginTransaction();
        final RealmResults<Course> results = realm.where(Course.class).findAll();
        results.deleteAllFromRealm();
        realm.copyToRealm(courseList);
        realm.commitTransaction();
        realm.close();
        return newCourses;
    }

    /**
     * @return returns course list from database
     */
    public List<Course> getCourseList() {
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        List<Course> courses = realm.copyFromRealm(realm.where(Course.class).findAll());
        realm.close();
        return courses;
    }

    /**
     * @param courseId    courseId for which the sectionList data is given
     * @param sectionList sectionList data
     * @return parts of section data structure which has new contents or null if userAccount is not logged in.
     */
    public List<CourseSection> setCourseData(@NonNull int courseId, @NonNull List<CourseSection> sectionList) {
        if (!userAccount.isLoggedIn()) {
            return null;
        }
        List<CourseSection> newPartsInSections = new ArrayList<>();

        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        //check if course initially had no data
        if (realm.where(CourseSection.class).equalTo("courseID", courseId).findFirst() == null) {
            for (CourseSection section : sectionList) {
                section.setCourseID(courseId);
            }
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(sectionList);
            realm.commitTransaction();
            newPartsInSections = sectionList; //returns the whole sectionList as the whole course is new

        } else {        //not a new course, compare parts for new data
            for (CourseSection section : sectionList) {
                if (realm.where(CourseSection.class).equalTo("id", section.getId()).findFirst() == null) {
                    //whole section is new
                    section.setCourseID(courseId);
                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(section);
                    realm.commitTransaction();
                    newPartsInSections.add(section);
                } else {
                    CourseSection realmSection =
                            realm.where(CourseSection.class).equalTo("id", section.getId()).findFirst();
                    CourseSection newPartInSection = getNewParts(section, realmSection, realm);
                    if (newPartInSection != null) {
                        newPartsInSections.add(newPartInSection);
                    }
                    /*CourseSection trimmedSection = deepCopy(section, CourseSection.class);
                    for (Module module : section.getModules()) {
                        //TODO: 23-11-2017 add checker for new content ie created time or updated time if being returned from server in module/content
                        if (realmSection.getModules().contains(module)) {
                            trimmedSection.getModules().remove(module);
                            //remove a module from section if it was already in database.
                            //todo nest for contents as well.
                            //todo write a better filter using new Java
                        }
                    }*/
                    section.setCourseID(courseId);
                    realm.beginTransaction();
                    realm.where(CourseSection.class)
                            .equalTo("id", section.getId())
                            .findAll().deleteAllFromRealm();
                    realm.copyToRealmOrUpdate(section);
                    realm.commitTransaction();
                    // newPartsInSections.add(trimmedSection);
                }
            }
        }
        realm.close();
        return newPartsInSections;

    }

    private CourseSection getNewParts(CourseSection section, CourseSection realmSection, Realm realm) {

        if (realmSection == null) {
            return section;
        }
        RealmList<Module> newModules = new RealmList<>();
        if (section.getModules() == null) {
            return null;
        }
        for (Module module : section.getModules()) {
            Module newModule = getNewParts(module,
                    realm.where(Module.class).equalTo("id", module.getId()).findFirst(), realm);
            if (newModule != null) {
                newModules.add(newModule);
                module.setNewContent(true);
            }

        }
        if (!newModules.isEmpty()) {
            CourseSection newSection = deepCopy(section, CourseSection.class);
            newSection.setModules(newModules);
            return newSection;
        }
        return null;

    }

    private Module getNewParts(Module module, Module realmModule, Realm realm) {
        if (realmModule == null) {  //new module as a whole
            return module;
        }
        if(realmModule.getDescription()!=null && !realmModule.getDescription().equals(module.getDescription())){  //the description of module has changed
            return module;
        }
        module.setNewContent(realmModule.isNewContent());
        //copying newContent variable from local db to cloud retrieved data

        RealmList<Content> newContents = new RealmList<>();
        if (module.getContents() == null) {
            return null;
        }
        for (Content content : module.getContents()) {
            Content realmContent = realm.where(Content.class)
                    .equalTo("timemodified", content.getTimemodified())
                    .equalTo("fileurl", content.getFileurl())
                    .findFirst();
            Content newContent = realmContent == null ? content : null;
            if (newContent != null) {
                newContents.add(newContent);
            }
        }
        if (!newContents.isEmpty()) {
            Module newModule = deepCopy(module, Module.class);
            newModule.setContents(newContents);
            return newModule;
        }
        return null;
    }

    public void deleteCourse(int courseId) {
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        realm.beginTransaction();
        realm.where(Course.class).equalTo("id", courseId).findAll().deleteAllFromRealm();
        realm.where(CourseSection.class).equalTo("courseID", courseId).findAll().deleteAllFromRealm();
        realm.commitTransaction();
        realm.close();
    }

    public List<CourseSection> getCourseData(int courseId) {
        List<CourseSection> courseSections;
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        courseSections = realm.copyFromRealm(realm
                .where(CourseSection.class)
                .equalTo("courseID", courseId)
                .findAll()
                .sort("id", Sort.ASCENDING));
        realm.close();
        return courseSections;
    }

    public void markAllAsRead(List<CourseSection> courseSections) {
        for(CourseSection courseSection:courseSections){
            if(courseSection.getModules()!=null){
                for(Module module:courseSection.getModules()){
                    markAsRead(module.getId());
                }
            }
        }
    }

    private void markAsRead(int moduleId) {
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        realm.beginTransaction();
        realm.where(Module.class).equalTo("id",moduleId).findFirst().setNewContent(false);
        realm.commitTransaction();
    }

    public int getUnreadCount(int id) {
        int count=0;
        List<CourseSection> courseSections= getCourseData(id);
        for(CourseSection courseSection:courseSections){
            if(courseSection.getModules()!=null){
                for(Module module:courseSection.getModules()){
                    if(module.isNewContent()){
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
