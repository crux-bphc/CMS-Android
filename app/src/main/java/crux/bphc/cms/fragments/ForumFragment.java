package crux.bphc.cms.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import app.MyApplication;
import crux.bphc.cms.R;
import helper.MyFileManager;
import io.realm.Realm;
import set.forum.Attachment;
import set.forum.Discussion;


public class ForumFragment extends Fragment implements MyFileManager.Callback {

    private static final String FOLDER_NAME = "Site News";
    private int id;

    private Realm realm;
    private MyFileManager mFileManager;

    private ImageView mUserPic;
    private TextView mSubject;
    private TextView mUserName;
    private TextView mTimeModified;
    private TextView mMessage;
    private LinearLayout mAttachmentContainer;

    public ForumFragment() {
        // Required empty public constructor
    }

    public static ForumFragment newInstance(int id) {
        ForumFragment fragment = new ForumFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt("id");
        }
        realm = MyApplication.getInstance().getRealmInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forum, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFileManager = new MyFileManager(getActivity());
        mFileManager.registerDownloadReceiver();
        mFileManager.setCallback(this);

        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();

        mUserPic = (ImageView) view.findViewById(R.id.user_pic);
        Picasso.with(getContext()).load(discussion.getUserpictureurl()).into(mUserPic);

        mSubject = (TextView) view.findViewById(R.id.subject);
        mSubject.setText(discussion.getSubject());

        mUserName = (TextView) view.findViewById(R.id.user_name);
        mUserName.setText(discussion.getUserfullname());

        mTimeModified = (TextView) view.findViewById(R.id.modified_time);
        mTimeModified.setText(SiteNewsFragment.formatDate(discussion.getTimemodified()));

        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.setText(Html.fromHtml(discussion.getMessage()));

        mAttachmentContainer = (LinearLayout) view.findViewById(R.id.attachments);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final Attachment attachment : discussion.getAttachments()) {
            View attachmentView = inflater.inflate(
                    R.layout.row_attachment_detail_site_news,
                    mAttachmentContainer);

            TextView fileName = (TextView) attachmentView.findViewById(R.id.fileName);
            fileName.setText(attachment.getFilename());

            ImageView download = (ImageView) attachmentView.findViewById(R.id.downloadIcon);
            if (mFileManager.searchFile(attachment.getFilename())) {
                download.setImageResource(R.drawable.eye);
            } else {
                download.setImageResource(R.drawable.content_save);
            }
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mFileManager.searchFile(attachment.getFilename())) {
                        mFileManager.downloadFile(
                                attachment.getFilename(),
                                attachment.getFileurl(),
                                "",
                                FOLDER_NAME
                        );
                    } else {
                        mFileManager.openFile(attachment.getFilename(), FOLDER_NAME);
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
            TextView fileNameTextView = (TextView) childView.findViewById(R.id.fileName);
            if (fileNameTextView != null &&
                    fileNameTextView.getText().toString().equalsIgnoreCase(fileName)) {
                ImageView downloadIcon = (ImageView) childView.findViewById(R.id.downloadIcon);
                downloadIcon.setImageResource(R.drawable.eye);
                break;
            }
        }
    }
}
