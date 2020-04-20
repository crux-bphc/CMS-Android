package crux.bphc.cms.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {

    private static MyApplication mInstance;
    private Realm realm;
    private boolean isDarkMode = false;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Realm.init(this);
        realm = Realm.getInstance(getRealmConfiguration());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.isDarkMode = prefs.getBoolean(Constants.DARK_MODE_KEY,false);
    }

    public boolean isDarkModeEnabled() {
        return this.isDarkMode;
    }

    public void setDarkModeEnabled(boolean isEnabled) {
        this.isDarkMode = isEnabled;
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefEdit.putBoolean(Constants.DARK_MODE_KEY, this.isDarkMode);
        prefEdit.apply();
    }

    public HashMap<String, String> getLoginLaunchData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String data = prefs.getString(Constants.LOGIN_LAUNCH_DATA, "");
        return new Gson().fromJson(data, HashMap.class);
    }

    public void setLoginLaunchData(HashMap<String, String> launchData) {
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefEdit.putString(Constants.LOGIN_LAUNCH_DATA, new Gson().toJson(launchData));
        prefEdit.apply();
    }

    public static RealmConfiguration getRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!realm.isClosed()) realm.close();
    }

    public Realm getRealmInstance() {
        return realm;
    }
}