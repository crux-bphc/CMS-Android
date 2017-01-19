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

    public ModulesAdapter(Context context, MyFileManager fileManager) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        modules = new ArrayList<>();
        mFileManager = fileManager;
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


    class ViewHolderResource extends RecyclerView.ViewHolder {
        TextView name;
        ImageView modIcon, download;
        int downloaded = -1;
        ProgressBar progressBar;
        View iconWrapper;

        ViewHolderResource(View itemView) {
            super(itemView);

            iconWrapper = itemView.findViewById(R.id.iconWrapper);
            name = (TextView) itemView.findViewById(R.id.fileName);
            modIcon = (ImageView) itemView.findViewById(R.id.fileIcon);
            download = (ImageView) itemView.findViewById(R.id.download);
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
                                                mFileManager.openFile(content.getFilename());

                                            }
                                        break;
                                    case 1:
                                        if (module.getContents() == null || module.getContents().size() == 0) {
                                            //todo open label/forum/etc in new activity
                                            return;
                                        }

                                        for (Content content : module.getContents()) {
                                            mFileManager.downloadFile(content, module);
                                        }
                                        break;
                                    case 2:
                                        if (module.getContents() != null)
                                            for (Content content : module.getContents()) {
                                                mFileManager.shareFile(content.getFilename());
                                            }

                                }
                            } else {
                                mFileManager.downloadFile(module.getContents().get(0), module);
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
            if (module.getContents() == null || module.getContents().size() == 0 || module.getModType() == Module.Type.URL) {
                download.setVisibility(View.GONE);
            } else {
                download.setVisibility(View.VISIBLE);
                List<Content> contents = module.getContents();
                downloaded = 1;
                for (Content content : contents) {
                    if (!mFileManager.searchFile(content.getFilename(), false)) {
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

        }
    }

}