@file:JvmName("NotificationHelper")
@file:JvmMultifileClass

package crux.bphc.cms.helper

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import crux.bphc.cms.R
import crux.bphc.cms.models.NotificationSet
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.widgets.HtmlTextView

const val NOTIFICATION_CHANNEL_UPDATES_BUNDLE: String = "channel_content_updates_bundle"
const val NOTIFICATION_CHANNEL_UPDATES: String = "channel_content_updates"

fun pushCourseSectionNotif(context: Context, notifManager: NotificationManager,
                           section: CourseSection, course: Course) {
    for (module in section.modules) {
        pushModuleNotif(context, notifManager, module, section, course)
    }
}

fun pushModuleNotif(context: Context, notifManager: NotificationManager, module: Module,
                    section: CourseSection, course:Course) {
   createNotifFromSet(context, notifManager,
           NotificationSet.createNotificationSetForModule(context, course, section, module))
}

fun pushDiscussionNotif(context: Context, notifManager: NotificationManager,
                        discussion: Discussion, module: Module, course: Course) {
      createNotifFromSet(context, notifManager,
        NotificationSet.createNotificationSetForDiscussion(context, course, module, discussion))
}

fun pushSiteNewsNotif(context: Context, notifManager: NotificationManager, discussion: Discussion) {
    createNotifFromSet(context, notifManager,
        NotificationSet.createNotificationSetForSiteNews(context, discussion))
}

fun createLoggedOutNotif(context: Context, notifManager: NotificationManager) {
    val notifSet = NotificationSet(
            System.currentTimeMillis().toInt(),
            System.currentTimeMillis().toInt(),
            context.getString(R.string.logout_notif_title),
            context.getString(R.string.logout_notif_content),
            null,
            null
    )
    createNotifFromSet(context, notifManager, notifSet)
}

fun createNotifFromSet(context: Context, notifManager: NotificationManager,
                       notifSet: NotificationSet) {
    // channel ID is ignored for below Oreo
    val mBuilder = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_bits_logo)
            .setGroup(notifSet.groupKey)
            .setGroupSummary(false)
            .setAutoCancel(true)
            .setContentIntent(notifSet.pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // Build the summary notification for Noutgat+ devices
        val groupBuilder = NotificationCompat
                .Builder(context, NOTIFICATION_CHANNEL_UPDATES_BUNDLE)
                .setSmallIcon(R.drawable.ic_bits_logo)
                .setContentText(HtmlTextView.parseHtml(notifSet.groupKey))
                .setStyle(NotificationCompat.InboxStyle()
                        .setBigContentTitle(notifSet.groupKey)
                        .setSummaryText(notifSet.groupKey))
                .setGroup(notifSet.groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(notifSet.pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
        notifManager.notify(notifSet.bundleId, groupBuilder.build())

        mBuilder.setContentTitle(notifSet.notifTitle)
                .setContentText(notifSet.notifContent)
                .setStyle(NotificationCompat.BigTextStyle()
                        .setSummaryText(notifSet.groupKey)
                        .bigText(notifSet.notifContent))
    } else {
        mBuilder.setContentTitle(notifSet.groupKey)
                .setContentText(notifSet.notifContent)
    }
    notifManager.notify(notifSet.uniqueId, mBuilder.build())
}
