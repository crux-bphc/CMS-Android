package crux.bphc.cms.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.Constants;
import helper.UserAccount;

public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            UserAccount userAccount = new UserAccount(context);

            /* Setting the alarm here */
            Intent intent1 = new Intent(context, NotificationService.class);
            PendingIntent pintent = PendingIntent.getService(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (!userAccount.isLoggedIn()) {
                alarm.cancel(pintent);

            } else {
                alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, Constants.TRIGGER_AT, Constants.INTERVAL, pintent);
            }
        }
    }
}