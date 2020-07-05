package crux.bphc.cms.fragments;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.jetbrains.annotations.NotNull;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.helper.UserAccount;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String KEY_SHOW_SETTINGS = "showSettings";
    boolean themeChanged;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            requireActivity().setTheme(R.style.AppTheme_NoActionBar_Dark);
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

        SwitchPreference darkMode;
        if ((darkMode = findPreference("DARK_MODE")) != null) {
            darkMode.setOnPreferenceChangeListener((preference, o) -> {
                this.themeChanged = true;
                MyApplication.getInstance().setDarkModeEnabled((Boolean) o);
                requireActivity().recreate();
                return true;
            });
        }

        SwitchPreference notifications;
        if ((notifications = findPreference("notifications")) != null) {
            final UserAccount userAccount = new UserAccount(requireActivity());

            notifications.setChecked(userAccount.isNotificationsEnabled());
            notifications.setOnPreferenceChangeListener(((preference, o) -> {
                userAccount.setNotificationsEnabled((Boolean) o);
                return true;
            }));
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState){
        if (themeChanged) outState.putBoolean(KEY_SHOW_SETTINGS, true);

        super.onSaveInstanceState(outState);
    }

}
