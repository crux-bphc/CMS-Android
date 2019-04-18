package crux.bphc.cms.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.HtmlTextView;
import helper.MyFileManager;
import io.realm.Realm;
import set.forum.Attachment;
import set.forum.Discussion;


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

        mFileManager = new MyFileManager(getActivity(), mFolderName);
        mFileManager.registerDownloadReceiver();
        mFileManager.setCallback(this);

        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();

        mUserPic = view.findViewById(R.id.user_pic);
        Picasso.with(getContext()).load(discussion.getUserpictureurl()).into(mUserPic);

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

            ImageView download = attachmentView.findViewById(R.id.downloadIcon);
            if (mFileManager.searchFile(attachment.getFilename())) {
                download.setImageResource(R.drawable.eye);
            } else {
                download.setImageResource(R.drawable.content_save);
            }
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mFileManager.searchFile(attachment.getFilename())) {
                        Toast.makeText(getActivity(), "Downloading file - " + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                        mFileManager.downloadFile(
                                attachment.getFilename(),
                                attachment.getFileurl(),
                                "",
                                mFolderName,
                                true
                        );
                    } else {
                        mFileManager.openFile(attachment.getFilename(), mFolderName);
                    }
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
                ImageView downloadIcon = childView.findViewById(R.id.downloadIcon);
                downloadIcon.setImageResource(R.drawable.eye);
                break;
            }
        }
        mFileManager.openFile(fileName, mFolderName);
    }
}
