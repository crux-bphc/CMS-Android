package app;

/**
 * Created by harsu on 16-12-2016.
 */

public class Constants {
    public static final int PER_PAGE = 20; // Number of course search results in a page
    // used for intent from CourseSearch to CorseDetailActivity for CourseEnrolFrag
    public static final String COURSE_PARCEL_INTENT_KEY = "course_parcel";
    public static final String WEBSITE_URL = "https://crux-bphc.github.io/";
    public static final String GITHUB_URL = "https://github.com/CRUx-BPHC/CMS-Android/";
    public static final String GITHUB_URL_ISSUE = GITHUB_URL + "issues/";
    //"https://goo.gl/forms/wKCukHQTCDCp7HsG3";//GITHUB_URL + "issues/";
    public static String API_URL = "https://td.bits-hyderabad.ac.in/moodle/";
    public static String LOGIN_URL = API_URL + "login/index.php";
    public static String COURSE_URL = API_URL + "course/view.php";
    public static String LOGIN_HELP_URL = "https://docs.google.com/document/d/1FUMAdVXCWhrnFT18LpYdeIMwlWPAnOezRweKOE-CRtA/edit";
    public static String TOKEN;
    public static final String DARK_MODE_KEY = "DARK_MODE";

    public static String getFeedbackURL(String username, String id) {
        id += "@hyderabad.bits-pilani.ac.in";
        return "https://docs.google.com/forms/d/e/1FAIpQLScEGd0DLv7qCkZZHmpkPgDxSW_SomUi83YPSVV5g7dPjELyKQ/viewform?entry.1072406768="
                + username
                + "&entry.309049076="
                + id
                + "&entry.1046611324&entry.1695717508&entry.1379315100";

    }

    public static String getCourseURL(int courseId) {
        return COURSE_URL + "?id=" + courseId;
    }

}
