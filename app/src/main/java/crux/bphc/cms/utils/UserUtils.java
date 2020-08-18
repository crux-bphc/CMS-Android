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
 * A utility class to deal with User state.
 *
 * @author Harshit Agarwal
 */

public class UserUtils {
    private static final String TAG = UserUtils.class.getName();

    private UserUtils() {}

    /**
     * Logs the user out by deleting the associated UserAccount and deletes
     * all data in Realm.
     */
    public static void logout(Context context) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(r -> r.deleteAll());
        UserAccount userAccount = new UserAccount(context);
        userAccount.logout();
    }

    /**
     * Check if token passed in is valid or not by making a network request.
     * @return True if valid token, else false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) return false;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<UserDetail> call = moodleServices.fetchUserDetail(token);
        try {
            Response<UserDetail> tokenResponse = call.execute();
            if (!tokenResponse.isSuccessful()) {
                return false;
            } else {
                // Moodle returns 200 OK for everything
                UserDetail user;
                if ((user = tokenResponse.body()) == null || user.getUsername() == null) {
                    return false;
                }
            }
        } catch (IOException e) {
            Log.wtf(TAG, e);
            return false;
        }
        return true;
    }

    /**
     * Clear the backstack, and launch {@link TokenActivity}.
     */
    public static void clearBackStackAndLaunchTokenActivity(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).finishAffinity();
            context.startActivity(new Intent(context, TokenActivity.class));
            Toast.makeText(context, "Please re-login to continue.", Toast.LENGTH_SHORT).show();
        }
    }
}
