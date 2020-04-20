package crux.bphc.cms.models.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;

import java.util.List;

/**
 * Created by siddhant on 12/17/16.
 */

public class Course implements Parcelable {

    private int id;
    private String fullname, displayname, shortname;
    private int categoryid;
    private String categoryname, summary;
    private int summaryformat;
    // as there is no information available for overviewfiles in api request, the data type of list
    // is kept String
    private List<String> overviewfiles;
    private List<Contact> contacts;
    private List<String> enrollmentmethods;

    public int getSummaryformat() {
        return summaryformat;
    }

    public String getFullname() {

        return fullname;
    }

    public String getSummary() {
        return summary;
    }

    public Course(int id,
                  String fullname, String displayname, String shortname,
                  int categoryid,
                  String categoryname, String summary,
                  int summaryformat,
                  List<String> overviewfiles,
                  List<Contact> contacts,
                  List<String> enrollmentmethods) {
        this.id = id;
        this.fullname = fullname;
        this.displayname = displayname;
        this.shortname = shortname;
        this.categoryid = categoryid;
        this.categoryname = categoryname;
        this.summary = summary;
        this.summaryformat = summaryformat;
        this.overviewfiles = overviewfiles;
        this.contacts = contacts;
        this.enrollmentmethods = enrollmentmethods;
    }

    public String getShortname() {
        return shortname;
    }

    private Course(Parcel source) {
        this.id = source.readInt();
        this.fullname = source.readString();
        this.displayname = source.readString();
        this.shortname = source.readString();
        this.categoryid = source.readInt();
        this.categoryname = source.readString();
        this.summary = source.readString();
        this.summaryformat = source.readInt();
        overviewfiles = source.readArrayList(String.class.getClassLoader());
        contacts = source.readArrayList(Contact.class.getClassLoader());
        enrollmentmethods = source.readArrayList(String.class.getClassLoader());
    }

    public int getId() {
        return id;
    }

    public String getDisplayname() {
        return Html.fromHtml(displayname).toString();
    }

    public String getCategoryname() {
        return categoryname;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(fullname);
        dest.writeString(displayname);
        dest.writeString(shortname);
        dest.writeInt(categoryid);
        dest.writeString(categoryname);
        dest.writeString(summary);
        dest.writeInt(summaryformat);
        dest.writeList(overviewfiles);
        dest.writeList(contacts);
        dest.writeList(enrollmentmethods);
    }

    public static final Parcelable.Creator<Course> CREATOR = new Creator<Course>() {
        @Override
        public Course createFromParcel(Parcel source) {
            return new Course(source);
        }

        @Override
        public Course[] newArray(int size) {
            return new Course[size];
        }
    };
}
