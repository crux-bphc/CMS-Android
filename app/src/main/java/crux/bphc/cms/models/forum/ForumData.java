package crux.bphc.cms.models.forum;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import crux.bphc.cms.network.MoodleServices;

/**
 * Model class to represent the response from
 * {@link MoodleServices#getForumDiscussions}
 *
 * @author Siddhant Kumar Patel (17-Jan-2017)
 * Created by siddhant on 1/17/17.
 */

public class ForumData {

    @SerializedName("discussions") public List<Discussion> discussions;

    @SuppressWarnings("unused")
    public ForumData() {

    }

    public List<Discussion> getDiscussions() {
        return discussions;
    }
}
