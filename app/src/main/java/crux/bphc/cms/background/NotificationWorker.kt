package crux.bphc.cms.background

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import crux.bphc.cms.exceptions.InvalidTokenException
import crux.bphc.cms.helper.*
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.Module
import io.realm.Realm


class NotificationWorker(context: Context, workerParam: WorkerParameters):
        Worker(context, workerParam) {

    override fun doWork(): Result {
        println("test")
        Log.d(TAG, "Started syncing course data")

        // course data can't be accessed without user login, so cancel jobs if they're not logged in
        if (!UserAccount.isLoggedIn) {
            return Result.failure()
        }

        val realm = Realm.getDefaultInstance()
        val courseDataHandler = CourseDataHandler(applicationContext, realm)
        val courseRequestHandler = CourseRequestHandler(applicationContext)
        val notifManager = applicationContext.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        // fetches list of enrolled courses from server
        val courseList: MutableList<Course> = emptyList<Course>().toMutableList()
        try {
            courseRequestHandler.fetchCourseListSync().filterNotNull().forEach() {
                it -> courseList.add(it)
            }
         } catch (e: InvalidTokenException) {
            Log.e(TAG, "Invalid User Token");
            createLoggedOutNotif(applicationContext, notifManager)
            UserAccount.clearUser()
            return Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
            return Result.retry() // The next time things will be fine
        }

        // replace the list of courses in db, and get new inserts
        val newCourses = courseDataHandler.isolateNewCourses(courseList)
        courseDataHandler.replaceCourses(courseList)
        for (course in courseList) {
            val courseSections = courseRequestHandler.getCourseData(course) ?: continue

            /* Update the sections of the course, and get new parts. Since new course notifications
               are skipped, default modules like "Announcements" will not get a notif.
             */
            val newPartsInSection = courseDataHandler.isolateNewCourseData(course.id,
                    courseSections)
            courseDataHandler.replaceCourseData(course.id, courseSections)

            // Generate notifications only if it is not a new course
            if (newCourses.contains(course)) continue
            for (courseSection in courseSections) {
                val modules: List<Module> = courseSection.modules
                for (module in modules) {
                    if (module.modType === Module.Type.FORUM) {
                        val discussions = courseRequestHandler.getForumDiscussions(module.instance)
                                ?: continue
                        for (d in discussions) {
                            d.forumId = module.instance
                        }

                        val newDiscussions = courseDataHandler.setForumDiscussions(module.instance,
                                discussions)
                        if (newDiscussions.size > 0){
                            courseDataHandler.markModuleAsReadOrUnread(module, true)
                        }
                        for (discussion in newDiscussions) {
                            pushDiscussionNotif(applicationContext, notifManager, discussion,
                                    module, course)
                        }
                    }
                }
            }

        for (section in newPartsInSection)
            pushCourseSectionNotif(applicationContext, notifManager, section, course)
        }

        // Create notifications for site news
        val discussions = courseRequestHandler.getForumDiscussions(1) // 1 is always site news
        if (discussions != null) {
            for (d in discussions) {
                d.forumId = 1
            }
            val newDiscussions = courseDataHandler.setForumDiscussions(1, discussions)
            for (discussion in newDiscussions) {
                pushSiteNewsNotif(applicationContext, notifManager, discussion)
            }
        }
        realm.close()
        return Result.success()
    }

    companion object {
        const val TAG = "NotificationWorker"
    }

}


