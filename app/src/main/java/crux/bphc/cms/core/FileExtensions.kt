package crux.bphc.cms.core

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import crux.bphc.cms.R
import crux.bphc.cms.utils.FileUtils
import java.io.File
import java.util.*

fun File.getFormattedFileSize(context: Context) = Formatter.formatFileSize(context, this.getContentSize()).toUpperCase(Locale.ROOT)

fun File.getContentSize(): Long {
    return if (isDirectory) walkBottomUp().fold(0L, { res, it ->
        res + it.length()
    }) else length()
}

fun File.getDescription(context: Context) = if (isDirectory) {
    when (listFiles()?.size ?: 0) {
        0 -> context.getString(R.string.folder_no_items_zero)
        1 -> context.getString(R.string.folder_no_items_one, getFormattedFileSize(context))
        else -> context.getString(R.string.folder_no_items_many, listFiles()?.size ?: 0, getFormattedFileSize(context))
    }
} else
    context.getString(
            R.string.file_description,
            DateUtils.getRelativeTimeSpanString(lastModified(), Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS),
            getFormattedFileSize(context),
            extension.toUpperCase(Locale.getDefault())
    )

fun File.getIconResource() = if (isDirectory) R.drawable.outline_folder_24 else FileUtils.getDrawableIconFromFileName(name)