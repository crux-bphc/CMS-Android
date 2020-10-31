package crux.bphc.cms.background

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.app.Urls
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.network.MoodleServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val retrofit = Retrofit.Builder()
                .baseUrl(Urls.MOODLE_URL.toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val moodleServices: MoodleServices = retrofit.create(MoodleServices::class.java)

        Log.d(TAG, token)

        val userAccount = UserAccount(this)
        val ret = moodleServices.registerUserDevice(
               userAccount.token,
                "crux.bphc.cms",
                Build.PRODUCT,
                Build.MODEL,
                "android-fcm",
                BuildConfig.VERSION_NAME,
                token,
                "1111-1111-1111-1111",
        ).execute()
        Log.d(TAG, ret.body()?.string() ?: "Empty Body")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "onMessageReceived called")
        Log.d(TAG, message.notification?.body ?: "No Notif Body")
        for (d in message.data) {
            Log.d(TAG, "Key: ${d.key ?: ""}, Value: ${d.value ?: ""}")
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }

}