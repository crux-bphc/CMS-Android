package helper;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.R;
import set.Content;
import set.Module;

public class ModulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private MyFileManager mFileManager;
    private Context context;
    private LayoutInflater inflater;
    private List<Module> modules;
    private ClickListener clickListener;
    private String courseName;

    public ModulesAdapter(Context context, MyFileManager fileManager, String courseName) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        modules = new ArrayList<>();
        mFileManager = fileManager;
        this.courseName = courseName;
    }


    public void setModules(List<Module> modules) {
        this.modules = modules;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolderResource(inflater.inflate(R.layout.row_course_module_resource2, parent, false));
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
        TextView name;
        ImageView modIcon, download;
        int downloaded = -1;
        ProgressBar progressBar;
        View iconWrapper, topDivider, bottomDivider;

        ViewHolderResource(View itemView) {
            super(itemView);

            iconWrapper = itemView.findViewById(R.id.iconWrapper);
            name = (TextView) itemView.findViewById(R.id.fileName);
            modIcon = (ImageView) itemView.findViewById(R.id.fileIcon);
            download = (ImageView) itemView.findViewById(R.id.download);
            topDivider = itemView.findViewById(R.id.topDivider);
            bottomDivider = itemView.findViewById(R.id.bottomDivider);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(modules.get(getLayoutPosition()), getLayoutPosition());
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final Module module = modules.get(getLayoutPosition());
                    if (module.getContents() == null || module.getContents().size() == 0) {
                        return false;
                    }
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                    alertDialog.setTitle(module.getName());
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
                    if (downloaded == 1) {
                        arrayAdapter.add("View");
                        arrayAdapter.add("Re-Download");
                        arrayAdapter.add("Share");
                    } else {
                        arrayAdapter.add("Download");
                    }

                    alertDialog.setNegativeButton("Cancel", null);
                    alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (downloaded == 1) {
                                switch (i) {
                                    case 0:
                                        if (module.getContents() != null)
                                            for (Content content : module.getContents()) {
                                                mFileManager.openFile(content.getFilename(), courseName);

                                            }
                                        break;
                                    case 1:
                                        if (module.getContents() == null || module.getContents().size() == 0) {
                                            //todo open label/forum/etc in new activity
                                            // TODO: 19-01-2017 handle all types
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

                                }
                            } else {
                                mFileManager.downloadFile(module.getContents().get(0), module, courseName);
                            }
                        }
                    });
                    alertDialog.show();
                    return true;

                }
            });
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
        }

        void bind(Module module) {
            name.setText(Html.fromHtml(module.getName()));
            iconWrapper.setVisibility(View.VISIBLE);
            if (!module.isDownloadable()) {
                download.setVisibility(View.GONE);
            } else {
                download.setVisibility(View.VISIBLE);
                List<Content> contents = module.getContents();
                downloaded = 1;
                for (Content content : contents) {
                    if (!mFileManager.searchFile(content.getFilename())) {
                        downloaded = 0;
                        break;
                    }
                }
                if (downloaded == 1) {
                    download.setImageResource(R.drawable.eye);
                } else {
                    download.setImageResource(R.drawable.content_save);
                }
            }
            progressBar.setVisibility(View.GONE);
            if (module.getModType() == Module.Type.LABEL) {
                iconWrapper.setVisibility(View.GONE);
            } else {
                int resourceIcon = module.getResourceIcon();
                if (resourceIcon != -1) {
                    modIcon.setImageResource(module.getResourceIcon());
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

            if (isNextLabel(getLayoutPosition())) {
                bottomDivider.setVisibility(View.GONE);
            } else {
                bottomDivider.setVisibility(View.VISIBLE);
            }

            if (module.getModType() == Module.Type.LABEL) {
                topDivider.setVisibility(View.VISIBLE);
            } else {
                topDivider.setVisibility(View.GONE);
            }

        }
    }

}