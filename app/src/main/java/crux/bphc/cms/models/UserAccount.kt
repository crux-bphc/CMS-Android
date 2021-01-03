package crux.bphc.cms.models

import android.content.Context
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.models.core.UserDetail

/**
 * @author Harshit Agarwal (16-Dec-2016)
 * @author Abhijeet Viswa
 */
object UserAccount {
    private const val MY_PREFS_NAME = "crux.bphc.cms.USER_ACCOUNT"

    private val prefs = MyApplication.getInstance()
        .getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)

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

    fun clearUser() {
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

    /**
     * Denotes the user's notification preference.
     * or not. A mismatch between this and [crux.bphc.cms.core.PushNotifRegManager.isRegistered]
     * means a (de)registration is in order.
     */
    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean("notificationEnable", true)
        set(b) {
            prefs.edit()
                    .putBoolean("notificationEnable", b)
                    .apply()
        }
}
