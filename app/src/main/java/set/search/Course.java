package set.search;

import java.util.List;

/**
 * Created by siddhant on 12/17/16.
 */

public class Course {

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

    public String getDisplayname() {
        return displayname;
    }
}
