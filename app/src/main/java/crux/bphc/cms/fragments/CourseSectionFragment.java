package crux.bphc.cms.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.MyApplication;
import crux.bphc.cms.CourseModulesActivity;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.ModulesAdapter;
import helper.MoodleServices;
import helper.MyFileManager;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.CourseSection;
import set.Module;

import static app.Constants.API_URL;
import static helper.MyFileManager.DATA_DOWNLOADED;

/**
 * Created by siddhant on 12/21/16.
 */

public class CourseSectionFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final String COURSE_ID_KEY = "id";
    private static final int MODULE_ACTIVITY = 101;
    Realm realm;
    View empty;
    MyFileManager mFileManager;
    List<CourseSection> courseSections;
    private String TOKEN;
    private int courseId;
    private LinearLayout linearLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String courseName;

    public static CourseSectionFragment newInstance(String token, int courseId) {
        CourseSectionFragment courseSectionFragment = new CourseSectionFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(COURSE_ID_KEY, courseId);
        courseSectionFragment.setArguments(args);
        return courseSectionFragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MODULE_ACTIVITY && resultCode == DATA_DOWNLOADED) {

            reloadSections();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            TOKEN = args.getString(TOKEN_KEY);
            courseId = args.getInt(COURSE_ID_KEY);
        }
        realm = MyApplication.getInstance().getRealmInstance();
        mFileManager = new MyFileManager(getActivity());
        mFileManager.registerDownloadReceiver();
        courseSections = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_course_section, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        courseName = MyFileManager.getCourseName(courseId, realm);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        linearLayout = (LinearLayout) view.findViewById(R.id.linearLayout);
        empty = view.findViewById(R.id.empty);
        courseSections = realm.copyFromRealm(realm
                .where(CourseSection.class)
                .equalTo("courseID", courseId)
                .findAll()
                .sort("id", Sort.ASCENDING));

        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        }
        for (CourseSection section : courseSections) {
            addSection(section);
        }

//        sendRequest(courseId);
        mFileManager.setCallback(new MyFileManager.Callback() {
            @Override
            public void onDownloadCompleted(String fileName) {
                reloadSections();
                mFileManager.openFile(fileName, courseName);
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                sendRequest(courseId);
            }
        });
        checkEmpty();
    }

    private boolean checkEmpty() {
        boolean isEmpty = true;
        for (CourseSection courseSection : courseSections) {
            if (!courseSection.getModules().isEmpty()) {
                isEmpty = false;
            }
        }
        ((TextView) empty).setText("No Course Data to display.\nTap to Reload");
        return isEmpty;
    }

    private void reloadSections() {
        mFileManager.reloadFileList();
        linearLayout.removeAllViews();
        for (CourseSection section : courseSections) {
            addSection(section);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();
    }

    private void sendRequest(final int courseId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<List<CourseSection>> courseCall = moodleServices.getCourseContent(TOKEN, courseId);
        System.out.println(courseCall.request().url().toString());
        courseCall.enqueue(new Callback<List<CourseSection>>() {
            @Override
            public void onResponse(Call<List<CourseSection>> call, Response<List<CourseSection>> response) {

                empty.setVisibility(View.GONE);

                final List<CourseSection> sectionList = response.body();
                if (sectionList == null) {
                    //todo not registered, ask to register, change UI, show enroll button
                    return;
                }

                final RealmResults<CourseSection> results = realm
                        .where(CourseSection.class)
                        .equalTo("courseID", courseId)
                        .findAll();
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
                        linearLayout.removeAllViews();
                        courseSections.clear();

                        for (CourseSection section : sectionList) {
                            addSection(section);
                            section.setCourseID(courseId);
                            realm.copyToRealmOrUpdate(section);
                            courseSections.add(section);
                        }
                    }
                });
                mSwipeRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                //no internet connection
                if (courseSections.isEmpty()) {
                    ((TextView) empty).setText("No internet connection.\nTap to retry");
                    empty.setVisibility(View.VISIBLE);
                    empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mSwipeRefreshLayout.setRefreshing(true);
                            sendRequest(courseId);
                            linearLayout.setOnClickListener(null);
                        }
                    });
                }
                Toast.makeText(getActivity(), "Unable to connect to server!", Toast.LENGTH_SHORT).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void addSection(final CourseSection section) {
        if (linearLayout == null || getActivity() == null)
            return;

        if (section.getModules() == null || section.getModules().isEmpty()) {
            return;
        }

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.row_course_section, linearLayout, false);

        ((TextView) v.findViewById(R.id.sectionName)).setText(section.getName());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CourseModulesActivity.class);
                intent.putExtra("id", section.getId());
                startActivityForResult(intent, MODULE_ACTIVITY);
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        if (!section.getSummary().isEmpty()) {
            v.findViewById(R.id.description).setVisibility(View.VISIBLE);
            Spanned htmlDescription = Html.fromHtml(section.getSummary().trim());
            String descriptionWithOutExtraSpace = new String(htmlDescription.toString()).trim();
            ((TextView) v.findViewById(R.id.description)).setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
        }
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recyclerView);

        final ModulesAdapter myAdapter = new ModulesAdapter(getContext(), mFileManager, courseName);
        myAdapter.setModules(section.getModules());
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), RecyclerView.VERTICAL));

        myAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                if (object instanceof Module) {
                    return mFileManager.onClickAction((Module) object, courseName);
                }
                return false;
            }
        });
        linearLayout.addView(v);
    }
}