package crux.bphc.cms.fragments;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.UserAccount;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String KEY_SHOW_SETTINGS = "showSettings";
    boolean themeChanged;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            getActivity().setTheme(R.style.AppTheme_NoActionBar_Dark);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        if(getActivity() != null) {
            getActivity().setTitle("Settings");
        }
        super.onStart();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);

        SwitchPreference darkMode = findPreference("DARK_MODE");
        darkMode.setOnPreferenceChangeListener((preference, o) -> {
            this.themeChanged = true;
            MyApplication.getInstance().setDarkModeEnabled((Boolean) o);
            getActivity().recreate();
            return true;
        });

        SwitchPreference notifications = findPreference("notifications");
        final UserAccount userAccount = new UserAccount(getActivity());

        notifications.setChecked(userAccount.isNotificationsEnabled());
        notifications.setOnPreferenceChangeListener(((preference, o) -> {
            userAccount.setNotificationsEnabled((Boolean) o);
            return true;
        }));

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        if (themeChanged) outState.putBoolean(KEY_SHOW_SETTINGS, true);

        super.onSaveInstanceState(outState);
    }

}
