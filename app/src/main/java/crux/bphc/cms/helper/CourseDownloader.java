package crux.bphc.cms.helper;

import android.app.Activity;
import android.content.Context;

import java.util.List;

import crux.bphc.cms.io.FileManager;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * @author Harshit Agarwal
 */

public class CourseDownloader implements FileManager.Callback {
    private DownloadCallback downloadCallback;
    private final FileManager fileManager;
    private final Realm realm;
    private final Context context;

    public CourseDownloader(Activity activity, String courseName) {
        this.context = activity;
        realm = Realm.getDefaultInstance();
        fileManager = new FileManager(activity, courseName);
        fileManager.registerDownloadReceiver();
        fileManager.setCallback(this);
    }

    public void setDownloadCallback(DownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    public void downloadCourseData(final int courseId) {
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(context);
        final CourseDataHandler courseDataHandler = new CourseDataHandler(context, realm);
        courseRequestHandler.getCourseData(courseId, new CourseRequestHandler.CallBack<List<CourseSection>>() {
            @Override
            public void onResponse(List<CourseSection> sectionList) {
                if (sectionList == null) {
                    if (downloadCallback != null)
                        downloadCallback.onFailure();
                    return;
                }

                courseDataHandler.replaceCourseData(courseId, sectionList);

                if (downloadCallback != null)
                    downloadCallback.onCourseDataDownloaded();
                for (CourseSection section : sectionList) {
                    downloadSection(section);
                }

            }

            @Override
            public void onFailure(String message, Throwable t) {
                if (downloadCallback != null)
                    downloadCallback.onFailure();
            }
        });
    }


    public void downloadSection(CourseSection section) {
        fileManager.reloadFileList();
        List<Module> modules = section.getModules();
        for (Module module : modules) {
            if (!module.isDownloadable())
                continue;
            for (Content content : module.getContents()) {
                if (!fileManager.isModuleContentDownloaded(content)) {
                    fileManager.downloadModuleContent(content, module);
                }
            }
        }
    }

    public int getDownloadedContentCount(int courseID) {
        fileManager.reloadFileList();
        int count = 0;
        RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseId", courseID).findAll();
        for (CourseSection section : courseSections) {
            List<Module> modules = section.getModules();
            for (Module module : modules) {
                if (module.isDownloadable())
                    for (Content content : module.getContents()) {
                        if (fileManager.isModuleContentDownloaded(content)) {
                            count++;
                        }
                    }
            }

        }
        return count;
    }

    public int getTotalContentCount(int courseID) {
        fileManager.reloadFileList();
        int count = 0;
        RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseId", courseID).findAll();
        for (CourseSection section : courseSections) {
            List<Module> modules = section.getModules();
            for (Module module : modules) {
                if (module.isDownloadable())
                    count += module.getContents().size();
            }

        }
        return count;

    }

    @Override
    public void onDownloadCompleted(String fileName) {
        if (downloadCallback != null) {
            downloadCallback.onCourseContentDownloaded();
        }
    }

    public void unregisterReceiver() {
        fileManager.unregisterDownloadReceiver();
    }


    public interface DownloadCallback {
        void onCourseDataDownloaded();

        void onCourseContentDownloaded();

        void onFailure();
    }
}
