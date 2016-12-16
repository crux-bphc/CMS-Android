package set;

/**
 * Created by harsu on 16-12-2016.
 */

public class Course {

    private int id, idnumber, enrolledusercount;
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
