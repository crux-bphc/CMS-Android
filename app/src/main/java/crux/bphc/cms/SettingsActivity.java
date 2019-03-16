package crux.bphc.cms;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

import app.MyApplication;
import butterknife.BindView;
import butterknife.ButterKnife;
import helper.UserAccount;

public class SettingsActivity extends AppCompatActivity {
    @BindView(R.id.notificationsSwitch)
    SwitchCompat notificationsSwitch;
    @BindView(R.id.darkModeSwitch)
    SwitchCompat darkModeSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        final UserAccount userAccount = new UserAccount(this);
        notificationsSwitch.setChecked(userAccount.isNotificationsEnabled());

        notificationsSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            userAccount.setNotifications(isChecked);
        });

        darkModeSwitch.setChecked(MyApplication.getInstance().isDarkModeEnabled());
        darkModeSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            MyApplication.getInstance().setDarkModeEnabled(isChecked);
            recreate();
        });
    }
}
