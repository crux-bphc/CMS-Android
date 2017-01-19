package helper;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.Constants;
import crux.bphc.cms.BuildConfig;
import set.Content;
import set.Module;

/**
 * Created by harsu on 19-01-2017.
 */

public class MyFileManager {
    private List<String> fileList;
    public static final int DATA_DOWNLOADED = 20;
    private Context context;
    private ArrayList<String> requestedDownloads;
    private Callback callback;
    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            int i = 0;
            for (String fileName : requestedDownloads) {
                if (searchFile(fileName, i == 0)) {
                    requestedDownloads.remove(fileName);
                    if (callback != null) {
                        callback.onDownloadCompleted(fileName);
                    }

                    return;
                }
                i++;
            }
        }
    };

    public MyFileManager(Context context) {
        this.context = context;
        requestedDownloads = new ArrayList<>();
    }

    public void registerDownloadReceiver() {
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void unregisterDownloadReceiver() {
        context.unregisterReceiver(onComplete);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void downloadFile(Content content, Module module) {
        requestedDownloads.add(content.getFilename());
        Toast.makeText(context, "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
        String url = content.getFileurl() + "&token=" + Constants.TOKEN;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(module.getModname());
        request.setTitle(content.getFilename());

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, content.getFilename());
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        manager.enqueue(request);
    }

    @NonNull
    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public boolean searchFile(String fileName, boolean reload) {
        if (reload || fileList == null) {
            File direc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            fileList = Arrays.asList(direc.list());
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

    public void openFile(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), filename);
        Uri path = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
        pdfOpenintent.setDataAndType(path, "application/" + getExtension(filename));
        pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pdfOpenintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(pdfOpenintent);
        } catch (ActivityNotFoundException e) {
            pdfOpenintent.setDataAndType(path, "application/*");
            context.startActivity(Intent.createChooser(pdfOpenintent, "No Application found to open File - " + filename));
        }
    }

    public void shareFile(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + filename);
        Uri path = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, path);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/*");

        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share File"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No app found to share the file - " + filename, Toast.LENGTH_SHORT).show();

        }
    }

    public static void showInWebsite(Activity activity, String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(activity, Uri.parse(url));
    }

    public void reloadFileList() {
        File direc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        fileList = Arrays.asList(direc.list());
    }

    public interface Callback {
        public void onDownloadCompleted(String fileName);
    }
}
