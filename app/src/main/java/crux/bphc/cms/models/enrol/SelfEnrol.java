package crux.bphc.cms.models.enrol;

import com.google.gson.annotations.SerializedName;

import crux.bphc.cms.network.MoodleServices;

/**
 * Model class to represent the response from
 * {@link MoodleServices#selfEnrolUserInCourse}.
 *
 * @author Siddhant Kumar Patel (19-Dec-2016)
 */

public class SelfEnrol {

    @SerializedName("status") private boolean status;

    @SuppressWarnings("unused")
    public SelfEnrol() {
    }

    public boolean getStatus() {
        return status;
    }
}
