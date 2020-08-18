package crux.bphc.cms.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import crux.bphc.cms.activities.TokenActivity;
import crux.bphc.cms.helper.UserAccount;
import crux.bphc.cms.models.core.UserDetail;
import crux.bphc.cms.network.MoodleServices;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static crux.bphc.cms.app.Constants.API_URL;

/**
 * Created by Harshit Agarwal on 24-11-2017.
 */

public class UserUtils {

    public static final String TAG = UserUtils.class.getName();

    public static void logout(Context context) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(r -> r.deleteAll());
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
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<UserDetail> userDetailCall = moodleServices.fetchUserDetail(userAccount.getToken());
        try {
            Response<UserDetail> tokenResponse = userDetailCall.execute();
            if (!tokenResponse.isSuccessful()) {
                logoutAndClearBackStack(context);
            } else {
                // Moodle returns 200 OK for everything
                UserDetail user;
                if ((user = tokenResponse.body()) == null || user.getUsername() == null) {
                    logoutAndClearBackStack(context);
                }
            }
        } catch (IOException e) {
            Log.wtf(TAG, e);
            logoutAndClearBackStack(context);
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
