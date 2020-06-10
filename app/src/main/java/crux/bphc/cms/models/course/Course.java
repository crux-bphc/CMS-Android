package crux.bphc.cms.models.course;

import androidx.core.text.HtmlCompat;

import com.google.gson.annotations.SerializedName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import crux.bphc.cms.models.enrol.SearchedCourseDetail;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * @author Harshit Agarwal (16-12-2016)
 */

public class Course extends RealmObject {

    private static final String NAME_REGEX = "([\\w\\d \\-/'&,]+ \\w\\d\\d\\d) ([\\w\\d \\-/():+\"'&.,?]+) ([LTP]\\d*)";

    @PrimaryKey
    @SerializedName("id") private int id;
    @SerializedName("shortname") private String shortname;
    @SerializedName("fullname") String fullname;

    private boolean favorite;

    @Ignore private int downloadStatus;
    @Ignore private int totalFiles;
    @Ignore private int downloadedFiles;


    @SuppressWarnings("unused")
    public Course() {
    }

    public Course(SearchedCourseDetail course) {
        this.id = course.getId();
        shortname = course.getShortName();
        fullname = course.getFullName();
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getDownloadedFiles() {
        return downloadedFiles;
    }

    public void setDownloadedFiles(int downloadedFiles) {
        this.downloadedFiles = downloadedFiles;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void setFavorite(boolean status) {
        favorite = status;
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return id;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getShortName() {
        return HtmlCompat.fromHtml(shortname, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    public String[] getCourseName(){
        String courseName = getShortName();
        // Specifies the string pattern which is to be searched
        final Pattern pattern = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(courseName);
        String[] parts = {courseName , "" , ""};

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                parts[i-1] = matcher.group(i);
            }
        }

        return parts;
    }

    public String getFullName() {
        return fullname;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Course && ((Course) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
