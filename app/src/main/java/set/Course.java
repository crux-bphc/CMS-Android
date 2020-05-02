package set;

import android.text.Html;

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

    public Course(set.search.Course course) {
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
        String regex =  " ";    // Specifies the string pattern which is to be searched
        Pattern pattern = Pattern.compile(regex , Pattern.CASE_INSENSITIVE);    // Used to perform case insensitive search of the string
        String[] parts = pattern.split(courseName);

        String courseCode = parts[0] + " " + parts[1];
        String name = "";
        if(parts.length > 2) {
            for (int i = 2; i < parts.length; i++) {
                name = name + parts[i] + " ";
            }
        }
        String[] result = new String[2];
        result[0] = courseCode;
        result[1] = name;

        return result;

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
