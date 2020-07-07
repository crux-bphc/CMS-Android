package crux.bphc.cms.widgets;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.forum.Attachment;
import crux.bphc.cms.utils.Utils;

public class PropertiesAlertDialog {

    final AlertDialog.Builder alertDialog;

    public PropertiesAlertDialog(Context context, String filename, int fileSize, long epoch) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        alertDialog.setTitle(filename);
        String properties = String.format("File Size: %s\n", Utils.humanReadableByteCount(fileSize, false));
        properties += String.format("Created: %s", Utils.epochToDateTime(epoch));
        alertDialog.setMessage(properties);
    }

    public PropertiesAlertDialog(Context context, Content content) {
        this(context, content.getFileName(), content.getFileSize(), content.getTimeCreated());
    }

    public PropertiesAlertDialog(Context context, Attachment attachment) {
        this(context, attachment.getFileName(), attachment.getFileSize(), attachment.getTimeModified());
    }

    public void show() {
        alertDialog.show();
    }
}
