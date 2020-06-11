package crux.bphc.cms.helper;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.BuildConfig;
import crux.bphc.cms.R;
import crux.bphc.cms.app.Constants;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.fragments.FolderModuleFragment;
import crux.bphc.cms.fragments.ForumFragment;
import crux.bphc.cms.models.Content;
import crux.bphc.cms.models.Module;
import crux.bphc.cms.models.forum.Attachment;

/**
 * Created by harsu on 19-01-2017.
 */

public class MyFileManager {
    public static final int DATA_DOWNLOADED = 20;
    private static final String ROOT_FOLDER = "CMS";
    private List<String> fileList;
    private Activity activity;
    private ArrayList<String> requestedDownloads;
    private Callback callback;
    private String courseName;
    private String courseDirName;

    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            reloadFileList();
            for (String filename : requestedDownloads) {
                if (searchFile(filename)) {
                    requestedDownloads.remove(filename);
                    if (callback != null) {
                        callback.onDownloadCompleted(filename);
                    }
                    return;
                }
            }
        }
    };

    public MyFileManager(Activity activity, String courseName) {
        this.activity = activity;
        requestedDownloads = new ArrayList<>();
        this.courseName = courseName;
        this.courseDirName = getSanitizedCoursePath(courseName);
    }

    public void registerDownloadReceiver() {
        activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void unregisterDownloadReceiver() {
        activity.unregisterReceiver(onComplete);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    // TODO check if the courseName params of these methods are needed, since we're passing it in constructor anyway
    public void downloadFile(Content content, Module module, String courseName) {
        deleteOldDownload(content, courseName); //delete any pre-existing version of the file
        downloadFile(content.getFilename(), content.getFileurl(), module.getDescription(),
                courseName, false);
    }

    public void downloadFile(String fileName, String fileUrl, String description, String courseName,
                             boolean isForum) {
        String url = "";
        if (isForum) {
            url = fileUrl + "?token=" + Constants.TOKEN;
        } else {
            url = fileUrl + "&token=" + Constants.TOKEN;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(description);
        request.setTitle(fileName);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                getFilePath(courseName, fileName));

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            request.allowScanningByMediaScanner();
        }

        ((DownloadManager) MyApplication.getInstance().getSystemService(Context.DOWNLOAD_SERVICE))
                .enqueue(request);
    }

    public void deleteOldDownload(Content content, String courseName) {
        String path = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getFilePath(courseName, content.getFilename());
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean searchFile(String fileName) {
        //if courseName is empty check sb folders
        if (fileList == null) {
            reloadFileList();
            if (fileList.size() == 0) {
                return false;
            }
        }
        if (fileList.contains(fileName)) {
            Log.d("File found:", fileName);
            return true;
        }
        return false;
    }

    public void openFile(String filename, String courseName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                getFilePath(courseName, filename));
        Uri path = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
        pdfOpenintent.setDataAndType(path, FileUtils.getFileMimeType(filename));
        pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pdfOpenintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(pdfOpenintent);
        } catch (ActivityNotFoundException e) {
            pdfOpenintent.setDataAndType(path, "application/*");
            activity.startActivity(Intent.createChooser(pdfOpenintent, "No Application found to open File - " + filename));
        }
    }


    public void shareFile(String filename, String courseName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getFilePath(courseName, filename));
        Uri path = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, path);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/*");

        try {
            activity.startActivity(Intent.createChooser(sendIntent, "Share File"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No app found to share the file - " + filename, Toast.LENGTH_SHORT).show();

        }
    }

    private String getFilePath(String courseName, String fileName) {
        return getSanitizedCoursePath(courseName) + File.separator + fileName;
    }

    private String getSanitizedCoursePath(String courseName) {
        return File.separator + ROOT_FOLDER + File.separator + getSanitizedCourseName(courseName);
    }

    private String getSanitizedCourseName(String courseName) {
        return courseName.replaceAll("/", "_");
    }

    public void reloadFileList() {
        fileList = new ArrayList<>();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String path = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + courseDirName;
            File courseDir = new File(path);
            if (courseDir.isDirectory()) {
                fileList.addAll(Arrays.asList(courseDir.list()));
            }
        } else {
            // MediaStore is backed by an SQLite database. We simply construct
            // an SQL query clauses which the API will run on the database.
            String[] projection = { MediaStore.Downloads.DISPLAY_NAME };
            String where = MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
            String[] args = { "%" + getSanitizedCourseName(courseName) + "%" };
            String order_by = MediaStore.Downloads.RELATIVE_PATH + " ASC";

            try (Cursor cursor = MyApplication.getInstance().getContentResolver().query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    where,
                    args,
                    order_by
            )){
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    fileList.add(cursor.getString(nameColumn));
                }
            }
        }
    }

    public boolean onClickAction(Module module, String courseName) {
        if (module.getModType() == Module.Type.URL) {
            if (module.getContents().size() > 0 && !module.getContents().get(0).getFileurl().isEmpty()) {
                Util.openURLInBrowser(activity, module.getContents().get(0).getFileurl());

            }
        } else if (module.getModType() == Module.Type.PAGE) {
            Util.openURLInBrowser(activity, module.getUrl());
        } else if (module.getModType() == Module.Type.FORUM) {
            Fragment forumFragment = ForumFragment.newInstance(Constants.TOKEN, module.getInstance(), courseName);
            FragmentTransaction fragmentTransaction = ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.course_section_enrol_container, forumFragment, "Announcements");
            fragmentTransaction.commit();
        } else if (module.getModType() == Module.Type.FOLDER) {
            Fragment folderModulerFragment = FolderModuleFragment.newInstance(Constants.TOKEN, module.getInstance(), courseName);
            FragmentTransaction fragmentTransaction = ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.course_section_enrol_container, folderModulerFragment, "Folder Module");
            fragmentTransaction.commit();
        } else if (module.getContents() == null || module.getContents().size() == 0) {
            if (module.getModType() == Module.Type.LABEL) {
                if (module.getDescription() == null || module.getDescription().length() == 0) {
                    return false;
                }
                AlertDialog.Builder alertDialog;

                if (MyApplication.getInstance().isDarkModeEnabled()) {
                    alertDialog = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_Dialog_Alert);
                } else {
                    alertDialog = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert);
                }

                Spanned htmlDescription = Html.fromHtml(module.getDescription());
                String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                alertDialog.setMessage(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                alertDialog.setNegativeButton("Close", null);
                alertDialog.show();
            } else

                Util.openURLInBrowser(activity, module.getUrl());
        } else {
            for (Content content : module.getContents()) {
                if (!searchFile(content.getFilename())) {
                    Toast.makeText(activity, "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
                    downloadFile(content, module, courseName);
                } else {
                    openFile(content.getFilename(), courseName);
                }
            }
        }
        return true;
    }

    public interface Callback {
        void onDownloadCompleted(String fileName);
    }
}
