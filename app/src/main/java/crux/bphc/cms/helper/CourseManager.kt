package crux.bphc.cms.helper

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import crux.bphc.cms.models.course.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CourseManager(val courseId: Int, val courseRequestHandler: CourseRequestHandler) {

    var startedUnenrol: Boolean = false

    @UiThread
    suspend fun unenrolCourse(context: Context): Boolean {
        /* Guard against multiple presses */
        if (startedUnenrol) return false
        startedUnenrol = true

        if (!UserSessionManager.hasValidSession()) {
            if (!UserSessionManager.createUserSession()) {
                Toast.makeText(
                    context,
                    "Failed to create session. Try logging out and back in!",
                    Toast.LENGTH_LONG
                ).show()
                startedUnenrol = false
                return false
            }
        }

        val ret = withContext(Dispatchers.IO) {
            val idsess = courseRequestHandler.getEnrolIdSessKey(courseId) ?:
                return@withContext false
            val enrolId = idsess.first ?: return@withContext false
            val sessKey = idsess.second ?: return@withContext false

            courseRequestHandler.unenrolSelf(enrolId, sessKey)
        }

        if (ret) {
            Toast.makeText(context, "Successfully unenroled from course!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to unenrol from course!", Toast.LENGTH_LONG ).show()
        }

        startedUnenrol = false
        return ret
    }

}