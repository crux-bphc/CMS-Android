package app;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {
    private static MyApplication mInstance;
    private Realm realm;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Realm.init(this);
        realm = Realm.getInstance(getRealmConfiguration());
    }

    public static RealmConfiguration getRealmConfiguration(){
        return new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
    }

    public Realm getRealmInstance() {
        return realm;
    }
}