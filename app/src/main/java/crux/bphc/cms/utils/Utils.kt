package crux.bphc.cms.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Set of utility functions that can be used throughout the entire project.
 *
 * Created by abhijeetviswa 21/04/2019
 */
object Utils {
    private val hexArray = "0123456789ABCDEF".toCharArray()

    /**
     * Convert `bytes` to human readable format
     *
     * [Source](https://stackoverflow.com/a/3758880/2198399)
     *
     * @param si use SI or binary units
     * @return Human-readable size in SI/binary units
     */
    @SuppressLint("DefaultLocale")
    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    /**
     * Constructs a DateTime string in the local timezone
     * @param epoch Unix epoch of the instant, in seconds
     * @return DateTime string in the format 10:10:10 AM 18-Nov-2019
     */
    fun epochToDateTime(epoch: Long): String {
        val sdf = SimpleDateFormat.getDateTimeInstance() as SimpleDateFormat
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epoch * 1000))
    }

    /**
     * Converts a byte array to a hex string. This function
     * was converted from Java to Kotlin. So a lot of type
     * related gymnastics is performed to do a simple mundane
     * task.
     *
     * [Source](https://stackoverflow.com/a/40907652)
     *
     * @return Required Hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF  // Lop of the extra bits
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0xF]
        }
        return String(hexChars)
    }

    fun showBadTokenDialog(ctxt: Context?) {
        MaterialAlertDialogBuilder(ctxt!!)
            .setMessage(
                "The login failed due to an invalid token. Please ensure" +
                    " that you are logged into your BITS Email on your default browser."
            )
            .setTitle("Invalid Token")
            .setPositiveButton("OK", null)
            .show()
    }

    fun userDetails(fullName: String, username: String): Array<String> {
        val arrOfStr = username.split("@".toRegex()).toTypedArray()
        val studentIDno = arrOfStr[0]
        val nameTitleCase = toTitleCase(fullName)
        return arrayOf(nameTitleCase, studentIDno)
    }

    private fun toTitleCase(str: String): String {
        val t = str.trim().replace("  ", " ")
        val builder = StringBuilder(t)
        var space = true
        for (i in builder.indices) {
            val c = builder[i]
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c))
                    space = false
                }
            } else if (Character.isWhitespace(c)) {
                space = true
            } else {
                builder.setCharAt(i, Character.toLowerCase(c))
            }
        }
        return builder.toString()
    }

    fun openURLInBrowser(activity: Activity, url: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        activity.startActivity(intent)
    }

    /** Trim trailing and leading whitespace as defined by
     * [Character.isWhiteSpace][Character.isWhitespace].
     * @return Empty string is source is null, otherwise string with all trailing
     * whitespace removed
     */
    fun trimWhiteSpace(source: CharSequence?): CharSequence {
        if (source == null) return ""

        // loop to the first non-white space from the back
        var i = source.length
        do {
            --i
        } while (i >= 0 && Character.isWhitespace(source[i]))
        val end = i + 1

        // loop to the first non-white space from the front
        i = 0
        while (i < source.length && Character.isWhitespace(source[i])) {
            ++i
        }
        val begin = i
        return if (begin >= 0 && end < source.length && begin > end) "" else source.subSequence(
            begin,
            end
        )
    }

    fun formatDate(seconds: Int): String {
        val cal = GregorianCalendar()
        cal.timeInMillis = seconds.toLong() * 1000
        val day = cal[Calendar.DAY_OF_MONTH]
        val month = cal.getDisplayName(
            Calendar.MONTH,
            Calendar.SHORT,
            Locale.getDefault()
        )
        val year = cal[Calendar.YEAR]
        return String.format(Locale.getDefault(), "%s %s, %s", day, month, year)
    }
}