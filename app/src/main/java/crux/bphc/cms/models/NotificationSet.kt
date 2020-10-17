package crux.bphc.cms.models

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import crux.bphc.cms.activities.MainActivity
import crux.bphc.cms.app.Constants
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.models.forum.Discussion

/**
 * A data class for notification data
 *
 * @author Harshit Agarwal (18-Jan-2017)
 * @author Abhijeet Viswa
 */
data class NotificationSet(
        val uniqueId: Int,
        val bundleId: Int,
        val notifTitle: String,
        val notifContent: String,
        val groupKey: String?,
        val pendingIntent: PendingIntent?
) {
    companion object {
        /**
         * Helper method to create [NotificationSet] object for a new module
         */
        fun createNotificationSetForModule(context: Context, course: Course, section: CourseSection,
                                           module: Module)
                : NotificationSet {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("courseId", course.id)
            intent.putExtra("modId", module.id)

            val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationSet(module.id, course.id, section.name, module.name,
                    course.shortName, pendingIntent)
        }

        /**
         * Helper method to create [NotificationSet] object for a new discussion
         */
        fun createNotificationSetForDiscussion(context: Context, course: Course, module: Module,
                                               discussion: Discussion)
                : NotificationSet {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("courseId", course.id)
            intent.putExtra("modId", module.id)
            intent.putExtra("forumId", module.instance)
            intent.putExtra("discussionId", discussion.id)

            val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationSet(discussion.id, course.id, module.name, discussion.message,
                    course.shortName, pendingIntent)
        }

        fun createNotificationSetForSiteNews(context: Context, discussion: Discussion)
               : NotificationSet {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("courseId", Constants.SITE_NEWS_COURSE_ID)
            intent.putExtra("forumId", 1)
            intent.putExtra("discussionId", discussion.id)

            val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationSet(1, 0, "Site News", discussion.message,
                    "Site News", pendingIntent)
        }
    }
}

