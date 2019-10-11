package crux.bphc.cms.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.CourseDetailActivity;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.MoodleServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.search.Course;
import set.search.CourseSearch;

import static app.Constants.API_URL;
import static app.Constants.COURSE_PARCEL_INTENT_KEY;
import static app.Constants.PER_PAGE;

public class SearchCourseFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    boolean containsMore = true;
    int mLastVisibleCount = 0;
    private RecyclerView mRecyclerView;
    private EditText mEditText;
    private View mButton;
    private SearchCourseAdapter mSearchCourseAdapter;
    private String TOKEN;
    private boolean mLoading = false;
    private int matchedSearches;
    private int page = 0;
    private String mPreviousSearch = "";
    private SwipeRefreshLayout mSwipeToRefresh;
    private TextView empty;
    private Call<CourseSearch> call;
    private MoodleServices moodleServices;

    public SearchCourseFragment() {
        // Required empty public constructor
    }

    public static SearchCourseFragment newInstance(String token) {
        SearchCourseFragment fragment = new SearchCourseFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(TOKEN_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_course, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        moodleServices = retrofit.create(MoodleServices.class);

        mSearchCourseAdapter = new SearchCourseAdapter(getActivity(), new ArrayList<Course>());
        mSearchCourseAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Course course = (Course) object;
                Intent intent = new Intent(getActivity(), CourseDetailActivity.class);
                intent.putExtra(COURSE_PARCEL_INTENT_KEY, course);
                startActivity(intent);
                return true;
            }
        });

        mRecyclerView = view.findViewById(R.id.searched_courses);
        empty = view.findViewById(R.id.empty);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        mSwipeToRefresh = view.findViewById(R.id.swipeRefreshLayout);
        mEditText = view.findViewById(R.id.course_search_edit_text);
        mButton = view.findViewById(R.id.course_search_button);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mSearchCourseAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                if (!mLoading) {
                    if (visibleItemCount + pastVisibleItems >= totalItemCount && visibleItemCount > mLastVisibleCount) {
                        mLastVisibleCount = visibleItemCount;
                        mLoading = true;
                        getSearchCourses(mPreviousSearch);

                    }
                }
            }
        });


        mSwipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                mSearchCourseAdapter.clearCourses();
                mLoading = true;
                containsMore = true;
                getSearchCourses(mPreviousSearch);
            }
        });

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });
    }

    private void performSearch() {
        String searchText = mEditText.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter a query to search", Toast.LENGTH_SHORT).show();
            return;
        }
        mPreviousSearch = searchText;
        page = 0;
        mSearchCourseAdapter.clearCourses();
        mLoading = true;
        containsMore = true;
        if (call != null) {
            call.cancel();
        }
        getSearchCourses(searchText);
    }

    private void getSearchCourses(final String searchString) {
        empty.setVisibility(View.GONE);


        call = moodleServices.getSearchedCourses(
                TOKEN,
                searchString,
                page,
                PER_PAGE
        );
        System.out.println("Page number is: " + page);
        if (!containsMore) {

            return;
        }
        mSwipeToRefresh.setRefreshing(true);
        call.enqueue(new Callback<CourseSearch>() {
            @Override
            public void onResponse(Call<CourseSearch> call, Response<CourseSearch> response) {


                if (response.body() == null) {
                    if (page == 0) {
                        empty.setVisibility(View.VISIBLE);
                        containsMore = false;

                    }
                } else {
                    int totalResults = response.body().getTotal();
                    int fetchedResults = (page + 1) * PER_PAGE;
                    if (fetchedResults >= totalResults) {
                        containsMore = false;
                    }

                    if (page == 0) {
                        List<Course> matchedCourses = response.body().getCourses();
                        if (matchedCourses == null || matchedCourses.size() == 0) {
                            empty.setVisibility(View.VISIBLE);
                            containsMore = false;

                        } else {

                            mSearchCourseAdapter.setCourses(matchedCourses);
                        }
                    } else {
                        List<Course> newMatchedCourses = response.body().getCourses();
                        mSearchCourseAdapter.addExtraCourses(newMatchedCourses);
                    }
                }
                mSwipeToRefresh.setRefreshing(false);
                mLoading = false;
                page++;

            }

            @Override
            public void onFailure(Call<CourseSearch> call, Throwable t) {
                mLoading = false;
                mSwipeToRefresh.setRefreshing(false);
                Toast.makeText(getActivity(), "Check your Internet Connection", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private class SearchCourseAdapter extends RecyclerView.Adapter<SearchCourseAdapter.SearchCourseViewHolder> {

        private Context mContext;
        private LayoutInflater mLayoutInflater;
        private List<Course> mCourses = new ArrayList<>();
        private ClickListener mClickListener;

        SearchCourseAdapter(Context context, List<Course> courses) {
            mContext = context;
            mLayoutInflater = LayoutInflater.from(mContext);
            mCourses = courses;
        }

        public List<Course> getCourses() {
            return mCourses;
        }

        void setCourses(List<Course> courses) {
            mCourses = courses;
            notifyDataSetChanged();
            System.out.println("Number of courses setCourses: " + mCourses.size());
        }

        void addExtraCourses(List<Course> courses) {
            mCourses.addAll(courses);
            notifyDataSetChanged();
            System.out.println("Number of courses addExtraCourses: " + mCourses.size());
        }

        void clearCourses() {
            mCourses.clear();
            notifyDataSetChanged();
        }

        public void setClickListener(ClickListener clickListener) {
            this.mClickListener = clickListener;
        }

        @Override
        public SearchCourseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SearchCourseViewHolder(
                    mLayoutInflater.inflate(R.layout.row_search_course, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(SearchCourseViewHolder holder, int position) {
            Course course = mCourses.get(position);
            holder.mSearchCourseDisplayName.setText(course.getDisplayname());
        }

        @Override
        public int getItemCount() {

            return mCourses == null ? 0 : mCourses.size();
        }


        class SearchCourseViewHolder extends RecyclerView.ViewHolder {

            TextView mSearchCourseDisplayName;

            SearchCourseViewHolder(View itemView) {
                super(itemView);
                mSearchCourseDisplayName = itemView.findViewById(R.id.search_course_display_name);

                mSearchCourseDisplayName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mClickListener != null) {
                            int pos = getLayoutPosition();
                            mClickListener.onClick(mCourses.get(pos), pos);
                        }
                    }
                });
            }
        }
    }
}
