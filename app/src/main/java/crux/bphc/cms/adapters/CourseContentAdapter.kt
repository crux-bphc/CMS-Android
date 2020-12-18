package crux.bphc.cms.adapters

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegatesManager
import crux.bphc.cms.activities.MainActivity
import crux.bphc.cms.adapters.delegates.CourseModuleDelegate
import crux.bphc.cms.adapters.delegates.CourseSectionDelegate
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.io.FileManager

/**
 * Adapter for Course Content. Utilizes the AdapterDelegates dependency to handle
 * multiple view types efficiently.
 *
 * @see CourseModuleDelegate
 * @see CourseSectionDelegate
 *
 * @author Abhijeet Viswa
 */
class CourseContentAdapter(
        activity: Activity,
        var contents: List<CourseContent>,
        fileManager: FileManager,
        clickWrapperListener: ClickListener,
        moreOptionsClickListener: ClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val delegatesManager: AdapterDelegatesManager<List<CourseContent>> = AdapterDelegatesManager()

    fun setCourseContents(contents: List<CourseContent>) {
        this.contents = contents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return delegatesManager.onCreateViewHolder(parent, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return delegatesManager.getItemViewType(contents, position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        delegatesManager.onBindViewHolder(contents, position, holder)
    }

    override fun getItemCount(): Int {
        return contents.size
    }

    init {
        delegatesManager.addDelegate(CourseSectionDelegate(activity))
                .addDelegate(CourseModuleDelegate(activity, fileManager, clickWrapperListener,
                        moreOptionsClickListener))
    }
}