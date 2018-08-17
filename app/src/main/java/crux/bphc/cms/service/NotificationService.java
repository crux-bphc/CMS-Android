package crux.bphc.cms.service;

import android.app.NotificationChannel;
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
import android.text.Spanned;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.Constants;
import crux.bphc.cms.LoginActivity;
import crux.bphc.cms.R;
import crux.bphc.cms.TokenActivity;
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

    public static final String NOTIFICATION_CHANNEL_SERVICE = "channel_service";
    public static final String NOTIFICATION_CHANNEL_UPDATES = "channel_content_updates";

    public static final int CMS_JOB_ID = 0;

    /**
     * Static helper method called in order to build and start the repeating job.
     * NOT called by the service itself.
     */
    public static void startService(Context context, boolean replace) {
         /*
         * Build JobInfo object. Job will run once per hour, on any type of network,
         * and persist across reboots.
         *
         * By using JobScheduler, the method `onStartJob` will be executed taking into consideration
         * Doze mode etc.
         *
         * This particular periodic job will execute exactly once within a 1 hour period,
         * but may do so at any time, not necessarily at the end of it. The exact time of execution
         * is subject to the optimizations of the Android OS based on other scheduled jobs, idle time etc.
         */
        ComponentName serviceComponent = new ComponentName(context, NotificationService.class);
        JobInfo.Builder builder = new JobInfo.Builder(CMS_JOB_ID, serviceComponent)
                .setPeriodic(TimeUnit.HOURS.toMillis(1))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true);

        // Get an instance of the system JobScheduler service.
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        /*
        * If the replace flag is false, check if the Job has already been scheduled.
        * Do nothing if it is queued, else schedule the job.
        */
        if (!replace) {
            // the null pointer warning is in case jobScheduler is null, which happens for API < 21
            List<JobInfo> jobInfos = jobScheduler.getAllPendingJobs();
            for (JobInfo jobInfo : jobInfos) {
                if (jobInfo.getId() == CMS_JOB_ID) {
                    return;
                }
            }
        }

        // Pass our job to the JobScheduler in order to queue it.
        jobScheduler.schedule(builder.build());
    }

    /**
     * The method that is called when the Job executes; called on the Main thread by default.
     */
    @Override
    public boolean onStartJob(final JobParameters job) {
        mJobRunning = true;

        runAsForeground();

        // Call our course update operation on a different thread
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mJobRunning = true;
                handleJob(job);
                stopForeground(true);
            }
        });

        /*
         * Return boolean that answers the question: "Is your program still doing work?"
         *
         * Returning true implies the wakelock needs to be held, since processing is being done
         * (usually in some other thread). If all work is completed here itself, false can be returned.
         */
        return true;
    }

    /**
     * Called if the job is interrupted in between due to change in parameters, or other factors.
     *
     * @return true if this job should be rescheduled; false if the fail can be ignored.
     *
     * This rescheduling is separate from any periodic conditions specified when building the Job,
     * and improper handling would cause unnecessary repeats.
     * Default rescheduling strategy should be exponential backoff.
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        return mJobRunning;
    }

    /**
     * Helper method that makes this a Foreground Service by passing a notification.
     */
    private void runAsForeground() {
        Intent notificationIntent = new Intent(this, TokenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SERVICE)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText("Searching for new content")
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(1, builder.build());

    }

    /**
     * Method which handles the bulk of the logic. Checks updates in each of the user's enrolled
     * courses, and accordingly creates grouped notifications.
     */
    protected void handleJob(JobParameters job) {
        Log.d("notifService ", "started");

        userAccount = new UserAccount(this);

        // course data can't be accessed without user login, so cancel jobs if they're not logged in
        if (!userAccount.isLoggedIn()) {
            JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
            }
            return;
        }

        CourseDataHandler courseDataHandler = new CourseDataHandler(this);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(this);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // fetches list of enrolled courses from server
        List<Course> courseList = courseRequestHandler.getCourseList();

        if (courseList == null) {
            UserUtils.checkTokenValidity(this);
            jobFinished(job, true); // TODO is this reschedule needed
            return;
        }

        // replace the list of courses in db, and get new inserts
        List<Course> newCourses = courseDataHandler.setCourseList(courseList);

        for (final Course course : courseList) {
            List<CourseSection> courseSections = courseRequestHandler.getCourseData(course);

            if (courseSections == null) {
                continue;
            }

            // update the sections of the course, and get new parts
            // Since new course notifications are skipped, default modules like "Announcements" will not get a notif
            List<CourseSection> newPartsInSection = courseDataHandler.setCourseData(course.getCourseId(), courseSections);

            // Generate notifications only if it is not a new course
            if (!newCourses.contains(course)) {
                for (CourseSection section : newPartsInSection)
                    createNotifSectionAdded(section, course);
            }
        }

        mJobRunning = false;
        jobFinished(job, false);
    }

    private void createNotifSectionAdded(CourseSection section, Course course) {
        for (Module module : section.getModules()) {
            createNotifModuleAdded(new NotificationSet(course, module));
        }
    }

    private void createNotifModuleAdded(NotificationSet notificationSet) {

        if (userAccount.isNotificationsEnabled()) {

            Intent intent = new Intent(this, TokenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("path", Uri.parse(Constants.getCourseURL(notificationSet.getCourseID())));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // channel ID is ignored for below Oreo
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPDATES)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("New Content in " + parseHtml(notificationSet.getTitle()))
                            .setContentText(parseHtml(notificationSet.getContentText()))
                            .setGroup(notificationSet.getGroupKey())
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


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
                        // TODO keep getting crashes around here
                        // cause "java.lang.ClassCastException: android.text.SpannableString cannot be cast to java.lang.String"
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

                Intent intent = new Intent(this, TokenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("path", Uri.parse(Constants.getCourseURL(notificationSet.getCourseID())));
                PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("lines", arrayLines);

                // channelID ignored below Oreo
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPDATES)
                                .setContentTitle(parseHtml(notificationSet.getTitle()))
                                .setContentText((arrayLines.size()) + " new content added")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setStyle(inbox)
                                .setGroup(notificationSet.getGroupKey())
                                .setGroupSummary(true)
                                .setAutoCancel(true)
                                .setVisibility(VISIBILITY_PUBLIC)
                                .setContentIntent(pendingIntent)
                                .addExtras(bundle)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

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

    // wrapper to use the correct version of Html.fromHtml method
    Spanned parseHtml(String content) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(content);
        }
    }
}
