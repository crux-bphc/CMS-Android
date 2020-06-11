package crux.bphc.cms.helper;

import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import crux.bphc.cms.R;

public class FileUtils {

    private FileUtils() {}

    public static int getDrawableIconFromFileName(String filename) {
        String mimeType = getFileMimeType(filename);
        if (mimeType == null)
            return -1;

        switch (mimeType) {
            case "application/pdf":
                return (R.drawable.file_pdf);

            case "application/vnd.ms-excel":
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return (R.drawable.file_excel);

            case "application/msword":
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return (R.drawable.file_word);

            case "application/vnd.ms-powerpoint":
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return (R.drawable.file_powerpoint);

            default:
                return -1;
        }
    }

    public static String getFileMimeType(String filename) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(filename));
    }

    @NonNull
    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
