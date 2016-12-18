package crux.bphc.cms.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.R;
import helper.MoodleServices;
import helper.UserAccount;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.search.Course;
import set.search.CourseSearch;

import static app.Constants.API_URL;
import static app.Constants.PER_PAGE;

public class SearchCourseFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final int GARBAGE_TOTAL_PAGES = 1000;

    private RecyclerView mRecyclerView;
    private EditText mEditText;
    private Button mButton;

    private SearchCourseAdapter mSearchCourseAdapter;
    private String TOKEN;

    private boolean mLoading = true;
    private int matchedSearches;
    private int page = 0;
    private int mTotalPages;
    private String mPreviousSearch = "";

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

        mSearchCourseAdapter = new SearchCourseAdapter(getActivity(), new ArrayList<Course>());
        mRecyclerView = (RecyclerView) view.findViewById(R.id.searched_courses);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        mEditText = (EditText) view.findViewById(R.id.course_search_edit_text);
        mButton = (Button) view.findViewById(R.id.course_search_button);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mSearchCourseAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                if(mLoading) {
                    if(visibleItemCount + pastVisibleItems >= totalItemCount) {
                        mLoading = false;
                        getSearchCourses(mPreviousSearch);
                    }
                }
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = mEditText.getText().toString().trim();
                if(!mPreviousSearch.equals(searchText)) {
                    mPreviousSearch = searchText;
                    mTotalPages = GARBAGE_TOTAL_PAGES;
                    page = 0;
                    mSearchCourseAdapter.clearCourses();
                    getSearchCourses(searchText);
                }
            }
        });
    }

    private void getSearchCourses(final String searchString) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //UserAccount userAccount = new UserAccount(getActivity());
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);
        Call<CourseSearch> call = moodleServices.getSearchedCourses(
                TOKEN,
                searchString,
                page,
                PER_PAGE
        );
        System.out.println("Page number is: " + page);
        call.enqueue(new Callback<CourseSearch>() {
            @Override
            public void onResponse(Call<CourseSearch> call, Response<CourseSearch> response) {

                if(mTotalPages == GARBAGE_TOTAL_PAGES) {
                    matchedSearches = response.body().getTotal();
                    mTotalPages = matchedSearches % PER_PAGE == 0 ?
                            ((matchedSearches / PER_PAGE) - 1) : (matchedSearches / PER_PAGE);
                }

                if (page <= mTotalPages) {
                    if (page == 0) {
                        List<Course> matchedCourses = response.body().getCourses();
                        mSearchCourseAdapter.setCourses(matchedCourses);
                    } else {
                        List<Course> newMatchedCourses = response.body().getCourses();
                        mSearchCourseAdapter.addExtraCourses(newMatchedCourses);
                    }
                    mLoading = true;
                    page++;
                }
            }

            @Override
            public void onFailure(Call<CourseSearch> call, Throwable t) {
                Toast.makeText(getActivity(), "Check your Internet Connection", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private class SearchCourseAdapter extends RecyclerView.Adapter<SearchCourseAdapter.SearchCourseViewHolder> {

        private Context mContext;
        private LayoutInflater mLayoutInflater;
        private List<Course> mCourses = new ArrayList<>();

        public SearchCourseAdapter(Context context, List<Course> courses) {
            mContext = context;
            mLayoutInflater = LayoutInflater.from(mContext);
            mCourses = courses;
        }

        public void setCourses(List<Course> courses) {
            mCourses = courses;
            notifyDataSetChanged();
            System.out.println("Number of courses setCourses: " + mCourses.size());
        }

        public void addExtraCourses(List<Course> courses) {
            mCourses.addAll(courses);
            notifyDataSetChanged();
            System.out.println("Number of courses addExtraCourses: " + mCourses.size());
        }

        public void clearCourses() {
            mCourses.clear();
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

            public SearchCourseViewHolder(View itemView) {
                super(itemView);
                mSearchCourseDisplayName = (TextView) itemView.findViewById(R.id.search_course_display_name);
            }
        }
    }
}
