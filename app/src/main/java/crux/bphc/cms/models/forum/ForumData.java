package crux.bphc.cms.models.forum;

import java.util.List;

/**
 * Created by siddhant on 1/17/17.
 */

public class ForumData {

    public List<Discussion> discussions = null;
    public List<Object> warnings = null;

    public ForumData() {

    }

    public List<Discussion> getDiscussions() {
        return discussions;
    }
}
