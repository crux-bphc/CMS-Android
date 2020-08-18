package crux.bphc.cms.models.course;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.utils.FileUtils;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 *
 * @author Harshit Agarwal (16-Dec-2016)
 */


public class Module extends RealmObject implements CourseContent {
    public enum Type {
        RESOURCE, FORUM, LABEL, ASSIGNMENT, FOLDER, QUIZ, URL, PAGE, DEFAULT, BOOK
    }

    @PrimaryKey
    @SerializedName("id") private int id;
    @SerializedName("instance") private int instance;
    @SerializedName("name") private String name;
    @SerializedName("url") private String url;
    @SerializedName("modicon") private String modIcon;
    @SerializedName("modname") private String modName;
    @SerializedName("description") private String description;
    @SerializedName("contents") private RealmList<Content> contents;

    private int courseSectionId;
    private boolean isUnread;

    @Ignore private Type modType;

    @SuppressWarnings("unused")
    public Module() {
        modType = Type.DEFAULT;
        contents = new RealmList<>();
    }

    public Module(Module module) {
        this.id = module.id;
        this.instance = module.instance;
        this.name = module.name;
        this.url = module.url;
        this.modIcon = module.modIcon;
        this.modName = module.modName;
        this.description = module.description;
        this.contents = new RealmList<>();

        for (Content content : module.getContents()) {
            this.contents.add(new Content(content));
        }

        this.courseSectionId = module.courseSectionId;
        this.modType = module.modType;
        this.isUnread = module.isUnread;
    }


    /**
     * @return True if module is a downloadable resource
     */
    public boolean isDownloadable() {
        return hasContents()
                && getModType() != Type.URL
                && getModType() != Type.FORUM
                && getModType() != Type.PAGE;
    }

    /**
     * @return resource id if icon available, else -1
     */
    public int getModuleIcon() {
        switch (getModType()) {
            case RESOURCE:
                if (hasContents()) {
                    Content content = getContents().first();
                    if (content == null) return -1;
                    return FileUtils.getDrawableIconFromFileName(content.getFileName());
                } else {
                    return -1;
                }
            case ASSIGNMENT:
                return R.drawable.assignment;
            case FOLDER:
                return R.drawable.folder;
            case URL:
                return R.drawable.web;
            case PAGE:
                return R.drawable.page;
            case QUIZ:
                return R.drawable.quiz;
            case FORUM:
                return R.drawable.forum;
            case BOOK:
                return R.drawable.book;
            case DEFAULT:
                return -1;
        }
        return -1;
    }

    private Type inferModuleTypefromModuleName() {
        switch (modName.toLowerCase()) {
            case "resource":
                return Type.RESOURCE;
            case "forum":
                return Type.FORUM;
            case "label":
                return Type.LABEL;
            case "assign":
                return Type.ASSIGNMENT;
            case "folder":
                return Type.FOLDER;
            case "quiz":
                return Type.QUIZ;
            case "url":
                return Type.URL;
            case "page":
                return Type.PAGE;
            default:
                return Type.DEFAULT;
        }
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setIsUnread(boolean isUnread) {
        this.isUnread = isUnread;
    }

    public String getDescription() {
        return description;
    }

    public Type getModType() {
        if (modType == Type.DEFAULT)
            modType = inferModuleTypefromModuleName();
        return modType;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return HtmlCompat.fromHtml(name, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    public int getInstance() {
        return instance;
    }

    public String getModIcon() {
        return modIcon;
    }

    public void setCourseSectionId(int courseSectionId) {
        this.courseSectionId = courseSectionId;
    }

    @NonNull
    public RealmList<Content> getContents() {
        if (contents == null) contents = new RealmList<>();
        return contents;
    }

    public void setContents(@NonNull List<Content> contents) {
        this.contents.addAll(contents);
    }

    public boolean hasContents() {
        return getContents().size() != 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Module && ((Module) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
