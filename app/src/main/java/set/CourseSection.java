package set;

import java.util.List;

/**
 * Created by harsu on 17-12-2016.
 */

public class CourseSection {
    int id;
    String name;
    List<Module> modules;

    public CourseSection(int id, String name, List<Module> modules) {
        this.id = id;
        this.name = name;
        this.modules = modules;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Module> getModules() {
        return modules;
    }
}
