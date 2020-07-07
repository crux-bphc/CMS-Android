package crux.bphc.cms.models.forum;

import androidx.core.text.HtmlCompat;

import com.google.gson.annotations.SerializedName;

import crux.bphc.cms.network.MoodleServices;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Model class to represent a Discussion. A Discussion may be the initial post
 * or a reply to a post. The lack of parent signifies it as the initial post.
 * Discussions can also be infinitely nested. However, this class only represents
 * the root discussion i.e the thread
 *
 * @author Siddhant Kumar Patel (17-Jan-2017)
 */
public class Discussion extends RealmObject {

    /**
     * The ID of this object. Serves as a PK and nothing more.
     */
    @PrimaryKey
    @SerializedName("id") private int id;

    /**
     * Discussion of the ID. Uniquely identifies a thread of discussions. Used
     * when querying the replies in a thread.
     */
    @SuppressWarnings("unused")
    @SerializedName("discussion") private int discussionId;

    /**
     * The name i.e subject of the thread.
     */
    @SuppressWarnings("unused")
    @SerializedName("name") private String name;

    /**
     * Unix epoch when this was last modified
     */
    @SerializedName("timemodified") private int timeModified;

    /**
     * The parent of this discussion. Equals the <code>id</code> if a parent
     * exists. Else, equals to 0.
     */
    @SuppressWarnings("unused")
    @SerializedName("parent") private int parent;

    /**
     *
     * Same as <code>name</code> if <code>parent</code> is 0. If may differ,
     * eg. user supplies custom subject when replying to a discussion.
     */
    @SerializedName("subject") private String subject;

    /**
     * The content of this discussion.
     */
    @SerializedName("message") private String message;

    /**
     * List of attachments of this post
     */
    @SerializedName("attachments") private RealmList<Attachment> attachments;

    /**
     * Name of the user who made this discussion.
     */
    @SerializedName("userfullname") private String userFullName;

    /**
     * Url to the user's profile picture
     */
    @SerializedName("userpictureurl") private String userPictureUrl;

    /**
     * If the root discussion i.e the thread has been pinned or not.
     */
    @SerializedName("pinned") private boolean pinned;

    /**
     * The id of the Forum instance that the Discussion is a part of. This is not
     * a part of the {@link MoodleServices#getForumDiscussions}.
     */
    private int forumId; // TODO Move this into a separate ForumThread class.


    public Discussion() {

    }

    public int getId() {
        return id;
    }

    public int getTimeModified() {
        return timeModified;
    }

    public String getSubject() {
        return HtmlCompat.fromHtml(subject, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    public String getMessage() {
        return message;
    }

    public RealmList<Attachment> getAttachments() {
        return attachments;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public String getUserPictureUrl() {
        return userPictureUrl;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setForumId(int forumId) {
        this.forumId = forumId;
    }

    public int getForumid() { return forumId; }

}
