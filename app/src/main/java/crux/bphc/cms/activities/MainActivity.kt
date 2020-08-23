package crux.bphc.cms.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.R
import crux.bphc.cms.app.Constants
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.fragments.*
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.course.Course
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.services.NotificationService
import crux.bphc.cms.utils.UserUtils
import crux.bphc.cms.utils.Utils
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var _realm: Realm
    private lateinit var _userAccount: UserAccount

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
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val view = currentFocus
                if (view != null) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Set the nav panel up
        nav_view.setNavigationItemSelectedListener(this)
        val headerView = nav_view.getHeaderView(0)
        val username = headerView.findViewById<TextView>(R.id.username)
        val fullName = headerView.findViewById<TextView>(R.id.firstname)
        val details = Utils.userDetails(_userAccount.firstName, _userAccount.username)
        fullName.text = details[0]
        username.text = details[1]

        // Set version code and commit hash
        app_version_name.text = BuildConfig.VERSION_NAME
        commit_hash.text = BuildConfig.COMMIT_HASH

        // Set up fragments
        if (savedInstanceState == null) {
            pushView(MyCoursesFragment.newInstance(), "My Courses", true)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val frag = supportFragmentManager.findFragmentById(R.id.content_main)
            if (frag is MyCoursesFragment) {
                nav_view.setCheckedItem(R.id.my_courses)
            }
        }
        askPermission()
        createNotificationChannels() // initialize channels before starting background service
        NotificationService.startService(this, false)
        resolveIntent()
        resolveModuleLinkShare()
        window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
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
                if (course.courseId == courseId) {
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
            val service = NotificationChannel(NotificationService.NOTIFICATION_CHANNEL_UPDATES_BUNDLE,
                    "New Content Bundle",
                    NotificationManager.IMPORTANCE_DEFAULT)
            service.description = "A default priority channel that bundles all the notifications"

            // Create the "Updates" channel which has the low importance level
            val updates = NotificationChannel(NotificationService.NOTIFICATION_CHANNEL_UPDATES,
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
        if (courseId == -1) return
        if (courseId == 1) {
            // Site news, modId will not be -1
            // We will push the fragment here itself
            val forumFragment: Fragment = ForumFragment.newInstance()
            pushView(forumFragment, "Site News", false)

            // Ensure that the fragment has been commited
            supportFragmentManager.executePendingTransactions()
            val discussionFragment: Fragment = DiscussionFragment.newInstance(modId, "Site News")
            pushView(discussionFragment, "Discussion", false)
        }
        if (modId == -1) {
            val intent = Intent(this, CourseDetailActivity::class.java)
            intent.putExtra("courseId", courseId)
            startActivity(intent)
            return
        }
        val realm = Realm.getDefaultInstance()
        val courseSections = realm.copyFromRealm(realm.where(CourseSection::class.java)
                .equalTo("courseId", courseId).findAll())
        if (courseSections == null || courseSections.isEmpty()) return
        for (courseSection in courseSections) {
            for (module in courseSection.modules) {
                if (module.id == modId) {
                    val intent = Intent(this, CourseDetailActivity::class.java)
                    intent.putExtra("courseId", courseId)
                    if (!module.isDownloadable && module.modType == Module.Type.FORUM) {
                        intent.putExtra("forumId", module.instance)
                    }
                    startActivity(intent)
                    return
                }
            }
        }

        // If we've gotten this far, we need to open a discussion
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

    private fun pushView(fragment: Fragment, tag: String, rootFrag: Boolean) {
        if (rootFrag) {
            clearBackStack()
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_main, fragment, tag)
        if (!rootFrag) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun askToLogout(): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(this)
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
        UserUtils.logout(this)
        startActivity(Intent(this, TokenActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.my_courses -> pushView(MyCoursesFragment.newInstance(), "My Courses", true)
            R.id.site_news -> pushView(ForumFragment.newInstance(), "Site News", false)
            R.id.course_search -> pushView(SearchCourseFragment.newInstance(Constants.TOKEN), "Course Search", false)
            R.id.website -> Utils.openURLInBrowser(this, Constants.API_URL + "my/")
            R.id.settings -> pushView(SettingsFragment(), "Settings", false)
            R.id.nav_share -> {
                val appPackageName = BuildConfig.APPLICATION_ID
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Check out the CMS App: https://play.google.com/store/apps/details?id=$appPackageName")
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            R.id.issue -> Utils.openURLInBrowser(this, Constants.getFeedbackURL(_userAccount.firstName, _userAccount.username))
            R.id.about -> startActivity(Intent(this, InfoActivity::class.java))
            R.id.logout -> askToLogout().show()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
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