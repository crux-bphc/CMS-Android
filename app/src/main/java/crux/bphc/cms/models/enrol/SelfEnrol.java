package crux.bphc.cms.models.enrol;

import java.util.List;

/**
 * Created by siddhant on 12/19/16.
 */

public class SelfEnrol {

    private boolean status;

    public SelfEnrol(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }
}
