package set;

/**
 * Created by harsu on 16-12-2016.
 */

public class Content {
    String type, filename, filepath, fileurl;
    int filesize;
    long timecreated;
    long timemodified;
    int sortorder;
    int userid;
    String author;

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
}
