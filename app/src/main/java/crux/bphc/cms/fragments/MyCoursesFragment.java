package crux.bphc.cms.fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.activities.CourseDetailActivity;
import crux.bphc.cms.helper.CourseDataHandler;
import crux.bphc.cms.helper.CourseDownloader;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.interfaces.ClickListener;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import crux.bphc.cms.utils.UserUtils;
import crux.bphc.cms.widgets.HtmlTextView;
import io.realm.Realm;

import static android.content.Context.INPUT_METHOD_SERVICE;


public class MyCoursesFragment extends Fragment {

    private static final int COURSE_SECTION_ACTIVITY = 105;

    Realm realm;

    CourseDataHandler courseDataHandler;

    private EditText mSearch;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View empty;
    private ImageView mSearchIcon;

    private boolean isClearIconSet = false;
    private String mSearchedText = "";
    private int coursesUpdated;

    private List<Course> courses;
    private Adapter mAdapter;

    private MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public MyCoursesFragment() {
        // Required empty public constructor
    }

    public static MyCoursesFragment newInstance() {
        return new MyCoursesFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(getActivity() != null) {
            getActivity().setTitle("My Courses");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        realm = Realm.getDefaultInstance();
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        courses = new ArrayList<>();
        courseDataHandler = new CourseDataHandler(requireContext(), realm);
        courses = courseDataHandler.getCourseList();

        RecyclerView mRecyclerView = view.findViewById(R.id.recyclerView);
        mSearch = view.findViewById(R.id.searchET);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mSearchIcon = view.findViewById(R.id.searchIcon);
        empty = view.findViewById(R.id.empty);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);
        mAdapter = new Adapter(getActivity(), courses);
        mAdapter.setClickListener((object, position) -> {
            Course course = (Course) object;

            Intent intent = new Intent(getActivity(), CourseDetailActivity.class);
            intent.putExtra("courseId", course.getCourseId());
            intent.putExtra("course_name", course.getShortName());
            startActivityForResult(intent, COURSE_SECTION_ACTIVITY);
            return true;
        });


        mAdapter.setCourses(courses);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSearch.addTextChangedListener(new TextWatcher() {
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
                    mSearchIcon.setImageResource(R.drawable.ic_cancel_black_24dp);
                    isClearIconSet = true;
                    mSearchIcon.setOnClickListener(view1 -> {
                        mSearch.setText("");
                        mSearchIcon.setImageResource(R.drawable.ic_search);
                        mSearchIcon.setOnClickListener(null);
                        isClearIconSet = false;
                        InputMethodManager inputManager;
                        if ((inputManager = (InputMethodManager) requireActivity()
                                .getSystemService(INPUT_METHOD_SERVICE)) != null) {
                            View currentFocus;
                            if ((currentFocus = requireActivity().getCurrentFocus()) != null) {
                                inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(),
                                        InputMethodManager.HIDE_NOT_ALWAYS);
                            }
                        }
                    });
                }
                if (mSearchedText.isEmpty()) {
                    mSearchIcon.setImageResource(R.drawable.ic_search);
                    mSearchIcon.setOnClickListener(null);
                    isClearIconSet = false;
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            makeRequest();
        });

        mAdapter.setDownloadClickListener((object, position) -> {
            final Course course = (Course) object;
            if (course.getDownloadStatus() != -1)
                return false;
            course.setDownloadStatus(0);
            mAdapter.notifyItemChanged(position);
            final CourseDownloader courseDownloader = new CourseDownloader(getActivity(),
                    courseDataHandler.getCourseName(course.getId()));
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
                if (message != null && message.contains("Invalid token")) {
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
        courseDataHandler.replaceCourses(courses);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        coursesUpdated = 0;
        if (courses.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        for (Course course : courses) {
            courseRequestHandler.getCourseData(course.getCourseId(),
                    new CourseRequestHandler.CallBack<List<CourseSection>>() {
                        @Override
                        public void onResponse(List<CourseSection> sections) {
                            for (CourseSection courseSection : sections) {
                                List<Module> modules = courseSection.getModules();
                                for (Module module : modules) {
                                    if (module.getModType() == Module.Type.FORUM) {
                                        courseRequestHandler.getForumDiscussions(module.getInstance(), new
                                                CourseRequestHandler.CallBack<List<Discussion>>() {
                                            @Override
                                            public void onResponse(List<Discussion> responseObject) {
                                                for (Discussion d : responseObject) {
                                                    d.setForumId(module.getInstance());
                                                }
                                                List<Discussion> newDiscussions = courseDataHandler.
                                                        setForumDiscussions(module.getInstance(), responseObject);
                                                if (newDiscussions.size() > 0)
                                                    courseDataHandler.markModuleAsReadOrUnread(module, true);
                                            }

                                            @Override
                                            public void onFailure(String message, Throwable t) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                    }
                                }
                            }
                            List<CourseSection> newPartsInSections = courseDataHandler
                                    .isolateNewCourseData(course.getCourseId(), sections);
                            courseDataHandler.replaceCourseData(course.getCourseId(), sections);
                            if (!newPartsInSections.isEmpty()) {
                                coursesUpdated++;
                            }
                            //Refresh the recycler view for the last course
                            if (course.getCourseId() == courses.get(courses.size() - 1).getCourseId()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                mAdapter.notifyDataSetChanged();
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
                if (course.getFullName().toLowerCase().contains(searchedText)) {
                    filteredCourses.add(course);
                }
            }
            mAdapter.setCourses(filteredCourses);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        realm.close();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {

        private final LayoutInflater inflater;
        private final Context context;
        private ClickListener clickListener;
        private ClickListener downloadClickListener;
        private List<Course> mCourseList;

        Adapter(Context context, List<Course> courseList) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            mCourseList = courseList;

        }

        void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

            final HtmlTextView courseNumber;
            final HtmlTextView courseName;
            final View rowClickWrapper;
            final ImageView more;
            final TextView unreadCount;

            MyViewHolder(View itemView) {
                super(itemView);
                courseNumber = itemView.findViewById(R.id.course_number);
                courseName = itemView.findViewById(R.id.course_name);
                more = itemView.findViewById(R.id.more);
                unreadCount = itemView.findViewById(R.id.unreadCount);
                rowClickWrapper = itemView.findViewById(R.id.click_wrapper);

                rowClickWrapper.setOnClickListener(view -> {
                    if (clickListener != null) {
                        int pos = getLayoutPosition();
                        clickListener.onClick(mCourseList.get(pos), pos);
                    }
                });

                itemView.setOnClickListener(view -> {
                    if (clickListener != null) {
                        int pos = getLayoutPosition();
                        clickListener.onClick(mCourseList.get(pos), pos);
                    }
                });

                more.setOnClickListener(view -> {
                    MoreOptionsFragment.OptionsViewModel moreOptionsViewModel = MyCoursesFragment.this.moreOptionsViewModel;
                    Observer<MoreOptionsFragment.Option> observer;  // to handle the selection
                    //Set up our options and their handlers
                    ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>(Arrays.asList(
                            new MoreOptionsFragment.Option(0, "Download course", R.drawable.download),
                            new MoreOptionsFragment.Option(1, "Mark all as read", R.drawable.eye)
                    ));

                    observer = option -> {
                        if (option == null) return;
                        switch (option.getId()) {
                            case 0:
                                confirmDownloadCourse();
                                break;

                            case 1:
                                markAllAsRead(getLayoutPosition());
                                break;
                        }
                        moreOptionsViewModel.getSelection().removeObservers((AppCompatActivity) context);
                        moreOptionsViewModel.clearSelection();
                    };

                    String courseName = courses.get(getLayoutPosition()).getShortName();
                    MoreOptionsFragment moreOptionsFragment = MoreOptionsFragment.newInstance(courseName, options);
                    moreOptionsFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), moreOptionsFragment.getTag());
                    moreOptionsViewModel.getSelection().observe((AppCompatActivity) context, observer);
                });
            }

            void bind(Course course) {
                courseNumber.setText(course.getCourseName()[0]);
                String name = course.getCourseName()[1] + " " + course.getCourseName()[2];
                courseName.setText(name);
                int count = courseDataHandler.getUnreadCount(course.getId());
                unreadCount.setText(DecimalFormat.getIntegerInstance().format(count));
                unreadCount.setVisibility(count == 0 ? View.INVISIBLE : View.VISIBLE);
            }

            void confirmDownloadCourse() {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Confirm Download")
                        .setMessage("Are you sure you want to all the contents of this course?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            if (downloadClickListener != null) {
                                int pos = getLayoutPosition();
                                if (!downloadClickListener.onClick(courses.get(pos), pos)) {
                                    Toast.makeText(getActivity(), "Download already in progress", Toast.LENGTH_SHORT).show();
                                }
                            }
                         })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            public void markAllAsRead(int position){
                int courseId = courses.get(position).getCourseId();
                List<CourseSection> courseSections;
                courseSections = courseDataHandler.getCourseData(courseId);
                courseDataHandler.markAllAsRead(courseSections);
                int count = courseDataHandler.getUnreadCount(courses.get(position).getId());
                unreadCount.setText(DecimalFormat.getIntegerInstance().format(count));
                unreadCount.setVisibility(count == 0 ? View.INVISIBLE : View.VISIBLE);
                Toast.makeText(getActivity(), "Marked all as read", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
