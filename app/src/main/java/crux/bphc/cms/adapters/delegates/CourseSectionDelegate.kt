package crux.bphc.cms.adapters.delegates

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import crux.bphc.cms.R
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.models.course.CourseSection
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.widgets.CollapsibleTextView

/**
 * Adapter delegate for course sections.
 *
 * @see crux.bphc.cms.adapters.CourseContentAdapter
 *
 * @author Abhijeet Viswa
 */
class CourseSectionDelegate(activity: Activity) : AdapterDelegate<List<CourseContent>>() {
    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<CourseContent>, position: Int): Boolean {
        return items[position] is CourseSection
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return CourseSectionViewHolder(inflater.inflate(R.layout.row_course_section, parent, false))
    }

    override fun onBindViewHolder(items: List<CourseContent>, position: Int,
                                  holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        val vh = holder as CourseSectionViewHolder
        val section = items[position] as CourseSection
        vh.sectionName.text = section.name
        val summary = Utils.trimWhiteSpace(HtmlCompat.fromHtml(section.summary.trim { it <= ' ' },
                HtmlCompat.FROM_HTML_MODE_COMPACT))
        if (summary.isNotEmpty()) {
            vh.sectionDescription.visibility = View.VISIBLE
            vh.sectionDescription.setFullText(summary)
            vh.sectionDescription.movementMethod = LinkMovementMethod.getInstance()
        } else {
            vh.sectionDescription.visibility = View.GONE
        }
    }

    internal class CourseSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sectionName: TextView = itemView.findViewById(R.id.section_name)
        val sectionDescription: CollapsibleTextView = itemView.findViewById(R.id.description)
    }
}