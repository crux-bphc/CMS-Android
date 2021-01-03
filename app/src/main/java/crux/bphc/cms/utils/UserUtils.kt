package crux.bphc.cms.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import crux.bphc.cms.R
import crux.bphc.cms.activities.TokenActivity
import crux.bphc.cms.app.Urls
import crux.bphc.cms.core.PushNotifRegManager
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.network.MoodleServices
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * A utility class to deal with User state.
 *
 * @author Harshit Agarwal
 * @author Abhijeet Viswa
 */
object UserUtils {
    private val TAG = UserUtils::class.java.name

    /**
     * Logs the user out by deleting the associated UserAccount and deletes
     * all data in Realm.
     */
    fun logout() {
        val realm = Realm.getDefaultInstance()
        realm.executeTransactionAsync { r: Realm -> r.deleteAll() }

        // Deregister from push notifications before we logout
        CoroutineScope(Dispatchers.Default).launch {
            PushNotifRegManager.deregisterDevice() // If this fail, user will continue to get push notifs :')
        }
        UserAccount.clearUser()
    }

    /**
     * Check if token passed in is valid or not by making a network request.
     * @return True if valid token, else false
     */
    fun isValidToken(token: String): Boolean {
        if (token.isEmpty()) return false

        val retrofit = Retrofit.Builder()
            .baseUrl(Urls.MOODLE_URL.toString())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val moodleServices = retrofit.create(MoodleServices::class.java)
        val call = moodleServices.fetchUserDetail(token)
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                return false
            } else {
                // Moodle returns 200 OK for everything
                response.body() ?: return false
            }
        } catch (e: IOException) {
            Log.wtf(TAG, e)
            return false
        }
        return true
    }

    /**
     * Clear the backstack, and launch [TokenActivity].
     */
    fun clearBackStackAndLaunchTokenActivity(context: Context) {
        if (context is Activity) {
            context.finishAffinity()
            context.startActivity(Intent(context, TokenActivity::class.java))
            Toast.makeText(
                context,
                context.getString(R.string.please_login_to_continue),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}