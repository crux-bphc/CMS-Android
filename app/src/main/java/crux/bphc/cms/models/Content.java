package crux.bphc.cms.models;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Created by harsu on 16-12-2016.
 */

public class Content extends RealmObject {
    private String type, filename, filepath, fileurl;
    private int filesize;
    private long timecreated;
    @Index
    private long timemodified;
    private int sortorder;
    private int userid;
    private String author;

    public Content() {
    }

    public Content(String type, String filename, String filepath, String fileurl, int filesize, long timecreated, long timemodified, int sortorder, int userid, String author) {
        this.type = type;
        this.filename = filename;
        this.filepath = filepath;
        this.fileurl = fileurl;
        this.filesize = filesize;
        this.timecreated = timecreated;
        this.timemodified = timemodified;
        this.sortorder = sortorder;
        this.userid = userid;
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFileurl() {
        return fileurl;
    }

    public void setFileurl(String fileurl) {
        this.fileurl = fileurl;
    }

    public int getFilesize() {
        return filesize;
    }

    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    public long getTimecreated() {
        return timecreated;
    }

    public void setTimecreated(long timecreated) {
        this.timecreated = timecreated;
    }

    public long getTimemodified() {
        return timemodified;
    }

    public void setTimemodified(long timemodified) {
        this.timemodified = timemodified;
    }

    public int getSortorder() {
        return sortorder;
    }

    public void setSortorder(int sortorder) {
        this.sortorder = sortorder;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}

