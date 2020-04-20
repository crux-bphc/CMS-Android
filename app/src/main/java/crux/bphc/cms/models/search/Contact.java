package crux.bphc.cms.models.search;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by siddhant on 12/17/16.
 */

public class Contact implements Parcelable {

    private int id;
    private String fullname;

    public Contact(int id, String fullname) {
        this.id = id;
        this.fullname = fullname;
    }

    public Contact(Parcel source) {
        this.id = source.readInt();
        this.fullname = source.readString();
    }

    public String getFullname() {
        return fullname;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(fullname);
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
