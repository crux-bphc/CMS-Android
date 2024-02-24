package crux.bphc.cms.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import crux.bphc.cms.R
import crux.bphc.cms.activities.CourseDetailActivity
import crux.bphc.cms.app.Constants.PER_PAGE
import crux.bphc.cms.databinding.FragmentSearchCourseBinding
import crux.bphc.cms.databinding.RowSearchCourseBinding
import crux.bphc.cms.interfaces.ClickListener
import crux.bphc.cms.models.UserAccount
import crux.bphc.cms.models.enrol.CourseSearch
import crux.bphc.cms.models.enrol.SearchedCourseDetail
import crux.bphc.cms.network.APIClient
import crux.bphc.cms.network.MoodleServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchCourseForEnrolFragment : Fragment() {
    var containsMore = true
    private var page = 0
    private var mPreviousSearch = ""
    private var mLoading = false

    private lateinit var searchCourseAdapter: SearchCourseAdapter
    private lateinit var moodleServices: MoodleServices
    private lateinit var binding: FragmentSearchCourseBinding

    private var call: Call<CourseSearch>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSearchCourseBinding.inflate(layoutInflater)
        val retrofit = APIClient.getRetrofitInstance()
        moodleServices = retrofit.create(MoodleServices::class.java)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = "Search Courses to Enrol"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        binding.searchedCourses.layoutManager = layoutManager
        binding.searchedCourses.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                if (!mLoading) {
                    if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                        mLoading = true
                        getSearchCourses(mPreviousSearch)
                    }
                }
            }
        })

        searchCourseAdapter = SearchCourseAdapter(requireActivity())
        binding.searchedCourses.adapter = searchCourseAdapter
        searchCourseAdapter.setClickListener { `object`: Any?, _: Int ->
            val course = `object` as SearchedCourseDetail?
            val intent = Intent(activity, CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.INTENT_ENROL_COURSE_KEY, course)
            startActivity(intent)
            true
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            page = 0
            searchCourseAdapter.clearCourses()
            mLoading = true
            containsMore = true
            getSearchCourses(mPreviousSearch)
        }

        binding.courseSearchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.courseSearchButton.setOnClickListener { performSearch() }
    }

    private fun performSearch() {
        val searchText = binding.courseSearchEditText.text.toString().trim { it <= ' ' }
        if (searchText.isEmpty()) {
            return
        }

        mPreviousSearch = searchText
        page = 0
        searchCourseAdapter.clearCourses()
        mLoading = true
        containsMore = true

        call?.cancel()
        getSearchCourses(searchText)
    }

    private fun getSearchCourses(searchString: String) {
        binding.empty.visibility = View.GONE
        call = moodleServices.searchCourses(
            UserAccount.token,
            searchString,
            page,
            PER_PAGE
        )
        if (!containsMore) {
            return
        }
        binding.swipeRefreshLayout.isRefreshing = true
        call?.enqueue(object : Callback<CourseSearch> {
            override fun onResponse(call: Call<CourseSearch>, response: Response<CourseSearch>) {
                if (response.body() == null) {
                    if (page == 0) {
                        binding.empty.visibility = View.VISIBLE
                        containsMore = false
                    }
                    return
                }

                val totalResults = response.body()!!.total
                val fetchedResults: Int = (page + 1) * PER_PAGE
                val matchedCourses = response.body()!!.courses

                if (fetchedResults >= totalResults) {
                    containsMore = false
                }

                if (matchedCourses.isEmpty()) {
                    binding.empty.visibility = View.VISIBLE
                    containsMore = false
                }

                if (page == 0) {
                    searchCourseAdapter.clearCourses()
                }

                searchCourseAdapter.addExtraCourses(matchedCourses)
                binding.swipeRefreshLayout.isRefreshing = false
                mLoading = false
                page++
            }

            override fun onFailure(call: Call<CourseSearch?>, t: Throwable) {
                mLoading = false
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    activity,
                    resources.getText(R.string.net_req_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private class SearchCourseAdapter(
        context: Context,
    ) : RecyclerView.Adapter<SearchCourseAdapter.SearchCourseViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var mCourses: MutableList<SearchedCourseDetail> = mutableListOf()
        private lateinit var mClickListener: ClickListener


        fun addExtraCourses(courses: List<SearchedCourseDetail>) {
            mCourses.addAll(courses)
            notifyDataSetChanged()
        }

        fun clearCourses() {
            mCourses.clear()
            notifyDataSetChanged()
        }

        fun setClickListener(clickListener: ClickListener) {
            mClickListener = clickListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchCourseViewHolder {
            val itemBinding = RowSearchCourseBinding.inflate(inflater, parent, false)
            return SearchCourseViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SearchCourseViewHolder, position: Int) {
            holder.bind(mCourses[position])
        }

        override fun getItemCount(): Int  = mCourses.size

        inner class SearchCourseViewHolder(val itemBinding: RowSearchCourseBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(course: SearchedCourseDetail) {
                itemBinding.searchCourseDisplayName.text = course.displayName
            }

            init {
                itemBinding.searchCourseDisplayName.setOnClickListener {
                    val pos = layoutPosition
                    mClickListener.onClick(mCourses[pos], pos)
                }
            }
        }
    }

    companion object {
        fun newInstance(): SearchCourseForEnrolFragment = SearchCourseForEnrolFragment()
    }
}