package crux.bphc.cms.models.core

import com.google.gson.annotations.SerializedName

/**
 * Model class to represent the response from [crux.bphc.cms.network.MoodleServices.fetchUserDetail]
 * @author Abhijeet Viswa
 */
data class UserDetail (
    @SerializedName("username") val username: String = "",
    @SerializedName("firstname")  val firstName: String = "",
    @SerializedName("lastname") val lastName: String = "",
    @SerializedName("userpictureurl") val userPictureUrl: String = "",
    @SerializedName("userid") val userId: Int = 0,
    var token: String = "",
)
