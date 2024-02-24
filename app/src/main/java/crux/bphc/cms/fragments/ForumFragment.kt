package crux.bphc.cms.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.databinding.FragmentForumBinding
import crux.bphc.cms.databinding.RowForumBinding
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.forum.Discussion
import crux.bphc.cms.utils.Utils
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [ForumFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ForumFragment : Fragment() {

    private var courseId = -1
    private var forumId = 1
    private var courseName: String = ""

    private lateinit var courseRequestHandler: CourseRequestHandler
    private lateinit var binding: FragmentForumBinding

    private lateinit var mAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentForumBinding.inflate(layoutInflater)

        courseId = requireArguments().getInt(COURSE_ID_KEY, -1)
        forumId = requireArguments().getInt(FORUM_ID_KEY, -1)
        courseName = requireArguments().getString(COURSE_NAME_KEY, "")

        courseRequestHandler = CourseRequestHandler()
    }

    override fun onStart() {
        requireActivity().title = courseName
        super.onStart()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?,
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefreshLayout.setOnRefreshListener { refreshContent() }

        val mClickListener = ClickListener { `object`: Any, _: Int ->
            val discussion = `object` as Discussion
            val fragment = DiscussionFragment.newInstance(
                courseId,
                forumId,
                discussion.discussionId,
                courseName
            )
            requireActivity().supportFragmentManager.commit {
                addToBackStack(null)
                replace((requireView().parent as ViewGroup).id, fragment, "ForumDetail")
            }
            true
        }

        mAdapter = Adapter(mClickListener, ArrayList())
        binding.discussions.layoutManager = LinearLayoutManager(requireContext())
        binding.discussions.adapter = mAdapter

        refreshContent()
    }

    private fun refreshContent() {
        binding.swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val realm = Realm.getDefaultInstance()
            val courseDataHandler = CourseDataHandler(realm)

            try {
                val discussions = courseRequestHandler.getForumDicussionsSync(forumId)
                for (discussion in discussions) {
                    discussion.forumId = forumId
                }

                courseDataHandler.setForumDiscussions(forumId, discussions)
                realm.close()

                CoroutineScope(Dispatchers.Main).launch {

                    if (discussions.size == 0) {
                        binding.tvEmpty.text = if (forumId != -1) getString(R.string.no_announcements) else getString(R.string.no_posts_to_display)
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        mAdapter.clearDiscussions()
                        mAdapter.addDiscussions(discussions)
                    }

                    mAdapter.clearDiscussions()
                    mAdapter.addDiscussions(discussions)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: IOException) {
                val realmDiscussions = courseDataHandler.getForumDiscussions(forumId)
                val discussions = realm.copyFromRealm(realmDiscussions)
                CoroutineScope(Dispatchers.Main).launch {

                    if (discussions.size == 0) {
                        binding.tvEmpty.text = if (forumId != -1) getString(R.string.no_announcements) else getString(R.string.no_posts_to_display)
                        binding.tvEmpty.visibility = View.VISIBLE
                        Toast
                            .makeText(context, getString(R.string.no_cached_data), Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            context, getString(R.string.loading_cached_data),
                            Toast.LENGTH_SHORT,
                        ).show()
                        mAdapter.clearDiscussions()
                        mAdapter.addDiscussions(discussions)
                    }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private inner class Adapter(
        private val clickListener: ClickListener,
        val mDiscussions: MutableList<Discussion>,
    ): RecyclerView.Adapter<Adapter.ForumViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        fun addDiscussions(discussions: List<Discussion>) {
            mDiscussions.addAll(discussions)
            notifyDataSetChanged()
        }

        fun clearDiscussions() {
            mDiscussions.clear()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
            val itemBinding = RowForumBinding.inflate(inflater, null, false)
            return ForumViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
            val discussion = mDiscussions[position]
            holder.setIsRecyclable(false)
            holder.bind(discussion)
        }

        override fun getItemCount(): Int {
            return mDiscussions.size
        }

        inner class ForumViewHolder(val itemBinding: RowForumBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(discussion: Discussion) {
                Glide.with(itemBinding.userPic.context)
                    .load(Urls.getProfilePicUrl(discussion.userPictureUrl))
                    .into(itemBinding.userPic)

                itemBinding.subject.text = discussion.subject
                itemBinding.userName.text = discussion.userFullName
                itemBinding.message.text = discussion.message
                itemBinding.modifiedTime.text = Utils.formatDate(discussion.timeModified)

                if (!discussion.isPinned) {
                    itemBinding.pinned.visibility = View.GONE
                }
            }

            init {
                itemBinding.root.setOnClickListener {
                    clickListener.onClick(mDiscussions[layoutPosition], layoutPosition)
                }
                itemBinding.clickWrapper.setOnClickListener {
                    val position = layoutPosition
                    clickListener.onClick(mDiscussions[position], position)
                }
            }
        }
    }

    companion object {
        const val COURSE_ID_KEY = "courseId"
        const val FORUM_ID_KEY = "forumId"
        const val COURSE_NAME_KEY = "courseName"

        @JvmOverloads
        fun newInstance(
            courseId: Int = -1,
            forumId: Int = 1,
            courseName: String = "Site News",
        ): ForumFragment {
            val fragment = ForumFragment()
            val args = Bundle()
            args.putInt(FORUM_ID_KEY, forumId)
            args.putInt(COURSE_ID_KEY, courseId)
            args.putString(COURSE_NAME_KEY, courseName)
            fragment.arguments = args
            return fragment
        }
    }
}