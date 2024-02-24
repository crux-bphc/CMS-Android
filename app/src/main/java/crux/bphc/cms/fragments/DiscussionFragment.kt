package crux.bphc.cms.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.core.FileManager
import crux.bphc.cms.databinding.FragmentDiscussionBinding
import crux.bphc.cms.databinding.RowAttachmentDetailForumBinding
import crux.bphc.cms.fragments.MoreOptionsFragment.Companion.newInstance
import crux.bphc.cms.fragments.MoreOptionsFragment.OptionsViewModel
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.models.forum.Attachment
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.utils.Utils
import crux.bphc.cms.widgets.PropertiesAlertDialog
import io.realm.Realm
import io.realm.RealmList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class DiscussionFragment : Fragment() {

    private lateinit var realm: Realm
    private lateinit var binding: FragmentDiscussionBinding
    private lateinit var fileManager: FileManager
    private lateinit var moreOptionsViewModel: OptionsViewModel
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
        binding = FragmentDiscussionBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsViewModel = ViewModelProvider(requireActivity())[OptionsViewModel::class.java]

        binding.refresh.isEnabled = false // Disable swiping, for now
        binding.message.movementMethod = LinkMovementMethod.getInstance()

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
        binding.content.visibility = View.VISIBLE
        binding.empty.visibility = View.GONE
        binding.refresh.isRefreshing = false

        binding.subject.text = discussion.subject
        binding.userName.text = discussion.userFullName
        binding.modifiedTime.text = Utils.formatDate(discussion.timeModified)
        binding.message.text = discussion.message
        Glide.with(requireContext())
            .load(Urls.getProfilePicUrl(discussion.userPictureUrl))
            .into(binding.userPic)

        val attachments = discussion.attachments
        if (attachments.isNotEmpty()) {
            val inflater = LayoutInflater.from(requireContext())
            binding.attachments.visibility = View.VISIBLE

            for (attachment in discussion.attachments) {
                val attachmentBinding = RowAttachmentDetailForumBinding.inflate(inflater, binding.attachments, true)
                attachmentBinding.let {
                    it.name.text = attachment.fileName
                    if (fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                        it.download.setImageResource(R.drawable.eye)
                        it.more.visibility = View.VISIBLE
                    } else {
                        it.download.setImageResource(R.drawable.outline_file_download_24)
                        it.more.visibility = View.GONE
                    }
                    it.clickWrapper.setOnClickListener {
                        if (!fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                            downloadAttachment(attachment)
                        } else {
                            fileManager.openDiscussionAttachment(attachment)
                        }
                    }
                    it.more.setOnClickListener {
                        if (fileManager.isDiscussionAttachmentDownloaded(attachment)) {
                            val options = ArrayList(listOf(
                                MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                                MoreOptionsFragment.Option(1, "Re-Download", R.drawable.outline_file_download_24),
                                MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                                MoreOptionsFragment.Option(3, "Properties", R.drawable.ic_info)
                            ))
                            val observer: Observer<MoreOptionsFragment.Option?> = Observer { option: MoreOptionsFragment.Option? ->
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
                    if(attachment != discussion.attachments.last()) {
                        it.barrier.setDpMargin(4)
                    }
                }
            }
        } else {
            binding.attachments.visibility = View.GONE
        }
    }

    private fun refreshContent(forumId: Int, discussionId: Int) {
        binding.refresh.isRefreshing = true
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
                        binding.empty.visibility = View.GONE
                        binding.refresh.isRefreshing = false
                        setDiscussion(discussion)
                    } else {
                        binding.empty.visibility = View.VISIBLE
                        binding.refresh.isRefreshing = false
                        Toast.makeText(requireContext(), getString(R.string.net_req_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.empty.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = false
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
        val child = binding.attachments.childCount
        // Count starts from 2 as the layout contains two extra views before further rows are attached
        for (i in 2 until child) {
            val childView = binding.attachments.getChildAt(i) ?: break
            val attachmentBinding = RowAttachmentDetailForumBinding.bind(childView)

            if (attachmentBinding.name.text.toString().equals(filename, true)) {
                attachmentBinding.download.setImageResource(R.drawable.eye)
                attachmentBinding.more.visibility = View.VISIBLE
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