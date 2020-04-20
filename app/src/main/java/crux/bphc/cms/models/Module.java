package crux.bphc.cms.models;

import android.text.Html;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.helper.MyFileManager;
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
    private Type modType;

    private boolean isNewContent;

    public Module() {
        modType = Type.DEFAULT;
        isNewContent = false;
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

    public boolean isNewContent() {
        return isNewContent;
    }

    public void setNewContent(boolean newContent) {
        isNewContent = newContent;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Module && ((Module) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Type getModType() {
        if (modType == Type.DEFAULT)
            setModType();
        return modType;
    }

    public void setModType(Type modType) {
        this.modType = modType;
    }

    private void setModType() {
        if (modname.equalsIgnoreCase("resource"))
            modType = Type.RESOURCE;
        else if (modname.equalsIgnoreCase("forum"))
            modType = Type.FORUM;
        else if (modname.equalsIgnoreCase("label"))
            modType = Type.LABEL;
        else if (modname.equalsIgnoreCase("assign"))
            modType = Type.ASSIGNMENT;
        else if (modname.equalsIgnoreCase("folder"))
            modType = Type.FOLDER;
        else if (modname.equalsIgnoreCase("quiz"))
            modType = Type.QUIZ;
        else if (modname.equalsIgnoreCase("url"))
            modType = Type.URL;
        else if (modname.equalsIgnoreCase("page"))
            modType = Type.PAGE;

        else
            modType = Type.DEFAULT;

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
        return Html.fromHtml(name).toString();
    }

    public void setName(String name) {
        this.name = Html.escapeHtml(name);
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

    public boolean hasResourceIcon() {
        if (modType == Type.DEFAULT) {
            return false;
        }
        if (modType == Type.RESOURCE && contents.size() > 0) {
            switch (MyFileManager.getExtension(contents.get(0).getFilename())) {
                case "pdf":
                case "xls":
                case "xlsx":
                case "doc":
                case "docx":
                case "ppt":
                case "pptx":
                    return true;
                default:
                    return false;
            }
        }
        return true;

    }

    /**
     * should be used in association with {@link #hasResourceIcon()} or should be checked for -1
     *
     * @return resource id if icon available, else returns -1
     */
    public int getModuleIcon() {

        switch (getModType()) {
            //  , QUIZ, URL, PAGE, DEFAULT
            case RESOURCE:
                return MyFileManager.getIconFromFileName(getContents().get(0).getFilename());

            case ASSIGNMENT:
                return (R.drawable.book);

            case FOLDER:
                return (R.drawable.folder);

            case URL:
                return (R.drawable.web);

            case PAGE:
                return (R.drawable.page);

            case QUIZ:
                return (R.drawable.quiz);

            case FORUM:
                return (R.drawable.forum);

            case DEFAULT:
                return -1;
        }
        return -1;
    }


    public boolean isDownloadable() {
        return getContents() != null && getContents().size() != 0 && getModType() != Type.URL && getModType() != Type.FORUM && getModType() != Type.PAGE;
    }

    public enum Type {
        RESOURCE, FORUM, LABEL, ASSIGNMENT, FOLDER, QUIZ, URL, PAGE, DEFAULT
    }
}
