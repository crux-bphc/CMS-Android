package crux.bphc.cms.models.forum

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Model class to represent a Discussion. A Discussion may be the initial post
 * or a reply to a post. The lack of parent signifies it as the initial post.
 * Discussions can also be infinitely nested. However, this class only represents
 * the root discussion i.e the thread
 *
 * @author Siddhant Kumar Patel (17-Jan-2017)
 * @author Abhijeet Viswa
 */
open class Discussion(
        /**
         * The ID of this object. Serves as a PK and nothing more.
         */
        @PrimaryKey @SerializedName("id") var id: Int = 0,

        /**
         * Discussion of the ID. Uniquely identifies a thread of discussions. Used
         * when querying the replies in a thread.
         */
        @SerializedName("discussion")  var discussionId:Int = 0,

        /**
         * The name i.e subject of the thread.
         */
        @SerializedName("name")  var name: String = "",

        /**
         * Unix epoch when this was last modified
         */
        @SerializedName("timemodified") var timeModified: Int = 0,

        /**
         * The parent of this discussion. Equals the `id` if a parent
         * exists. Else, equals to 0.
         */
        @SerializedName("parent")  var parent:Int = 0,

        subject: String = "" ,

        /**
         * The content of this discussion.
         */
        @SerializedName("message") var message: String = "",

        /**
         * List of attachments of this post
         */
        @SerializedName("attachments") var attachments: RealmList<Attachment> = RealmList(),

        /**
         * Name of the user who made this discussion.
         */
        @SerializedName("userfullname") var userFullName: String = "",

        /**
         * Url to the user's profile picture
         */
        @SerializedName("userpictureurl") var userPictureUrl: String = "",

        /**
         * If the root discussion i.e the thread has been pinned or not.
         */
        @SerializedName("pinned") var isPinned:Boolean = false,

        /**
         * The id of the Forum instance that the Discussion is a part of. This is not
         * a part of the [crux.bphc.cms.network.MoodleServices.getForumDiscussions].
         */
        var forumId:Int = 0,
) : RealmObject() {

    var subject: String = subject
        get() {
            return HtmlCompat.fromHtml(field, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    .trim { it < ' ' }
        }
        private set
}