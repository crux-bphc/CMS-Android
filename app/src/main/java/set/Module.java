package set;

import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by harsu on 16-12-2016.
 */


public class Module extends RealmObject {
    @PrimaryKey
    private int id;
    private String url, name;
    private int instance;
    private String modicon, modname, modplural, description;
    private RealmList<Content> contents;
    @Ignore
    private int modType;

    public Module() {
        modType = -1;
    }

    public Module(int id, String url, String name, int instance, String modicon, String modname, String modplural, String description, RealmList<Content> contents) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.instance = instance;
        this.modicon = modicon;
        this.modname = modname;
        this.modplural = modplural;
        this.description = description;
        this.contents = contents;
        setModType();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getModType() {
        if (modType == -1)
            setModType();
        return modType;
    }

    public void setModType(int modType) {
        this.modType = modType;
    }

    private void setModType() {
        if (modname.equalsIgnoreCase("resource"))
            modType = 0;
        else if (modname.equalsIgnoreCase("forum"))
            modType = 1;
        else if (modname.equalsIgnoreCase("label"))
            modType = 2;
        else if (modname.equalsIgnoreCase("assign"))
            modType = 3;
        else if (modname.equalsIgnoreCase("folder"))
            modType = 4;
        else
            modType = 100;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public String getModicon() {
        return modicon;
    }

    public void setModicon(String modicon) {
        this.modicon = modicon;
    }

    public String getModname() {
        return modname;
    }

    public void setModname(String modname) {
        this.modname = modname;
        setModType();
    }

    public String getModplural() {
        return modplural;
    }

    public void setModplural(String modplural) {
        this.modplural = modplural;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(RealmList<Content> contents) {
        this.contents = contents;
    }
}
