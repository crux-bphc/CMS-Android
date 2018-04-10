package crux.bphc.cms.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import app.MyApplication;
import crux.bphc.cms.R;
import helper.MyFileManager;
import io.realm.Realm;
import set.Content;
import set.Module;

public class MoreOptionsFragment extends BottomSheetDialogFragment{

    private static final String IS_DOWNLOAD = "download";
    private static final String COURSE_NAME = "coursename";
    private static final String MODULE_ID = "moduleid";

    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private MyFileManager mFileManager;
    private Module courseModule;
    private int downloaded;
    private String courseName;
    private int moduleId;

    public MoreOptionsFragment() {

    }

    public static MoreOptionsFragment newInstance(int downloaded, String couseName ,int moduleId) {
        Bundle args = new Bundle();
        args.putInt(IS_DOWNLOAD,downloaded);
        args.putString(COURSE_NAME,couseName);
        args.putInt(MODULE_ID,moduleId);
        MoreOptionsFragment fragment = new MoreOptionsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args!=null)
        {
           downloaded = args.getInt(IS_DOWNLOAD);
           courseName = args.getString(COURSE_NAME);
           moduleId = args.getInt(MODULE_ID);
        }
        mFileManager = new MyFileManager(getActivity());
        mFileManager.registerDownloadReceiver();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more_options,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        listView = view.findViewById(R.id.more_options_list);
        arrayAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1);
        if (downloaded == 1) {
            arrayAdapter.add("View");
            arrayAdapter.add("Re-Download");
            arrayAdapter.add("Share");
        } else {
            arrayAdapter.add("Download");
        }
        getDatafromRealm();
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (downloaded == 1) {
                    switch (i) {
                        case 0:
                            if (courseModule.getContents() != null)
                                for (Content content : courseModule.getContents()) {
                                    mFileManager.openFile(content.getFilename(), courseName);

                                }
                            break;
                        case 1:
                            if (!courseModule.isDownloadable()) {
                                return;
                            }

                            for (Content content : courseModule.getContents()) {
                                Toast.makeText(getContext(), "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
                                mFileManager.downloadFile(content, courseModule, courseName);
                            }
                            break;
                        case 2:
                            if (courseModule.getContents() != null)
                                for (Content content : courseModule.getContents()) {
                                    mFileManager.shareFile(content.getFilename(), courseName);
                                }

                    }
                } else {
                    mFileManager.downloadFile(courseModule.getContents().get(0), courseModule, courseName);
                }
            }
        });
    }

    private void getDatafromRealm(){
        Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
        Module module = realm.where(Module.class).equalTo("id",moduleId).findFirst();
        courseModule = module;
        realm.close();

    }
}
