package crux.bphc.cms.helper;

import android.app.Activity;
import android.content.Context;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import crux.bphc.cms.models.Content;
import crux.bphc.cms.models.CourseSection;
import crux.bphc.cms.models.Module;

/**
 * Created by harsu on 21-12-2016.
 */

public class CourseDownloader implements MyFileManager.Callback {
    private DownloadCallback downloadCallback;
    private MyFileManager myFileManager;
    private Realm realm;
    private Context context;

    public CourseDownloader(Activity activity, String courseName) {
        this.context = activity;
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        myFileManager = new MyFileManager(activity, courseName);
        myFileManager.registerDownloadReceiver();
        myFileManager.setCallback(this);
    }

    public void setDownloadCallback(DownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    public void downloadCourseData(final int courseId) {

        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(context);
        final CourseDataHandler courseDataHandler = new CourseDataHandler(context);
        courseRequestHandler.getCourseData(courseId, new CourseRequestHandler.CallBack<List<CourseSection>>() {
            @Override
            public void onResponse(List<CourseSection> sectionList) {
                if (sectionList == null) {
                    if (downloadCallback != null)
                        downloadCallback.onFailure();
                    return;
                }

                courseDataHandler.setCourseData(courseId, sectionList);

                if (downloadCallback != null)
                    downloadCallback.onCourseDataDownloaded();
                for (CourseSection section : sectionList) {
                    downloadSection(section, CourseDataHandler.getCourseName(courseId));
                }

            }

            @Override
            public void onFailure(String message, Throwable t) {
                if (downloadCallback != null)
                    downloadCallback.onFailure();
            }
        });
    }


    public void downloadSection(CourseSection section, String courseName) {
        myFileManager.reloadFileList();
        List<Module> modules = section.getModules();
        for (Module module : modules) {
            if (!module.isDownloadable())
                continue;
            for (Content content : module.getContents()) {
                if (!myFileManager.searchFile(content.getFilename())) {
                    myFileManager.downloadFile(content, module, courseName);
//                    if(downloadCallback!=null)
//                    downloadCallback.onCourseContentDownloaded();// onSuccess(new DownloadReq(-1, module.getId(), content.getFilename()));
                }
            }
        }
//        if(downloadCallback!=null)
//        downloadCallback.onFailure();
    }

    public boolean searchFile(String fileName) {
        return myFileManager.searchFile(fileName);
    }

    public int getDownloadedContentCount(int courseID) {
        myFileManager.reloadFileList();
        int count = 0;
        RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseID", courseID).findAll();
        for (CourseSection section : courseSections) {
            List<Module> modules = section.getModules();
            for (Module module : modules) {
                if (module.isDownloadable())
                    for (Content content : module.getContents()) {
                        if (myFileManager.searchFile(content.getFilename())) {
                            count++;
                        }
                    }
            }

        }
        return count;
    }

    public int getTotalContentCount(int courseID) {
        myFileManager.reloadFileList();
        int count = 0;
        RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseID", courseID).findAll();
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
        myFileManager.unregisterDownloadReceiver();
    }


    public interface DownloadCallback {
        void onCourseDataDownloaded();

        void onCourseContentDownloaded();

        void onFailure();
    }

    public class DownloadReq {
        int position;
        int id;
        String fileName;

        public DownloadReq(int position, int id, String fileName) {
            this.position = position;
            this.id = id;
            this.fileName = fileName;
        }

        public int getId() {
            return id;
        }

        public String getFileName() {
            return fileName;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }
}
