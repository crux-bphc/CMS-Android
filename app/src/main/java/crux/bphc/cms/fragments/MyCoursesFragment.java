package crux.bphc.cms.fragments;


import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.MyApplication;
import crux.bphc.cms.CourseDetailActivity;
import crux.bphc.cms.MainActivity;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.CourseDownloader;
import helper.MoodleServices;
import helper.UserAccount;
import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.Course;
import set.CourseSection;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static app.Constants.API_URL;


public class MyCoursesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    RecyclerView mRecyclerView;
    EditText mFilter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<Course> courses;
    Realm realm;

    View empty;
    ImageView mFilterIcon;
    boolean isClearIconSet = false;
    CourseDownloader courseDownloader;
    List<CourseDownloader.DownloadReq> requestedDownloads;
    String mSearchedText = "";
    private String TOKEN;
    private MyAdapter mAdapter;
    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            for (CourseDownloader.DownloadReq downloadReq : requestedDownloads) {
                if (courseDownloader.searchFile(downloadReq.getFileName())) {
                    requestedDownloads.remove(downloadReq);
                    courses.get(downloadReq.getPosition())
                            .setDownloadedFiles(courses.get(downloadReq.getPosition()).getDownloadedFiles() + 1);
                    if (courses.get(downloadReq.getPosition()).getDownloadedFiles() == courses.get(downloadReq.getPosition()).getTotalFiles()) {
                        courses.get(downloadReq.getPosition()).setDownloadStatus(-1);
                        //todo notification all files downloaded for this course
                    }
                    mAdapter.notifyItemChanged(downloadReq.getPosition());
                    return;
                }
            }
        }
    };

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
        realm = MyApplication.getInstance().getRealmInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_courses, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(onComplete);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        requestedDownloads = new ArrayList<>();

        RealmResults<Course> result = realm.where(Course.class).findAll();

        empty = view.findViewById(R.id.empty);
        courses = new ArrayList<>();
        courses = realm.copyFromRealm(result);


        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mFilter = (EditText) view.findViewById(R.id.filterET);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mFilterIcon = (ImageView) view.findViewById(R.id.filterIcon);

        mAdapter = new MyAdapter(getActivity(), courses);
        mAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Course course = (Course) object;

                Intent intent = new Intent(getActivity(), CourseDetailActivity.class);
                intent.putExtra("id", course.getCourseId());
                intent.putExtra("course_name", course.getShortname());
                startActivity(intent);
                return true;
            }
        });


        mAdapter.setCourses(courses);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        mSwipeRefreshLayout.setRefreshing(true);
        mFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mSearchedText = s.toString().toLowerCase().trim();
                filterMyCourses(mSearchedText);

                if (!isClearIconSet) {
                    mFilterIcon.setImageResource(R.drawable.ic_clear_black_24dp);
                    isClearIconSet = true;
                    mFilterIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mFilter.setText("");
                            mFilterIcon.setImageResource(R.drawable.filter);
                            mFilterIcon.setOnClickListener(null);
                            isClearIconSet = false;
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    });
                }
                if (mSearchedText.isEmpty()) {
                    mFilterIcon.setImageResource(R.drawable.filter);
                    mFilterIcon.setOnClickListener(null);
                    isClearIconSet = false;
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                makeRequest();
            }
        });
        makeRequest();
        courseDownloader = new CourseDownloader(getActivity());

        mAdapter.setDownloadClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, final int position) {
                final Course course = (Course) object;
                if (course.getDownloadStatus() != -1)
                    return false;
                courses.get(position).setDownloadStatus(0);
                mAdapter.notifyItemChanged(position);

                courseDownloader.downloadCourseData(course.getCourseId(), new CourseDownloader.DownloadCallback() {
                    @Override
                    public void onSuccess(Object object) {

                        RealmResults<CourseSection> courseSections = realm.where(CourseSection.class).equalTo("courseID", course.getCourseId()).findAll();
                        for (CourseSection section : courseSections) {
                            courseDownloader.downloadSection(section, new CourseDownloader.DownloadCallback() {

                                @Override
                                public void onSuccess(Object object) {
                                    if (object instanceof CourseDownloader.DownloadReq) {
                                        CourseDownloader.DownloadReq downloadReq = (CourseDownloader.DownloadReq) object;
                                        downloadReq.setPosition(position);
                                        requestedDownloads.add(downloadReq);
                                    }
                                }

                                @Override
                                public void onFailure() {
                                    int totalFiles = 0;
                                    for (CourseDownloader.DownloadReq downloadReq : requestedDownloads) {

                                        if (downloadReq.getPosition() == position) {
                                            totalFiles++;
                                        }
                                    }
                                    courses.get(position).setTotalFiles(totalFiles);
                                    if (totalFiles == 0) {
                                        Toast.makeText(getActivity(), "All files already downloaded", Toast.LENGTH_SHORT).show();
                                        courses.get(position).setDownloadStatus(-1);
                                    } else {
                                        courses.get(position).setDownloadStatus(1);
                                    }
                                    mAdapter.notifyItemChanged(position);
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), "Check your internet connection", Toast.LENGTH_SHORT).show();
                        courses.get(position).setDownloadStatus(-1);
                        mAdapter.notifyItemChanged(position);
                    }
                });
                return true;
            }
        });

        checkEmpty();
    }

    private void checkEmpty() {
        if (courses.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    private void makeRequest() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        System.out.println("Inside make request");

        UserAccount userAccount = new UserAccount(getActivity());

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<ResponseBody> courseCall = moodleServices.getCourses(TOKEN, userAccount.getUserID());
        System.out.println(courseCall.request().url().toString());

        courseCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseString = response.body().string();
                    if (responseString.contains("Invalid token")) {
                        ((MainActivity) getActivity()).logout();
                        return;
                    }
                    Gson gson = new Gson();
                    final List<Course> coursesList = gson
                            .fromJson(responseString, new TypeToken<List<Course>>() {
                            }.getType());

                    courses.clear();
                    courses.addAll(coursesList);
//                    mAdapter.setCourses(courses);
                    filterMyCourses(mSearchedText);
                    System.out.println("number of courses in coursesList: " + coursesList.size());
                    mSwipeRefreshLayout.setRefreshing(false);


                    final RealmResults<Course> results = realm.where(Course.class).findAll();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            results.deleteAllFromRealm();
                            realm.copyToRealm(coursesList);
                        }
                    });
                    checkEmpty();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println(t.toString());
                //no internet connection
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    private void filterMyCourses(String searchedText) {
        if (searchedText.isEmpty()) {
            mAdapter.setCourses(courses);

        } else {
            List<Course> filteredCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.getFullname().toLowerCase().contains(searchedText)) {
                    filteredCourses.add(course);
                }
            }
            mAdapter.setCourses(filteredCourses);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        LayoutInflater inflater;
        Context context;
        ClickListener clickListener;
        ClickListener downloadClickListener;
        private List<Course> mCourseList;

        MyAdapter(Context context, List<Course> courseList) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            mCourseList = courseList;

        }

        void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(inflater.inflate(R.layout.row_course, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.bind(mCourseList.get(position));
        }

        @Override
        public int getItemCount() {
            return mCourseList != null ? mCourseList.size() : 0;
        }

        void setCourses(List<Course> courseList) {
            mCourseList = courseList;
            for (int i = 0; i < mCourseList.size(); i++) {
                mCourseList.get(i).setDownloadStatus(-1);
            }
            notifyDataSetChanged();
        }

        public void setDownloadClickListener(ClickListener downloadClickListener) {
            this.downloadClickListener = downloadClickListener;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            TextView courseName;
            View download;
            ImageView downloadIcon;
            ProgressBar progressBar;
            TextView downloadText;


            MyViewHolder(View itemView) {
                super(itemView);
                courseName = (TextView) itemView.findViewById(R.id.courseName);
                download = itemView.findViewById(R.id.download);
                downloadText = (TextView) itemView.findViewById(R.id.downloadText);
                progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
                downloadIcon = (ImageView) itemView.findViewById(R.id.downloadIcon);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (clickListener != null) {
                            int pos = getLayoutPosition();
                            clickListener.onClick(courses.get(pos), pos);
                        }
                    }
                });
                download.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Confirm Download");
                        alertDialog.setMessage("Are you sure, you want to download the course?");
                        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (downloadClickListener != null) {
                                    int pos = getLayoutPosition();
                                    if (!downloadClickListener.onClick(courses.get(pos), pos)) {
                                        Toast.makeText(getActivity(), "Download already in progress", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        alertDialog.setNegativeButton("Cancel", null);
                        alertDialog.show();

                    }
                });
            }


            void bind(Course course) {
                courseName.setText(course.getShortname());
                if (course.getDownloadStatus() == -1) {
                    progressBar.setVisibility(View.GONE);
                    downloadIcon.setVisibility(View.VISIBLE);
                    downloadText.setText("Download Course");
                } else {
                    //the course is downloading and is in midway
                    progressBar.setVisibility(View.VISIBLE);
                    downloadIcon.setVisibility(View.INVISIBLE);
                    if (course.getDownloadStatus() == 0)   // downloading section data
                        downloadText.setText("Downloading course information... ");
                    else if (course.getDownloadStatus() == 1)
                        downloadText.setText("Downloading files... ( " + course.getDownloadedFiles() + " / " + course.getTotalFiles() + " )");
                    else
                        downloadText.setText("Downloaded");
                }
            }
        }

    }


}
