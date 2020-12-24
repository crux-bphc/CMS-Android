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
}

fun Uri.Builder.appendOrSetQueryParameter(key: String, value: String): Uri.Builder {
    val uri = build()
    clearQuery()
    uri.queryParameterNames.forEach {
        when (it) {
            null -> return@forEach
            key -> appendQueryParameter(key, value)
            else -> appendQueryParameter(key, uri.getQueryParameter(it))
        }
    }

    return this
}