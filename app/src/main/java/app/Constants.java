package app;

import android.app.AlarmManager;

/**
 * Created by harsu on 16-12-2016.
 */

public class Constants {
    public static final int PER_PAGE = 20; // Number of course search results in a page
    // used for intent from CourseSearch to CorseDetailActivity for CourseEnrolFrag
    public static final String COURSE_PARCEL_INTENT_KEY = "course_parcel";
    public static String API_URL = "http://id.bits-hyderabad.ac.in/moodle/";
    public static String TOKEN;
    public static long INTERVAL = AlarmManager.INTERVAL_HALF_DAY;
    public static long TRIGGER_AT = 5 * 1000;
}
