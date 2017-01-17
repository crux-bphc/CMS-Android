package crux.bphc.cms.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import app.MyApplication;
import crux.bphc.cms.CourseModulesActivity;
import crux.bphc.cms.R;
import helper.MoodleServices;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.CourseSection;

import static app.Constants.API_URL;

/**
 * Created by siddhant on 12/21/16.
 */

public class CourseSectionFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final String COURSE_ID_KEY = "id";
    Realm realm;
    ProgressDialog progressDialog;
    View empty;
    private String TOKEN;
    private int courseId;
    private LinearLayout linearLayout;

    public static CourseSectionFragment newInstance(String token, int courseId) {
        CourseSectionFragment courseSectionFragment = new CourseSectionFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(COURSE_ID_KEY, courseId);
        courseSectionFragment.setArguments(args);
        return courseSectionFragment;
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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_course_section, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        linearLayout = (LinearLayout) view.findViewById(R.id.linearLayout);
        empty = view.findViewById(R.id.empty);
        RealmResults<CourseSection> courseSections = realm
                .where(CourseSection.class)
                .equalTo("courseID", courseId)
                .findAll()
                .sort("id", Sort.ASCENDING);

        if (courseSections.isEmpty()) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
        for (CourseSection section : courseSections) {
            addSection(section);
        }

        sendRequest(courseId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null)
            progressDialog.dismiss();
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
                if (progressDialog != null)
                    progressDialog.dismiss();

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
                        for (CourseSection section : sectionList) {
                            addSection(section);
                            section.setCourseID(courseId);
                            realm.copyToRealmOrUpdate(section);

                        }
                    }
                });


            }

            @Override
            public void onFailure(Call<List<CourseSection>> call, Throwable t) {
                //no internet connection
                if (progressDialog != null) {
                    progressDialog.hide();
                    empty.setVisibility(View.VISIBLE);
                    empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            progressDialog.show();
                            sendRequest(courseId);
                            linearLayout.setOnClickListener(null);
                        }
                    });

                }
            }
        });
    }

    private void addSection(final CourseSection section) {
        if (linearLayout == null || getActivity() == null)
            return;
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.row_course_section, linearLayout, false);
        ((TextView) v.findViewById(R.id.sectionName)).setText(section.getName());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CourseModulesActivity.class);
                intent.putExtra("id", section.getId());
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        if (!section.getSummary().isEmpty()) {
            v.findViewById(R.id.descriptionWrapper).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.description)).setText(Html.fromHtml(section.getSummary()));
        }
        linearLayout.addView(v);
    }
}