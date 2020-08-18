package crux.bphc.cms.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import crux.bphc.cms.models.UserAccount;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Class that interfaces with RealmDB for Course Data.
 *
 * @author Harshit Agarwal (24-Nov-2017)
 * @author Abhijeet Viswa (08-Jul-2020)
 */

public class CourseDataHandler {

    /**
     * A convenient tag that can be used when using {@link Log}.
     */
    @SuppressWarnings("unused")
    private final static String TAG = CourseDataHandler.class.getName();

    private final UserAccount userAccount;
    private Realm realm;

    /**
     * Construct a CourseDataHandler object for handling data.
     *
     * @param context Application or Activity context.
     * @param realm Realm instance. Can be null. However, set realm instance using
                    {@link #setRealmInstance} before calling a data functions.
     */
    public CourseDataHandler(@NotNull Context context, @Nullable Realm realm) {
        userAccount = new UserAccount(context);
        this.realm = realm;
    }

    /**
     * Replaces the courses present in the db with the incoming
     * <code>courses</code> list.
     *
     * @param courses the list of new courses to be replaced in db.
     * @throws NullPointerException if the Realm instance has not been set during
     *                              construction or using {@link #setRealmInstance}
     */
    public void replaceCourses(@NotNull List<Course> courses) {
        if (realm != null) {
            realm.beginTransaction();
            realm.delete(Course.class);
            realm.copyToRealm(courses);
            realm.commitTransaction();
        } else {
            throw new NullPointerException("Realm instance is null");
        }
    }

    /**
     * Isolates and returns all <code>Course</code> instances from
     * <code>courses</code>.
     *
     * @param courses the list of courses to check
     * @return list of courses not present in the database
     */
    @NotNull
    public List<Course> isolateNewCourses(@NotNull List<Course> courses) {
        if (realm != null) {
            Set<Integer> courseIds = courses.stream().map(Course::getId).collect(Collectors.toSet());

            RealmResults<Course> results = realm.where(Course.class).in("id", courseIds.toArray(new Integer[0]))
                    .findAll();
            Set<Integer> inDBIds = results.stream().map(Course::getId).collect(Collectors.toSet());

            courseIds.removeAll(inDBIds); // contains ids of new courses
            return courses.stream().filter(course -> courseIds.contains(course.getId())).collect(Collectors.toList());
        } else {
            throw new NullPointerException("Realm instance is null");
        }
    }

    /**
     * @return  Realm unmanaged list of courses from Realm database
     */
    @NotNull
    public List<Course> getCourseList() {
        if (realm != null) {
            return realm.copyFromRealm(realm.where(Course.class).findAll());
        } else {
            throw new NullPointerException("Realm instance is null");
        }
    }

    /**
     * @param courseId courseId for which the sectionList data is given
     * @param sections sectionList data
     */
    public void replaceCourseData(int courseId, @NonNull List<CourseSection> sections) {
        realm.executeTransaction(realm -> {
            sections.forEach(section -> {
                section.setCourseId(courseId);
                section.getModules().forEach(module -> { // Worst case complexity is O(n^3).
                    module.setCourseSectionId(section.getId());                      // Let's hope it never gets that bad.
                    module.getContents().forEach(content -> content.setModuleId(module.getId()));
                });
            });
            realm.where(CourseSection.class).equalTo("courseId", courseId).findAll().deleteAllFromRealm();
            realm.copyToRealmOrUpdate(sections);
        });
    }

    /**
     * Return a list of <code>CourseSection</code> for a course that contains
     * only new course sections or contain course sections with only new modules.
     * Note that if a CourseSection has new modules, only those modules will be
     * available through {@link CourseSection#getModules}.
     * @param courseId The courseId for which data is to be isolated
     * @param sections The list of course sections to compare against the local
     *                 database
     * @return List of <code>CourseSection</code> that either contains new
     *         course sections or contain <code>CourseSection</code> with only
     *         new modules.
     */
    public List<CourseSection> isolateNewCourseData(int courseId, @NonNull List<CourseSection> sections) {
        final List<CourseSection> newData = new ArrayList<>();

        if (realm.where(CourseSection.class).equalTo("courseId", courseId).findAll().isEmpty()) {
            newData.addAll(sections); // Everything is new
        } else {
            newData.addAll(isolateNewSections(sections));
            newData.addAll(isolateNewAndModifiedModulesInSections(sections.stream()
                    .filter(section -> !newData.contains(section))
                    .collect(Collectors.toList())));
        }
        return newData;
    }

    /**
     * Return a list of <code>CourseSection</code> that are new and are not
     * present locally.
     * @param sections The sections that should be compared against the database
     * @return List of <code>CourseSection</code> that are new
     */
    private List<CourseSection> isolateNewSections(@NonNull List<CourseSection> sections) {
        Set<Integer> sectionsIds = sections.stream().map(CourseSection::getId).collect(Collectors.toSet());
        RealmResults<CourseSection> results = realm.where(CourseSection.class)
                .in("id", sectionsIds.toArray(new Integer[0]))
                .findAll();
        Set<Integer> inDBIds = results.stream().map(CourseSection::getId).collect(Collectors.toSet());

        sectionsIds.removeAll(inDBIds); // contains ids of new sections
        return sections.stream().filter(section -> sectionsIds.contains(section.getId())).collect(Collectors.toList());
    }


    /**
     * Return a list of <code>CourseSection</code> that differ from the local
     * copy. The difference can be either by the existence of new modules or
     * modification of existing modules. Note that If a
     * <code>CourseSection</code> instance not present locally is passed to the
     * function, it will treat all its modules as new.
     *
     * @param sections The sections that should be compared against the database
     * @return List of <code>CourseSection</code> that have been modified.
     */
    private List<CourseSection> isolateNewAndModifiedModulesInSections(@NonNull List<CourseSection> sections) {
        List<CourseSection> retSections = new ArrayList<>();
        for (CourseSection section : sections) {
            CourseSection retSection = new CourseSection(section);
            List<Module> modules = section.getModules();
            List<Module> retModules = new ArrayList<>();

            RealmResults<Module> results = realm.where(Module.class)
                    .in("id", sections.stream().flatMap(courseSection -> courseSection.getModules().stream())
                    .map(Module::getId).distinct().toArray(Integer[]::new))
                    .findAll();
            Set<Integer> inDBIds = results.stream().map(Module::getId).collect(Collectors.toSet());

            for (Module module : modules) {
                if (!inDBIds.contains(module.getId())) {
                    // Add the modules that are brand new
                    module.setIsUnread(true);
                    retModules.add(module);
                } else {
                    Module mod = isolateNewContentInModules(module);
                    if (mod != null) {
                        retModules.add(mod);
                    }
                }
            }
            if (!retModules.isEmpty()) {
                retSection.setModules(retModules);
                retSections.add(retSection);
            }
        }
        return retSections;
    }

    /**
     * Return an instance of <code>Module</code> with a list of new contents only.
     * The a new <code>Content</code> is determined by using it's file name,
     * file url and modified time, the same way as <code>Content</code> {@link
     * Content#equals equality}. If there is no new content in this module,
     * <code>null</code> is returned.
     *
     * @param module The module that should be compared against the database
     * @return <code>Module</code> instance with contents set to list of new
     *         Contents, if any, else a null instance
     */
    @Nullable
    private Module isolateNewContentInModules(Module module) {
        Module realmModule = realm.where(Module.class).equalTo("id", module.getId()).findFirst();

        if (realmModule == null) {
            module.setIsUnread(true);
            return module; // Everything is new
        }

        // Always copy over flag in case user manually set unread and for it
        // to persist across multiple data requests, irrespective of whether
        // content is new or not
        module.setIsUnread(realmModule.isUnread());

        RealmResults<Content> results = realm.where(Content.class)
                .in("fileName", module.getContents().stream().map(Content::getFileName).toArray(String[]::new))
                .in("fileUrl", module.getContents().stream().map(Content::getFileUrl).toArray(String[]::new))
                .in("timeModified", module.getContents().stream().map(Content::getTimeModified).toArray(Long[]::new))
                .findAll();

        List<Content> newContents = module.getContents().stream().filter(content -> !results.contains(content))
                .map(Content::new).collect(Collectors.toList());
        if (newContents.isEmpty()) {
            return null;
        }

        module.setIsUnread(true);
        Module retModule = new Module(module);
        retModule.setContents(newContents);
        return retModule;
    }

    public List<Discussion> setForumDiscussions(int forumId, List<Discussion> discussions) {
        if (!userAccount.isLoggedIn()) {
            return null;
        }
        List<Discussion> newDiscussions = new ArrayList<>();

        for (Discussion discussion : discussions) {
            if (realm.where(Discussion.class).equalTo("id", discussion.getId()).findFirst() == null) {
                newDiscussions.add(discussion);
            }
        }
        realm.beginTransaction();
        realm.where(Discussion.class).equalTo("forumId", forumId).findAll().deleteAllFromRealm();
        realm.copyToRealm(discussions);
        realm.commitTransaction();
        return newDiscussions;

    }

    public void deleteCourse(int courseId) {
        realm.executeTransactionAsync(r -> {
            r.where(Course.class).equalTo("id", courseId).findAll().deleteAllFromRealm();
            r.where(CourseSection.class).equalTo("courseId", courseId).findAll().deleteAllFromRealm();
        });
    }

    public List<CourseSection> getCourseData(int courseId) {
        List<CourseSection> courseSections;
        courseSections = realm.copyFromRealm(realm
                .where(CourseSection.class)
                .equalTo("courseId", courseId)
                .findAll()
                .sort("id", Sort.ASCENDING));
        return courseSections;
    }

    public void markAllAsRead(List<CourseSection> courseSections) {
        realm.executeTransaction(r -> {
            for (CourseSection courseSection : courseSections) {
                for (Module module : courseSection.getModules()) {
                    markModuleAsReadOrUnread(module, false);
                }
            }
        });
    }

    public void markModuleAsReadOrUnread(Module module, boolean isUnread) {
        module.setIsUnread(isUnread);
        boolean endTransaction = false;

        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            endTransaction = true;
        }

        Module mod = realm.where(Module.class).equalTo("id", module.getId()).findFirst();
        if (mod != null) mod.setIsUnread(isUnread);

        if (endTransaction)
        {
            realm.commitTransaction();
        }
    }

    public int getUnreadCount(int courseId) {
        return realm.where(Module.class)
                .in("courseSectionId", getCourseData(courseId).stream()
                        .map(CourseSection::getCourseId)
                        .toArray(Integer[]::new)
                ).equalTo("isUnread", true)
                .findAll().size();
    }

    public String getCourseName(int courseId) {
        Course course = realm.where(Course.class).equalTo("id", courseId).findFirst();
        if (course == null) return "";
        return course.getShortName() != null ? course.getShortName() : "";
    }

    public String getCourseNameForActionBarTitle(int courseId){
        Course course = realm.where(Course.class).equalTo("id", courseId).findFirst();
        if (course == null) return "";
        return course.getCourseName()[0] + " " + course.getCourseName()[2];
    }

    /**
     * Set's the handler's Realm Instance. Note that the caller is responsible
     * for the lifecycle of the Realm instance.
     *
     * @param realm A Realm instance that is tied to the lifecycle of the caller.
     */
    public void setRealmInstance(@NotNull Realm realm) {
        this.realm = realm;
    }
}
