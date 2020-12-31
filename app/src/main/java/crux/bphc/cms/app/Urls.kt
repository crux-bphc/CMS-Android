package crux.bphc.cms.app

import android.net.Uri

/**
 * @author Abhijeet Viswa
 */
object Urls {

    @JvmField
    val WEBSITE_URL: Uri = with(Uri.Builder()) {
        scheme("https")
        authority("crux-bphc.github.io")
        build()
    }

    @JvmField
    val MOODLE_URL: Uri = with(Uri.Builder()) {
        scheme("https")
        authority("cms.bits-hyderabad.ac.in")
        path("")
        build()
    }

    const val SSO_URL_SCHEME = "cmsbphc"

    @JvmField
    val SSO_LOGIN_URL: Uri = with(MOODLE_URL.buildUpon()) {
        path("admin/tool/mobile/launch.php")
        appendQueryParameter("service", "moodle_mobile_app")
        appendQueryParameter("oauthsso", "1")
        appendQueryParameter("passport", "")
        appendQueryParameter("urlscheme", "")
        build()
    }

    @JvmField
    val COURSE_URL: Uri = with(MOODLE_URL.buildUpon()) {
        path("course/view.php")
        appendQueryParameter("id", "")
        build()
    }

    @JvmStatic
    fun getCourseUrl(courseId: Int, sectionNum: Int = 0): Uri = COURSE_URL.buildUpon()
            .appendOrSetQueryParameter("id", courseId.toString())
            .fragment("section-$sectionNum")
            .build()

    @JvmStatic
    fun getFeedbackURL(username: String, id: String): String = with(Uri.Builder()) {
        scheme("https")
        authority("docs.google.com")
        path("forms/d/e/1FAIpQLScEGd0DLv7qCkZZHmpkPgDxSW_SomUi83YPSVV5g7dPjELyKQ/viewform")
        appendQueryParameter("entry.1072406768", username)
        appendQueryParameter("entry.309049076", "$id@hyderabad.bits-pilani.ac.in")
        build()
    }.toString()

    //    fun isMoodleUrl(url: Uri) = url.authority == MOODLE_URL.authority
    fun isMoodleUrl(url: Uri) = true

    fun isCourseSectionUrl(url: Uri): Boolean {
        if (!isMoodleUrl(url)) return false;
        return url.path ?: "" == "/course/view.php";
    }

    fun isCourseModuleUrl(url: Uri): Boolean {
        if (!isMoodleUrl(url)) return false;
        return (url.path ?: "").matches(Regex("/mod/.*/view.php"))
    }

    fun isForumDiscussionUrl(url: Uri): Boolean {
        if (!isMoodleUrl(url)) return false;
        return url.path ?: "" == "/mod/forum/discuss.php";
    }

    fun getModIdFromUrl(url: Uri): Int {
        if (!isCourseModuleUrl(url)) return -1
        return url.getQueryParameter("id")?.toIntOrNull() ?: -1
    }

    fun getSectionNumFromUrl(url: Uri): Int {
        if (!isCourseSectionUrl(url)) return 0
        val fragment = url.fragment ?: ""
        var ret = 0
        if (fragment.startsWith("section-")) {
            ret = fragment.substringAfter("-").toIntOrNull() ?: 0
        }
        return ret
    }
}

/**
 * Extension function to easily set an existing parameter on a [Uri.Builder].
 */
fun Uri.Builder.appendOrSetQueryParameter(key: String, value: String): Uri.Builder {
    val uri = build()
    clearQuery()
    uri.queryParameterNames.forEach {
        when (it) {
            null -> return@forEach
            key -> appendQueryParameter(key, value)
            else -> appendQueryParameter(it, uri.getQueryParameter(it))
        }
    }

    return this
}
