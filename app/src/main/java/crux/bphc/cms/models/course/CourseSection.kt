package crux.bphc.cms.models.course

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.models.UserAccount
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Harshit Agarwal (17-Dec-2016)
 * @author Abhijeet Viswa
 */
open class CourseSection(
        @PrimaryKey  @SerializedName("id") var id: Int = 0,
        name: String = "",
        @SerializedName("section") var sectionNum: Int = 0,
        summary: String = "",
        @SerializedName("modules") var modules: RealmList<Module> = RealmList<Module>(),
        var courseId: Int = 0,
) : RealmObject(), CourseContent {

    @SerializedName("name") var name: String = name
        get() {
            return HtmlCompat.fromHtml(field, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                .trim { it <= ' ' }
        }

    @SerializedName("summary") var summary: String = summary
        get() {
            val pattern: Pattern = Pattern.compile(URL_REGEX)
            val matcher: Matcher = pattern.matcher(field)

            val summaryBuffer = StringBuffer(field.length)

            while (matcher.find()) {
                val foundLink = matcher.group(1)
                val replaceWith = "<a href=\"$foundLink?token=${UserAccount.token}\">"
                matcher.appendReplacement(summaryBuffer, replaceWith)
            }
            matcher.appendTail(summaryBuffer)

            return summaryBuffer.toString().trim { it <= ' ' }
        }

    fun deepCopy(): CourseSection  = CourseSection(
        id,
        name,
        sectionNum,
        summary,
        RealmList<Module>(*modules.map { it.deepCopy() }.toTypedArray()),
        courseId,
    )

    override fun equals(other: Any?): Boolean {
        return other is CourseSection && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        private const val URL_REGEX = "<a href=\"(.*?)\">"
    }
}