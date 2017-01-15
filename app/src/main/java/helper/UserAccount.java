package helper;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by harsu on 16-12-2016.
 */

public class UserAccount {

    private static final String MY_PREFS_NAME = "CMS.userAccount";
    private Context context;

    public UserAccount(Context context) {
        this.context = context;
    }


    public boolean isLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return !(prefs.getString("token", "").isEmpty());
    }

    public void setUser(UserDetail userDetail) {
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("username", userDetail.getUsername());
        editor.putString("token", userDetail.getToken());
        editor.putString("firstname", userDetail.getFirstname());
        editor.putString("lastname", userDetail.getLastname());
        editor.putString("userpictureurl", userDetail.getUserpictureurl());
        editor.putInt("userid", userDetail.getUserid());
        editor.commit();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    public String getUsername() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("username", "");

    }

    public String getFirstName() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("firstname", "");

    }

    public int getUserID() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("userid", 0);

    }

    public void logout() {
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public void waitForInternetConnection(boolean b) {
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("internet", b);
        editor.commit();
    }

    public boolean waitForInternetConnection() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean("internet", false);

    }
}
