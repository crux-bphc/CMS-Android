package crux.bphc.cms;


import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuItem;
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

    boolean themeChanged;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("themeChanged")) {
            themeChanged = savedInstanceState.getBoolean("themeChanged");
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
            this.themeChanged = !themeChanged; // If we go light -> dark -> light, we don't need to retheme
            recreate();
        });
    }

    @Override
    public void onBackPressed() {
        if (themeChanged) {
            onNavigateUp();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putBoolean("themeChanged", themeChanged);
        super.onSaveInstanceState(outState);
    }

}
