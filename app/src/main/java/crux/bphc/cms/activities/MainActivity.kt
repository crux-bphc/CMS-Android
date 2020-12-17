package crux.bphc.cms.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import crux.bphc.cms.R
import crux.bphc.cms.app.Constants
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.background.NotificationWorker
import crux.bphc.cms.fragments.*
import crux.bphc.cms.helper.NOTIFICATION_CHANNEL_UPDATES
import crux.bphc.cms.helper.NOTIFICATION_CHANNEL_UPDATES_BUNDLE
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var _realm: Realm
    private lateinit var _userAccount: UserAccount

    private lateinit var _bottomNavSelectionListener: BottomNavigationView.OnNavigationItemSelectedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MyApplication.getInstance().isDarkModeEnabled) {
            setTheme(R.style.AppTheme_NoActionBar_Dark)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        _userAccount = UserAccount(this)
        Constants.TOKEN = _userAccount.token
        _realm = Realm.getDefaultInstance()

        _bottomNavSelectionListener = BottomNavigationView.OnNavigationItemSelectedListener{
            menuItem ->
            when (menuItem.itemId) {
                R.id.myCoursesFragment -> {
                    pushView(MyCoursesFragment.newInstance(), "My Courses", true)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.searchCourseFragment -> {
                    pushView(SearchCourseForEnrolFragment.newInstance(Constants.TOKEN),
                            "Search Course to Enrol", false)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.forumFragment -> {
                    pushView(ForumFragment.newInstance(), "Site News", false)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.moreFragment -> {
                    pushView(MoreFragment.newInstance(), "More", false)
                    return@OnNavigationItemSelectedListener true
                }
                else -> return@OnNavigationItemSelectedListener false
            }
        }
        bottom_nav.setOnNavigationItemSelectedListener(_bottomNavSelectionListener)

        if (savedInstanceState == null) {
            pushView(MyCoursesFragment.newInstance(), "My Courses", true)
        }

        askPermission()
        createNotificationChannels() // initialize channels before starting background service
        // Enqueue background worker for course updates and notifications
        val notifWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("NotificationWorker",
                ExistingPeriodicWorkPolicy.KEEP, notifWorkRequest)
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
                            _userAccount.token)
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                    startActivity(browserIntent)
                }
            } else {
                Toast.makeText(this, "You need to be enrolled in " + uri.getQueryParameter("courseName") + " in order to view", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Create channels for devices running Oreo and above; Can be safely called even if channel exists
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the "Updates Bundle" channel, which is channel for summary notifications that bundles thr
            // individual notifications
            val service = NotificationChannel(NOTIFICATION_CHANNEL_UPDATES_BUNDLE,
                    "New Content Bundle",
                    NotificationManager.IMPORTANCE_DEFAULT)
            service.description = "A default priority channel that bundles all the notifications"

            // Create the "Updates" channel which has the low importance level
            val updates = NotificationChannel(NOTIFICATION_CHANNEL_UPDATES,
                    "New Content",
                    NotificationManager.IMPORTANCE_LOW)
            updates.description = "All low importance channel that relays all the updates."
            val nm = getSystemService(NotificationManager::class.java)
            // create both channels
            nm.createNotificationChannel(service)
            nm.createNotificationChannel(updates)
        }
    }

    private fun resolveIntent() {
        val courseId = intent.getIntExtra("courseId", -1)
        val modId = intent.getIntExtra("modId", -1)
        val forumId = intent.getIntExtra("forumId", -1)
        val discussionId = intent.getIntExtra("discussionId", -1)

        if (courseId == -1) return
        if (courseId == Constants.SITE_NEWS_COURSE_ID) {
            // Site news, modId will not be -1
            // We will push the fragment here itself
            val forumFragment = ForumFragment.newInstance()
            pushView(forumFragment, "Site News", false)

            // Ensure that the fragment has been commited
            supportFragmentManager.executePendingTransactions()
            val discussionFragment = DiscussionFragment.newInstance(discussionId, "Site News")
            pushView(discussionFragment, "Discussion", false)
        }

        if (modId == -1) {
            val intent = Intent(this, CourseDetailActivity::class.java)
            intent.putExtra("courseId", courseId)
            startActivity(intent)
            return
        }

        if (discussionId != -1) {
            // Open up the discussion first
            val intent = Intent(this, CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.INTENT_COURSE_ID_KEY, courseId)
            intent.putExtra(CourseDetailActivity.INTENT_MOD_ID_KEY, modId)
            intent.putExtra(CourseDetailActivity.INTENT_FORUM_ID_KEY, forumId)
            intent.putExtra(CourseDetailActivity.INTENT_DISCUSSION_ID_KEY, discussionId)
            startActivity(intent)
            return
        }

        val realm = Realm.getDefaultInstance()
        val courseSections = realm.copyFromRealm(realm.where(CourseSection::class.java)
                .equalTo("courseId", courseId).findAll())
        if (courseSections == null || courseSections.isEmpty()) return
        for (module in courseSections.flatMap { it.modules }) {
            if (module.id == modId) {
                val intent = Intent(this, CourseDetailActivity::class.java)
                intent.putExtra("courseId", courseId)
                intent.putExtra("modId", modId)
                startActivity(intent)
                return
            }
        }
        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra("courseId", courseId)
        intent.putExtra("discussionId", modId)
        startActivity(intent)
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
        _realm.close()
    }
}