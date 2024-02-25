package crux.bphc.cms.models.core

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Model class to represent the response from [crux.bphc.cms.network.MoodleServices.fetchNotifications]
 */

open class Notification(
    @PrimaryKey @SerializedName("id") var notificationId: Int = 0,
    @SerializedName("useridto") var userIdTo: Int = 0,
    @SerializedName("subject") var subject: String = "",
    @SerializedName("timecreatedpretty") var timeCreated: String = "",
    @SerializedName("smallmessage") var message: String = "",
    @SerializedName("read") var read: Boolean = false,
    @SerializedName("contexturl") var url: String? = null
): RealmObject()