package crux.bphc.cms.helper;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.fragments.MoreOptionsFragment;
import crux.bphc.cms.models.Content;
import crux.bphc.cms.models.Module;

public class ModulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    CourseDataHandler courseDataHandler;
    private FileManager mFileManager;
    private Context context;
    private LayoutInflater inflater;
    private List<Module> modules;
    private ClickListener clickListener;
    private String courseName;
    private int courseID;
    private int maxDescriptionlines = 3;

    MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public ModulesAdapter(Context context, FileManager fileManager, String courseName, int courseID, MoreOptionsFragment.OptionsViewModel moreOptionsViewModel) {
        this.context = context;
        this.mFileManager = fileManager;
        this.courseName = courseName;
        this.courseID = courseID;
        this.moreOptionsViewModel = moreOptionsViewModel;

        this.inflater = LayoutInflater.from(context);
        modules = new ArrayList<>();
        courseDataHandler = new CourseDataHandler(context);

    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolderResource(inflater.inflate(R.layout.row_course_module_resource, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        ((ViewHolderResource) holder).bind(modules.get(position));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private boolean isNextLabel(int position) {
        position++;
        return modules.size() > position && modules.get(position).getModType() == Module.Type.LABEL;

    }

    class ViewHolderResource extends RecyclerView.ViewHolder {
        HtmlTextView name;
        TextView description;
        ImageView modIcon, more, downloadIcon;
        boolean downloaded = false;
        ProgressBar progressBar;
        View iconWrapper, topDivider, bottomDivider, nameAndDescriptionDivider;
        View clickWrapper, textWrapper, clickWrapperName;
        CardView cardView;

        ViewHolderResource(View itemView) {
            super(itemView);

            iconWrapper = itemView.findViewById(R.id.iconWrapper);
            name = itemView.findViewById(R.id.fileName);
            modIcon = itemView.findViewById(R.id.fileIcon);
            more = itemView.findViewById(R.id.more);
            cardView = itemView.findViewById(R.id.row_course_module_cardView);
            //topDivider = itemView.findViewById(R.id.topDivider);
            //bottomDivider = itemView.findViewById(R.id.bottomDivider);
            description = itemView.findViewById(R.id.description);
            clickWrapper = itemView.findViewById(R.id.clickWrapper);
            clickWrapperName = itemView.findViewById(R.id.clickWrapper);
            textWrapper = itemView.findViewById(R.id.textWrapper);
            downloadIcon = itemView.findViewById(R.id.downloadButton);
            nameAndDescriptionDivider = itemView.findViewById(R.id.nameAndDescriptionDivider);
            description.setMovementMethod(LinkMovementMethod.getInstance());
            description.setLinksClickable(true);

            clickWrapperName.setOnClickListener(view -> {
                if (clickListener != null) {
                    clickListener.onClick(modules.get(getLayoutPosition()), getLayoutPosition());
                }
                markAsReadandUnread(modules.get(getLayoutPosition()), getLayoutPosition(), false);
            });


            more.setOnClickListener(v -> {
                final Module module = modules.get(getLayoutPosition());
                final int position = getLayoutPosition();
                final MoreOptionsFragment.OptionsViewModel moreOptionsViewModel = ModulesAdapter.this.moreOptionsViewModel;
                Observer<MoreOptionsFragment.Option> observer;  // to handle the selection

                /* Set up our options and their handlers */
                ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>();
                if (downloaded) {
                    options.addAll(Arrays.asList(
                            new MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                            new MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                            new MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                            new MoreOptionsFragment.Option(3, "Mark as Unread", R.drawable.eye_off)
                    ));
                    if (module.getModType() == Module.Type.RESOURCE) {
                        options.add(new MoreOptionsFragment.Option(
                                4, "Properties", R.drawable.ic_info));
                    }
                   observer = option -> {
                        if (option == null) return;
                       switch (option.getId()) {
                            case 0:
                                if (module.getContents() != null)
                                    for (Content content : module.getContents()) {
                                        mFileManager.openModuleContent(content);
                                    }
                                break;
                            case 1:
                                if (!module.isDownloadable()) {
                                    return;
                                }

                                for (Content content : module.getContents()) {
                                    Toast.makeText(context, "Downloading file - " + content.getFilename(),
                                            Toast.LENGTH_SHORT).show();
                                    mFileManager.downloadModuleContent(content, module);
                                }
                                break;
                            case 2:
                                if (module.getContents() != null)
                                    for (Content content : module.getContents()) {
                                        mFileManager.shareModuleContent(content);
                                    }
                                break;
                            case 3:
                                markAsReadandUnread(module, position, true);
                                break;
                            case 4:
                                new PropertiesAlertDialog(context, module.getContents().get(0)).show();
                                break;
                        }
                        moreOptionsViewModel.getSelection().removeObservers((AppCompatActivity) context);
                       moreOptionsViewModel.clearSelection();
                   };
                } else {
                    options.addAll(Arrays.asList(
                            new MoreOptionsFragment.Option(0, "Download", R.drawable.download),
                            new MoreOptionsFragment.Option(1, "Share", R.drawable.ic_share),
                            new MoreOptionsFragment.Option(2, "Mark as Unread", R.drawable.eye_off)
                    ));
                    if (module.getModType() == Module.Type.RESOURCE) {
                        options.add(new MoreOptionsFragment.Option(
                                3, "Properties", R.drawable.ic_info));
                    }
                    observer = option -> {
                        if (option == null) return;
                        switch (option.getId()) {
                            case 0:
                                mFileManager.downloadModuleContent(module.getContents().get(0), module);
                                break;
                            case 1:
                                shareModuleLinks(module);
                                break;
                            case 2:
                                markAsReadandUnread(module, position, true);
                                break;
                            case 3:
                                new PropertiesAlertDialog(context, module.getContents().get(0)).show();
                                break;
                        }
                        moreOptionsViewModel.getSelection().removeObservers((AppCompatActivity) context);
                        moreOptionsViewModel.clearSelection();
                    };
                }

                /* Show the fragment and register the observer */
                MoreOptionsFragment moreOptionsFragment = MoreOptionsFragment.newInstance(module.getName(), options);
                moreOptionsFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), moreOptionsFragment.getTag());
                moreOptionsViewModel.getSelection().observe((AppCompatActivity) context, observer);
                markAsReadandUnread(modules.get(getLayoutPosition()), getLayoutPosition(), false);
            });
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        private void markAsReadandUnread(Module module, int position, boolean isNewContent) {
            courseDataHandler.markAsReadandUnread(module.getId(), isNewContent);
            modules.get(position).setNewContent(isNewContent);
            notifyItemChanged(position);
        }

        private void shareModuleLinks(Module module) {
            String toShare = "";
            if (module.getContents() != null)
                for (Content content : module.getContents()) {
                    toShare += content.getFileurl().replace("/moodle", "/fileShare/moodle") + "&courseName=" + courseName.replace(" ", "%20") + "&courseId=" + courseID;
                }

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, toShare);
            itemView.getContext().startActivity(Intent.createChooser(sharingIntent, null));
        }

        void bind(Module module) {
            if (module.isNewContent()) {
                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.unReadModule,value,true);
                textWrapper.setBackgroundColor(value.data);
            } else {
                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.cardBgColor,value,true);
                textWrapper.setBackgroundColor(value.data);
            }

            name.setText(module.getName());
            if (module.getDescription() != null && !module.getDescription().isEmpty()) {
                Spanned htmlDescription = Html.fromHtml(module.getDescription());
                String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                description.setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                makeTextViewResizable(description, maxDescriptionlines, "show more", true);
            } else {
                description.setVisibility(View.GONE);
                nameAndDescriptionDivider.setVisibility(View.GONE);
            }
            iconWrapper.setVisibility(View.VISIBLE);
            if (!module.isDownloadable() || module.getModType() == Module.Type.FOLDER) {
                downloadIcon.setImageResource(R.drawable.eye);
            } else {
                List<Content> contents = module.getContents();
                downloaded = true;
                for (Content content : contents) {
                    if (!mFileManager.isModuleContentDownloaded(content)) {
                        downloaded = false;
                        break;
                    }
                }
                if (downloaded) {
                    downloadIcon.setImageResource(R.drawable.eye);
                } else {
                    downloadIcon.setImageResource(R.drawable.download);
                }
            }
            progressBar.setVisibility(View.GONE);
            if (module.getModType() == Module.Type.LABEL) {
                iconWrapper.setVisibility(View.GONE);
            } else {
                int resourceIcon = module.getModuleIcon();
                if (resourceIcon != -1) {
                    modIcon.setImageResource(module.getModuleIcon());
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    Picasso.get().load(module.getModicon()).into(modIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                }
            }

            /*if (isNextLabel(getLayoutPosition()) || getLayoutPosition() == modules.size() - 1) {
                bottomDivider.setVisibility(View.GONE);
            } else {
                bottomDivider.setVisibility(View.VISIBLE);
            }

            if (module.getModType() == Module.Type.LABEL) {
                topDivider.setVisibility(View.VISIBLE);
            } else {
                topDivider.setVisibility(View.GONE);
            }*/
            more.setVisibility(module.isDownloadable() ? View.VISIBLE : View.GONE);

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

        private  SpannableStringBuilder addClickablePartTextViewResizable(
                final String spannedString, final TextView description, final String spanableText, final boolean viewMore) {

            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannedString);

            if (spannedString.contains(spanableText)) {
                spannableStringBuilder.setSpan(new ClickableSpan() {

                    @Override
                    public void onClick(View widget) {

                        description.setLayoutParams(description.getLayoutParams());
                        description.setText(description.getTag().toString(), TextView.BufferType.SPANNABLE);
                        description.invalidate();
                        if (viewMore) {
                            makeTextViewResizable(description, -1, "show less", false);
                        } else {
                            makeTextViewResizable(description, maxDescriptionlines, "show more", true);
                        }

                    }

                    @Override
                    public void updateDrawState(TextPaint textpaint) {
                        super.updateDrawState(textpaint);
                        TypedValue value = new TypedValue();
                        context.getTheme().resolveAttribute(R.attr.colorAccent,value,true);
                        textpaint.setColor(value.data);
                        textpaint.setUnderlineText(false);
                        textpaint.setFakeBoldText(true);
                    }
                }, spannedString.indexOf(spanableText), spannedString.indexOf(spanableText) + spanableText.length(), 0);

            }
            description.setHighlightColor(Color.TRANSPARENT);
            return spannableStringBuilder;

        }
    }

}