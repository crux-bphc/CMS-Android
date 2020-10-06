package crux.bphc.cms.models.enrol

import com.google.gson.annotations.SerializedName

/**
 * Model class to represent the response from
 * [crux.bphc.cms.network.MoodleServices.selfEnrolUserInCourse].
 *
 * @author Siddhant Kumar Patel (19-Dec-2016)
 */
data class SelfEnrol(
        @SerializedName("status") val status:Boolean = false,
)
