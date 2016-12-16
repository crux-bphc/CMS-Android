package set;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by harsu on 16-12-2016.
 */

public class Course implements Parcelable {

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

    protected Course(Parcel in) {
        id = in.readInt();
        idnumber = in.readInt();
        enrolledusercount = in.readInt();
        shortname = in.readString();
        fullname = in.readString();
        summary = in.readString();
        format = in.readString();
    }

    public static final Creator<Course> CREATOR = new Creator<Course>() {
        @Override
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        @Override
        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    public int getCourseId() {
        return id;
    }

    public String getShortname() {
        return shortname;
    }

    public String getFullname() {
        return fullname;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(idnumber);
        parcel.writeInt(enrolledusercount);
        parcel.writeString(shortname);
        parcel.writeString(fullname);
        parcel.writeString(summary);
        parcel.writeString(format);
    }
}
