package crux.bphc.cms.activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import crux.bphc.cms.R
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.app.Urls
import crux.bphc.cms.app.appendOrSetQueryParameter
import crux.bphc.cms.models.UserAccount
import java.util.*

class TokenActivity : AppCompatActivity() {

    private val viewModel by viewModels<TokenViewModel>()

//    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // reverse condition because SplashTheme is default dark
        if (!UserAccount.isDarkModeEnabled) {
            setTheme(R.style.AppTheme)
        }
        setContentView(R.layout.activity_token)

//        progressDialog = ProgressDialog(this)

        findViewById<View>(R.id.google_login).setOnClickListener { viewModel.onLogin() }
    }

    // TODO: Observe onErrorMessage and show Snackbar on error
    // TODO: Observe showBadTokenDialog and show Utils.showBadTokenDialog(this) on error
    // TODO: Observe dataRetrievalStatus and show status (Fetching your course list/courses' content/ site news)
    // TODO: Observe startMainActivity and start intent to move to MainActivity and finish this activity

    override fun onResume() {
        super.onResume()
        viewModel.processIntent(intent)
        viewModel.checkLoggedIn()
    }

    override fun onDestroy() {
        super.onDestroy()
//        progressDialog.dismiss()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

//    private fun showProgress(show: Boolean, message: String) {
//        if (show) progressDialog.show() else progressDialog.hide()
//        progressDialog.setMessage(message)
//    }
//
//    private fun dismissProgress() {
//        if (progressDialog.isShowing) {
//            progressDialog.dismiss()
//        }
//    }
}