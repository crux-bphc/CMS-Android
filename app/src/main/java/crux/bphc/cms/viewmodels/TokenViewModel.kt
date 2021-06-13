package crux.bphc.cms.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.app.Urls
import crux.bphc.cms.app.appendOrSetQueryParameter
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.models.SingleLiveEvent
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.core.UserDetail
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.network.APIClient
import crux.bphc.cms.network.MoodleServices
import crux.bphc.cms.utils.UserUtils
import crux.bphc.cms.utils.Utils
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class TokenViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = javaClass.simpleName

    private val moodleServices: MoodleServices = APIClient.getRetrofitInstance().create(MoodleServices::class.java)

    private val _onErrorMessage: MutableLiveData<SingleLiveEvent<String>> = MutableLiveData()
    val onErrorMessage: LiveData<SingleLiveEvent<String>> = _onErrorMessage

    private val _showBadTokenDialog: MutableLiveData<SingleLiveEvent<Boolean>> = MutableLiveData()
    val showBadTokenDialog: LiveData<SingleLiveEvent<Boolean>> = _showBadTokenDialog

    private val _status: MutableLiveData<Int> = MutableLiveData(NO_STATUS)
    val status: LiveData<Int> = _status

    private val _startMainActivity: MutableLiveData<SingleLiveEvent<Boolean>> = MutableLiveData()
    val startMainActivity: LiveData<SingleLiveEvent<Boolean>> = _startMainActivity

    private val _startIntent: MutableLiveData<SingleLiveEvent<Intent>> = MutableLiveData()
    val startIntent: LiveData<SingleLiveEvent<Intent>> = _startIntent

    fun processIntent(intent: Intent?) {
        if (intent != null && intent.action != null && intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null) {
                val scheme = data.scheme
                if (scheme != null && scheme != Urls.SSO_URL_SCHEME) {
                    _onErrorMessage.postValue(SingleLiveEvent("Invalid token URI Schema."))
                    _showBadTokenDialog.postValue(SingleLiveEvent(true))
                    return
                }
                val hostScheme = "token="
                var host = data.host
                if (host != null) {
                    if (!host.contains(hostScheme)) {
                        _onErrorMessage.postValue(SingleLiveEvent("Invalid token URI Schema."))
                        _showBadTokenDialog.postValue(SingleLiveEvent(true))
                        return
                    }

                    // Clean up the host so that we can extract the token
                    host = host.replace(hostScheme, "")
                    host = host.replace("/?#?$".toRegex(), "")
                    host = String(Base64.decode(host, Base64.DEFAULT))
                    val parts = host.split(":::").toTypedArray()
                    if (parts.size < 2) {
                        _onErrorMessage.postValue(SingleLiveEvent("Invalid token response length"))
                        _showBadTokenDialog.postValue(SingleLiveEvent(true))
                        return
                    }
                    val digest = parts[0].toUpperCase(Locale.ROOT)
                    val token = parts[1]
                    val privateToken = if (parts.size >= 3) parts[2] else ""

                    val launchData = MyApplication.instance.loginLaunchData
                    val signature = launchData["SITE_URL"] + launchData["PASSPORT"]
                    try {
                        if (Utils.bytesToHex(MessageDigest.getInstance("md5")
                                        .digest(signature.toByteArray(StandardCharsets.US_ASCII))) != digest) {
                            _onErrorMessage.postValue(SingleLiveEvent("Invalid token digest"))
                            _showBadTokenDialog.postValue(SingleLiveEvent(true))
                            return
                        }
                    } catch (e: NoSuchAlgorithmException) {
                        _onErrorMessage.postValue(SingleLiveEvent("MD5 not a valid MessageDigest algorithm! :o"))
                    }
                    loginUsingToken(token, privateToken)
                }
            }
        }
    }

    private fun loginUsingToken(token: String, privateToken: String) {
        _status.postValue(FETCHING_DETAILS)
        val call = moodleServices.fetchUserDetail(token)
        call.enqueue(object : Callback<UserDetail> {
            override fun onResponse(call: Call<UserDetail>, response: Response<UserDetail>) {
                if (response.isSuccessful) {
                    var userDetail: UserDetail
                    if (response.body().also { userDetail = it!! } != null) {
                        userDetail.token = token
                        userDetail.privateToken = privateToken

                        UserAccount.setUser(userDetail)
                        viewModelScope.launch(Dispatchers.IO) {
                            fetchUserData()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<UserDetail>, t: Throwable) {
                Log.wtf(TAG, t)
                _status.postValue(NO_STATUS)
            }
        })
    }

    private suspend fun fetchUserData() {
        val courseDataHandler = CourseDataHandler(Realm.getDefaultInstance())
        val courseRequestHandler = CourseRequestHandler()

        /* Fetch User's Course List */
        _status.postValue(PROGRESS_COURSE_LIST)
        val courseList = courseRequestHandler.getCourseList(getApplication() as Context)
        if (courseList == null) {
            if (!UserUtils.isValidToken(UserAccount.token)) {
                UserUtils.logout()
                UserUtils.clearBackStackAndLaunchTokenActivity(getApplication())
            }
            return
        }
        courseDataHandler.replaceCourses(courseList)

        /* Fetch Course Content */
        _status.postValue(PROGRESS_COURSE_CONTENT)
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

        /* Fetch Site News */
        _status.postValue(PROGRESS_SITE_NEWS)
        val discussions = courseRequestHandler.getForumDiscussions(1) // 1 is always site news
        if (discussions != null) {
            for (d in discussions) {
                d.forumId = 1
            }
            courseDataHandler.setForumDiscussions(1, discussions)
        }

        checkLoggedIn()
    }

    fun checkLoggedIn() {
        if (UserAccount.isLoggedIn) {
            _startMainActivity.postValue(SingleLiveEvent(true))
        }
    }

    fun onLogin() {
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
        _startIntent.postValue(SingleLiveEvent(intent))
    }

    companion object {
        const val FETCHING_DETAILS = 0
        const val PROGRESS_COURSE_LIST = 1
        const val PROGRESS_COURSE_CONTENT = 2
        const val PROGRESS_SITE_NEWS = 3
        const val NO_STATUS = 5
    }
}