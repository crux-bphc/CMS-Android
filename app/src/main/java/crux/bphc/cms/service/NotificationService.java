package crux.bphc.cms.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.Constants;
import crux.bphc.cms.LoginActivity;
import crux.bphc.cms.R;
import helper.CourseDataHandler;
import helper.CourseRequestHandler;
import helper.UserAccount;
import helper.UserUtils;
import set.Course;
import set.CourseSection;
import set.Module;
import set.NotificationSet;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

public class NotificationService extends JobService {
    private static boolean mJobRunning;
    UserAccount userAccount;
    NotificationManager mNotifyMgr;

    public static void startService(Context context, boolean replace) {

        ComponentName serviceComponent = new ComponentName(context, NotificationService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setPeriodic(TimeUnit.HOURS.toMillis(1));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // require unmetered network
        builder.setPersisted(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (!replace) {
            List<JobInfo> jobInfos = jobScheduler.getAllPendingJobs();
            for (JobInfo jobInfo : jobInfos) {
                if (jobInfo.getId() == 0) {
                    return;
                }
            }
        }

        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters job) {
        mJobRunning = true;
        runAsForeground();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mJobRunning = true;
                handleJob(job);
                stopForeground(true);
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return mJobRunning;
    }

    private void runAsForeground() {
        Intent notificationIntent = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Searching for new content")
                .setContentIntent(pendingIntent).build();

        startForeground(1, notification);

    }

    protected void handleJob(JobParameters job) {
        Log.d("service ", "started");

        userAccount = new UserAccount(this);
        if (!userAccount.isLoggedIn()) {
            JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }
            mJobRunning = false;
            jobFinished(job, false);
            return;
        }

        CourseDataHandler courseDataHandler = new CourseDataHandler(this);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(this);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        List<Course> courseList = courseRequestHandler.getCourseList();

        if (courseList == null) {
            UserUtils.checkTokenValidity(this);
            jobFinished(job, true);
            return;
        }
        courseDataHandler.setCourseList(courseList);
        List<Course> courses = courseDataHandler.getCourseList();
        List<Course> newCourses = courseDataHandler.setCourseList(courses);

        for (final Course course : courses) {
            List<CourseSection> courseSections = courseRequestHandler.getCourseData(course);
            if (courseSections == null) {
                continue;
            }
            List<CourseSection> newPartsInSection = courseDataHandler.setCourseData(course.getCourseId(), courseSections);

            if (!newCourses.contains(course) && newPartsInSection != courseSections) {//stop from generating notification if it is a new course
                for (CourseSection section : newPartsInSection)
                    createNotifSectionAdded(section, course);
            }

        }
        mJobRunning = false;
        jobFinished(job, false);
    }

    private void logout() {
        UserUtils.logout(this);
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
                            .setContentText(Html.fromHtml( notificationSet.getContentText()))
                            .setGroup(notificationSet.getGroupKey())
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);


            mNotifyMgr.notify(notificationSet.getGroupKey(), UserAccount.getNotifId(this), mBuilder.build());

            groupNotifications(notificationSet);


        }
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
