package crux.bphc.cms.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.realm.Realm
import io.realm.RealmConfiguration

class MyApplication : Application() {
    private lateinit var sharedPref: SharedPreferences

    private val realmConfiguration: RealmConfiguration
        get() = RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .build()

    val loginLaunchData: HashMap<String, String>
        get() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val data = prefs.getString(Constants.LOGIN_LAUNCH_DATA, "")
            val type = object : TypeToken<HashMap<String, String>>(){}.type
            return Gson().fromJson(data, type)
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initRealm()
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun initRealm() {
        Realm.init(this)
        Realm.setDefaultConfiguration(realmConfiguration)
    }

    fun setLoginLaunchData(loginLaunchData: HashMap<String, String>) {
        sharedPref.edit()
            .putString(Constants.LOGIN_LAUNCH_DATA, Gson().toJson(loginLaunchData))
            .apply()
    }

    companion object {
        @get:Synchronized
        lateinit var instance: MyApplication
            private set
    }
}