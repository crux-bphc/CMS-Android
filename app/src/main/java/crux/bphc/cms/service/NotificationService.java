package crux.bphc.cms.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.Constants;
import crux.bphc.cms.LoginActivity;
import crux.bphc.cms.R;
import helper.MoodleServices;
import helper.UserAccount;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;
import set.Module;
import set.NotificationSet;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;
import static app.Constants.API_URL;

public class NotificationService extends IntentService {
    UserAccount userAccount;
    Realm realm;
    NotificationManager mNotifyMgr;

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

        userAccount = new UserAccount(this);
        userAccount.waitForInternetConnection(false);
        if (!userAccount.isLoggedIn()) {
            return;
        }

        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        Call<ResponseBody> myCoursesListCall = moodleServices.getCourses(userAccount.getToken(), userAccount.getUserID());

        try {
            Response<ResponseBody> courseListResp = myCoursesListCall.execute();
            String responseString = courseListResp.body().string();
            if (responseString.contains("Invalid token")) {
                logout();
                return;
            }
            Gson gson = new Gson();
            final List<Course> courses = gson
                    .fromJson(responseString, new TypeToken<List<Course>>() {
                    }.getType());

            realm.beginTransaction();
            realm.where(Course.class).findAll().deleteAllFromRealm();
            realm.copyToRealmOrUpdate(courses);
            realm.commitTransaction();

            for (final Course course : courses) {

                Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(userAccount.getToken(), course.getId());
                try {
                    Response response = courseCall.execute();
                    if (response.code() == 200) {
                        List<CourseSection> sectionList = (List<CourseSection>) response.body();
                        if (sectionList == null) {
                            return;
                        }
                        //stop from generating notification if it is a new course
                        if (realm.where(CourseSection.class).equalTo("courseID", course.getId()).findFirst() == null) {
                            for (CourseSection section : sectionList) {
                                section.setCourseID(course.getId());
                                realm.beginTransaction();
                                realm.copyToRealmOrUpdate(section);
                                realm.commitTransaction();
                            }
                        } else {        //not a new course
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
                                            createNotifModuleAdded(new NotificationSet(course, module));
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
                        }
                    } else {
                        userAccount.waitForInternetConnection(true);
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    userAccount.waitForInternetConnection(true);
                    break;
                }
            }
            realm.close();
        } catch (IOException e) {

        }

    }

    private void logout() {
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();

        userAccount.logout();
    }

    private void createNotifSectionAdded(CourseSection section, Course course) {
        for (Module module : section.getModules()) {
            createNotifModuleAdded(new NotificationSet(course, module));
        }
    }

    private void createNotifModuleAdded(NotificationSet notificationSet) {

        if (userAccount.isNotificationsEnabled()) {

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("path", Uri.parse(Constants.getCourseURL(notificationSet.getCourseID())));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("New Content in " + Html.fromHtml(notificationSet.getTitle()))
                            .setContentText(notificationSet.getContentText())
                            .setGroup(notificationSet.getGroupKey())
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);


            mNotifyMgr.notify(notificationSet.getGroupKey(), UserAccount.getNotifId(this), mBuilder.build());

            groupNotifications(notificationSet);


        }

        /*realm.beginTransaction();
        realm.copyToRealmOrUpdate(notificationSet);
        realm.commitTransaction();*/
    }


    private boolean groupNotifications(NotificationSet notificationSet) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<StatusBarNotification> groupedNotification = new ArrayList<>();
            for (StatusBarNotification statusBarNotification : mNotifyMgr.getActiveNotifications()) {
                if (statusBarNotification.getTag() != null && statusBarNotification.getTag().equalsIgnoreCase(notificationSet.getGroupKey())) {
                    groupedNotification.add(statusBarNotification);
                }
            }
            if (groupedNotification.size() > 1) {

                ArrayList<String> arrayLines = new ArrayList<>();
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
                for (StatusBarNotification activeSbn : groupedNotification) {
                    ArrayList<String> previousLines = activeSbn.getNotification().extras.getStringArrayList("lines");
                    if (previousLines == null || previousLines.isEmpty()) {
                        String stackNotificationLine = (String) activeSbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT);
                        if (stackNotificationLine != null) {
                            inbox.addLine(stackNotificationLine);
                            arrayLines.add(stackNotificationLine);
                        }
                    } else {
                        for (String string : previousLines) {
                            inbox.addLine(string);
                            arrayLines.add(string);
                        }
                    }
                }
                inbox.setSummaryText((arrayLines.size()) + " new content added");

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("path", Uri.parse(Constants.getCourseURL(notificationSet.getCourseID())));
                PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("lines", arrayLines);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                builder.setContentTitle(Html.fromHtml(notificationSet.getTitle()))
                        .setContentText((arrayLines.size()) + " new content added")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setStyle(inbox)
                        .setGroup(notificationSet.getGroupKey())
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setVisibility(VISIBILITY_PUBLIC)
                        .setContentIntent(pendingIntent)
                        .addExtras(bundle);

                for (StatusBarNotification statusBarNotification : groupedNotification)
                    mNotifyMgr.cancel(statusBarNotification.getId());
                mNotifyMgr.notify(notificationSet.getGroupKey(), groupedNotification.get(0).getId(), builder.build());

                return true;

            } else {
                return false;
            }

        } else {
            return false;
        }
    }


}
