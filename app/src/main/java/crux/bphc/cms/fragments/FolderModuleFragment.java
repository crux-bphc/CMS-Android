package crux.bphc.cms.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.MyFileManager;
import io.realm.Realm;
import set.Content;
import set.Module;

public class FolderModuleFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final String MODULE_ID_KEY = "moduleID";
    private static final String COURSE_NAME_KEY = "courseName";

    private String TOKEN = "";
    private int MODULE_INSTANCE = 0;
    private String COURSE_NAME = "";
    private String MOD_NAME = "";

    private Module module;
    private List<Content> contents;

    private FolderModuleFragment.FolderModuleAdapter mAdapter;
    private ClickListener mClickListener;
    private Realm realm;
    private MyFileManager mFileManager;

    private RecyclerView mRecyclerView;

    public FolderModuleFragment() {

    }

    public static FolderModuleFragment newInstance(String token, int moduleId, String courseName) {
        FolderModuleFragment fragment = new FolderModuleFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(MODULE_ID_KEY, moduleId);
        args.putString(COURSE_NAME_KEY, courseName);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(TOKEN_KEY);
            MODULE_INSTANCE = getArguments().getInt(MODULE_ID_KEY);
            COURSE_NAME = getArguments().getString(COURSE_NAME_KEY);
        }

        realm = MyApplication.getInstance().getRealmInstance();

        // If we NPE, lite
        module = realm.where(Module.class).equalTo("instance", MODULE_INSTANCE).findFirst();
        contents = module.getContents();

        mFileManager = new MyFileManager(getActivity(), COURSE_NAME);
        mFileManager.registerDownloadReceiver();
        mFileManager.setCallback(new MyFileManager.Callback() {
            @Override
            public void onDownloadCompleted(String fileName) {
                mAdapter.notifyDataSetChanged();
                mFileManager.openFile(fileName, COURSE_NAME);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_folder_module, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mClickListener = (object, position) -> {
            Content content = (Content) object;
            downloadOrOpenFile(content, false);
            return true;  // why is this here?
        };

        mRecyclerView = view.findViewById(R.id.files);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new FolderModuleFragment.FolderModuleAdapter(mClickListener, new ArrayList<Content>());
        mRecyclerView.setAdapter(mAdapter);

        updateContents();
    }

    private void updateContents() {
        mAdapter.setContents(contents);
    }

    private void downloadOrOpenFile(Content content, boolean forceDownload) {
        if (forceDownload || !mFileManager.searchFile(content.getFilename())) {
            Toast.makeText(getActivity(), "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
            mFileManager.downloadFile(content, module, COURSE_NAME);
        } else {
            mFileManager.openFile(content.getFilename(), COURSE_NAME);
        }
    }


    private class FolderModuleAdapter extends RecyclerView.Adapter<FolderModuleFragment.FolderModuleAdapter.FolderModuleViewHolder> {

        private List<Content> mContents;
        private ClickListener mClickListener;

        public FolderModuleAdapter(ClickListener clickListener, List<Content> contents) {
            mClickListener = clickListener;
            mContents = contents;
        }

        public void setContents(List<Content> contents) {
            mContents.addAll(contents);
            notifyDataSetChanged();
        }

        public List<Content> getContents() {
            return mContents;
        }

        public void clearContents() {
            mContents.clear();
            notifyDataSetChanged();
        }

        @Override
        public FolderModuleFragment.FolderModuleAdapter.FolderModuleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new FolderModuleFragment.FolderModuleAdapter.FolderModuleViewHolder(inflater.inflate(R.layout.row_folder_module_file, parent, false));
        }

        @Override
        public void onBindViewHolder(FolderModuleFragment.FolderModuleAdapter.FolderModuleViewHolder holder, int position) {
            Content content = mContents.get(position);
            holder.bind(content);
        }

        @Override
        public int getItemCount() {
            return mContents.size();
        }


        public class FolderModuleViewHolder extends RecyclerView.ViewHolder {

            private TextView fileName;
            private ImageView fileIcon;
            private ImageView download;
            private ImageView ellipsis;
            private LinearLayout clickWrapper;
            
            public FolderModuleViewHolder(View itemView) {
                super(itemView);

                fileName = itemView.findViewById(R.id.fileName);
                fileIcon = itemView.findViewById(R.id.fileIcon);
                download = itemView.findViewById(R.id.downloadButton);
                ellipsis = itemView.findViewById(R.id.more);
                clickWrapper = itemView.findViewById(R.id.clickWrapper);
            }

            public void bind(Content content) {
                fileName.setText(content.getFilename());

                int icon = MyFileManager.getIconFromFileName(content.getFilename());
                if (icon != -1) {
                    fileIcon.setImageResource(icon);
                } else {
                    fileIcon.setImageResource(R.drawable.quiz); // Quiz because that's a question mark xD
                }

                boolean downloaded = mFileManager.searchFile(content.getFilename());
                if (downloaded) {
                    download.setImageResource(R.drawable.eye);
                }

                clickWrapper.setOnClickListener(view -> {
                    if (mClickListener != null) {
                        mClickListener.onClick(contents.get(getLayoutPosition()), getLayoutPosition());
                    }
                });

                ellipsis.setOnClickListener(view -> {
                    AlertDialog.Builder alertDialog;

                    if (MyApplication.getInstance().isDarkModeEnabled()) {
                        alertDialog = new AlertDialog.Builder(getContext(), R.style.Theme_AppCompat_Dialog_Alert);
                    } else {
                        alertDialog = new AlertDialog.Builder(getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
                    }

                    alertDialog.setTitle(content.getFilename());

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1);
                    alertDialog.setNegativeButton("Cancel", null);

                    if (downloaded) {
                        arrayAdapter.add("View");
                        arrayAdapter.add("Re-Download");
                        arrayAdapter.add("Share");
                        arrayAdapter.add("Properties");

                        alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        switch (i) {
                                            case 0:
                                                downloadOrOpenFile(content, false);
                                                break;
                                            case 1:
                                                downloadOrOpenFile(content, true);
                                                break;
                                            case 2:
                                                mFileManager.shareFile(content.getFilename(), COURSE_NAME);
                                                break;
                                            case 3:
                                                mFileManager.showPropertiesDialog(getActivity(), content);
                                                break;
                                        }
                                    }
                                }
                        );
                    } else {
                        arrayAdapter.add("Download");
                        arrayAdapter.add("Properties");

                        alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        downloadOrOpenFile(content, false);
                                        break;
                                    case 1:
                                        mFileManager.showPropertiesDialog(getActivity(), content);
                                        break;
                                }
                            }
                        });
                    }

                    alertDialog.show();
                });
            }
        }
    }
}
