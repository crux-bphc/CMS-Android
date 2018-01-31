package helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;

import app.MyApplication;
import crux.bphc.cms.LoginActivity;
import crux.bphc.cms.TokenActivity;
import io.realm.Realm;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static app.Constants.API_URL;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class UserUtils {
    public static void logout(Context context) {
        Realm realm = MyApplication.getInstance().getRealmInstance();
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
        UserAccount userAccount = new UserAccount(context);
        userAccount.logout();
    }

    /**
     * Synchronous call to server
     *
     * @param context
     */
    public static void checkTokenValidity(Context context) {

        UserAccount userAccount = new UserAccount(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        LoginActivity.MoodleLogin moodleLogin = retrofit.create(LoginActivity.MoodleLogin.class);

        Call<ResponseBody> userDetailCall = moodleLogin.checkToken(userAccount.getToken());
        try {
            //todo check token validity checker logic
            //auto logout logic, finish current activity. clear back stack. start login activity

            Response<ResponseBody> tokenResponse = userDetailCall.execute();
            if (tokenResponse.code() == 500 || tokenResponse.code() == 404) {
                logoutAndClearBackStack(context);
                return;
            }
            if (tokenResponse.code() != 200) {
                return;
            }
            if (tokenResponse.body() == null || tokenResponse.body().string() == null || tokenResponse.body().string().contains("Invalid token")) {
                logoutAndClearBackStack(context);
                return;
            }

        } catch (IOException e) {
        }
    }

    public static void logoutAndClearBackStack(Context context) {

        logout(context);
        if (context instanceof Activity) {
            ((Activity) context).finishAffinity();
            context.startActivity(new Intent(context, TokenActivity.class));
            Toast.makeText(context, "Please re-login to continue.", Toast.LENGTH_SHORT).show();
        }
    }
}
