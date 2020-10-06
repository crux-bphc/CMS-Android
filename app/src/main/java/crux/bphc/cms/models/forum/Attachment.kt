package crux.bphc.cms.models.forum

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject

/**
 * Model class to represent attachment of Discussions.
 *
 * @author Siddhant Kumar Patel (01-Jul-2016)
 * @author Abhijeet Viswa
 */
open class Attachment(
        @SerializedName("filename") var fileName: String = "",
        @SerializedName("mimetype") var mimeType: String = "",
        @SerializedName("fileurl") var fileUrl: String = "",
        @SerializedName("filesize") var fileSize: Int = 0,
        @SerializedName("timemodified") var timeModified: Long = 0,
) : RealmObject()

