package crux.bphc.cms.models.forum;

import io.realm.RealmObject;

/**
 * Created by siddhant on 1/17/17.
 */

public class Attachment extends RealmObject {

    private String filename;
    private String mimetype;
    private String fileurl;
    private int filesize;
    private long timemodified;

    public Attachment() {
    }

    public String getFilename() {
        return filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getFileurl() {
        return fileurl;
    }

    public int getFileSize() { return filesize; }

    public long getTimemodified() { return timemodified; }
}
