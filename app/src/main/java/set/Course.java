package set;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by harsu on 16-12-2016.
 */

public class Course extends RealmObject {
    @PrimaryKey
    private int id;
    private int idnumber, enrolledusercount;
    private String shortname, fullname, summary, format;

    public Course(int id, int idnumber, int enrolledusercount, String shortname, String fullname, String summary, String format) {
        this.id = id;
        this.idnumber = idnumber;
        this.enrolledusercount = enrolledusercount;
        this.shortname = shortname;
        this.fullname = fullname;
        this.summary = summary;
        this.format = format;
    }

    public Course() {
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIdnumber(int idnumber) {
        this.idnumber = idnumber;
    }

    public void setEnrolledusercount(int enrolledusercount) {
        this.enrolledusercount = enrolledusercount;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getId() {
        return id;
    }

    public int getIdnumber() {
        return idnumber;
    }

    public int getEnrolledusercount() {
        return enrolledusercount;
    }

    public String getSummary() {
        return summary;
    }

    public String getFormat() {
        return format;
    }

    public int getCourseId() {
        return id;
    }

    public String getShortname() {
        return shortname;
    }

    public String getFullname() {
        return fullname;
    }

}
