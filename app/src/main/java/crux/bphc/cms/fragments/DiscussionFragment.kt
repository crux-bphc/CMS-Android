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
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import crux.bphc.cms.R
import crux.bphc.cms.core.FileManager
import crux.bphc.cms.fragments.MoreOptionsFragment.Companion.newInstance
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.models.forum.Attachment
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.widgets.HtmlTextView
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class DiscussionFragment : Fragment() {

    private lateinit var realm: Realm
    private lateinit var fileManager: FileManager
    private lateinit var moreOptionsViewModel: OptionsViewModel

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var empty: TextView
    private lateinit var content: LinearLayout
    private lateinit var userPic: ImageView
    private lateinit var subject: TextView
    private lateinit var userName: TextView
    private lateinit var timeModified: TextView
    private lateinit var message: HtmlTextView
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var discussion: Discussion

    private var courseId: Int = 0
    private var forumId: Int = 0
    private var discussionId: Int = 0
    private var mCourseName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        courseId = requireArguments().getInt(COURSE_ID_KEY, -1)
        forumId = requireArguments().getInt(FORUM_ID_KEY, -1)
        discussionId = requireArguments().getInt(DISCUSSION_ID_KEY, -1)
        mCourseName = requireArguments().getString(COURSE_NAME_KEY, "")

        fileManager = FileManager(requireActivity(), mCourseName) { oneFileDownloaded(it) }
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discussion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsViewModel = ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)

        swipeRefreshLayout = view.findViewById(R.id.refresh)
        empty = view.findViewById(R.id.empty)
        content = view.findViewById(R.id.content)
        userPic = view.findViewById(R.id.user_pic)
        subject = view.findViewById(R.id.subject)
        userName = view.findViewById(R.id.user_name)
        timeModified = view.findViewById(R.id.modified_time)
        message = view.findViewById(R.id.message)
        attachmentContainer = view.findViewById(R.id.attachments)

        swipeRefreshLayout.isEnabled = false // Disable swiping, for now
        message.movementMethod = LinkMovementMethod.getInstance()

        fileManager.registerDownloadReceiver()

        val discussion = realm.where(Discussion::class.java).equalTo("discussionId", discussionId)
                .findFirst()
        if (discussion != null) {
            setDiscussion(discussion)
        } else {
            refreshContent(forumId, discussionId)
        }
    }

    @MainThread
    private fun setDiscussion(discussion: Discussion) {
        this.discussion = discussion
        content.visibility = View.VISIBLE
        empty.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        subject.text = discussion.subject
        userName.text = discussion.userFullName
        timeModified.text = Utils.formatDate(discussion.timeModified)
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
                        if (!fileManager.isDiscussionAttachmentDownloaded(attachment)) {
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
        } else {
            attachmentContainer.visibility = View.GONE
        }
    }

    private fun refreshContent(forumId: Int, discussionId: Int) {
        swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
           val realm = Realm.getDefaultInstance()
           val courseDataHandler = CourseDataHandler(realm)
           val courseRequestHandler = CourseRequestHandler()

            try {
                val discussions = courseRequestHandler.getForumDicussionsSync(forumId)
                discussions.forEach { it.forumId = forumId }
                courseDataHandler.setForumDiscussions(forumId, discussions)

                val discussion = discussions.firstOrNull { it.discussionId == discussionId }
                CoroutineScope(Dispatchers.Main).launch {
                    if (discussion != null) {
                        empty.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                        setDiscussion(discussion)
                    } else {
                        empty.visibility = View.VISIBLE
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(requireContext(), getString(R.string.net_req_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                CoroutineScope(Dispatchers.Main).launch {
                    empty.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), getString(R.string.net_req_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
            realm.close()
        }
    }

    private fun downloadAttachment(attachment: Attachment) {
        Toast.makeText(activity, getString(R.string.downloading_file) + attachment.fileName,
                Toast.LENGTH_SHORT).show()
        fileManager.downloadDiscussionAttachment(attachment, discussion.subject)
    }

    private fun oneFileDownloaded(filename: String) {
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
        val attachment = attachments.firstOrNull { it.fileName == filename }
        attachment?.let { fileManager.openDiscussionAttachment(it) }

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
        private const val COURSE_NAME_KEY = "contextUrl"
        private const val FORUM_ID_KEY = "forumId"
        private const val DISCUSSION_ID_KEY = "discussionId"

        @JvmStatic
        fun newInstance(
            courseId: Int,
            forumId: Int,
            discussionId: Int,
            mCourseName: String,
        ): DiscussionFragment {
            val fragment = DiscussionFragment()
            val args = Bundle()
            args.putInt(COURSE_ID_KEY, courseId)
            args.putInt(FORUM_ID_KEY, forumId)
            args.putInt(DISCUSSION_ID_KEY, discussionId)
            args.putString(COURSE_NAME_KEY, mCourseName)
            fragment.arguments = args
            return fragment
        }
    }
}