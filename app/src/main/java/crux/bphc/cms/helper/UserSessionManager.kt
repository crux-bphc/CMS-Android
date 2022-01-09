package crux.bphc.cms.helper

import android.util.Log
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.network.APIClient
import crux.bphc.cms.network.MoodleServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object UserSessionManager {
    /**
     * Create an HTTP session using a private token.
     */
    suspend fun createUserSession(): Boolean {
        val token = UserAccount.token
        val privateToken = UserAccount.privateToken
        if (privateToken.isEmpty()) {
            return false;
        }

        val retrofit = APIClient.getRetrofitInstance(false)
        val moodleServices = retrofit.create(MoodleServices::class.java)

        /*
         * We first use the private token to get an autologin key. This key
         * then can be used to generate a session cookie.
         */
        val detail = withContext(Dispatchers.IO) {
            val call = moodleServices.autoLoginGetKey(token, privateToken) ?: return@withContext null
            try {
                val response = call.execute()
                if (!response.isSuccessful()) {
                    return@withContext null
                }

                return@withContext response.body()
            } catch (e: IOException) {
                Log.e("UserAccount", "IOException when fetching autologin key", e)
                return@withContext null
            }
        } ?: return false

        return withContext(Dispatchers.IO) {
            val call = moodleServices.autoLoginWithKey(detail.autoLoginUrl, UserAccount.userID,
                detail.key) ?: return@withContext false

            try {
                val response = call.execute()
                if (!response.raw().isRedirect) {
                    return@withContext false
                }

                /*
                 * The server responds with 'Set-Cookie' headers for each
                 * cookie it wants to set. Attributes are separated by ;.
                 * The first attribute is the 'cookie-name=value' pair
                 * we require.
                 */
                val cookies = response.raw().headers("Set-Cookie") ?: return@withContext false
                for (cookie in cookies) {
                    val kv = cookie.trim().split(";")[0].split("=")
                    if (kv[0] != "MoodleSession") {
                        continue
                    }

                    UserAccount.sessionCookie = kv[1]
                    UserAccount.sessionCookieGenEpoch = System.currentTimeMillis() / 1000;
                    return@withContext true
                }
                return@withContext false
            } catch(e: IOException) {
                Log.e("UserAccount", "IOException when attempting to autologin", e)
                return@withContext false
            }
        }
    }

    fun getFormattedSessionCookie(): String {
        return "MoodleSession=${UserAccount.sessionCookie}"
    }

    fun hasValidSession(): Boolean {
        /*
         * We have no real way of determining if the session is still valid.
         * Futher more, Moodle rate limits autologin to 6 minutes. We simply
         * assume a session generated more than 6 minutes ago is invalid.
         * A less naive implementation could perform an actual HTTP request
         * and determine if the session is valid or not.
         */
        return UserAccount.sessionCookieGenEpoch + (6 * 60) > System.currentTimeMillis() / 1000;
    }
}