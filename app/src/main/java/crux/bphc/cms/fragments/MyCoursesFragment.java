package crux.bphc.cms.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.R;
import helper.ClickListener;
import helper.MoodleServices;
import helper.UserAccount;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;

import static app.Constants.API_URL;


public class MyCoursesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    RecyclerView mRecyclerView;
    EditText mFilter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<Course> courses;
    private String TOKEN;
    private MyAdapter mAdapter;

    public MyCoursesFragment() {
        // Required empty public constructor
    }

    public static MyCoursesFragment newInstance(String token) {
        MyCoursesFragment fragment = new MyCoursesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(ARG_PARAM1);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_courses, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        courses = new ArrayList<>();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mFilter = (EditText) view.findViewById(R.id.filterET);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);

        mAdapter = new MyAdapter(getActivity());
        mAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Course course = (Course) object;
                //todo implement transfer control to course detail activity
                return true;
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSwipeRefreshLayout.setRefreshing(true);
        makeRequest();
    }

    private void makeRequest() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        UserAccount userAccount = new UserAccount(getActivity());

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<List<Course>> courseCall = moodleServices.getCourses(TOKEN, userAccount.getUserID());

        courseCall.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                List<Course> coursesList = response.body();
                if (coursesList == null) {
                    //todo error token. logout and ask to re-login
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }

                for (Course course : coursesList) {
                    courses.add(course);
                    mAdapter.notifyItemInserted(courses.size() - 1);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                //no internet connection
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        LayoutInflater inflater;
        Context context;
        ClickListener clickListener;

        public MyAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);

        }

        public void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(inflater.inflate(R.layout.row_course, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }

        @Override
        public int getItemCount() {
            return courses != null ? courses.size() : 0;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            TextView courseName;

            MyViewHolder(View itemView) {
                super(itemView);
                courseName = (TextView) itemView.findViewById(R.id.courseName);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (clickListener != null) {
                            int pos = getLayoutPosition();
                            clickListener.onClick(courses.get(pos), pos);
                        }
                    }
                });
            }

            public void bind(Course course) {
                courseName.setText(course.getShortname());
            }
        }

    }


}
