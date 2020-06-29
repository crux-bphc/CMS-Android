package crux.bphc.cms.adapters.delegates;

import android.app.Activity;
import android.graphics.Color;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hannesdorfmann.adapterdelegates4.AdapterDelegate;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.helper.ClickListener;
import crux.bphc.cms.helper.FileManager;
import crux.bphc.cms.helper.HtmlTextView;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.models.Module;

/**
 * Adapter delegate for course modules.
 * @author Abhijeet Viswa
 */
public class CourseModuleDelegate extends AdapterDelegate<List<CourseContent>> {
    /**
     The number of lines to be shown before being collapsed.
     */
    private static final int MAX_DESC_LINES = 3;

    private Activity activity;
    private LayoutInflater inflater;

    private FileManager fileManager;

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
        if (module.isNewContent()) {
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
        if (module.getDescription() != null && !module.getDescription().isEmpty()) {
            Spanned htmlDescription = HtmlCompat.fromHtml(module.getDescription(), HtmlCompat.FROM_HTML_MODE_COMPACT);
            String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
            vh.description.setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
            makeTextViewResizable(vh.description, MAX_DESC_LINES, "show more", true);
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
                Picasso.get().load(module.getModicon()).into(vh.modIcon, new Callback() {
                    @Override
                    public void onSuccess() {
                        vh.progressBar.setVisibility(View.GONE);
                        vh.modIcon.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
            }
        }

        // Bind download/view icon
        if (!module.isDownloadable() || module.getModType() == Module.Type.FOLDER) {
            vh.downloadIcon.setImageResource(R.drawable.eye);
        } else {
            boolean downloaded = module.getContents().stream().
                    allMatch(content -> fileManager.isModuleContentDownloaded(content));
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

    public  void makeTextViewResizable(final TextView description, final int maxLine, final String expandText, final boolean viewMore) {
        if (description.getTag() == null) {
            description.setTag(description.getText());
        }

        ViewTreeObserver vto = description.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                String text;
                int lineEndIndex;
                ViewTreeObserver obs = description.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                if (maxLine == 0) {
                    text = expandText;
                } else if (maxLine>0 && description.getLineCount() > maxLine) {
                    lineEndIndex = description.getLayout().getLineEnd(maxLine - 1);
                    text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                } else if(description.getLineCount() <= maxLine) {
                    text = description.getText().toString();
                } else {
                    lineEndIndex = description.getLayout().getLineEnd(description.getLayout().getLineCount() - 1);
                    text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                }
                description.setText(text);
                description.setMovementMethod(LinkMovementMethod.getInstance());
                description.setText(
                        addClickablePartTextViewResizable(description.getText().toString(), description, expandText,
                                viewMore), TextView.BufferType.SPANNABLE);
            }
        });
    }

    private SpannableStringBuilder addClickablePartTextViewResizable(
            final String spannedString, final TextView description, final String spanableText, final boolean viewMore) {

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannedString);
        if (spannedString.contains(spanableText)) {
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NotNull View widget) {
                    description.setLayoutParams(description.getLayoutParams());
                    description.setText(description.getTag().toString(), TextView.BufferType.SPANNABLE);
                    description.invalidate();
                    if (viewMore) {
                        makeTextViewResizable(description, -1, "show less", false);
                    } else {
                        makeTextViewResizable(description, MAX_DESC_LINES, "show more", true);
                    }

                }

                @Override
                public void updateDrawState(@NotNull TextPaint textpaint) {
                    super.updateDrawState(textpaint);
                    TypedValue value = new TypedValue();
                    activity.getTheme().resolveAttribute(R.attr.colorAccent,value,true);
                    textpaint.setColor(value.data);
                    textpaint.setUnderlineText(false);
                    textpaint.setFakeBoldText(true);
                }
            }, spannedString.indexOf(spanableText), spannedString.indexOf(spanableText) + spanableText.length(), 0);
        }
        description.setHighlightColor(Color.TRANSPARENT);
        return spannableStringBuilder;
    }


    static class CourseModuleViewHolder extends RecyclerView.ViewHolder {
        public HtmlTextView name;
        public TextView description;
        public ImageView modIcon, more, downloadIcon;
        public ProgressBar progressBar;
        public View nameAndDescriptionDivider;
        public View layout_wrapper;
        public View clickWrapper;


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
