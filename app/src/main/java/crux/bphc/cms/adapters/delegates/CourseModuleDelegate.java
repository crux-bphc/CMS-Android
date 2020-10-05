package crux.bphc.cms.adapters.delegates;

import android.app.Activity;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.interfaces.ClickListener;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.io.FileManager;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.widgets.CollapsibleTextView;
import crux.bphc.cms.widgets.HtmlTextView;

/**
 * Adapter delegate for course modules.
 *
 * @see crux.bphc.cms.adapters.CourseContentAdapter
 *
 * @author Abhijeet Viswa
 */
public class CourseModuleDelegate extends AdapterDelegate<List<CourseContent>> {
    private final Activity activity;
    private final LayoutInflater inflater;

    private final FileManager fileManager;

    private final ClickListener clickWrapperClickListener;
    private final ClickListener moreOptionsClickListener;

    public CourseModuleDelegate(Activity activity, FileManager fileManager, ClickListener clickWrapperClickListener,
                                ClickListener moreOptionsClickListener) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.clickWrapperClickListener = clickWrapperClickListener;
        this.moreOptionsClickListener = moreOptionsClickListener;
        inflater = activity.getLayoutInflater();
    }

    @Override
    protected boolean isForViewType(@NonNull List<CourseContent> items, int position) {
        return items.get(position) instanceof Module;
    }

    @NonNull
    @Override
    protected RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new  CourseModuleViewHolder(inflater.inflate(R.layout.row_course_module, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull List<CourseContent> items, int position,
                                    @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
        CourseModuleViewHolder vh = (CourseModuleViewHolder) holder;
        Module module = (Module) items.get(position);

        bindClickListeners(vh, module, position);
        setLayoutTheme(vh, module);
        bindNameAndDescription(vh, module);
        bindIcons(vh, module);
        bindMoreOptions(vh, module);
    }

    private void setLayoutTheme(CourseModuleViewHolder vh, Module module) {
        TypedValue value = new TypedValue();
        if (module.isUnread()) {
            activity.getTheme().resolveAttribute(R.attr.unReadModule, value, true);
        } else {
            activity.getTheme().resolveAttribute(R.attr.cardBgColor, value, true);
        }
        vh.layout_wrapper.setBackgroundColor(value.data);
    }

    private void bindClickListeners(CourseModuleViewHolder vh, Module module, int position) {
        vh.clickWrapper.setOnClickListener(v -> clickWrapperClickListener.onClick(module, position));
        vh.more.setOnClickListener(v -> moreOptionsClickListener.onClick(module, position));
    }

    private void bindNameAndDescription(CourseModuleViewHolder vh, Module module) {
        vh.name.setText(module.getName());
        if (!module.getDescription().isEmpty()) {
            vh.description.setVisibility(View.VISIBLE);
            vh.nameAndDescriptionDivider.setVisibility(View.VISIBLE);
            vh.description.setFullText(new SpannableStringBuilder()
                    .append(HtmlCompat.fromHtml(module.getDescription(), HtmlCompat.FROM_HTML_MODE_COMPACT)));
            vh.description.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            vh.description.setVisibility(View.GONE);
            vh.nameAndDescriptionDivider.setVisibility(View.GONE);
        }

    }

    private void bindIcons(CourseModuleViewHolder vh, Module module) {
        // Bind file icon
        vh.progressBar.setVisibility(View.GONE);
        if (module.getModuleIcon() != -1) {
            vh.modIcon.setImageResource(module.getModuleIcon());
        } else {
            // Either no icon is needed or it needs to be fetched
            vh.modIcon.setVisibility(View.GONE);
            if (module.getModType() != Module.Type.LABEL) {
                vh.progressBar.setVisibility(View.VISIBLE);
                Glide.with(vh.modIcon.getContext()).addDefaultRequestListener(new RequestListener<Object>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Object> target, boolean isFirstResource) {
                        Toast.makeText(vh.modIcon.getContext(), String.format("Load failed for %s",
                                module.getModIcon()), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Object resource, Object model,
                                                   Target<Object> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        vh.progressBar.setVisibility(View.GONE);
                        vh.modIcon.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).load(Uri.parse(module.getModIcon())).into(vh.modIcon);
            }
        }

        // Bind download/view icon
        if (!module.isDownloadable() || module.getModType() == Module.Type.FOLDER) {
            vh.downloadIcon.setImageResource(R.drawable.eye);
        } else {
            boolean downloaded = module.getContents().stream().
                    allMatch(fileManager::isModuleContentDownloaded);
            if (downloaded) {
                vh.downloadIcon.setImageResource(R.drawable.eye);
            } else {
                vh.downloadIcon.setImageResource(R.drawable.download);
            }
        }
    }

    private void bindMoreOptions(CourseModuleViewHolder vh, Module module) {
        vh.more.setVisibility(module.isDownloadable() ? View.VISIBLE : View.GONE);
    }

    static class CourseModuleViewHolder extends RecyclerView.ViewHolder {
        public final HtmlTextView name;
        public final CollapsibleTextView description;
        public final ImageView modIcon, more, downloadIcon;
        public final ProgressBar progressBar;
        public final View nameAndDescriptionDivider;
        public final View layout_wrapper;
        public final View clickWrapper;


        public CourseModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            progressBar = itemView.findViewById(R.id.progress_bar);
            modIcon = itemView.findViewById(R.id.icon);
            more = itemView.findViewById(R.id.more);
            description = itemView.findViewById(R.id.description);
            layout_wrapper = itemView.findViewById(R.id.layout_wrapper);
            downloadIcon = itemView.findViewById(R.id.download);
            nameAndDescriptionDivider = itemView.findViewById(R.id.nameAndDescriptionDivider);
            clickWrapper = itemView.findViewById(R.id.click_wrapper);
        }
    }
}
