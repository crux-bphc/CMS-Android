package crux.bphc.cms.models.course;

import androidx.core.text.HtmlCompat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import crux.bphc.cms.interfaces.CourseContent;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * @author Harshit Agarwal (17-Dec-2016)
 */

public class CourseSection extends RealmObject implements CourseContent {

    @PrimaryKey
    @SerializedName("id") private int id;
    @SerializedName("name") private String name;
    @SerializedName("summary") private String summary;
    @SerializedName("modules") private RealmList<Module> modules;

    int courseId;

    @SuppressWarnings("unused")
    public CourseSection() {
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return HtmlCompat.fromHtml(name, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    public String getSummary() {
        return summary;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules.clear();
        this.modules.addAll(modules);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CourseSection && ((CourseSection) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
