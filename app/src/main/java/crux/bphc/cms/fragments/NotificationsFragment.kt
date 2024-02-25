package crux.bphc.cms.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import crux.bphc.cms.R
import crux.bphc.cms.databinding.FragmentNotificationsBinding
import crux.bphc.cms.databinding.RowNotificationBinding
import crux.bphc.cms.exceptions.InvalidTokenException
import crux.bphc.cms.helper.CourseDataHandler
import crux.bphc.cms.helper.CourseRequestHandler
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.core.Notification
import crux.bphc.cms.utils.UserUtils
import crux.bphc.cms.utils.Utils
import io.realm.Realm
import kotlinx.coroutines.*
import java.util.*

class NotificationsFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var courseDataHandler: CourseDataHandler
    private lateinit var courseRequestHandler: CourseRequestHandler
    private lateinit var binding: FragmentNotificationsBinding

    private var notifications: MutableList<Notification> = ArrayList()

    private lateinit var mAdapter: NotificationsFragment.Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentNotificationsBinding.inflate(layoutInflater)

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = "Notifications"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        realm = Realm.getDefaultInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseDataHandler = CourseDataHandler(realm)
        courseRequestHandler = CourseRequestHandler()
        notifications = courseDataHandler.notifications

        // Set up the adapter
        mAdapter = Adapter(requireActivity(), notifications)
        mAdapter.notifications = notifications
        mAdapter.clickListener = ClickListener { `object`: Any, position: Int ->
            val notification = `object` as Notification
            if(notification.url != null) {
                Utils.openURLInBrowser(requireActivity(), notification.url)
            }
            else {
                val message = "No URL associated with this notification"
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
            return@ClickListener true
        }

        binding.notificationRecyclerView.adapter = mAdapter
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(activity)

        binding.notificationSwipeRefreshLayout.setOnRefreshListener {
            binding.notificationSwipeRefreshLayout.isRefreshing = true
            refreshNotifications()
        }
        lifecycleScope.launch {
            launch(Dispatchers.IO) {
                updateNotificationContent()
            }
        }
        checkEmpty()
    }

    private fun checkEmpty() {
        if (notifications.isEmpty()) {
            binding.notificationEmpty?.visibility = View.VISIBLE
        } else {
            binding.notificationEmpty?.visibility = View.GONE
        }
    }

    private fun refreshNotifications() {
        lifecycleScope.launch {
            launch(Dispatchers.IO) { // lifecycle scope allows cancellation of this scope
                val courseRequestHandler = CourseRequestHandler()
                try {
                    val notificationList = courseRequestHandler.fetchNotificationListSync()
                    notifications.clear()
                    notifications.addAll(notificationList)
                    Log.i(TAG, "${notificationList.size} notifications")
                    val realm = Realm.getDefaultInstance() // tie a realm instance to this thread
                    val courseDataHandler = CourseDataHandler(realm)
                    courseDataHandler.replaceNotifications(notificationList)
                    realm.close()
                    withContext(Dispatchers.Main) {
                        checkEmpty()
                    }
                    updateNotificationContent()
                    withContext(Dispatchers.Main) {
                        val message = "Notifications are up to date"
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        if (e is InvalidTokenException) {
                            UserUtils.logout()
                            UserUtils.clearBackStackAndLaunchTokenActivity(requireActivity())
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.notificationSwipeRefreshLayout?.isRefreshing = false
                    }
                }
            }
        }
    }

    private suspend fun updateNotificationContent() {
        withContext(Dispatchers.Main) {
            Log.i(MyCoursesFragment.TAG, "Fetching notifications")
            binding.notificationSwipeRefreshLayout?.isRefreshing = false
            mAdapter.filterNotifications(notifications)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        realm.close()
    }

    private inner class Adapter constructor(
        val context: Context,
        notificationList: List<Notification>
    ) : RecyclerView.Adapter<Adapter.MyViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        var clickListener: ClickListener? = null
        var notifications: List<Notification> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemBinding = RowNotificationBinding.inflate(inflater, parent, false)
            return MyViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val notification = notifications[position]
            setLayoutTheme(holder, notification)
            holder.bind(notification)
        }

        override fun getItemCount(): Int {
            return notifications.size
        }

        private fun setLayoutTheme(vh: NotificationsFragment.Adapter.MyViewHolder, notification: Notification) {
            val value = TypedValue()
            if (!notification.read) {
                activity?.theme?.resolveAttribute(R.attr.unReadModule, value, true)
            } else {
                activity?.theme?.resolveAttribute(R.attr.cardBgColor, value, true)
            }
            vh.itemView.findViewById<View>(R.id.layout_wrapper).setBackgroundColor(value.data)
        }

        fun filterNotifications(notificationList: List<Notification>) {
            val filteredCourses: MutableList<Notification> = notificationList as MutableList<Notification>
            notifications = filteredCourses
            notifyDataSetChanged()
        }

        inner class MyViewHolder(val itemBinding: RowNotificationBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(notification: Notification) {
                itemBinding.notificationSubject.text = notification.subject
                itemBinding.notificationMessage.text = notification.message
                itemBinding.markAsReadButton.isVisible = notification.read == false
                itemBinding.timeCreated.text = notification.timeCreated
            }

            fun markNotificationAsRead() {
                val notificationId = notifications[layoutPosition].notificationId
                lifecycleScope.launch {
                    launch(Dispatchers.IO) {
                        courseRequestHandler.markNotificationAsRead(notificationId)
                    }
                }
                courseDataHandler.markNotificationAsRead(notifications[layoutPosition])
                notifyItemChanged(layoutPosition)

                Toast.makeText(activity, "Marked as read", Toast.LENGTH_SHORT).show()
            }

            init {
                itemBinding.notificationCard.setOnClickListener {
                    if (clickListener != null) {
                        val pos = layoutPosition
                        clickListener!!.onClick(notifications[pos], pos)
                    }
                }
                itemBinding.markAsReadButton.setOnClickListener { markNotificationAsRead() }
            }
        }

        init {
            this.notifications = notificationList
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotificationsFragment()

        @JvmStatic
        val TAG = "NotificationsFragment"
    }
}