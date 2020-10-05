package crux.bphc.cms.models.course

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName
import crux.bphc.cms.interfaces.CourseContent
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * @author Harshit Agarwal (17-Dec-2016)
 * @author Abhijeet Viswa
 */
open class CourseSection(
        @PrimaryKey  @SerializedName("id") var id: Int = 0,
        name: String = "",
        @SerializedName("summary") var summary: String = "",
        @SerializedName("modules") var modules: RealmList<Module> = RealmList<Module>(),
        var courseId: Int = 0,
) : RealmObject(), CourseContent {

    @SerializedName("name") var name: String = name
        get() {
            return HtmlCompat.fromHtml(field, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    .trim { it <= ' ' }
        }

    fun deepCopy(): CourseSection  = CourseSection(
            id,
            name,
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
}