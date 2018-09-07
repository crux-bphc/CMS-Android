package crux.bphc.cms.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.CourseDetailActivity;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.CourseDataHandler;
import helper.CourseDownloader;
import helper.CourseRequestHandler;
import helper.HtmlTextView;
import helper.UserUtils;
import set.Course;
import set.CourseSection;
import set.Module;
import set.forum.Discussion;

import static android.content.Context.INPUT_METHOD_SERVICE;


public class MyCoursesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final int COURSE_SECTION_ACTIVITY = 105;
    RecyclerView mRecyclerView;
    EditText mFilter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<Course> courses;
    View empty;
    ImageView mFilterIcon;
    boolean isClearIconSet = false;
    List<CourseDownloader.DownloadReq> requestedDownloads;
    String mSearchedText = "";
    private String TOKEN;
    private MyAdapter mAdapter;
    private int coursesUpdated;

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
        courseDataHandler = new CourseDataHandler(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_courses, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == COURSE_SECTION_ACTIVITY) {
            courses = courseDataHandler.getCourseList();
            filterMyCourses(mSearchedText);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestedDownloads = new ArrayList<>();

        empty = view.findViewById(R.id.empty);
        courses = new ArrayList<>();
        courses = courseDataHandler.getCourseList();

        mRecyclerView = view.findViewById(R.id.recyclerView);
        mFilter = view.findViewById(R.id.filterET);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mFilterIcon = view.findViewById(R.id.filterIcon);

        mAdapter = new MyAdapter(getActivity(), courses);
        mAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                Course course = (Course) object;

                Intent intent = new Intent(getActivity(), CourseDetailActivity.class);
                intent.putExtra("id", course.getCourseId());
                intent.putExtra("course_name", course.getShortname());
                startActivityForResult(intent, COURSE_SECTION_ACTIVITY);
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


        mAdapter.setDownloadClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, final int position) {
                final Course course = (Course) object;
                if (course.getDownloadStatus() != -1)
                    return false;
                course.setDownloadStatus(0);
                mAdapter.notifyItemChanged(position);
                final CourseDownloader courseDownloader = new CourseDownloader(getActivity(),
                        CourseDataHandler.getCourseName(course.getId()));
                courseDownloader.setDownloadCallback(new CourseDownloader.DownloadCallback() {
                    @Override
                    public void onCourseDataDownloaded() {
                        course.setDownloadedFiles(courseDownloader.getDownloadedContentCount(course.getId()));
                        course.setTotalFiles(courseDownloader.getTotalContentCount(course.getId()));
                        if (course.getTotalFiles() == course.getDownloadedFiles()) {
                            Toast.makeText(getActivity(), "All files already downloaded", Toast.LENGTH_SHORT).show();
                            course.setDownloadStatus(-1);
                        } else {
                            course.setDownloadStatus(1);
                        }
                        mAdapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onCourseContentDownloaded() {
                        course.setDownloadedFiles(course.getDownloadedFiles() + 1);

                        if (course.getDownloadedFiles() == course.getTotalFiles()) {
                            course.setDownloadStatus(-1);
                            courseDownloader.unregisterReceiver();
                            //todo notification all files downloaded for this course
                        }
                        mAdapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), "Check your internet connection", Toast.LENGTH_SHORT).show();
                        courses.get(position).setDownloadStatus(-1);
                        mAdapter.notifyItemChanged(position);
                        courseDownloader.unregisterReceiver();
                    }
                });
                courseDownloader.downloadCourseData(course.getCourseId());

                return true;

            }
        });

        checkEmpty();
        if (courses.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            makeRequest();
        }
    }

    private void checkEmpty() {
        if (courses.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    CourseDataHandler courseDataHandler;

    private void makeRequest() {
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        courseRequestHandler.getCourseList(new CourseRequestHandler.CallBack<List<Course>>() {
            @Override
            public void onResponse(List<Course> courseList) {
                courses.clear();
                courses.addAll(courseList);
                checkEmpty();
                filterMyCourses(mSearchedText);
                updateCourseContent(courses);
            }

            @Override
            public void onFailure(String message, Throwable t) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (t.getMessage().contains("Invalid token")) {
                    Toast.makeText(
                            getActivity(),
                            "Invalid token! Probably your token was reset.",
                            Toast.LENGTH_SHORT).show();
                    UserUtils.logoutAndClearBackStack(getActivity());
                    return;
                }
                Toast.makeText(getActivity(), "Unable to connect to server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCourseContent(List<Course> courses) {
        courseDataHandler.setCourseList(courses);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        coursesUpdated = 0;
        if (courses.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        for (Course course : courses) {
            courseRequestHandler.getCourseData(course.getCourseId(),
                    new CourseRequestHandler.CallBack<List<CourseSection>>() {
                        @Override
                        public void onResponse(List<CourseSection> responseObject) {
                            for (CourseSection courseSection : responseObject) {
                                List<Module> modules = courseSection.getModules();
                                for (Module module : modules) {
                                    if (module.getModType() == Module.Type.FORUM) {
                                        courseRequestHandler.getForumDiscussions(module.getInstance(), new CourseRequestHandler.CallBack<List<Discussion>>() {
                                            @Override
                                            public void onResponse(List<Discussion> responseObject) {
                                                for (Discussion d : responseObject) {
                                                    d.setForumId(module.getInstance());
                                                }
                                                List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(module.getInstance(), responseObject);
                                                if (newDiscussions.size() > 0)
                                                    courseDataHandler.markAsReadandUnread(module.getId(), true);
                                            }

                                            @Override
                                            public void onFailure(String message, Throwable t) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                    }
                                }
                            }
                            List<CourseSection> newPartsinSections = courseDataHandler.setCourseData(course.getCourseId(), responseObject);
                            if (newPartsinSections.size() > 0) {
                                coursesUpdated++;
                            }
                            //Refresh the recycler view for the last course
                            if (course.getCourseId() == courses.get(courses.size() - 1).getCourseId()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                mRecyclerView.getAdapter().notifyDataSetChanged();
                                String message;
                                if (coursesUpdated == 0) {
                                    message = getString(R.string.upToDate);
                                } else {
                                    message = getResources().getQuantityString(R.plurals.noOfCoursesUpdated, coursesUpdated, coursesUpdated);
                                }
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(String message, Throwable t) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
        }
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

            HtmlTextView courseName;
            View download;
            ImageView downloadIcon;
            ProgressBar progressBar;
            TextView downloadText, unreadCount;


            MyViewHolder(View itemView) {
                super(itemView);
                courseName = itemView.findViewById(R.id.courseName);
                download = itemView.findViewById(R.id.download);
                downloadText = itemView.findViewById(R.id.downloadText);
                progressBar = itemView.findViewById(R.id.progressBar);
                downloadIcon = itemView.findViewById(R.id.downloadIcon);
                unreadCount = itemView.findViewById(R.id.unreadCount);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (clickListener != null) {
                            int pos = getLayoutPosition();
                            clickListener.onClick(mCourseList.get(pos), pos);
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
                int count = courseDataHandler.getUnreadCount(course.getId());
                unreadCount.setText(Integer.toString(count));
                unreadCount.setVisibility(count == 0 ? View.INVISIBLE : View.VISIBLE);
            }
        }

    }


}
