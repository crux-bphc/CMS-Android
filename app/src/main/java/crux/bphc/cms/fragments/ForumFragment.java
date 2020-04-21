package crux.bphc.cms.fragments;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.CourseRequestHandler;
import helper.CourseRequestHandler.CallBack;
import helper.HtmlTextView;
import helper.MoodleServices;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.forum.Discussion;

import static app.Constants.API_URL;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForumFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForumFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    public static final String FORUM_ID_KEY = "forum_id";
    public static final String COURSE_NAME_KEY = "courseName";
    private final int PER_PAGE = 20;

    private String TOKEN;
    private int FORUM_ID = 1;
    private String COURSE_NAME;
    private int page = 0;
    private boolean mLoading = false;

    private ForumAdapter mAdapter;
    private ClickListener mClickListener;
    private Realm realm;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mEmptyView;

    private MoodleServices moodleServices;

    private CourseRequestHandler courseRequestHandler;

    private Context context;

    public ForumFragment() {
        // Required empty public constructor
    }

    public static ForumFragment newInstance(String token, int forumId, String courseName) {
        ForumFragment fragment = new ForumFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(FORUM_ID_KEY, forumId);
        args.putString(COURSE_NAME_KEY, courseName);
        fragment.setArguments(args);
        return fragment;
    }

    public static ForumFragment newInstance(String token) {
        return newInstance(token, 1, "Site News");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(TOKEN_KEY);
            FORUM_ID = getArguments().getInt(FORUM_ID_KEY);
            COURSE_NAME = getArguments().getString(COURSE_NAME_KEY);
        }
        realm = MyApplication.getInstance().getRealmInstance();
        courseRequestHandler = new CourseRequestHandler(this.getActivity());
        context = getContext();
    }

    @Override
    public void onStart() {
        Objects.requireNonNull(getActivity()).setTitle("Site News");
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forum, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyView = view.findViewById(R.id.tv_empty);

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                mLoading = false;
                makeRequest();
            }
        });

        mClickListener = new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Discussion discussion = (Discussion) object;
                DiscussionFragment fragment = DiscussionFragment.newInstance(discussion.getId(), COURSE_NAME);
                FragmentTransaction transaction = getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.addToBackStack(null);
                transaction.replace(((ViewGroup) getView().getParent()).getId(), fragment, "ForumDetail");
                transaction.commit();
                return true;
            }
        };

        mRecyclerView = view.findViewById(R.id.site_news);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(layoutManager);

        // Instantiate retrofit instance for sending requests
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        moodleServices = retrofit.create(MoodleServices.class);

        mAdapter = new ForumAdapter(mClickListener, new ArrayList<>());
        mRecyclerView.setAdapter(mAdapter);

        makeRequest();
    }

    private void makeRequest() {
        mSwipeRefreshLayout.setRefreshing(true);
        courseRequestHandler.getForumDiscussions(FORUM_ID, new CallBack<List<Discussion>>()
        {
            @Override
            public void onResponse(List<Discussion> discussions) {
                if (discussions.size() == 0) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    for (Discussion discussion : discussions) discussion.setForumId(FORUM_ID); // TODO replace with lambda
                    mEmptyView.setVisibility(View.GONE);
                    mAdapter.clearDiscussions();
                    mAdapter.addDiscussions(discussions);

                    // reset cached data
                    final RealmResults<Discussion> results = realm.where(Discussion.class).equalTo("forumId", FORUM_ID).findAll();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            results.deleteAllFromRealm();
                            realm.copyToRealm(mAdapter.getDiscussions());
                        }
                    });
                    mLoading = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(String message, Throwable t){
                Log.e("ForumFragment", t.toString());
                System.err.println(t.getStackTrace());
                mLoading = false;
                mSwipeRefreshLayout.setRefreshing(false);

                // Load and display cached discussion data from database, if present
                RealmResults<Discussion> results = realm.where(Discussion.class).equalTo("forumId", FORUM_ID).findAll();
                List<Discussion> dbDiscussions = realm.copyFromRealm(results);
                if (dbDiscussions.size() == 0) {
                    mEmptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(context, "No cached data. Check connection and refresh", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Loading cached data. Refresh for latest", Toast.LENGTH_SHORT).show();
                    mAdapter.clearDiscussions();
                    mAdapter.addDiscussions(dbDiscussions);
                }
            }
        });
    }

    public static String formatDate(int seconds) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis((long) seconds * 1000);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String month = cal.getDisplayName(
                Calendar.MONTH,
                Calendar.SHORT,
                Locale.getDefault());
        int year = cal.get(Calendar.YEAR);
        return String.valueOf(day) + " " + month + ", " + String.valueOf(year);
    }

    private class ForumAdapter extends RecyclerView.Adapter<ForumAdapter.ForumViewHolder> {

        private List<Discussion> mDiscussions = new ArrayList<>();
        private ClickListener mClickListener;

        public ForumAdapter(ClickListener clickListener, List<Discussion> discussions) {
            mClickListener = clickListener;
            mDiscussions = discussions;
        }

        public void addDiscussions(List<Discussion> discussions) {
            mDiscussions.addAll(discussions);
            notifyDataSetChanged();
        }

        public List<Discussion> getDiscussions() {
            return mDiscussions;
        }

        public void clearDiscussions() {
            mDiscussions.clear();
            notifyDataSetChanged();
        }

        @Override
        public ForumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ForumViewHolder(inflater.inflate(R.layout.row_forum, parent, false));
        }

        @Override
        public void onBindViewHolder(ForumViewHolder holder, int position) {
            Discussion discussion = mDiscussions.get(position);
            holder.bind(discussion);
        }

        @Override
        public int getItemCount() {
            return mDiscussions.size();
        }


        public class ForumViewHolder extends RecyclerView.ViewHolder {

            private ImageView mUserPic;
            private TextView mSubject;
            private TextView mUserName;
            private TextView mModifiedTime;
            private HtmlTextView mMessage;

            public ForumViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getLayoutPosition();
                        mClickListener.onClick(mDiscussions.get(position), position);
                    }
                });

                mUserPic = itemView.findViewById(R.id.user_pic);
                mSubject = itemView.findViewById(R.id.subject);
                mUserName = itemView.findViewById(R.id.user_name);
                mModifiedTime = itemView.findViewById(R.id.modified_time);
                mMessage = itemView.findViewById(R.id.message);
            }

            public void bind(Discussion discussion) {
                Picasso.with(context).load(discussion.getUserpictureurl()).into(mUserPic);
                mSubject.setText(discussion.getSubject());
                mUserName.setText(discussion.getUserfullname());
                mModifiedTime.setText(formatDate(discussion.getTimemodified()));
                mMessage.setText(HtmlTextView.parseHtml((discussion.getMessage())));
            }
        }
    }
}
