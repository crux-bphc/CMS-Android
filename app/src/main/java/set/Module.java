package set;

import java.util.List;

/**
 * Created by harsu on 16-12-2016.
 */

public class Module {
    int id;
    String url, name;
    int instance;
    String modicon, modname, modplural;
    List<Content> contents;

    public Module(int id, String url, String name, int instance, String modicon, String modname, String modplural, List<Content> contents) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.instance = instance;
        this.modicon = modicon;
        this.modname = modname;
        this.modplural = modplural;
        this.contents = contents;
    }
}
