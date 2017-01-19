package crux.bphc.cms.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.MoodleServices;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.forum.Discussion;
import set.forum.SiteNews;

import static app.Constants.API_URL;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SiteNewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SiteNewsFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private final int FORUM_ID = 1;
    private final int PER_PAGE = 10;

    private String TOKEN;
    private int page = 0;
    private boolean mLoading = false;
    private boolean mRemaining = true;

    private SiteNewsAdapter mAdapter;
    private ClickListener mClickListener;
    private Realm realm;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public SiteNewsFragment() {
        // Required empty public constructor
    }

    public static SiteNewsFragment newInstance(String token) {
        SiteNewsFragment fragment = new SiteNewsFragment();
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
        realm = MyApplication.getInstance().getRealmInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_site_news, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                mLoading = false;
                mRemaining = true;
                makeRequest();
            }
        });

        mClickListener = new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Discussion discussion = (Discussion) object;
                ForumFragment fragment = ForumFragment.newInstance(discussion.getId());
                FragmentTransaction transaction = getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.addToBackStack(null);
                transaction.replace(R.id.content_main, fragment, "SiteNewsDetail");
                transaction.commit();
                return true;
            }
        };

        mRecyclerView = (RecyclerView) view.findViewById(R.id.site_news);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visible = layoutManager.getChildCount();
                int total = layoutManager.getItemCount();
                int visibleRowIndex = layoutManager.findFirstVisibleItemPosition();

                if (!mLoading) {
                    if (visible + visibleRowIndex >= total) {
                        mLoading = true;
                        makeRequest();
                    }
                }
            }
        });

        RealmResults<Discussion> results = realm.where(Discussion.class).findAll();
        List<Discussion> dbDiscussions = realm.copyFromRealm(results);
        mAdapter = new SiteNewsAdapter(mClickListener, dbDiscussions);
        mRecyclerView.setAdapter(mAdapter);

        makeRequest();
    }

    private void makeRequest() {
        if (!mRemaining)
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        mSwipeRefreshLayout.setRefreshing(true);
        Call<SiteNews> call = moodleServices.getForumDiscussions(TOKEN, FORUM_ID, page, PER_PAGE);
        call.enqueue(new Callback<SiteNews>() {
            @Override
            public void onResponse(Call<SiteNews> call, Response<SiteNews> response) {
                List<Discussion> discussions = response.body().getDiscussions();

                if (discussions.size() == 0) {
                    mRemaining = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    mAdapter.clearDiscussions();
                    mAdapter.addDiscussions(discussions);

                    final RealmResults<Discussion> results = realm.where(Discussion.class).findAll();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            results.deleteAllFromRealm();
                            realm.copyToRealm(mAdapter.getDiscussions());
                        }
                    });

                    page++;
                    mLoading = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<SiteNews> call, Throwable t) {
                System.out.println(t.toString());
                mLoading = false;
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public static  String formatDate(int seconds) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis((long)seconds*1000);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String month = cal.getDisplayName(
                Calendar.MONTH,
                Calendar.SHORT,
                Locale.getDefault());
        int year = cal.get(Calendar.YEAR);
        return String.valueOf(day) + " " + month + ", " + String.valueOf(year);
    }

    private class SiteNewsAdapter extends RecyclerView.Adapter<SiteNewsAdapter.SiteNewsViewHolder> {

        private List<Discussion> mDiscussions = new ArrayList<>();
        private ClickListener mClickListener;

        public SiteNewsAdapter(ClickListener clickListener, List<Discussion> discussions) {
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
        public SiteNewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new SiteNewsViewHolder(inflater.inflate(R.layout.row_site_news, parent, false));
        }

        @Override
        public void onBindViewHolder(SiteNewsViewHolder holder, int position) {
            Discussion discussion = mDiscussions.get(position);
            holder.bind(discussion);
        }

        @Override
        public int getItemCount() {
            return mDiscussions.size();
        }


        public class SiteNewsViewHolder extends RecyclerView.ViewHolder {

            private ImageView mUserPic;
            private TextView mSubject;
            private TextView mUserName;
            private TextView mModifiedTime;
            private TextView mMessage;

            public SiteNewsViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getLayoutPosition();
                        mClickListener.onClick(mDiscussions.get(position), position);
                    }
                });

                mUserPic = (ImageView) itemView.findViewById(R.id.user_pic);
                mSubject = (TextView) itemView.findViewById(R.id.subject);
                mUserName = (TextView) itemView.findViewById(R.id.user_name);
                mModifiedTime = (TextView) itemView.findViewById(R.id.modified_time);
                mMessage = (TextView) itemView.findViewById(R.id.message);
            }

            public void bind(Discussion discussion) {
                Picasso.with(getContext()).load(discussion.getUserpictureurl()).into(mUserPic);
                mSubject.setText(discussion.getSubject());
                mUserName.setText(discussion.getUserfullname());
                mModifiedTime.setText(formatDate(discussion.getTimemodified()));
                mMessage.setText(Html.fromHtml(discussion.getMessage()));
            }
        }
    }
}
