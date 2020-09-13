package crux.bphc.cms.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Set of utility functions that can be used throughout the entire project.
 *
 * Created by abhijeetviswa 21/04/2019
 */
public class Utils {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Convert `bytes` to human readable format
     * <p>
     * <a href=https://stackoverflow.com/a/3758880/2198399>Source</a>
     *
     * @param si use SI or binary units
     * @return Human-readable size in SI/binary units
     */
    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    /**
     * Constructs a DateTime string in the local timezone
     * @param epoch Unix epoch of the instant, in seconds
     * @return DateTime string in the format 10:10:10 AM 18-Nov-2019
     */
    public static String epochToDateTime(long epoch) {
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        sdf.setTimeZone(TimeZone.getDefault());

        return sdf.format(new Date(epoch * 1000));
    }


    /**
     * Converts a byte array to a hex string
     * <p>
     * <a href=https://stackoverflow.com/a/40907652>Source</a>
     *
     * @return Hex string
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void showBadTokenDialog(Context ctxt) {
        new MaterialAlertDialogBuilder(ctxt)
            .setMessage("The login failed due to an invalid token. Please ensure" +
                        " that you are logged into your BITS Email on your default browser.")
            .setTitle("Invalid Token")
            .setPositiveButton("OK", null)
            .show();
    }

    public static String[] userDetails(String fullName, String username) {
        String[] arrOfStr = username.split("@");
        String studentIDno = arrOfStr[0];

        String nameTitleCase = toTitleCase(fullName);
        System.out.println(nameTitleCase);

        return new String[]{nameTitleCase, studentIDno};
    }

    public static String toTitleCase(String str) {
        if (str == null) {
            return null;
        }

        str=str.replace("  "," ");

        boolean space = true;
        StringBuilder builder = new StringBuilder(str);
        final int len = builder.length();

        for (int i = 0; i < len; ++i) {
            char c = builder.charAt(i);
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c));
                    space = false;
                }
            } else if (Character.isWhitespace(c)) {
                space = true;
            } else {
                builder.setCharAt(i, Character.toLowerCase(c));
            }
        }

        return builder.toString();
    }

    public static void openURLInBrowser(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
    }

    /** Trim trailing and leading whitespace as defined by {@link
     * Character#isWhitespace}.
     * @return Empty string is source is null, otherwise string with all trailing
     *         whitespace removed
     */
    @NotNull
    public static CharSequence trimWhiteSpace(CharSequence source) {
        if (source == null) return "";

        // loop to the first non-white space from the back
        int i = source.length();
        do {
            --i;
        } while (i >= 0 && Character.isWhitespace(source.charAt(i)));
        int end = i + 1;

        // loop to the first non-white space from the front
        i = 0;
        while(i < source.length() && Character.isWhitespace(source.charAt(i))) {
            ++i;
        }
        int begin = i;

        if (begin >= 0 && end < source.length() && begin > end) return "";
        return source.subSequence(begin, end);
    }
}
