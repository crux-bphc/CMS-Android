package crux.bphc.cms.models.forum

import com.google.gson.annotations.SerializedName

/**
 * Model class to represent the response from
 * [crux.bphc.cms.network.MoodleServices.getForumDiscussions]
 *
 * @author Siddhant Kumar Patel (17-Jan-2017)
 * @author Abhijeet Viswa
 */
data class ForumData(
        @SerializedName("discussions") var discussions: List<Discussion> = emptyList()
)
