package crux.bphc.cms.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import crux.bphc.cms.R;
import crux.bphc.cms.activities.TokenActivity;
import crux.bphc.cms.helper.CourseDataHandler;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.helper.UserAccount;
import crux.bphc.cms.utils.UserUtils;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import io.realm.Realm;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;
import static crux.bphc.cms.widgets.HtmlTextView.parseHtml;

public class NotificationService extends JobService {
    private static boolean mJobRunning;
    UserAccount userAccount;
    NotificationManager mNotifyMgr;

    public static final String NOTIFICATION_CHANNEL_UPDATES_BUNDLE = "channel_content_updates_bundle";
    public static final String NOTIFICATION_CHANNEL_UPDATES = "channel_content_updates";

    public static final int CMS_JOB_ID = 0;

    /**
     * Static helper method called in order to build and start the repeating job.
     * NOT called by the service itself.
     */
    public static void startService(Context context, boolean replace) {

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

        // Pass our job to the JobScheduler in order to queue it.
        jobScheduler.schedule(builder.build());
    }

    /**
     * The method that is called when the Job executes; called on the Main thread by default.
     */
    @Override
    public boolean onStartJob(final JobParameters job) {
        mJobRunning = true;

        // Call our course update operation on a different thread
        AsyncTask.execute(() -> {
            mJobRunning = true;
            handleJob(job);
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
     * <p>
     * This rescheduling is separate from any periodic conditions specified when building the Job,
     * and improper handling would cause unnecessary repeats.
     * Default rescheduling strategy should be exponential backoff.
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        return mJobRunning;
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
            jobFinished(job, false);
            mJobRunning = false;
            return;
        }

        Realm realm = Realm.getDefaultInstance();
        CourseDataHandler courseDataHandler = new CourseDataHandler(this, realm);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(this);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // fetches list of enrolled courses from server
        List<Course> courseList = courseRequestHandler.getCourseList((Context) null);

        if (courseList == null) {
            if (!UserUtils.isValidToken(userAccount.getToken())) {
                UserUtils.logout(this);
            }
            jobFinished(job, true); // TODO is this reschedule needed
            realm.close();
            return;
        }

        // replace the list of courses in db, and get new inserts
        List<Course> newCourses = courseDataHandler.isolateNewCourses(courseList);
        courseDataHandler.replaceCourses(courseList);

        for (final Course course : courseList) {
            List<CourseSection> courseSections = courseRequestHandler.getCourseData(course);

            if (courseSections == null) {
                continue;
            }

            // update the sections of the course, and get new parts
            // Since new course notifications are skipped, default modules like "Announcements" will not get a notif
            List<CourseSection> newPartsInSection = courseDataHandler.isolateNewCourseData(course.getCourseId(),
                    courseSections);
            courseDataHandler.replaceCourseData(course.getCourseId(), courseSections);

            // Generate notifications only if it is not a new course
            if (!newCourses.contains(course)) {
                for (CourseSection courseSection : courseSections) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == Module.Type.FORUM) {
                            List<Discussion> discussions = courseRequestHandler.getForumDiscussions(module.getInstance());

                            if (discussions == null) continue;
                            for (Discussion d : discussions) {
                                d.setForumId(module.getInstance());
                            }
                            List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(module.getInstance(), discussions);
                            if (newDiscussions.size() > 0)  courseDataHandler.markModuleAsReadOrUnread(module, true);
                            for (Discussion discussion : newDiscussions) {
                                createNotifModuleAdded(NotificationSet.createNotificationSet(course, module, discussion));
                            }
                        }
                    }
                }
                for (CourseSection section : newPartsInSection)
                    createNotifSectionAdded(section, course);
            }
        }

        // Create notifications for site news
        List<Discussion> discussions = courseRequestHandler.getForumDiscussions(1); // 1 is always site news
        if (discussions != null) {
            for (Discussion d : discussions) {
                d.setForumId(1);
            }
            List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(1, discussions);
            for (Discussion discussion : newDiscussions) {
                createNotifModuleAdded(new NotificationSet(discussion.getId(), 1, "Site News", discussion.getMessage(), "Site News"));
            }
        }

        realm.close();
        mJobRunning = false;
        jobFinished(job, false);
    }

    private void createNotifSectionAdded(CourseSection section, Course course) {
        for (Module module : section.getModules()) {
            createNotifModuleAdded(NotificationSet.createNotificationSet(course, section, module));
        }
    }

    private void createNotifModuleAdded(NotificationSet notificationSet) {

        if (userAccount.isNotificationsEnabled()) {

            Intent intent = new Intent(this, TokenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("courseId", notificationSet.getBundleID());
            intent.putExtra("modId", notificationSet.getUniqueId()); // This can be a module or a discussion id, and will be resolved later
            PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPDATES_BUNDLE)
                    .setSmallIcon(R.drawable.ic_bits_logo)
                    .setContentText(parseHtml(notificationSet.getNotifSummary()))
                    .setStyle(new NotificationCompat.InboxStyle()
                            .setBigContentTitle(parseHtml(notificationSet.getNotifSummary()))
                            .setSummaryText(parseHtml(notificationSet.getNotifSummary())))
                    .setGroup(notificationSet.getGroupKey())
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(PRIORITY_DEFAULT)
                    .setOnlyAlertOnce(true);

            // channel ID is ignored for below Oreo
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPDATES)
                            .setSmallIcon(R.drawable.ic_bits_logo)
                            .setGroup(notificationSet.getGroupKey())
                            .setGroupSummary(false)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBuilder.setContentTitle(parseHtml(notificationSet.getNotifTitle()))
                        .setContentText(parseHtml(notificationSet.getNotifContent()))
                        .setStyle(new NotificationCompat.InboxStyle()
                                .setSummaryText(parseHtml(notificationSet.getNotifSummary()))
                                .addLine(parseHtml(notificationSet.getNotifContent())));
                // Notify the summary notification for post nougat devices only
                mNotifyMgr.notify(notificationSet.getBundleID(), groupBuilder.build());
            } else {
                mBuilder.setContentTitle(parseHtml(notificationSet.getNotifSummary()))
                        .setContentText(parseHtml(notificationSet.getNotifContent()));
            }
            mNotifyMgr.notify(notificationSet.getUniqueId(), mBuilder.build());
        }
    }


    /**
     * A data class for Notification data
     *
     * @author Harshit Agarwal (18-01-2017)
     * @author Abhijeet Viswa (05-July-2017)
     */
    private static class NotificationSet {

        private final int uniqueId;
        private final int bundleId;
        private final String notifSummary;
        private final String notifTitle;
        private final String notifContent;

        public NotificationSet(int uniqueId, int bundleId, String notifTitle, String notifContent, String notifSummary) {
            this.uniqueId = uniqueId;
            this.bundleId = bundleId;
            this.notifTitle = notifTitle;
            this.notifContent = notifContent;
            this.notifSummary = notifSummary;
        }

        /**
         *  Helper method to create NotificationSet object for a new module
         */
        public static NotificationSet createNotificationSet(Course course, CourseSection section, Module module) {
            return new NotificationSet(module.getId(), course.getId(), section.getName(), module.getName(),
                    course.getShortName());
        }

        /**
         *  Helper method to create NotificationSet object for a new discussion
         */
        public static NotificationSet createNotificationSet(Course course, Module module, Discussion discussion) {
            return new NotificationSet(discussion.getId(), course.getId(), module.getName(), discussion.getMessage(),
                    course.getShortName());
        }

        public int getUniqueId() {
            return uniqueId;
        }

        public int getBundleID() {
            return bundleId;
        }

        public String getNotifSummary() {
            return notifSummary;
        }

        public String getNotifTitle() {
            return notifTitle;
        }

        public String getNotifContent() {
            return notifContent;
        }

        public String getGroupKey() {
            return notifSummary;

        }

    }
}
