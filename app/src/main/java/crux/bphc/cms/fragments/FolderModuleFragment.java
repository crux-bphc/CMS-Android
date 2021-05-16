package crux.bphc.cms.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.interfaces.ClickListener;
import crux.bphc.cms.core.FileManager;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.utils.FileUtils;
import crux.bphc.cms.widgets.PropertiesAlertDialog;
import io.realm.Realm;
import io.realm.RealmList;
import kotlin.Unit;

public class FolderModuleFragment extends Fragment {

    private static final String MODULE_ID_KEY = "moduleID";
    private static final String COURSE_NAME_KEY = "courseName";

    Realm realm;

    private int MODULE_INSTANCE = 0;
    private String COURSE_NAME = "";

    private Module module;
    private RealmList<Content> contents;

    private FolderModuleFragment.FolderModuleAdapter mAdapter;
    private FileManager mFileManager;

    private MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public FolderModuleFragment() {

    }

    public static FolderModuleFragment newInstance(int moduleId, String courseName) {
        FolderModuleFragment fragment = new FolderModuleFragment();
        Bundle args = new Bundle();
        args.putInt(MODULE_ID_KEY, moduleId);
        args.putString(COURSE_NAME_KEY, courseName);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            MODULE_INSTANCE = getArguments().getInt(MODULE_ID_KEY);
            COURSE_NAME = getArguments().getString(COURSE_NAME_KEY);
        }

        mFileManager = new FileManager(requireActivity(), COURSE_NAME, fileName -> {
            onDownloadComplete(fileName);
            return Unit.INSTANCE;
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        realm = Realm.getDefaultInstance();
        return inflater.inflate(R.layout.fragment_folder_module, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);

        ClickListener mClickListener = (object, position) -> {
            Content content = (Content) object;
            downloadOrOpenFile(content, false);
            return true;
        };

        module = realm.where(Module.class).equalTo("instance", MODULE_INSTANCE).findFirst();
        contents = module != null ? module.getContents() : null;

        RecyclerView mRecyclerView = view.findViewById(R.id.files);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new FolderModuleFragment.FolderModuleAdapter(mClickListener, new ArrayList<>());
        mRecyclerView.setAdapter(mAdapter);

        updateContents();
    }

    private void updateContents() {
        mAdapter.setContents(contents);
    }

    private void downloadOrOpenFile(Content content, boolean forceDownload) {
        if (forceDownload || !mFileManager.isModuleContentDownloaded(content)) {
            Toast.makeText(getActivity(), "Downloading file - " + content.getFileName(), Toast.LENGTH_SHORT).show();
            mFileManager.downloadModuleContent(content, module);
        } else {
            mFileManager.openModuleContent(content);
        }
    }

    private void onDownloadComplete(String fileName) {
        mAdapter.notifyDataSetChanged();
        Content content;
        if (contents != null
                && (content = contents.where().equalTo("fileName", fileName).findFirst()) != null) {
            mFileManager.openModuleContent(content);
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        realm.close();
    }

    private class FolderModuleAdapter extends RecyclerView.Adapter<FolderModuleFragment.FolderModuleAdapter.FolderModuleViewHolder> {

        private final List<Content> mContents;
        private final ClickListener mClickListener;

        public FolderModuleAdapter(ClickListener clickListener, List<Content> contents) {
            mClickListener = clickListener;
            mContents = contents;
        }

        public void setContents(List<Content> contents) {
            mContents.addAll(contents);
            notifyDataSetChanged();
        }

        @NotNull
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

            private final TextView fileName;
            private final ImageView fileIcon;
            private final ImageView download;
            private final ImageView ellipsis;
            private final LinearLayout clickWrapper;
            
            public FolderModuleViewHolder(View itemView) {
                super(itemView);

                fileName = itemView.findViewById(R.id.name);
                fileIcon = itemView.findViewById(R.id.icon);
                download = itemView.findViewById(R.id.download);
                ellipsis = itemView.findViewById(R.id.more);
                clickWrapper = itemView.findViewById(R.id.click_wrapper);
            }

            public void bind(Content content) {
                fileName.setText(content.getFileName());

                int icon = FileUtils.INSTANCE.getDrawableIconFromFileName(content.getFileName());
                if (icon != -1) {
                    fileIcon.setImageResource(icon);
                } else {
                    fileIcon.setImageResource(R.drawable.quiz); // Quiz because that's a question mark xD
                }

                boolean downloaded = mFileManager.isModuleContentDownloaded(content);
                if (downloaded) {
                    download.setImageResource(R.drawable.eye);
                }

                clickWrapper.setOnClickListener(view -> {
                    if (mClickListener != null) {
                        mClickListener.onClick(contents.get(getLayoutPosition()), getLayoutPosition());
                    }
                });

                ellipsis.setOnClickListener(view -> {
                    ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>();
                    Observer<MoreOptionsFragment.Option> observer;  // to handle the selection

                    if (downloaded) {
                        options.addAll(Arrays.asList(
                                new MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                                new MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                                new MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                                new MoreOptionsFragment.Option(3, "Properties", R.drawable.ic_info)
                        ));

                        observer = option -> {
                            if (option == null) return;
                            switch (option.getId()) {
                                case 0:
                                    downloadOrOpenFile(content, false);
                                    break;
                                case 1:
                                    downloadOrOpenFile(content, true);
                                    break;
                                case 2:
                                    mFileManager.shareModuleContent(content);
                                    break;
                                case 3:
                                    new PropertiesAlertDialog(getContext(), content).show();
                            }
                            moreOptionsViewModel.getSelection().removeObservers(requireActivity());
                            moreOptionsViewModel.clearSelection();
                        };
                    } else {
                        options.addAll(Arrays.asList(
                                new MoreOptionsFragment.Option(0, "Download", R.drawable.download),
                                new MoreOptionsFragment.Option(1, "Properties", R.drawable.ic_info)
                        ));

                        observer = option -> {
                            if (option == null) return;
                            switch (option.getId()) {
                                case 0:
                                    downloadOrOpenFile(content, false);
                                    break;
                                case 1:
                                    new PropertiesAlertDialog(getContext(), content).show();
                                    break;
                            }
                        };
                        moreOptionsViewModel.getSelection().removeObservers(requireActivity());
                        moreOptionsViewModel.clearSelection();
                    }
                    MoreOptionsFragment fragment = MoreOptionsFragment.newInstance(content.getFileName(), options);
                    fragment.show(requireActivity().getSupportFragmentManager(),
                            fragment.getTag());
                    moreOptionsViewModel.getSelection().observe(requireActivity(), observer);
                });
            }
        }
    }
}
