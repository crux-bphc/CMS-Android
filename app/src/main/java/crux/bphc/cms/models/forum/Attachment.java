package crux.bphc.cms.models.forum;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * Model class to represent attachment of Discussions.
 *
 * @author Siddhant Kumar Patel (01-Jul-2016)
 */

public class Attachment extends RealmObject {

    @SerializedName("filename") private String fileName;
    @SerializedName("mimetype") @SuppressWarnings("unused") private String mimeType;
    @SerializedName("fileurl") private String fileUrl;
    @SerializedName("filesize") private int fileSize;
    @SerializedName("timemodified")  private long timeModified;

    @SuppressWarnings("unused")
    public Attachment() {
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public int getFileSize() { return fileSize; }

    public long getTimeModified() { return timeModified; }
}
