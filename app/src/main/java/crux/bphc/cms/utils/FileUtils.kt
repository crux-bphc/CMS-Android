package crux.bphc.cms.utils

import android.webkit.MimeTypeMap
import crux.bphc.cms.R

object FileUtils {
    fun getDrawableIconFromFileName(filename: String): Int {
        val mimeType = getFileMimeType(filename) ?: return R.drawable.ic_document_unknown_type
        return when (mimeType) {
            "application/pdf" -> R.drawable.file_pdf
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> R.drawable.file_excel
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> R.drawable.file_word
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.template", "application/vnd.openxmlformats-officedocument.presentationml.slideshow", "application/vnd.oasis.opendocument.presentation" -> R.drawable.file_powerpoint
            else -> R.drawable.ic_document_unknown_type
        }
    }

    fun getFileMimeType(filename: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(filename))
    }

    private fun getExtension(filename: String): String {
        return filename.substring(filename.lastIndexOf('.') + 1)
    }
}