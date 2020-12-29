package crux.bphc.cms.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import crux.bphc.cms.R
import crux.bphc.cms.fragments.MoreOptionsFragment.Companion.newInstance
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.io.FileManager
import crux.bphc.cms.models.forum.Attachment
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.widgets.HtmlTextView
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import java.util.*

class DiscussionFragment : Fragment() {

    private lateinit var realm: Realm
    private lateinit var fileManager: FileManager
    private lateinit var moreOptionsViewModel: OptionsViewModel

    private lateinit var userPic: ImageView
    private lateinit var subject: TextView
    private lateinit var userName: TextView
    private lateinit var timeModified: TextView
    private lateinit var message: HtmlTextView
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var discussion: Discussion

    private var mCourseName: String = ""
    private var discussionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        discussionId = requireArguments().getInt("id", -1)
        mCourseName = requireArguments().getString("courseName", "")

        fileManager = FileManager(requireActivity(), mCourseName)
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discussion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsViewModel = ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)

        userPic = view.findViewById(R.id.user_pic)
        subject = view.findViewById(R.id.subject)
        userName = view.findViewById(R.id.user_name)
        timeModified = view.findViewById(R.id.modified_time)
        message = view.findViewById(R.id.message)
        attachmentContainer = view.findViewById(R.id.attachments)

        message.movementMethod = LinkMovementMethod.getInstance()

        fileManager.registerDownloadReceiver()
        fileManager.setCallback { filename: String? ->
            val child = attachmentContainer.childCount
            for (i in 0 until child) {
                val childView = attachmentContainer.getChildAt(i) ?: break
                val fileNameTextView = childView.findViewById<TextView>(R.id.name)
                val downloadIcon = childView.findViewById<ImageView>(R.id.download)
                val ellipsis = childView.findViewById<ImageView>(R.id.more)

                if (fileNameTextView?.text.toString().equals(filename, true)) {
                    downloadIcon?.setImageResource(R.drawable.eye)
                    ellipsis?.visibility = View.VISIBLE
                }
            }
            val attachments = discussion.attachments
            val attachment = attachments.where().equalTo("fileName", filename).findFirst()
            attachment?.let { fileManager.openDiscussionAttachment(it) }
        }

        val discussion = realm.where(Discussion::class.java).equalTo("discussionId", discussionId)
                .findFirst()
        if (discussion != null) {
            this.discussion = discussion
            setDiscussion(discussion)
        }
    }

    private fun setDiscussion(discussion: Discussion) {
        subject.text = discussion.subject
        userName.text = discussion.userFullName
        timeModified.text = ForumFragment.formatDate(discussion.timeModified)
        message.text = discussion.message
        Glide.with(requireContext()).load(discussion.userPictureUrl).into(userPic)

        val attachments = discussion.attachments
        if (attachments.isNotEmpty()) {
            val inflater = LayoutInflater.from(requireContext())
            attachmentContainer.visibility = View.VISIBLE
            for (attachment in discussion.attachments) {
                val attachmentView = inflater.inflate(R.layout.row_attachment_detail_forum,
                        attachmentContainer) ?: continue
                attachmentView.let {
                    val fileName = attachmentView.findViewById<TextView>(R.id.name)
                    val clickWrapper = attachmentView.findViewById<View>(R.id.click_wrapper)
                    val download = attachmentView.findViewById<ImageView>(R.id.download)
                    val ellipsis = attachmentView.findViewById<ImageView>(R.id.more)

                    fileName.text = attachment.fileName
                    if (fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                        download.setImageResource(R.drawable.eye)
                        ellipsis.visibility = View.VISIBLE
                    } else {
                        download.setImageResource(R.drawable.download)
                        ellipsis.visibility = View.GONE
                    }
                    clickWrapper.setOnClickListener {
                        if (fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                            downloadAttachment(attachment)
                        } else {
                            fileManager.openDiscussionAttachment(attachment)
                        }
                    }
                    ellipsis.setOnClickListener {
                        if (fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                            val observer: Observer<MoreOptionsFragment.Option?>
                            val options = ArrayList(listOf(
                                    MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                                    MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                                    MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                                    MoreOptionsFragment.Option(3, "Properties", R.drawable.ic_info)
                            ))
                            observer = Observer {  option: MoreOptionsFragment.Option? ->
                                option ?: return@Observer
                                when (option.id) {
                                    0 -> fileManager.openDiscussionAttachment(attachment)
                                    1 -> {
                                        downloadAttachment(attachment)
                                    }
                                    2 -> fileManager.shareDiscussionAttachment(attachment)
                                    3 -> PropertiesAlertDialog(requireActivity(), attachment).show()
                                }
                                moreOptionsViewModel.selection.removeObservers(requireActivity())
                                moreOptionsViewModel.clearSelection()
                            }
                            val fragment = newInstance(attachment.fileName, options)
                            fragment.show(requireActivity().supportFragmentManager, fragment.tag)
                            moreOptionsViewModel.selection.observe(requireActivity(), observer)
                        }
                    }
                }
                }
            attachmentContainer.visibility = View.GONE
            }
    }

    private fun downloadAttachment(attachment: Attachment) {
        Toast.makeText(activity, getString(R.string.downloading_file) + attachment.fileName,
                Toast.LENGTH_SHORT).show()
        fileManager.downloadDiscussionAttachment(attachment, discussion.subject, mCourseName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fileManager.unregisterDownloadReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    companion object {

        private const val COURSE_ID_KEY = "courseId"
        private const val DISCUSSION_ID_KEY = "discussionId"
        private const val COURSE_NAME_KEY = "contextUrl"

        @JvmStatic
        fun newInstance(courseId: Int, discussionId: Int, mCourseName: String): DiscussionFragment {
            val fragment = DiscussionFragment()
            val args = Bundle()
            args.putInt(COURSE_ID_KEY, courseId)
            args.putInt(DISCUSSION_ID_KEY, discussionId)
            args.putString(COURSE_NAME_KEY, mCourseName)
            fragment.arguments = args
            return fragment
        }
    }
}