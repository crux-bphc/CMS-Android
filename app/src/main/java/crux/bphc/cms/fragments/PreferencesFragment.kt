package crux.bphc.cms.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import crux.bphc.cms.R
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.models.UserAccount

class PreferencesFragment : PreferenceFragmentCompat() {

    var themeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MyApplication.getInstance().isDarkModeEnabled) {
            requireActivity().setTheme(R.style.AppTheme_NoActionBar_Dark)
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = "Settings"
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        setPreferencesFromResource(R.xml.preferences, s)

        val darkMode: SwitchPreference? = findPreference("DARK_MODE")
        darkMode?.apply {
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener{ _: Preference?, o: Any? ->
                    themeChanged = true
                    MyApplication.getInstance().isDarkModeEnabled = o as Boolean
                    requireActivity().recreate()
                    true
                }
        }

        val notifications: SwitchPreference? = findPreference("notifications")
        notifications?.apply {
            isChecked = UserAccount.isNotificationsEnabled
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                    UserAccount.isNotificationsEnabled = (o as Boolean?)!!
                    true
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (themeChanged) outState.putBoolean(KEY_SHOW_SETTINGS, true)
    }

    companion object {
        const val KEY_SHOW_SETTINGS = "showSettings"
    }
}