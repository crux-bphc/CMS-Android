package crux.bphc.cms.models.course;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * Model class to represent the <code>content</code> of a Module.
 *
 * @author Harshit Agarwal (16-Dec-2016)
 */

public class Content extends RealmObject {

    @SerializedName("filename") private String fileName;
    @SerializedName("fileurl") private String fileUrl;
    @SerializedName("filesize") private int fileSize;
    @SerializedName("timecreated") private long timeCreated;

    @SuppressWarnings("unused")
    public Content() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public int getFileSize() {
        return fileSize;
    }

    public long getTimeCreated() {
        return timeCreated;
    }
}

