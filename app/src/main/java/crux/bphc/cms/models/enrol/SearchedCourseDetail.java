package crux.bphc.cms.models.enrol;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.core.text.HtmlCompat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model class to represent objects in the <code>courses</code> list from
 * {@link crux.bphc.cms.helper.MoodleServices#getSearchedCourses}.
 *
 * @author Siddhant Kumar Patel (17-Dec-2016)
 */

public class SearchedCourseDetail implements Parcelable {

    @SerializedName("id") private int id;
    @SerializedName("fullname") private String fullName;
    @SerializedName("displayname") private String displayName;
    @SerializedName("shortname") private String shortName;
    @SerializedName("categoryid") private int categoryId;
    @SerializedName("categoryname") private String categoryName;
    @SerializedName("contacts") private List<Contact> contacts;


    public static final Parcelable.Creator<SearchedCourseDetail> CREATOR = new Creator<SearchedCourseDetail>() {
        @Override
        public SearchedCourseDetail createFromParcel(Parcel source) {
            return new SearchedCourseDetail(source);
        }

        @Override
        public SearchedCourseDetail[] newArray(int size) {
            return new SearchedCourseDetail[size];
        }
    };

    @SuppressWarnings("unused")
    public SearchedCourseDetail() {

    }

    @SuppressWarnings("unchecked")
    private SearchedCourseDetail(Parcel source) {
        this.id = source.readInt();
        this.fullName = source.readString();
        this.displayName = source.readString();
        this.shortName = source.readString();
        this.categoryId = source.readInt();
        this.categoryName = source.readString();
        this.contacts = source.readArrayList(Contact.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(fullName);
        dest.writeString(displayName);
        dest.writeString(shortName);
        dest.writeInt(categoryId);
        dest.writeString(categoryName);
        dest.writeList(contacts);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return HtmlCompat.fromHtml(displayName, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public String getFullName() {

        return fullName;
    }

    public String getShortName() {
        return shortName;
    }
}
