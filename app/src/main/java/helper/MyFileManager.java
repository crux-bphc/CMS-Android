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
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.Constants;
import crux.bphc.cms.BuildConfig;
import crux.bphc.cms.WebSiteActivity;
import io.realm.Realm;
import set.Content;
import set.Course;
import set.Module;

/**
 * Created by harsu on 19-01-2017.
 */

public class MyFileManager {
    public static final int DATA_DOWNLOADED = 20;
    private static final String CMS = "CMS";
    private List<String> fileList;
    private Activity activity;
    private ArrayList<String> requestedDownloads;
    private Callback callback;
    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            int i = 0;
            reloadFileList();
            for (String filename : requestedDownloads) {
                if (searchFile(filename)) {
                    requestedDownloads.remove(filename);
                    if (callback != null) {
                        callback.onDownloadCompleted(filename);
                    }
                    return;
                }
                i++;
            }
        }
    };

    public MyFileManager(Activity activity) {
        this.activity = activity;
        requestedDownloads = new ArrayList<>();
    }


    @NonNull
    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public static void showInWebsite(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);

        //activity.startActivity(WebSiteActivity.getIntent(activity, "CMS", url));

        /*CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(activity, Uri.parse(url));*/
    }

    private String getApplicationType(String filename) {
        String extension=getExtension(filename);
        switch (extension) {
            case "pdf":
                return "pdf";

            case "xls":
            case "xlsx":
                return "vnd.ms-excel";

            case "doc":
            case "docx":
                return "msword";

            case "ppt":
            case "pptx":
                return "vnd.ms-powerpoint";

            default:
                return extension;
        }
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

    public void downloadFile(Content content, Module module, String courseName) {
        downloadFile(content.getFilename(), content.getFileurl(), module.getDescription(), courseName, false);
    }

    public void downloadFile(String fileName, String fileurl, String description, String courseName, boolean isForum) {
        String url = "";
        if (isForum) {
            url = fileurl + "?token=" + Constants.TOKEN;
        } else {
            url = fileurl + "&token=" + Constants.TOKEN;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(description);
        request.setTitle(fileName);

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getPathExtension(courseName);
        File direct = new File(path);
        if (!direct.exists()) {
            File dir = new File(path);
            dir.mkdirs();
        }

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                getFilePath(courseName, fileName));
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        requestedDownloads.add(fileName);
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
        pdfOpenintent.setDataAndType(path, "application/" + getApplicationType(filename));
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

        return getPathExtension(courseName) + File.separator + fileName;
    }

    private String getPathExtension(String courseName) {
        return File.separator + CMS
                + File.separator + courseName.replaceAll("/","_");
    }

    public void reloadFileList() {
        fileList = new ArrayList<>();
        File cmsDirec = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + CMS);
        if (cmsDirec.isDirectory()) {
            for (File courseDir : cmsDirec.listFiles()) {
                if (courseDir.isDirectory()) {
                    fileList.addAll(Arrays.asList(courseDir.list()));
                }
            }
        }

    }

    public boolean onClickAction(Module module, String courseName) {
        if (module.getModType() == Module.Type.URL) {
            if (module.getContents().size() > 0 && !module.getContents().get(0).getFileurl().isEmpty()) {
                MyFileManager.showInWebsite(activity, module.getContents().get(0).getFileurl());

            }
        } else if (module.getModType() == Module.Type.FORUM || module.getModType() == Module.Type.PAGE) {
            MyFileManager.showInWebsite(activity, module.getUrl());
        } else if (module.getContents() == null || module.getContents().size() == 0) {
            if (module.getModType() == Module.Type.FORUM || module.getModType() == Module.Type.LABEL) {
                if (module.getDescription() == null || module.getDescription().length() == 0) {
                    return false;
                }
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                Spanned htmlDescription = Html.fromHtml(module.getDescription());
                String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                alertDialog.setMessage(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                alertDialog.setNegativeButton("Close", null);
                alertDialog.show();
            } else

                showInWebsite(activity, module.getUrl());

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
        public void onDownloadCompleted(String fileName);
    }
}
