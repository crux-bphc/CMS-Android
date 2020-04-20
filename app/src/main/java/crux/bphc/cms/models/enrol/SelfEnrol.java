package crux.bphc.cms.models.enrol;

import java.util.List;

/**
 * Created by siddhant on 12/19/16.
 */

public class SelfEnrol {

    private boolean status;
    private List<SelfEnrolWarning> warnings;

    public SelfEnrol(boolean status, List<SelfEnrolWarning> warnings) {
        this.status = status;
        this.warnings = warnings;
    }

    public boolean getStatus() {
        return status;
    }
}
