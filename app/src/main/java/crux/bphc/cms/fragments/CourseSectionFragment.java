package crux.bphc.cms.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import set.CourseSection;

/**
 * Created by siddhant on 12/21/16.
 */

public class CourseSectionFragment extends Fragment {

    private static final String TOKEN_KEY = "token";

    public static CourseSectionFragment newInstance(String token) {
        CourseSectionFragment courseSectionFragment = new CourseSectionFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        courseSectionFragment.setArguments(args);
        return courseSectionFragment;
    }


}
