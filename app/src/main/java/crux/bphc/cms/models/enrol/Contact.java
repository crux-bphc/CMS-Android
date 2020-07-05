package crux.bphc.cms.models.enrol;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Model class to represent <code>contacts</code> list for a course.
 *
 * @author Siddhant Kumar Patel (17-Dec-2016)
 */

public class Contact implements Parcelable {

    @SerializedName("id") private final int id;
    @SerializedName("fullname") private final String fullName;

    public Contact(Parcel source) {
        this.id = source.readInt();
        this.fullName = source.readString();
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(fullName);
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
