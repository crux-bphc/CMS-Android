package crux.bphc.cms.models;

import android.text.Html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by harsu on 16-12-2016.
 */

public class Course extends RealmObject {
    @Ignore
    private int downloadStatus;
    @Ignore
    private int totalFiles;
    @Ignore
    private int downloadedFiles;
    @PrimaryKey
    private int id;
    private int enrolledusercount;
    private String shortname, fullname, summary, format;

    public Course(int id, int enrolledusercount, String shortname, String fullname, String summary, String format) {
        this.id = id;
        this.enrolledusercount = enrolledusercount;
        this.shortname = shortname;
        this.fullname = fullname;
        this.summary = summary;
        this.format = format;
    }

    public Course() {
    }

    public Course(crux.bphc.cms.models.search.Course course) {
        this.id = course.getId();
        enrolledusercount = 0;
        shortname = course.getShortname();
        fullname = course.getFullname();
        summary = course.getSummary();
        format = "";

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getEnrolledusercount() {
        return enrolledusercount;
    }

    public void setEnrolledusercount(int enrolledusercount) {
        this.enrolledusercount = enrolledusercount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getCourseId() {
        return id;
    }

    public String getShortname() {
        return Html.fromHtml(shortname).toString();
    }

    public String[] getCourseName(){
        String courseName = getShortname();
        String regex =  "([\\w\\d \\-'&,]+ \\w\\d\\d\\d) ([\\w\\d \\-'&,]+) ([LTP]\\d*)";  // Specifies the string pattern which is to be searched
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(courseName);
        String[] parts = {courseName , "" , ""};

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                parts[i-1] = matcher.group(i);
            }
        }

        return parts;

    }

    public void setShortname(String shortname) {
        this.shortname = Html.fromHtml(shortname).toString();
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
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
