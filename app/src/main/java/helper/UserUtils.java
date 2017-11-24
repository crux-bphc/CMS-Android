package helper;

import android.content.Context;

import app.MyApplication;
import io.realm.Realm;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class UserUtils {
    public static void logout(Context context) {
        Realm realm = MyApplication.getInstance().getRealmInstance();
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
        realm.close();
        UserAccount userAccount = new UserAccount(context);
        userAccount.logout();
    }

    public static void checkTokenValidity() {
        //todo implement token validity call, and auto logout logic implementation
    }
}
