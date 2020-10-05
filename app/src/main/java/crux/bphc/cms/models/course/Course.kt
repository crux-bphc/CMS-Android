package crux.bphc.cms.models.course

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName
import crux.bphc.cms.models.enrol.SearchedCourseDetail
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.regex.Pattern

/**
 * @author Harshit Agarwal (16-12-2016)
 * @author Abhijeet Viswa
 */
open class Course(
        @PrimaryKey @SerializedName("id") var id: Int = 0,
        shortName: String = "",
        var fullName: String = "",
        var isFavorite: Boolean = false,
) : RealmObject() {

    @SerializedName("shortname")
    var shortName = shortName
        get() = HtmlCompat.fromHtml(field, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

    @Ignore private var _courseName: Array<String>? = null
    @Ignore var courseName: Array<String> = emptyArray() // The entire courseName
        get() {
            if (_courseName == null) {
                val courseName = shortName
                // Specifies the string pattern which is to be searched
                val pattern = Pattern.compile(NAME_REGEX, Pattern.MULTILINE)
                val matcher = pattern.matcher(courseName)
                val parts = arrayOf(courseName, "", "")
                if (matcher.find()) {
                    for (i in 1..matcher.groupCount()) {
                        parts[i - 1] = matcher.group(i) ?: ""
                    }
                }
                _courseName = parts
            }
            return _courseName as Array<String>
        }
        private set

    @Ignore var downloadStatus = 0
    @Ignore var totalFiles = 0
    @Ignore var downloadedFiles = 0

    constructor(course: SearchedCourseDetail) : this(course.id, course.shortName, course.fullName)

    override fun equals(other: Any?): Boolean {
        return other is Course && other.id == id
    }

    override fun hashCode(): Int = id

    companion object {
        private const val NAME_REGEX = "([\\w\\d \\-/'&,]+ \\w\\d\\d\\d) ([\\w\\d \\-/():+\"'&.,?]+) ([LTP]\\d*)"
    }
}