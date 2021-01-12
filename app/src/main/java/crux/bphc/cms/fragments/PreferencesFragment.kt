package crux.bphc.cms.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import crux.bphc.cms.R
import crux.bphc.cms.core.PushNotifRegManager
import crux.bphc.cms.models.UserAccount
import kotlinx.coroutines.launch

class PreferencesFragment : PreferenceFragmentCompat() {

    var themeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserAccount.isDarkModeEnabled) {
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
            isChecked = UserAccount.isDarkModeEnabled
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener{ _: Preference?, o: Any? ->
                    themeChanged = true
                    UserAccount.isDarkModeEnabled = o as Boolean
                    requireActivity().recreate()
                    true
                }
        }

        val notifications: SwitchPreference? = findPreference("notifications")
        notifications?.apply {
            lifecycleScope.launch {
                isChecked = PushNotifRegManager.isRegistered()
            }
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                    val newlyChecked = o as Boolean
                    isChecked = newlyChecked
                    // This ensures MainActivity will reattempt
                    // (de)registration in case coroutine gets
                    // cancelled or network call fails
                    UserAccount.isNotificationsEnabled = newlyChecked

                    lifecycleScope.launch {
                        val context = requireContext()
                        var toastResource = 0
                        if (newlyChecked) {
                            if (!PushNotifRegManager.registerDevice()) {
                                toastResource = R.string.push_notif_reg_failure
                                isChecked = false
                            }
                        } else {
                            if (!PushNotifRegManager.deregisterDevice()) {
                                toastResource = R.string.push_notif_dereg_failure
                                isChecked = true
                            }
                        }

                        if (toastResource != 0) {
                            Toast.makeText(
                                context,
                                context.getString(toastResource),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
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