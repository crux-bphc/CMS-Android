package crux.bphc.cms.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import app.Constants;
import crux.bphc.cms.LoginActivity;
import crux.bphc.cms.R;
import helper.MoodleServices;
import helper.UserAccount;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;
import set.Module;

import static app.Constants.API_URL;

public class NotificationService extends IntentService {
    private static final String COURSE_GROUP = "course_group";

    public NotificationService() {
        super("NotificationService");
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("service ", "started");
        /*Handler mHandler = new Handler(getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "service running", Toast.LENGTH_SHORT).show();
            }
        });*/
        UserAccount userAccount = new UserAccount(this);
        userAccount.waitForInternetConnection(false);
        if (!userAccount.isLoggedIn()) {
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();

        final Realm realm = Realm.getInstance(config);
        List<Course> courses = realm.copyFromRealm(realm.where(Course.class).findAll());

        for (final Course course : courses) {

            Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(userAccount.getToken(), course.getId());
            try {
                Response response = courseCall.execute();
                if (response.code() == 200) {
                    List<CourseSection> sectionList = (List<CourseSection>) response.body();
                    if (sectionList == null) {
                        return;
                    }
                    for (CourseSection section : sectionList) {

                        if (realm.where(CourseSection.class).equalTo("id", section.getId()).findFirst() == null) {
                            section.setCourseID(course.getId());
                            realm.beginTransaction();
                            realm.copyToRealmOrUpdate(section);
                            realm.commitTransaction();
                            createNotifSectionAdded(section, course);
                        } else {
                            CourseSection realmSection =
                                    realm.copyFromRealm(realm.where(CourseSection.class).equalTo("id", section.getId()).findFirst());
                            for (Module module : section.getModules()) {
                                if (!realmSection.getModules().contains(module)) {
                                    createNotifModuleAdded(module, course);
                                }

                            }
                            section.setCourseID(course.getId());
                            realm.beginTransaction();
                            realm.where(CourseSection.class)
                                    .equalTo("id", section.getId())
                                    .findAll().deleteAllFromRealm();
                            realm.copyToRealmOrUpdate(section);
                            realm.commitTransaction();
                        }
                    }
                } else {
                    userAccount.waitForInternetConnection(true);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                /*Handler mHandler2 = new Handler(getMainLooper());
                mHandler2.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Network error", Toast.LENGTH_SHORT).show();
                    }
                });*/
                userAccount.waitForInternetConnection(true);
                break;
            }
        }
        realm.close();

    }

    private void createNotifSectionAdded(CourseSection section, Course course) {
        for (Module module : section.getModules()) {
            createNotifModuleAdded(module, course);
        }
    }

    private void createNotifModuleAdded(Module module, Course course) {
        //todo group notification and add pending intent to redirect to course section

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("path",Uri.parse(Constants.getCourseURL(course.getCourseId())));
//        intent.setData();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("New content in " + course.getShortname())
                        .setContentText(module.getName())
                        .setGroup(COURSE_GROUP)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.

        mNotifyMgr.notify(UserAccount.getNotifId(this), mBuilder.build());
    }

}
