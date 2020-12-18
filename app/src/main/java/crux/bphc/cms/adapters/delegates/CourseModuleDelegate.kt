package crux.bphc.cms.adapters.delegates

import android.app.Activity
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import crux.bphc.cms.R
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.interfaces.CourseContent
import crux.bphc.cms.io.FileManager
import crux.bphc.cms.models.course.Content
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.widgets.CollapsibleTextView
import crux.bphc.cms.widgets.HtmlTextView

/**
 * Adapter delegate for course modules.
 *
 * @see crux.bphc.cms.adapters.CourseContentAdapter
 *
 * @author Abhijeet Viswa
 */
class CourseModuleDelegate(
        private val activity: Activity,
        private val fileManager: FileManager,
        private val clickWrapperClickListener: ClickListener,
        private val moreOptionsClickListener: ClickListener
) : AdapterDelegate<List<CourseContent>>() {
    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<CourseContent>, position: Int): Boolean {
        return items[position] is Module
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return CourseModuleViewHolder(inflater.inflate(R.layout.row_course_module, parent, false))
    }

    override fun onBindViewHolder(items: List<CourseContent>, position: Int,
                                  holder: RecyclerView.ViewHolder, payloads: List<Any>) {
        val vh = holder as CourseModuleViewHolder
        val module = items[position] as Module
        bindClickListeners(vh, module, position)
        setLayoutTheme(vh, module)
        bindNameAndDescription(vh, module)
        bindIcons(vh, module)
        bindMoreOptions(vh, module)
    }

    private fun setLayoutTheme(vh: CourseModuleViewHolder, module: Module) {
        val value = TypedValue()
        if (module.isUnread) {
            activity.theme.resolveAttribute(R.attr.unReadModule, value, true)
        } else {
            activity.theme.resolveAttribute(R.attr.cardBgColor, value, true)
        }
        vh.layoutWrapper.setBackgroundColor(value.data)
    }

    private fun bindClickListeners(vh: CourseModuleViewHolder, module: Module, position: Int) {
        vh.clickWrapper.setOnClickListener { clickWrapperClickListener.onClick(module, position) }
        vh.more.setOnClickListener { moreOptionsClickListener.onClick(module, position) }
    }

    private fun bindNameAndDescription(vh: CourseModuleViewHolder, module: Module) {
        vh.name.text = module.name
        if (module.description.isNotEmpty()) {
            vh.description.visibility = View.VISIBLE
            vh.nameAndDescriptionDivider.visibility = View.VISIBLE
            vh.description.setFullText(SpannableStringBuilder()
                    .append(HtmlCompat.fromHtml(module.description, HtmlCompat.FROM_HTML_MODE_COMPACT)))
            vh.description.movementMethod = LinkMovementMethod.getInstance()
        } else {
            vh.description.visibility = View.GONE
            vh.nameAndDescriptionDivider.visibility = View.GONE
        }
    }

    private fun bindIcons(vh: CourseModuleViewHolder, module: Module) {
        // Bind file icon
        vh.progressBar.visibility = View.GONE
        if (module.moduleIcon != -1) {
            vh.modIcon.setImageResource(module.moduleIcon)
        } else {
            // Either no icon is needed or it needs to be fetched
            vh.modIcon.visibility = View.GONE
            if (module.modType !== Module.Type.LABEL) {
                vh.progressBar.visibility = View.VISIBLE
                Glide.with(vh.modIcon.context).addDefaultRequestListener(object : RequestListener<Any?> {
                    override fun onLoadFailed(e: GlideException?, model: Any,
                                              target: Target<Any?>, isFirstResource: Boolean): Boolean {
                        Toast.makeText(vh.modIcon.context, String.format("Load failed for %s",
                                module.modIcon), Toast.LENGTH_SHORT).show()
                        return false
                    }

                    override fun onResourceReady(resource: Any?, model: Any,
                                                 target: Target<Any?>, dataSource: DataSource,
                                                 isFirstResource: Boolean): Boolean {
                        vh.progressBar.visibility = View.GONE
                        vh.modIcon.visibility = View.VISIBLE
                        return false
                    }
                }).load(Uri.parse(module.modIcon)).into(vh.modIcon)
            }
        }

        // Bind download/view icon
        if (!module.isDownloadable || module.modType === Module.Type.FOLDER) {
            vh.downloadIcon.setImageResource(R.drawable.eye)
        } else {
            val downloaded = module.contents.stream().allMatch { content: Content? -> fileManager.isModuleContentDownloaded(content) }
            if (downloaded) {
                vh.downloadIcon.setImageResource(R.drawable.eye)
            } else {
                vh.downloadIcon.setImageResource(R.drawable.download)
            }
        }
    }

    private fun bindMoreOptions(vh: CourseModuleViewHolder, module: Module) {
        vh.more.visibility = if (module.isDownloadable) View.VISIBLE else View.GONE
    }

    internal class CourseModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: HtmlTextView = itemView.findViewById(R.id.name)

        val description: CollapsibleTextView = itemView.findViewById(R.id.description)
        val modIcon: ImageView = itemView.findViewById(R.id.icon)
        val more: ImageView = itemView.findViewById(R.id.more)
        val downloadIcon: ImageView = itemView.findViewById(R.id.download)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        val nameAndDescriptionDivider: View = itemView.findViewById(R.id.nameAndDescriptionDivider)
        val layoutWrapper: View = itemView.findViewById(R.id.layout_wrapper)
        val clickWrapper: View = itemView.findViewById(R.id.click_wrapper)
    }

}