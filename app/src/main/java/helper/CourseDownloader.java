package helper;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import app.Constants;
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

public class CourseDownloader {

    private Realm realm;
    private Context context;

    public CourseDownloader(Context context) {
        this.context = context;
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
    }

    public void downloadCourseData(final int courseId, final DownloadCallback downloadCallback) {
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
                    downloadCallback.onFailure();
                    return;
                }

                final RealmResults<CourseSection> results = realm.where(CourseSection.class).equalTo("courseID", courseId).findAll();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
                        for (CourseSection section : sectionList) {
                            section.setCourseID(courseId);
                            realm.copyToRealmOrUpdate(section);
                        }
                        downloadCallback.onSuccess(sectionList);
                    }
                });


            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                downloadCallback.onFailure();
            }
        });
    }

    public void downloadSection(CourseSection section, DownloadCallback downloadCallback) {
        List<Module> modules = section.getModules();
        for (Module module : modules) {
            if (module.getContents().size() == 0) {
                continue;
            }
            for (Content content : module.getContents()) {
                if (!searchFile(content.getFilename())) {
                    downloadFile(content, module);
                    downloadCallback.onSuccess(new DownloadReq(-1, module.getId(), content.getFilename()));
                }
            }
        }
        downloadCallback.onFailure();
    }

    private void downloadFile(Content content, Module module) {
        String url = content.getFileurl() + "&token=" + Constants.TOKEN;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(module.getModname());
        request.setTitle(content.getFilename());

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, content.getFilename());
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        manager.enqueue(request);
    }

    public boolean searchFile(String fileName) {
        File direc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        List<String> fileList = Arrays.asList(direc.list());
        if (fileList.size() == 0) {
            return false;
        }

        if (fileList.contains(fileName)) {
            Log.d("File found:", fileName);
            return true;
        }
        return false;
    }

    public interface DownloadCallback {
        void onSuccess(Object object);

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
