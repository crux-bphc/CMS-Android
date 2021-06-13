package crux.bphc.cms.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import crux.bphc.cms.R
import crux.bphc.cms.viewmodels.TokenViewModel.Companion.PROGRESS_COURSE_CONTENT
import crux.bphc.cms.viewmodels.TokenViewModel.Companion.PROGRESS_COURSE_LIST
import crux.bphc.cms.viewmodels.TokenViewModel.Companion.PROGRESS_SITE_NEWS
import crux.bphc.cms.databinding.ActivityTokenBinding
import crux.bphc.cms.models.SingleLiveEvent
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.viewmodels.TokenViewModel

class TokenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTokenBinding
    private val viewModel by viewModels<TokenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // reverse condition because SplashTheme is default dark
        if (!UserAccount.isDarkModeEnabled) {
            setTheme(R.style.AppTheme)
        }
        binding = ActivityTokenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitButton.setOnClickListener { viewModel.onLogin() }

        viewModel.onErrorMessage.observe(this, onErrorMessageObserver)
        viewModel.showBadTokenDialog.observe(this, showBadTokenDialogObserver)
        viewModel.status.observe(this, statusObserver)
        viewModel.startMainActivity.observe(this, startMainActivityObserver)
        viewModel.startIntent.observe(this, startIntentObserver)
    }

    private val onErrorMessageObserver = Observer<SingleLiveEvent<String>> { error ->
        error.getContentIfNotHandled()?.let {
            Snackbar.make(binding.tokenCoordinator, it, Snackbar.LENGTH_SHORT).show()
        }
    }

    private val showBadTokenDialogObserver = Observer<SingleLiveEvent<Boolean>> { showDialog ->
        showDialog.getContentIfNotHandled()?.let {
            if (it)
                Utils.showBadTokenDialog(this)
        }
    }

    private val statusObserver = Observer<Int> { status ->
        with(binding) {
            if (status == TokenViewModel.NO_STATUS) {
                submitButton.isEnabled = true
                loginProgress.isVisible = false
                loginStatus.isVisible = false
            } else {
                submitButton.isEnabled = false
                loginProgress.isVisible = true
                loginStatus.isVisible = true
                loginStatus.text = when (status) {
                    PROGRESS_COURSE_LIST -> "Fetching your course list"
                    PROGRESS_COURSE_CONTENT -> "Fetching your courses' contents"
                    PROGRESS_SITE_NEWS -> "Fetching site news"
                    else -> "Fetching your details"
                }
            }
        }
    }

    private val startMainActivityObserver = Observer<SingleLiveEvent<Boolean>> { startActivity ->
        startActivity.getContentIfNotHandled()?.let {
            if (it) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private val startIntentObserver = Observer<SingleLiveEvent<Intent>> { intent ->
        intent.getContentIfNotHandled()?.let {
            startActivity(it)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.processIntent(intent)
        viewModel.checkLoggedIn()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}