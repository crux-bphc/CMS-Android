package crux.bphc.cms.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.activities.MainActivity
import crux.bphc.cms.activities.TokenActivity
import crux.bphc.cms.app.Urls
import crux.bphc.cms.databinding.FragmentMoreBinding
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.utils.UserUtils
import crux.bphc.cms.utils.Utils

class MoreFragment : Fragment() {
    private lateinit var binding: FragmentMoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMoreBinding.inflate(layoutInflater)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = "More"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.websiteCard.setOnClickListener {
            Utils.openURLInBrowser(requireActivity(), Urls.MOODLE_URL.toString())
        }

        binding.shareCard.setOnClickListener {
            val appPackageName = BuildConfig.APPLICATION_ID
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Check out the CMS App: https://play.google.com/store/apps/details?id=$appPackageName")
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        }

        binding.issueCard.setOnClickListener {
            Utils.openURLInBrowser(requireActivity(), Urls.getFeedbackURL(
                UserAccount.firstName,
                UserAccount.username
            ))
        }

        binding.aboutCard.setOnClickListener {
            pushView(InfoFragment(), "info")
        }

        binding.settingsCard.setOnClickListener {
            pushView(PreferencesFragment(), "settings")
        }

        binding.logoutCard.setOnClickListener {
            askToLogout().show()
        }

        val details = Utils.userDetails(UserAccount.firstName, UserAccount.username)
        binding.nameText.text = details[0]
        binding.usernameText.text = details[1]

        // Set version code and commit hash
        binding.appVersionName.text = BuildConfig.VERSION_NAME
        binding.commitHash.text = BuildConfig.COMMIT_HASH
    }

    private fun pushView(fragment: Fragment, tag: String) {
        try {
            val activity = requireActivity() as MainActivity
            activity.pushView(fragment, tag, false)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to launch fragment $tag", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
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
        UserUtils.logout()
        startActivity(Intent(requireActivity(), TokenActivity::class.java))
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = MoreFragment()
    }

}