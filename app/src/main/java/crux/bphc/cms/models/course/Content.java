package crux.bphc.cms.models.course;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

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
    @SerializedName("timemodified") private long timeModified;

    private int moduleId;

    @SuppressWarnings("unused")
    public Content() {
    }

    public Content(Content content) {
        this.fileName = content.fileName;
        this.fileUrl = content.fileUrl;
        this.fileSize = content.fileSize;
        this.timeCreated = content.timeCreated;
        this.timeModified = content.timeModified;

        this.moduleId = content.moduleId;
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

    public long getTimeModified() {
        return timeModified;
    }

    public void setModuleId(int moduleId){
        this.moduleId = moduleId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Content && obj.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, fileUrl, timeModified);
    }
}

