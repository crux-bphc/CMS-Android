package crux.bphc.cms.background

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import crux.bphc.cms.core.PushNotifRegManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        CoroutineScope(Dispatchers.Default).launch {
            // registerDevice() will automatically register based
            // on user preferences etc.
            PushNotifRegManager.registerDevice()
        }
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
