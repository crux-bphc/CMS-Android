package crux.bphc.cms.fragments;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import crux.bphc.cms.R;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.helper.CourseRequestHandler.CallBack;
import crux.bphc.cms.interfaces.ClickListener;
import crux.bphc.cms.models.forum.Discussion;
import crux.bphc.cms.widgets.HtmlTextView;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForumFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForumFragment extends Fragment {

    public static final String FORUM_ID_KEY = "forum_id";
    public static final String COURSE_NAME_KEY = "courseName";

    private int FORUM_ID = 1;
    private String COURSE_NAME;

    private ForumAdapter mAdapter;
    private Realm realm;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mEmptyView;

    private CourseRequestHandler courseRequestHandler;

    private Context context;

    public ForumFragment() {
        // Required empty public constructor
    }

    public static ForumFragment newInstance(int forumId, String courseName) {
        ForumFragment fragment = new ForumFragment();
        Bundle args = new Bundle();
        args.putInt(FORUM_ID_KEY, forumId);
        args.putString(COURSE_NAME_KEY, courseName);
        fragment.setArguments(args);
        return fragment;
    }

    public static ForumFragment newInstance() {
        return newInstance(1, "Site News");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            FORUM_ID = getArguments().getInt(FORUM_ID_KEY);
            COURSE_NAME = getArguments().getString(COURSE_NAME_KEY);
        }
        realm = Realm.getDefaultInstance();
        courseRequestHandler = new CourseRequestHandler(this.getActivity());
        context = getContext();
    }

    @Override
    public void onStart() {
        if(getActivity() != null) {
            getActivity().setTitle(COURSE_NAME);
        }
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        realm = Realm.getDefaultInstance();
        return inflater.inflate(R.layout.fragment_forum, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyView = view.findViewById(R.id.tv_empty);

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this::makeRequest);

        ClickListener mClickListener = (object, position) -> {
            Discussion discussion = (Discussion) object;
            DiscussionFragment fragment = DiscussionFragment.newInstance(discussion.getId(), COURSE_NAME);
            FragmentTransaction transaction = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();

            transaction.addToBackStack(null);
            transaction.replace(((ViewGroup) requireView().getParent()).getId(), fragment, "ForumDetail");
            transaction.commit();

            return true;
        };

        RecyclerView mRecyclerView = view.findViewById(R.id.site_news);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(layoutManager);

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
                    realm.executeTransaction(realm -> {
                        results.deleteAllFromRealm();
                        realm.copyToRealm(mAdapter.getDiscussions());
                    });
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(String message, Throwable t){
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
        return day + " " + month + ", " + year;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        realm.close();
    }

    private static class ForumAdapter extends RecyclerView.Adapter<ForumAdapter.ForumViewHolder> {

        private final List<Discussion> mDiscussions;
        private final ClickListener mClickListener;

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

        @NotNull
        @Override
        public ForumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ForumViewHolder(inflater.inflate(R.layout.row_forum, parent, false));
        }

        @Override
        public void onBindViewHolder(ForumViewHolder holder, int position) {
            Discussion discussion = mDiscussions.get(position);
            holder.setIsRecyclable(false);
            holder.bind(discussion);
        }

        @Override
        public int getItemCount() {
            return mDiscussions.size();
        }


        public class ForumViewHolder extends RecyclerView.ViewHolder {

            private final ImageView mUserPic;
            private final ImageView mPinned;
            private final TextView mSubject;
            private final TextView mUserName;
            private final TextView mModifiedTime;
            private final HtmlTextView mMessage;

            public ForumViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(v -> {
                    int position = getLayoutPosition();
                    mClickListener.onClick(mDiscussions.get(position), position);
                });

                mUserPic = itemView.findViewById(R.id.user_pic);
                mSubject = itemView.findViewById(R.id.subject);
                mUserName = itemView.findViewById(R.id.user_name);
                mModifiedTime = itemView.findViewById(R.id.modified_time);
                mMessage = itemView.findViewById(R.id.message);
                mPinned = itemView.findViewById(R.id.pinned);

                itemView.findViewById(R.id.click_wrapper).setOnClickListener(v -> {
                    int position = getLayoutPosition();
                    mClickListener.onClick(mDiscussions.get(position), position);
                });

            }

            public void bind(Discussion discussion) {
                Picasso.get().load(discussion.getUserPictureUrl()).into(mUserPic);
                mSubject.setText(discussion.getSubject());
                mUserName.setText(discussion.getUserFullName());
                mModifiedTime.setText(formatDate(discussion.getTimeModified()));
                mMessage.setText(HtmlTextView.parseHtml((discussion.getMessage())));
                if(!discussion.isPinned()){
                    mPinned.setVisibility(View.GONE);
                }
            }
        }
    }
}
