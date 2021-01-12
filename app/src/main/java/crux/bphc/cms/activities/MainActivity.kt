package crux.bphc.cms.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.R
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.core.PushNotifRegManager
import crux.bphc.cms.fragments.*
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.utils.UserUtils
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var _realm: Realm
    private lateinit var courseDataHandler: CourseDataHandler

    private val _bottomNavSelectionListener
        get() = listener@ {
            menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.myCoursesFragment -> {
                    pushView(MyCoursesFragment.newInstance(), "My Courses", true)
                    return@listener true
                }
                R.id.searchCourseFragment -> {
                    pushView(SearchCourseForEnrolFragment.newInstance(UserAccount.token),
                            "Search Course to Enrol", false)
                    return@listener true
                }
                R.id.forumFragment -> {
                    pushView(ForumFragment.newInstance(), "Site News", false)
                    return@listener true
                }
                R.id.moreFragment -> {
                    pushView(MoreFragment.newInstance(), "More", false)
                    return@listener true
                }
                else -> return@listener false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!UserAccount.isLoggedIn) {
           UserUtils.clearBackStackAndLaunchTokenActivity(this)
            return
        }

        // Override the splash theme since it sets its own
        // image background
        if (UserAccount.isDarkModeEnabled) {
            setTheme(R.style.AppTheme_NoActionBar_Dark)
        } else {
            setTheme(R.style.AppTheme_NoActionBar)
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        bottom_nav.setOnNavigationItemSelectedListener(_bottomNavSelectionListener)

        _realm = Realm.getDefaultInstance()
        courseDataHandler = CourseDataHandler(this, _realm)

        if (savedInstanceState == null) {
            pushView(MyCoursesFragment.newInstance(), "My Courses", true)
        }

        askPermission()

        // Register for push notifs if required
        lifecycleScope.launch {
            var toastResource = 0
            if (UserAccount.isNotificationsEnabled && !PushNotifRegManager.isRegistered()) {
                if (!PushNotifRegManager.registerDevice()) {
                    if (UserAccount.isLoggedIn) { // We failed inspite being logged in
                        toastResource = R.string.push_notif_reg_failure
                    }
                }
            } else if (!UserAccount.isNotificationsEnabled && PushNotifRegManager.isRegistered()) {
                if (!PushNotifRegManager.deregisterDevice()) {
                    toastResource = R.string.push_notif_dereg_failure
                }
            }

            if (toastResource != 0) {
                val context = this@MainActivity
                Toast.makeText(
                    context,
                    context.getString(toastResource),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        resolveIntent()
        resolveModuleLinkShare()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        bottom_nav.setOnNavigationItemSelectedListener(null) // Remove the listener to prevent an infinite loop
        val frag = supportFragmentManager.findFragmentById(R.id.content_frame)
        bottom_nav.selectedItemId = when (frag) {
            is MyCoursesFragment -> R.id.myCoursesFragment
            is SearchCourseForEnrolFragment -> R.id.searchCourseFragment
            is ForumFragment -> R.id.forumFragment
            is MoreFragment -> R.id.moreFragment
            else -> bottom_nav.selectedItemId
        }
        bottom_nav.setOnNavigationItemSelectedListener(_bottomNavSelectionListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun resolveModuleLinkShare() {
        val intent = intent
        val action = intent.action
        val uri = intent.data
        if (uri != null && action != null && action == "android.intent.action.VIEW") {
            val courses = _realm.copyFromRealm(_realm.where(Course::class.java).findAll())
            var courseId = -1
            val q = uri.getQueryParameter("courseId")
            if (q != null) {
                courseId = q.toInt()
            }
            var isEnrolled = false
            for (course in courses) {
                if (course.id == courseId) {
                    isEnrolled = true
                    break
                }
            }
            if (isEnrolled) {
                val scheme = uri.scheme
                val path = uri.path
                val host = uri.host
                if (scheme != null && host != null && path != null) {
                    val fileUrl = String.format("%s://%s%s+?forcedownload=1&token=%s", scheme, host, path,
                            UserAccount.token)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                    startActivity(browserIntent)
                }
            } else {
                Toast.makeText(this, "You need to be enrolled in " + uri.getQueryParameter("courseName") + " in order to view", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resolveIntent() {
        if (intent == null) return

        if (!intent.action.equals(Intent.ACTION_MAIN)) {
            return
        }

        val contextUrl = intent.getStringExtra("contexturl") ?: ""
        val courseId = (intent.getStringExtra("courseid") ?: "-1").toInt()
        val customData = intent.getStringExtra("customdata") ?: ""

        if (contextUrl == "" && courseId == -1) return

        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra(CourseDetailActivity.INTENT_CONTEXT_URL_KEY, contextUrl)
        intent.putExtra(CourseDetailActivity.INTENT_COURSE_ID_KEY, courseId)
        intent.putExtra(CourseDetailActivity.INTENT_CUSTOM_DATA_KEY, customData)

        startActivity(intent)
        finish()

//        val courseId = intent.getIntExtra("courseId", -1)
//        val modId = intent.getIntExtra("modId", -1)
//        val forumId = intent.getIntExtra("forumId", -1)
//        val discussionId = intent.getIntExtra("discussionId", -1)
//
//        if (courseId == -1) return
//        if (courseId == Constants.SITE_NEWS_COURSE_ID) {
//            // Site news, modId will not be -1
//            // We will push the fragment here itself
//            val forumFragment = ForumFragment.newInstance()
//            pushView(forumFragment, "Site News", false)
//
//            // Ensure that the fragment has been commited
//            supportFragmentManager.executePendingTransactions()
//            val discussionFragment = DiscussionFragment.newInstance(discussionId, "Site News")
//            pushView(discussionFragment, "Discussion", false)
//        }
//
//        if (modId == -1) {
//            val intent = Intent(this, CourseDetailActivity::class.java)
//            intent.putExtra("courseId", courseId)
//            startActivity(intent)
//            return
//        }
//
//        if (discussionId != -1) {
//            // Open up the discussion first
//            val intent = Intent(this, CourseDetailActivity::class.java)
//            intent.putExtra("courseId", courseId)
//            intent.putExtra("modId", modId)
//            intent.putExtra("forumId", forumId)
//            intent.putExtra("discussionId", discussionId)
//            startActivity(intent)
//            return
//        }
//
//        val realm = Realm.getDefaultInstance()
//        val courseSections = realm.copyFromRealm(realm.where(CourseSection::class.java)
//                .equalTo("courseId", courseId).findAll())
//        if (courseSections == null || courseSections.isEmpty()) return
//        for (module in courseSections.flatMap { it.modules }) {
//            if (module.id == modId) {
//                val intent = Intent(this, CourseDetailActivity::class.java)
//                intent.putExtra("courseId", courseId)
//                intent.putExtra("modId", modId)
//                startActivity(intent)
//                return
//            }
//        }
//        val intent = Intent(this, CourseDetailActivity::class.java)
//        intent.putExtra("courseId", courseId)
//        intent.putExtra("discussionId", modId)
//        startActivity(intent)
    }

    private fun askPermission() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                MaterialAlertDialogBuilder(this)
                        .setTitle("Permission required")
                        .setMessage("We need permission to download course content onto your phone")
                        .setPositiveButton("OK") { _, _ ->
                            requestWriteStoragePermission()
                        }
                        .show()
            } else {
                requestWriteStoragePermission()
            }
        }
    }

    private fun requestWriteStoragePermission() {
        val askPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                askPermission()
        }
        askPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun pushView(fragment: Fragment, tag: String, rootFrag: Boolean) {
        if (rootFrag) {
            clearBackStack()
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment, tag)
        if (!rootFrag) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun clearBackStack() {
        for (i in 0 until supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::_realm.isInitialized) {
            _realm.close()
        }
    }
}
