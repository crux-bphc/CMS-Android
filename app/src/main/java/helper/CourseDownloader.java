package helper;

import android.app.Activity;
import android.content.Context;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Content;
import set.CourseSection;
import set.Module;

import static app.Constants.API_URL;
import static app.Constants.TOKEN;

/**
 * Created by harsu on 21-12-2016.
 */

public class CourseDownloader implements MyFileManager.Callback {
    // TODO: 19-01-2017 rewrite using MyFileManager properly

    private DownloadCallback downloadCallback;
    private MyFileManager myFileManager;
    private Realm realm;
    private Context context;

    public CourseDownloader(Activity activity) {
        this.context = activity;
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        myFileManager = new MyFileManager(activity);
        myFileManager.registerDownloadReceiver();
        myFileManager.setCallback(this);
    }

    public void setDownloadCallback(DownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    public void downloadCourseData(final int courseId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(TOKEN, courseId);

        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {
                final List<CourseSection> sectionList = response.body();
                if (sectionList == null) {
                    if (downloadCallback != null)
                        downloadCallback.onFailure();
                    return;
                }


                final RealmResults<CourseSection> results = realm.where(CourseSection.class).equalTo("courseID", courseId).findAll();
                realm.beginTransaction();
                results.deleteAllFromRealm();
                for (CourseSection section : sectionList) {
                    section.setCourseID(courseId);
                    realm.copyToRealmOrUpdate(section);

                }
                realm.commitTransaction();
                if (downloadCallback != null)
                    downloadCallback.onCourseDataDownloaded();
                for (CourseSection section : sectionList) {
                    downloadSection(section, MyFileManager.getCourseName(courseId, realm));
                }

            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
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
