package crux.bphc.cms.fragments;


import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.helper.HtmlTextView;
import crux.bphc.cms.helper.MyFileManager;
import crux.bphc.cms.helper.PropertiesAlertDialog;
import io.realm.Realm;
import crux.bphc.cms.models.forum.Attachment;
import crux.bphc.cms.models.forum.Discussion;


public class DiscussionFragment extends Fragment implements MyFileManager.Callback {

    private  String mFolderName;
    private int id;

    private Realm realm;
    private MyFileManager mFileManager;

    private ImageView mUserPic;
    private TextView mSubject;
    private TextView mUserName;
    private TextView mTimeModified;
    private HtmlTextView mMessage;
    private LinearLayout mAttachmentContainer;

    private MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public DiscussionFragment() {
        // Required empty public constructor
    }

    public static DiscussionFragment newInstance(int id, String folderName) {
        DiscussionFragment fragment = new DiscussionFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putString("folderName", folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt("id");
            mFolderName = getArguments().getString("folderName");
        }
        realm = MyApplication.getInstance().getRealmInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_discussion, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);

        mFileManager = new MyFileManager(getActivity(), mFolderName);
        mFileManager.registerDownloadReceiver();
        mFileManager.setCallback(this);

        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();

        mUserPic = view.findViewById(R.id.user_pic);
        Picasso.get().load(discussion.getUserpictureurl()).into(mUserPic);

        mSubject = view.findViewById(R.id.subject);
        mSubject.setText(discussion.getSubject());

        mUserName = view.findViewById(R.id.user_name);
        mUserName.setText(discussion.getUserfullname());

        mTimeModified = view.findViewById(R.id.modified_time);
        mTimeModified.setText(ForumFragment.formatDate(discussion.getTimemodified()));

        mMessage = view.findViewById(R.id.message);
        mMessage.setText(HtmlTextView.parseHtml(discussion.getMessage()));
        mMessage.setMovementMethod(LinkMovementMethod.getInstance());

        mAttachmentContainer = view.findViewById(R.id.attachments);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (discussion.getAttachments().size() == 0) mAttachmentContainer.setVisibility(View.GONE);

        for (final Attachment attachment : discussion.getAttachments()) {
            View attachmentView = inflater.inflate(
                    R.layout.row_attachment_detail_forum,
                    mAttachmentContainer);

            TextView fileName = attachmentView.findViewById(R.id.fileName);
            fileName.setText(attachment.getFilename());

            LinearLayout clickWrapper = attachmentView.findViewById(R.id.clickWrapper);
            ImageView download = attachmentView.findViewById(R.id.downloadButton);
            ImageView ellipsis = attachmentView.findViewById(R.id.more);

            boolean downloaded = mFileManager.searchFile(attachment.getFilename());
            if (downloaded) {
                download.setImageResource(R.drawable.eye);
                ellipsis.setVisibility(View.VISIBLE);
            } else {
                download.setImageResource(R.drawable.download);
                ellipsis.setVisibility(View.GONE);
            }

            clickWrapper.setOnClickListener(v -> {
                if (!downloaded) {
                    Toast.makeText(getActivity(), "Downloading file - " + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                    mFileManager.downloadFile(attachment.getFilename(), attachment.getFileurl(), "", mFolderName, true);
                } else {
                    mFileManager.openFile(attachment.getFilename());
                }
            });

            ellipsis.setOnClickListener(v -> {
                // Check if downloaded once again, for consistency (user downloaded and then opens ellipsis immediately)
                boolean isDownloaded = mFileManager.searchFile(attachment.getFilename());
                if (isDownloaded) {
                    ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>();
                    Observer<MoreOptionsFragment.Option> observer;  // to handle the selection

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
                                mFileManager.openFile(attachment.getFilename());
                                break;
                            case 1:
                                Toast.makeText(getActivity(), "Downloading file - " + attachment.getFilename(),
                                        Toast.LENGTH_SHORT).show();
                                mFileManager.downloadFile(attachment.getFilename(), attachment.getFileurl(), "",
                                        mFolderName, true);
                                break;
                            case 2:
                                mFileManager.shareFile(attachment.getFilename());
                                break;
                            case 3:
                                new PropertiesAlertDialog(getContext(), attachment).show();
                        }
                        moreOptionsViewModel.getSelection().removeObservers((AppCompatActivity) getContext());
                        moreOptionsViewModel.clearSelection();
                    };

                    MoreOptionsFragment fragment = MoreOptionsFragment.newInstance(attachment.getFilename(), options);
                    fragment.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                            fragment.getTag());
                    moreOptionsViewModel.getSelection().observe((AppCompatActivity) getContext(), observer);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();
    }

    @Override
    public void onDownloadCompleted(String fileName) {
        int child = mAttachmentContainer.getChildCount();
        for (int i = 0; i < child; i++) {
            View childView = mAttachmentContainer.getChildAt(i);
            TextView fileNameTextView = childView.findViewById(R.id.fileName);
            if (fileNameTextView != null &&
                    fileNameTextView.getText().toString().equalsIgnoreCase(fileName)) {
                ImageView downloadIcon = childView.findViewById(R.id.downloadButton);
                downloadIcon.setImageResource(R.drawable.eye);
                ImageView ellipsis = childView.findViewById(R.id.more);
                ellipsis.setVisibility(View.VISIBLE);
                break;
            }
        }
        mFileManager.openFile(fileName);
    }
}
