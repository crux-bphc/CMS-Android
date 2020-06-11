package crux.bphc.cms.helper;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.models.Content;
import crux.bphc.cms.models.forum.Attachment;

public class PropertiesAlertDialog {

    AlertDialog.Builder alertDialog;

    public PropertiesAlertDialog(Context context, String filename, int fileSize, long epoch) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        alertDialog.setTitle(filename);
        String properties = String.format("File Size: %s\n", Util.humanReadableByteCount(fileSize, false));
        properties += String.format("Created: %s", Util.epochToDateTime(epoch));
        alertDialog.setMessage(properties);
    }

    public PropertiesAlertDialog(Context context, Content content) {
        this(context, content.getFilename(), content.getFilesize(), content.getTimecreated());
    }

    public PropertiesAlertDialog(Context context, Attachment attachment) {
        this(context, attachment.getFilename(), attachment.getFileSize(), attachment.getTimemodified());
    }

    public void show() {
        alertDialog.show();
    }
}
