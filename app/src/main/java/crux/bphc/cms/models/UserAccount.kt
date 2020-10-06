package crux.bphc.cms.models

import android.content.Context
import crux.bphc.cms.models.core.UserDetail

/**
 * @author Harshit Agarwal (16-Dec-2016)
 * @author Abhijeet Viswa
 */
class UserAccount(context: Context) {

    private val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)

    val userID: Int
        get() = prefs.getInt("userid", 0)

    val token: String
        get() = prefs.getString("token", "")?: ""

    val username: String
        get() = prefs.getString("username", "")?: ""

    val firstName: String
        get() = prefs.getString("firstname", "")?: ""

    val isLoggedIn: Boolean
        get() = prefs.getString("token", "")?.isNotEmpty() ?: false

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun setUser(userDetail: UserDetail) {
        prefs.edit()
                .putString("username", userDetail.username)
                .putString("token", userDetail.token)
                .putString("firstname", userDetail.firstName)
                .putString("lastname", userDetail.lastName)
                .putString("userpictureurl", userDetail.userPictureUrl)
                .putInt("userid", userDetail.userId)
                .apply()
    }

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean("notificationEnable", true)
        set(b) {
            prefs.edit()
                    .putBoolean("notificationEnable", b)
                    .apply()
        }

    companion object {
        private const val MY_PREFS_NAME = "CMS.userAccount3"
    }

}
