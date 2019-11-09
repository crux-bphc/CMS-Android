package helper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import app.MyApplication;
import crux.bphc.cms.R;
import set.Content;
import set.Module;

public class ModulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    CourseDataHandler courseDataHandler;
    private MyFileManager mFileManager;
    private Context context;
    private LayoutInflater inflater;
    private List<Module> modules;
    private ClickListener clickListener;
    private String courseName;
    private int courseID;
    private int maxDescriptionlines = 3;

    public ModulesAdapter(Context context, MyFileManager fileManager, String courseName, int courseID) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        modules = new ArrayList<>();
        mFileManager = fileManager;
        this.courseName = courseName;
        this.courseID = courseID;
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
        View iconWrapper, topDivider, bottomDivider;
        View clickWrapper, textWrapper, clickWrapperName;

        ViewHolderResource(View itemView) {
            super(itemView);

            iconWrapper = itemView.findViewById(R.id.iconWrapper);
            name = itemView.findViewById(R.id.fileName);
            modIcon = itemView.findViewById(R.id.fileIcon);
            more = itemView.findViewById(R.id.more);
            topDivider = itemView.findViewById(R.id.topDivider);
            bottomDivider = itemView.findViewById(R.id.bottomDivider);
            description = itemView.findViewById(R.id.description);
            clickWrapper = itemView.findViewById(R.id.clickWrapper);
            clickWrapperName = itemView.findViewById(R.id.clickWrapper);
            textWrapper = itemView.findViewById(R.id.textWrapper);
            downloadIcon = itemView.findViewById(R.id.downloadButton);
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
                AlertDialog.Builder alertDialog;

                if (MyApplication.getInstance().isDarkModeEnabled()) {
                    alertDialog = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Dialog_Alert);
                } else {
                    alertDialog = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Light_Dialog_Alert);
                }

                alertDialog.setTitle(module.getName());
                alertDialog.setNegativeButton("Cancel", null);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
                if (downloaded) {
                    arrayAdapter.add("View");
                    arrayAdapter.add("Re-Download");
                    arrayAdapter.add("Share");
                    arrayAdapter.add("Mark as Unread");
                    if (module.getModType() == Module.Type.RESOURCE) // Properties are available only for a single file
                        arrayAdapter.add("Properties");

                    alertDialog.setAdapter(arrayAdapter, (dialogInterface, selection) -> {
                        switch (selection) {
                            case 0:
                                if (module.getContents() != null)
                                    for (Content content : module.getContents()) {
                                        mFileManager.openFile(content.getFilename(), courseName);
                                    }
                                break;
                            case 1:
                                if (!module.isDownloadable()) {
                                    return;
                                }

                                for (Content content : module.getContents()) {
                                    Toast.makeText(context, "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
                                    mFileManager.downloadFile(content, module, courseName);
                                }
                                break;
                            case 2:
                                if (module.getContents() != null)
                                    for (Content content : module.getContents()) {
                                        mFileManager.shareFile(content.getFilename(), courseName);
                                    }
                                break;
                            case 3:
                                markAsReadandUnread(module, position, true);
                                break;
                            case 4:
                                mFileManager.showPropertiesDialog(context, module.getContents().get(0));
                                break;
                        }
                    });
                } else {
                    arrayAdapter.add("Download");
                    arrayAdapter.add("Share");
                    arrayAdapter.add("Mark as Unread");
                    if (module.getModType() == Module.Type.RESOURCE) // Properties are available only for a single file
                        arrayAdapter.add("Properties");

                    alertDialog.setAdapter(arrayAdapter, (dialogInterface, selection) -> {
                        switch (selection) {
                            case 0:
                                mFileManager.downloadFile(module.getContents().get(0), module, courseName);
                                break;
                            case 1:
                                shareModuleLinks(module);
                                break;
                            case 2:
                                markAsReadandUnread(module, position, true);
                            case 3:
                                mFileManager.showPropertiesDialog(context, module.getContents().get(0));
                                break;
                        }
                    });
                }

                alertDialog.show();
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
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.navBarSelected));
            } else {
                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.cardBgColor,value,true);
                itemView.setBackgroundColor(value.data);
            }

            name.setText(module.getName());
            if (module.getDescription() != null && !module.getDescription().isEmpty()) {
                Spanned htmlDescription = Html.fromHtml(module.getDescription());
                String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                description.setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                makeTextViewResizable(description, maxDescriptionlines, "show more", true);
            } else {
                description.setVisibility(View.GONE);
            }
            iconWrapper.setVisibility(View.VISIBLE);
            if (!module.isDownloadable() || module.getModType() == Module.Type.FOLDER) {
                downloadIcon.setImageResource(R.drawable.eye);
            } else {
                List<Content> contents = module.getContents();
                downloaded = true;
                for (Content content : contents) {
                    if (!mFileManager.searchFile(content.getFilename())) {
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
                    Picasso.with(context).load(module.getModicon()).into(modIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {

                        }
                    });
                }
            }

            if (isNextLabel(getLayoutPosition()) || getLayoutPosition() == modules.size() - 1) {
                bottomDivider.setVisibility(View.GONE);
            } else {
                bottomDivider.setVisibility(View.VISIBLE);
            }

            if (module.getModType() == Module.Type.LABEL) {
                topDivider.setVisibility(View.VISIBLE);
            } else {
                topDivider.setVisibility(View.GONE);
            }
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