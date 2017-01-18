package crux.bphc.cms;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

import helper.UserAccount;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final SwitchCompat notificationsSwitch = (SwitchCompat) findViewById(R.id.notificationsSwitch);

        final UserAccount userAccount = new UserAccount(this);
        notificationsSwitch.setChecked(userAccount.isNotificationsEnabled());

        notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                userAccount.setNotifications(b);
            }
        });


    }
}
