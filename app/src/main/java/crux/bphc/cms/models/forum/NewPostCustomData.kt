package crux.bphc.cms.models.forum

import com.google.gson.annotations.SerializedName

/**
 * This class represents the custom data sent from Moodle
 * in a push notification.
 *
 * @author Abhijeet Viswa
 */
data class NewPostCustomData(
    @SerializedName("cmid") private val _modId: String = "",

    @SerializedName("instance") private val _instance: String = "",

    @SerializedName("discussionid") private val _discussionId: String = "",

    @SerializedName("postid") private val _postId: String = "",
) {
   /**
    * The id of the module this post belongs to
    */
   val modId: Int by lazy {
      _modId.toIntOrNull() ?: 0
   }

   /**
    * The id of the forum this post belongs to
    */
   val forumId: Int by lazy {
      _instance.toIntOrNull() ?: 0
   }

   /**
    * The id of the discussion this post belongs to
    */
   val discussionId: Int by lazy {
      _discussionId.toIntOrNull() ?: 0
   }

   /**
    * The id of the post itself
    */
   val postId: Int by lazy {
      _postId.toIntOrNull() ?: 0
   }
}
