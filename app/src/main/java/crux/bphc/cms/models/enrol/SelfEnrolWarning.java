package crux.bphc.cms.models.enrol;

/**
 * Created by siddhant on 12/19/16.
 */

public class SelfEnrolWarning {

    private String item;
    private int itemid;
    private String warningcode;
    private String message;

    public SelfEnrolWarning(String item, int itemid, String warningcode, String message) {
        this.item = item;
        this.itemid = itemid;
        this.warningcode = warningcode;
        this.message = message;
    }
}
