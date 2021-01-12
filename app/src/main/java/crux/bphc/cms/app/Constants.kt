package crux.bphc.cms.app

import android.net.Uri

/**
 * Class that contains constants and other near-constant information
 *
 * @author Harshit Agarwal (16-12-2016)
 * @author Abhijeet Viswa
 */
object Constants {
    const val PER_PAGE = 20 // Number of course search results in a page

    const val LOGIN_LAUNCH_DATA = "LOGIN_LAUNCH_DATA"

    const val AIRNOTIFIER_PLATFORM_NAME = "android-fcm"

    /*
     * Site News isn't part of any course. However, internally we assume it to be part of course 0.
     */
    var SITE_NEWS_COURSE_ID = 0
}