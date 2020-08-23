package crux.bphc.cms.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.R
import crux.bphc.cms.activities.InfoActivity
import crux.bphc.cms.activities.MainActivity
import crux.bphc.cms.activities.TokenActivity
import crux.bphc.cms.app.Constants
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.utils.UserUtils
import kotlinx.android.synthetic.main.fragment_more.*
import crux.bphc.cms.utils.Utils
import java.lang.Exception


class MoreFragment : Fragment() {

    private lateinit var _userAccount: UserAccount

    override fun onStart() {
        super.onStart()
        requireActivity().title = "More"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _userAccount = UserAccount(requireContext())
        Constants.TOKEN = _userAccount.token

        website_card.setOnClickListener {
            Utils.openURLInBrowser(requireActivity(), Constants.API_URL + "my/")
        }

        share_card.setOnClickListener {
            val appPackageName = BuildConfig.APPLICATION_ID
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out the CMS App: https://play.google.com/store/apps/details?id=$appPackageName")
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        }

        issue_card.setOnClickListener {
            Utils.openURLInBrowser(requireActivity(), Constants.getFeedbackURL(_userAccount.firstName, _userAccount.username))
        }

        about_card.setOnClickListener {
            startActivity(Intent(requireActivity(), InfoActivity::class.java))
        }

        settings_card.setOnClickListener {
            try {
                val activity = requireActivity() as MainActivity
                activity.pushView(SettingsFragment(), "Settings", false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        logout_card.setOnClickListener {
            askToLogout().show()
        }

        val details = Utils.userDetails(_userAccount.firstName, _userAccount.username)
        name_text.text = details[0]
        username_text.text = details[1]

        // Set version code and commit hash
        app_version_name.text = BuildConfig.VERSION_NAME
        commit_hash.text = BuildConfig.COMMIT_HASH
    }

    private fun askToLogout(): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    logout()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Do nothing
                }
        return dialog.create()
    }

    private fun logout() {
        UserUtils.logout(requireContext())
        startActivity(Intent(requireActivity(), TokenActivity::class.java))
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = MoreFragment()
    }

}