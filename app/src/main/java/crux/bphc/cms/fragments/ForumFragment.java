package crux.bphc.cms.fragments;


import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import app.Constants;
import app.MyApplication;
import crux.bphc.cms.BuildConfig;
import crux.bphc.cms.R;
import io.realm.Realm;
import set.forum.Attachment;
import set.forum.Discussion;


public class ForumFragment extends Fragment {

    private int id;

    private Realm realm;

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

        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();

        mUserPic = (ImageView) view.findViewById(R.id.user_pic);
        Picasso.with(getContext()).load(discussion.getUserpictureurl()).into(mUserPic);

        mSubject = (TextView) view.findViewById(R.id.subject);
        mSubject.setText(discussion.getSubject());

        mUserName = (TextView) view.findViewById(R.id.user_name);
        mUserName.setText(discussion.getUserfullname());

        mTimeModified= (TextView) view.findViewById(R.id.modified_time);
        mTimeModified.setText(SiteNewsFragment.formatDate(discussion.getTimemodified()));

        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.setText(Html.fromHtml(discussion.getMessage()));

        mAttachmentContainer = (LinearLayout) view.findViewById(R.id.attachments);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final Attachment attachment: discussion.getAttachments()) {
            View attachmentView = inflater.inflate(
                    R.layout.row_attachment_detail_site_news,
                    mAttachmentContainer);

            TextView fileName = (TextView) attachmentView.findViewById(R.id.fileName);
            fileName.setText(attachment.getFilename());

            ImageView download = (ImageView) attachmentView.findViewById(R.id.downloadIcon);
            download.setImageResource(R.drawable.content_save);
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!searchFile(attachment.getFilename())) {
                        downloadFile(attachment);
                    } else {
                        openFile(attachment.getFilename());
                    }
                }
            });
        }
    }

    @NonNull
    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void downloadFile(Attachment attachment) {
        Toast.makeText(getContext(), "Downloading file - " + attachment.getFilename(), Toast.LENGTH_SHORT).show();
        String url = attachment.getFileurl() + "?token=" + Constants.TOKEN;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(attachment.getFilename());

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                attachment.getFilename());
        DownloadManager manager = (DownloadManager) getActivity()
                .getSystemService(Context.DOWNLOAD_SERVICE);

        manager.enqueue(request);
    }

    public void openFile(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), filename);
        Uri path = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", file);
        Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
        pdfOpenintent.setDataAndType(path, "application/" + getExtension(filename));
        pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pdfOpenintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(pdfOpenintent);
        } catch (ActivityNotFoundException e) {
            pdfOpenintent.setDataAndType(path, "application/*");
            startActivity(Intent.createChooser(pdfOpenintent, "No Application found to open File - " + filename));
        }
    }

    private boolean searchFile(String fileName) {

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        List<String> fileList = Arrays.asList(directory.list());
        if (fileList.size() == 0) {
            return false;
        }
        if (fileList.contains(fileName)) {
            Log.d("File found:", fileName);
            return true;
        }
        return false;
    }

}
