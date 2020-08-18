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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.BuildConfig;
import crux.bphc.cms.app.Constants;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.models.Content;
import crux.bphc.cms.models.Module;
import crux.bphc.cms.models.forum.Attachment;

/**
 * <p>
 * A manager class to manage file access and downloads of module Contents and
 * discussion Attachments. An instance of this class is associated with a
 * particular course.
 * <p>
 * On {@link Build.VERSION_CODES#P Android P} and below, files are downloaded to
 * the location specified by {@link Environment#DIRECTORY_DOWNLOADS}.
 * <p>
 * Starting from {@link android.os.Build.VERSION_CODES#Q Android Q}, files are
 * downloaded to the "primary" external storage volume ({@link
 * MediaStore.Downloads#EXTERNAL_CONTENT_URI}) {@link MediaStore.Downloads}.
 *
 * @author Harshit Agarwal, Abhijeet Viswa
 */
public class FileManager {
    public static final int DATA_DOWNLOADED = 20;

    /**
     * The folder into which files will be downloaded. This root folder will
     * itself be inside {@link MediaStore.Downloads} or
     * {@link Environment#DIRECTORY_DOWNLOADS}
     */
    private static final String ROOT_FOLDER = "CMS";

    private List<String> fileList;
    private Activity activity;
    private ArrayList<String> requestedDownloads;
    private Callback callback;
    private String courseName;
    private String courseDirName;
    private BroadcastReceiver onComplete;

    /**
     * @param activity An Activity context, to launch new activities when
     *                 opening files.
     * @param courseName Course name the FileManager instance should be attached
     *                   to. Only files inside the folder <code>{@linkplain
     *                   #ROOT_FOLDER}/courseName</code> will be accessible from
     *                   the given FileManager instance.
     */
    public FileManager(@NonNull Activity activity, @NonNull String courseName) {
        this.activity = activity;
        requestedDownloads = new ArrayList<>();
        this.courseName = courseName;
        this.courseDirName = getSanitizedRelativeCoursePath(courseName);

        onComplete = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                reloadFileList();
                for (String filename : requestedDownloads) {
                    if (isFileDownloaded(filename)) {
                        requestedDownloads.remove(filename);
                        if (callback != null) {
                            callback.onDownloadCompleted(filename);
                        }
                        return;
                    }
                }
            }
        };
    }

    public void downloadModuleContent(Content content, Module module) {
        deleteExistingModuleContent(content);
        downloadFile(content.getFilename(), content.getFileurl(), module.getDescription(),  courseName, false);
    }

    public void downloadDiscussionAttachment(Attachment attachment, String description, String courseName) {
        deleteExistingDiscussionAttachment(attachment);
        downloadFile(attachment.getFilename(), attachment.getFileurl(), description, courseName, true);
    }

    private void downloadFile(String fileName, String fileUrl, String description, String courseName, boolean isForum) {
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
                getRelativeFilePath(courseName, fileName));

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            request.allowScanningByMediaScanner();
        }

        requestedDownloads.add(fileName);
        ((DownloadManager) MyApplication.getInstance().getSystemService(Context.DOWNLOAD_SERVICE))
                .enqueue(request);
    }

    public void openModuleContent(Content content) {
        openFile(content.getFilename());
    }

    public void openDiscussionAttachment(Attachment attachment) {
        openFile(attachment.getFilename());
    }

    private void openFile(String filename) {
        Uri fileUri = null;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                    getRelativeFilePath(courseName, filename));
            fileUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            final Uri BASE_CONTENT_URI = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

            String[] projection = { MediaStore.Downloads._ID };
            String where = "(" + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?" + ") AND "
                    + MediaStore.Downloads.DISPLAY_NAME + " = ?";
            String[] args = { "%" + getSanitizedCourseName(courseName) + "%", filename };
            String order_by = MediaStore.Downloads.RELATIVE_PATH + " ASC";

            try (Cursor cursor = MyApplication.getInstance().getContentResolver().query(
                    BASE_CONTENT_URI,
                    projection,
                    where,
                    args,
                    order_by
            )){
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                if (cursor.moveToNext()) {
                    fileUri = Uri.withAppendedPath(BASE_CONTENT_URI, "" + cursor.getInt(idColumn));
                }
            }
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, FileUtils.getFileMimeType(filename));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setDataAndType(fileUri, "application/*");
            activity.startActivity(Intent.createChooser(intent, "No Application found to open File - " +
                    filename));
        }
    }

    public void shareModuleContent(Content content) {
        shareFile(content.getFilename());
    }

    public void shareDiscussionAttachment(Attachment attachment) {
        shareFile(attachment.getFilename());
    }

    private void shareFile(String filename) {
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String path = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + getRelativeFilePath(courseName, filename);
            fileUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", new File(path));
        } else {
            final Uri BASE_CONTENT_URI = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

            String[] projection = { MediaStore.Downloads._ID };
            String where = "(" + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?" + ") AND "
                    + MediaStore.Downloads.DISPLAY_NAME + " = ?";
            String[] args = { "%" + getSanitizedCourseName(courseName) + "%", filename };
            String order_by = MediaStore.Downloads.RELATIVE_PATH + " ASC";

            try (Cursor cursor = MyApplication.getInstance().getContentResolver().query(
                    BASE_CONTENT_URI,
                    projection,
                    where,
                    args,
                    order_by
            )){
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                if (cursor.moveToNext()) {
                    fileUri = Uri.withAppendedPath(BASE_CONTENT_URI, "" + cursor.getInt(idColumn));
                }
            }
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/*");

        try {
            activity.startActivity(Intent.createChooser(sendIntent, "Share File"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No app found to share the file - " + filename, Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteExistingModuleContent(Content content) {
        deleteExistingFile(content.getFilename());
    }

    public void deleteExistingDiscussionAttachment(Attachment attachment) {
        deleteExistingFile(attachment.getFilename());
    }

    public void deleteExistingFile(String filename) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String path = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + getRelativeFilePath(courseName, filename);
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } else {
            String where = "(" + MediaStore.Downloads.RELATIVE_PATH + " LIKE ?" + ") AND "
                    + MediaStore.Downloads.DISPLAY_NAME + " = ?";
            String[] args = { "%" + getSanitizedCourseName(courseName) + "%", filename };

            MyApplication.getInstance().getContentResolver().delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    where,
                    args
            );
        }
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

    public boolean isModuleContentDownloaded(Content content) {
        return isFileDownloaded(content.getFilename());
    }

    public boolean isDiscussionAttachmentDownloaded(Attachment attachment) {
        return isFileDownloaded(attachment.getFilename());
    }

    private boolean isFileDownloaded(String fileName) {
        if (fileList == null) {
            reloadFileList();
        }
        return fileList.contains(fileName);
    }

    private String getRelativeFilePath(String courseName, String fileName) {
        return getSanitizedRelativeCoursePath(courseName) + File.separator + fileName;
    }

    private String getSanitizedRelativeCoursePath(String courseName) {
        return File.separator + ROOT_FOLDER + File.separator + getSanitizedCourseName(courseName);
    }

    private String getSanitizedCourseName(String courseName) {
        return courseName.replaceAll("/", "_");
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


    public interface Callback {
        void onDownloadCompleted(String fileName);
    }
}
