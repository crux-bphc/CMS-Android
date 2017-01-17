package crux.bphc.cms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import app.Constants;
import helper.UserAccount;

public class NetworkStateReceiver extends BroadcastReceiver {
    public static boolean hasActiveInternetConnection() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL(Constants.API_URL).openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
        } catch (IOException e) {

        }
        return false;
    }

    public void onReceive(final Context context, Intent intent) {
        UserAccount userAccount = new UserAccount(context);
        if (userAccount.isLoggedIn() && userAccount.waitForInternetConnection()) {
            if (intent.getExtras() != null) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = networkInfo != null &&
                        networkInfo.isConnectedOrConnecting();
                if (isConnected) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (hasActiveInternetConnection())
                                NotificationService.startService(context);
                        }
                    });
                    return;
                }

            }
        }
    }
}