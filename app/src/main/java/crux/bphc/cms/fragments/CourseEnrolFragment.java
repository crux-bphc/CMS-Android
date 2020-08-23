package crux.bphc.cms.fragments;

import android.app.ProgressDialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.network.MoodleServices;
import crux.bphc.cms.models.course.Course;
import crux.bphc.cms.models.enrol.Contact;
import crux.bphc.cms.models.enrol.SearchedCourseDetail;
import crux.bphc.cms.models.enrol.SelfEnrol;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static crux.bphc.cms.app.Constants.API_URL;

public class CourseEnrolFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final String COURSE_KEY = "course";

    private Realm realm;

    private String TOKEN;
    private SearchedCourseDetail course;

    public CourseEnrolFragment() {
        // Required empty public constructor
    }

    public static CourseEnrolFragment newInstance(String token, SearchedCourseDetail course) {
        CourseEnrolFragment fragment = new CourseEnrolFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putParcelable(COURSE_KEY, course);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(TOKEN_KEY);
            course = getArguments().getParcelable(COURSE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        realm = Realm.getDefaultInstance();
        return inflater.inflate(R.layout.fragment_course_enrol, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView mCourseDisplayName = view.findViewById(R.id.course_enrol_course_display_name);
        mCourseDisplayName.setText(course.getDisplayName());

        TextView mCourseCategory = view.findViewById(R.id.course_enrol_course_category);
        mCourseCategory.setText(course.getCategoryName());

        LinearLayout mTeachers = view.findViewById(R.id.course_enrol_teachers);
        List<Contact> teachers = course.getContacts();
        TextView noTeacherInfo = view.findViewById(R.id.course_enrol_teacher_no_info);
        if (teachers == null || teachers.size() == 0) {
            noTeacherInfo.setVisibility(View.VISIBLE);
        } else {
            noTeacherInfo.setVisibility(View.GONE);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) noTeacherInfo.getLayoutParams();
            layoutParams.setMargins(0, 8, 0, 0);

            TypedValue typedValue = new TypedValue();
            requireActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            TypedArray arr = requireActivity().obtainStyledAttributes(typedValue.data, new int[]{
                    android.R.attr.textColorSecondary});
            int textColor = arr.getColor(0, -1);
            arr.recycle();

            for (Contact contact : teachers) {
                TextView teacherName = new TextView(getActivity());
                teacherName.setLayoutParams(layoutParams);
                teacherName.setText(contact.getFullName());
                teacherName.setTextColor(textColor);
                teacherName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                teacherName.setPaddingRelative(8, 0, 8, 0);
                mTeachers.addView(teacherName);
            }
        }

        Button mEnrolButton = view.findViewById(R.id.course_enrol_enrol_button);
        mEnrolButton.setOnClickListener(v -> createEnrollmentConfirmationDialog().show());

    }

    private AlertDialog createEnrollmentConfirmationDialog() {
        AlertDialog.Builder builder;

        if (MyApplication.getInstance().isDarkModeEnabled()) {
            builder = new AlertDialog.Builder(requireContext(),R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(requireContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        builder.setMessage(R.string.course_enrol_confirmation_msg);

        builder.setPositiveButton("OK", (dialog, which) -> enrolInCourse());

        builder.setNegativeButton("Cancel", (dialog, which) -> { });
        return builder.create();
    }

    private void enrolInCourse() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Attempting to enroll");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<SelfEnrol> call = moodleServices.selfEnrolUserInCourse(TOKEN, course.getId());
        System.out.println(call.request().url());
        call.enqueue(new Callback<SelfEnrol>() {
            @Override
            public void onResponse(@NotNull Call<SelfEnrol> call, @NotNull Response<SelfEnrol> response) {
                progressDialog.dismiss();
                SelfEnrol body;
                if ((body = response.body()) != null && body.getStatus()) {
                    Toast.makeText(
                            getActivity(),
                            "Successfully enrolled in " + course.getDisplayName(),
                            Toast.LENGTH_SHORT).show();

                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

                    CourseContentFragment courseSectionFragment = CourseContentFragment
                            .newInstance(TOKEN, course.getId());
                    Course courseSet = new Course(course);

                    realm.executeTransaction(r -> r.copyToRealmOrUpdate(courseSet));

                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.course_section_enrol_container, courseSectionFragment)
                            .commit();
                } else {
                    Toast.makeText(getActivity(), "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<SelfEnrol> call, @NotNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "Unable to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        realm.close();
    }
}
