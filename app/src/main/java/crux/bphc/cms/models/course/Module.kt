package crux.bphc.cms.models.course

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName
import crux.bphc.cms.R
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.utils.FileUtils
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.util.*

/**
 * @author Harshit Agarwal (16-Dec-2016)
 * @author Abhijeet Viswa
 */
open class Module(
        @PrimaryKey @SerializedName("id") var id: Int = 0,
        @SerializedName("instance") var instance: Int = 0,
        name: String = "",
        @SerializedName("url") var url: String = "",
        @SerializedName("modicon") var modIcon: String = "",
        @SerializedName("modname") private var modName: String = "",
        @SerializedName("description") var description: String = "",
        @SerializedName("contents") var contents: RealmList<Content> = RealmList(),
        var courseSectionId: Int = 0,
        var isUnread: Boolean = false,
) : RealmObject(), CourseContent {
    enum class Type {
        RESOURCE, FORUM, LABEL, ASSIGNMENT, FOLDER, QUIZ, URL, PAGE, DEFAULT, BOOK
    }

    @SerializedName("name") var name: String = name
        get() {
            return HtmlCompat.fromHtml(field, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    .trim{ it <= ' ' }
        }

    @Ignore var modType: Type = Type.DEFAULT
        get() {
            if (field == Type.DEFAULT) field = inferModuleTypeFromModuleName()
            return field
        }

    val isDownloadable: Boolean
        get() = contents.isNotEmpty() && modType !in arrayOf(Type.URL, Type.FORUM, Type.PAGE)

    val moduleIcon: Int
        get() {
            return when (modType) {
                Type.RESOURCE -> if (contents.isNotEmpty()) {
                    val content = contents.first() ?: return -1
                    FileUtils.getDrawableIconFromFileName(content.fileName)
                } else {
                    -1
                }
                Type.ASSIGNMENT -> R.drawable.assignment
                Type.FOLDER -> R.drawable.outline_folder_24
                Type.URL -> R.drawable.ic_link
                Type.PAGE -> R.drawable.page
                Type.QUIZ -> R.drawable.quiz
                Type.FORUM -> R.drawable.forum
                Type.BOOK -> R.drawable.book
                Type.DEFAULT, Type.LABEL -> -1
            }
        }

    fun deepCopy(): Module = Module(
            id,
            instance,
            name,
            url,
            modIcon,
            modName,
            description,
            RealmList<Content>(*contents.map { it.copy() }.toTypedArray()),
            courseSectionId,
            isUnread,
    )

    private fun inferModuleTypeFromModuleName(): Type {
        return when (modName.toLowerCase(Locale.ROOT)) {
            "resource" -> Type.RESOURCE
            "forum" -> Type.FORUM
            "label" -> Type.LABEL
            "assign" -> Type.ASSIGNMENT
            "folder" -> Type.FOLDER
            "quiz" -> Type.QUIZ
            "url" -> Type.URL
            "page" -> Type.PAGE
            else -> Type.DEFAULT
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Module && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }
}