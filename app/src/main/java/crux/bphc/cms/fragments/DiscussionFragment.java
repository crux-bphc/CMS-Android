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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import crux.bphc.cms.R;
import crux.bphc.cms.io.FileManager;
import crux.bphc.cms.models.forum.Attachment;
import crux.bphc.cms.models.forum.Discussion;
import crux.bphc.cms.widgets.HtmlTextView;
import crux.bphc.cms.widgets.PropertiesAlertDialog;
import io.realm.Realm;
import io.realm.RealmList;


public class DiscussionFragment extends Fragment {

    private  String mCourseName;
    private int id;

    private Realm realm;
    private FileManager mFileManager;

    private MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public DiscussionFragment() {
        // Required empty public constructor
    }

    public static DiscussionFragment newInstance(int id, String mCourseName) {
        DiscussionFragment fragment = new DiscussionFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putString("courseName", mCourseName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt("id");
            mCourseName = getArguments().getString("courseName");
        }
        mFileManager = new FileManager(requireActivity(), mCourseName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        realm = Realm.getDefaultInstance();
        return inflater.inflate(R.layout.fragment_discussion, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);

        ImageView mUserPic = view.findViewById(R.id.user_pic);
        TextView mSubject = view.findViewById(R.id.subject);
        TextView mUserName = view.findViewById(R.id.user_name);
        TextView mTimeModified = view.findViewById(R.id.modified_time);
        HtmlTextView mMessage = view.findViewById(R.id.message);
        LinearLayout mAttachmentContainer = view.findViewById(R.id.attachments);

        mMessage.setMovementMethod(LinkMovementMethod.getInstance());

        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();
        if (discussion != null) {
            RealmList<Attachment> attachments = discussion.getAttachments();

            mFileManager.registerDownloadReceiver();
            mFileManager.setCallback(filename -> {
                int child = mAttachmentContainer.getChildCount();
                for (int i = 0; i < child; i++) {
                    View childView = mAttachmentContainer.getChildAt(i);
                    TextView fileNameTextView = childView.findViewById(R.id.name);
                    if (fileNameTextView != null &&
                            fileNameTextView.getText().toString().equalsIgnoreCase(filename)) {
                        ImageView downloadIcon = childView.findViewById(R.id.download);
                        downloadIcon.setImageResource(R.drawable.eye);
                        ImageView ellipsis = childView.findViewById(R.id.more);
                        ellipsis.setVisibility(View.VISIBLE);
                        break;
                    }
                }
                Attachment attachment;
                if (attachments != null
                        && (attachment = attachments.where().equalTo("fileName", filename).findFirst()) != null) {
                    mFileManager.openDiscussionAttachment(attachment);
                }
            });

            Picasso.get().load(discussion.getUserPictureUrl()).into(mUserPic);

            mSubject.setText(discussion.getSubject());
            mUserName.setText(discussion.getUserFullName());
            mTimeModified.setText(ForumFragment.formatDate(discussion.getTimeModified()));
            mMessage.setText(HtmlTextView.parseHtml(discussion.getMessage()));

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            if (attachments != null && !attachments.isEmpty()) {
                mAttachmentContainer.setVisibility(View.VISIBLE);
                for (Attachment attachment : discussion.getAttachments()) {
                    View attachmentView = inflater.inflate(
                            R.layout.row_attachment_detail_forum,
                            mAttachmentContainer);

                    TextView fileName = attachmentView.findViewById(R.id.name);
                    View clickWrapper = attachmentView.findViewById(R.id.click_wrapper);
                    ImageView download = attachmentView.findViewById(R.id.download);
                    ImageView ellipsis = attachmentView.findViewById(R.id.more);

                    fileName.setText(attachment.getFileName());

                    boolean downloaded = mFileManager.isDiscussionAttachmentDownloaded(attachment);
                    if (downloaded) {
                        download.setImageResource(R.drawable.eye);
                        ellipsis.setVisibility(View.VISIBLE);
                    } else {
                        download.setImageResource(R.drawable.download);
                        ellipsis.setVisibility(View.GONE);
                    }

                    clickWrapper.setOnClickListener(v -> {
                        if (!downloaded) {
                            Toast.makeText(getActivity(), "Downloading file - " + attachment.getFileName(),
                                    Toast.LENGTH_SHORT).show();
                            mFileManager.downloadDiscussionAttachment(attachment, discussion.getSubject(), mCourseName);
                        } else {
                            mFileManager.openDiscussionAttachment(attachment);
                        }
                    });

                    ellipsis.setOnClickListener(v -> {
                        // Check if downloaded once again, for consistency (user downloaded and then opens
                        // ellipsis immediately)
                        boolean isDownloaded = mFileManager.isDiscussionAttachmentDownloaded(attachment);
                        if (isDownloaded) {
                            Observer<MoreOptionsFragment.Option> observer;  // to handle the selection

                            ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>(Arrays.asList(
                                    new MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                                    new MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                                    new MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                                    new MoreOptionsFragment.Option(3, "Properties", R.drawable.ic_info)
                            ));

                            observer = option -> {
                                if (option == null)
                                    return;
                                switch (option.getId()) {
                                    case 0:
                                        mFileManager.openDiscussionAttachment(attachment);
                                        break;
                                    case 1:
                                        Toast.makeText(getActivity(), "Downloading file - " + attachment.getFileName(),
                                                Toast.LENGTH_SHORT).show();
                                        mFileManager.downloadDiscussionAttachment(attachment, discussion.getSubject(),
                                                mCourseName);
                                        break;
                                    case 2:
                                        mFileManager.shareDiscussionAttachment(attachment);
                                        break;
                                    case 3:
                                        new PropertiesAlertDialog(requireActivity(), attachment).show();
                                }
                                moreOptionsViewModel.getSelection().removeObservers(requireActivity());
                                moreOptionsViewModel.clearSelection();
                            };

                            MoreOptionsFragment fragment = MoreOptionsFragment.newInstance(attachment.getFileName(),
                                    options);
                            fragment.show(requireActivity().getSupportFragmentManager(), fragment.getTag());
                            moreOptionsViewModel.getSelection().observe((AppCompatActivity) requireActivity(), observer);
                        }
                    });
                }
            } else {
                mAttachmentContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFileManager.unregisterDownloadReceiver();
        realm.close();
    }
}
