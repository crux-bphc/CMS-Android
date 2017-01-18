package helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import app.Constants;
import crux.bphc.cms.service.NotificationService;

/**
 * Created by harsu on 16-01-2017.
 */

public class MyNotificationManager {
    public static void startNotificationServices(Context context) {
        UserAccount userAccount = new UserAccount(context);
        Intent intent = new Intent(context, NotificationService.class);
        PendingIntent pintent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
        if (userAccount.isLoggedIn() && userAccount.isNotificationsEnabled()) {
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, Constants.TRIGGER_AT, Constants.INTERVAL, pintent);
        }

    }
}
