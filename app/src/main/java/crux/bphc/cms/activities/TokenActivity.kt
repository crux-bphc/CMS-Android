package crux.bphc.cms.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import crux.bphc.cms.R
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.app.Urls
import crux.bphc.cms.app.appendOrSetQueryParameter
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.core.UserDetail
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.network.APIClient
import crux.bphc.cms.network.MoodleServices
import crux.bphc.cms.utils.UserUtils
import crux.bphc.cms.utils.Utils
import io.realm.Realm
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class TokenActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    private lateinit var moodleServices: MoodleServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // reverse condition because SplashTheme is default dark
        if (!UserAccount.isDarkModeEnabled) {
            setTheme(R.style.AppTheme)
        }
        setContentView(R.layout.activity_token)

        progressDialog = ProgressDialog(this)

        val retrofit = APIClient.getRetrofitInstance()
        moodleServices = retrofit.create(MoodleServices::class.java)

        findViewById<View>(R.id.google_login).setOnClickListener { onLogin() }
    }

    override fun onResume() {
        super.onResume()
        if (intent != null && intent.action != null && intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null) {
                val scheme = data.scheme
                if (scheme != null && scheme != Urls.SSO_URL_SCHEME) {
                    Toast.makeText(this, "Invalid token URI Schema.",
                            Toast.LENGTH_SHORT).show()
                    Utils.showBadTokenDialog(this)
                    return
                }
                val hostScheme = "token="
                var host = data.host
                if (host != null) {
                    if (!host.contains(hostScheme)) {
                        Toast.makeText(this, "Invalid token URI Schema.",
                                Toast.LENGTH_SHORT).show()
                        Utils.showBadTokenDialog(this)
                        return
                    }

                    // Clean up the host so that we can extract the token
                    host = host.replace(hostScheme, "")
                    host = host.replace("/?#?$".toRegex(), "")
                    host = String(Base64.decode(host, Base64.DEFAULT))
                    val parts = host.split(":::").toTypedArray()
                    if (parts.size < 2) {
                        Toast.makeText(this, "Invalid token response length",
                                Toast.LENGTH_SHORT).show()
                        Utils.showBadTokenDialog(this)
                        return
                    }
                    val digest = parts[0].toUpperCase(Locale.ROOT)
                    val token = parts[1]
                    val privateToken = if(parts.size >= 3) parts[2] else ""

                    val launchData = MyApplication.instance.loginLaunchData
                    val signature = launchData["SITE_URL"] + launchData["PASSPORT"]
                    try {
                        if (Utils.bytesToHex(MessageDigest.getInstance("md5")
                                        .digest(signature.toByteArray(StandardCharsets.US_ASCII))) != digest) {
                            Toast.makeText(this, "Invalid token digest",
                                    Toast.LENGTH_SHORT).show()
                            Utils.showBadTokenDialog(this)
                            return
                        }
                    } catch (e: NoSuchAlgorithmException) {
                        Toast.makeText(this, "MD5 not a valid MessageDigest algorithm! :o",
                                Toast.LENGTH_SHORT).show()
                    }
                    loginUsingToken(token, privateToken)
                }
            }
        }
        checkLoggedIn()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog.dismiss()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun loginUsingToken(token: String, privateToken: String) {
        showProgress(true, "Fetching your details")
        val call = moodleServices.fetchUserDetail(token)
        call.enqueue(object : Callback<UserDetail> {
            override fun onResponse(call: Call<UserDetail>, response: Response<UserDetail>) {
                if (response.isSuccessful) {
                    var userDetail: UserDetail
                    if (response.body().also { userDetail = it!! } != null) {
                        userDetail.token = token
                        userDetail.privateToken = privateToken

                        UserAccount.setUser(userDetail)
                        fetchUserData()
                    }
                }
            }

            override fun onFailure(call: Call<UserDetail>, t: Throwable) {
                Log.wtf(TAG, t)
                showProgress(false, "")
            }
        })
    }

    private fun onLogin() {
        /*
            We'll just create an into a specific Moodle endpoint. The Moodle website will handle the authentication,
            generate a token and redirect the browser to the Uri with specified schema. The browser will create an
            intent that'll launch this activity again.
         */

        // A random number that identifies the request
        val passport = Random().nextInt(1000).toString()
        var builder = Urls.SSO_LOGIN_URL.buildUpon()
        builder = builder.appendOrSetQueryParameter("passport", passport)
        builder = builder.appendOrSetQueryParameter("urlscheme", Urls.SSO_URL_SCHEME)
        val loginUrl = builder.toString()

        // Set the launch data, we need this to verify the token obtained after SSO
        val data = HashMap<String, String>()
        // SITE_URL must not end with trailing /
        data["SITE_URL"] = Urls.MOODLE_URL.toString().replace("/$".toRegex(), "")
        data["PASSPORT"] = passport
        MyApplication.instance.setLoginLaunchData(data)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(loginUrl)
        startActivity(intent)
    }

    private fun showProgress(show: Boolean, message: String) {
        if (show) progressDialog.show() else progressDialog.hide()
        progressDialog.setMessage(message)
    }

    private fun dismissProgress() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun fetchUserData() {
        CourseDataRetriever(this).execute()
    }

    private fun checkLoggedIn() {
        if (UserAccount.isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            dismissProgress()
            startActivity(intent)
            finish()
        }
    }

    internal class CourseDataRetriever(activity: TokenActivity) : AsyncTask<Void?, Int?, Boolean>() {
        private val activityRef: WeakReference<TokenActivity> = WeakReference(activity)
        private val courseDataHandler: CourseDataHandler = CourseDataHandler(null)
        private val courseRequestHandler: CourseRequestHandler = CourseRequestHandler()

        override fun doInBackground(vararg voids: Void?): Boolean {
            // We set the realm instance for this worker thread
            courseDataHandler.setRealmInstance(Realm.getDefaultInstance())

            /* Fetch User's Course List */publishProgress(PROGRESS_COURSE_LIST)
            val courseList = courseRequestHandler.getCourseList(activityRef.get())
            if (courseList == null) {
                if (!UserUtils.isValidToken(UserAccount.token)) {
                    UserUtils.logout()
                    UserUtils.clearBackStackAndLaunchTokenActivity(activityRef.get() as Activity)
                }
                return false
            }
            courseDataHandler.replaceCourses(courseList)

            /* Fetch Course Content */publishProgress(PROGRESS_COURSE_CONTENT)
            val courses = courseDataHandler.courseList
            for (course in courses) {
                val courseSections = courseRequestHandler.getCourseData(course) ?: continue
                for (courseSection in courseSections) {
                    val modules: List<Module> = courseSection.modules
                    for (module in modules) {
                        if (module.modType === Module.Type.FORUM) {
                            val discussions = courseRequestHandler.getForumDiscussions(module.instance)
                                    ?: continue
                            for (d in discussions) {
                                d.forumId = module.instance
                            }
                            courseDataHandler.setForumDiscussions(module.instance, discussions)
                        }
                    }
                }
                courseDataHandler.replaceCourseData(course.id, courseSections)
            }

            /* Fetch Site News */publishProgress(PROGRESS_SITE_NEWS)
            val discussions = courseRequestHandler.getForumDiscussions(1) // 1 is always site news
            if (discussions != null) {
                for (d in discussions) {
                    d.forumId = 1
                }
                courseDataHandler.setForumDiscussions(1, discussions)
            }
            return true
        }

        override fun onProgressUpdate(vararg values: Int?) {
            if (values.isNotEmpty()) {
                when (values[0]) {
                    PROGRESS_COURSE_LIST -> activityRef.get()!!.showProgress(true, "Fetching your course list")
                    PROGRESS_COURSE_CONTENT -> activityRef.get()!!.showProgress(true, "Fetching your courses' contents")
                    PROGRESS_SITE_NEWS -> activityRef.get()!!.showProgress(true, "Fetching site news")
                }
            }
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(bool: Boolean) {
            super.onPostExecute(bool)
            activityRef.get()!!.checkLoggedIn()
        }

        companion object {
            private const val PROGRESS_COURSE_LIST = 1
            private const val PROGRESS_COURSE_CONTENT = 2
            private const val PROGRESS_SITE_NEWS = 3
        }
    }

    companion object {
        private const val TAG = "TokenActivity"
    }
}