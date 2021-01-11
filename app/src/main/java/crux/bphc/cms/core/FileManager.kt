package crux.bphc.cms.core

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import crux.bphc.cms.BuildConfig
import crux.bphc.cms.app.MyApplication
import crux.bphc.cms.app.appendOrSetQueryParameter
import crux.bphc.cms.models.UserAccount.token
import crux.bphc.cms.models.course.Content
import crux.bphc.cms.models.course.Module
import crux.bphc.cms.models.forum.Attachment
import crux.bphc.cms.utils.FileUtils
import java.io.File

/**
 * A manager class to manage file access and downloads of module Contents and
 * discussion Attachments. An instance of this class is associated with a
 * particular course.
 *
 * On [Android P][Build.VERSION_CODES.P] and below, files are downloaded to
 * the location specified by [Environment.DIRECTORY_DOWNLOADS].
 *
 * Starting from [Android Q][android.os.Build.VERSION_CODES.Q], files are
 * downloaded to the "primary" external storage volume's Download directory using
 * the Mediastore API.
 *
 * @param activity An activity context, to launch new activities etc
 * @param courseName Course name the FileManager instance should be attached
 * to. Only files inside the course's folder will be accessible from
 * the given FileManager instance.

 * @author Harshit Agarwal, Abhijeet Viswa
 */
class FileManager(
    private val activity: Activity,
    private val courseName: String,
    private val callback: (String) -> Unit,
) {

    private val sanitizedCourseName: String = courseName.replace("/".toRegex(), "_")

    private val fileList: MutableList<String> = emptyList<String>().toMutableList()
    private val requestedDownloads: MutableList<String> = emptyList<String>().toMutableList()

    private val onComplete: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadFileList()
            for (filename in requestedDownloads) {
                if (isFileDownloaded(filename)) {
                    requestedDownloads.remove(filename)
                    callback.invoke(filename)
                    return
                }
            }
        }

    }

    fun downloadModuleContent(content: Content, module: Module) {
        deleteExistingModuleContent(content)
        downloadFile(content.fileUrl, content.fileName, module.description)
    }

    fun downloadDiscussionAttachment(attachment: Attachment, description: String) {
        deleteExistingDiscussionAttachment(attachment)
        downloadFile(attachment.fileUrl, attachment.fileName, description)
    }

    fun openModuleContent(content: Content) =
        openFile(content.fileName)

    fun openDiscussionAttachment(attachment: Attachment) =
        openFile(attachment.fileName)

    private fun downloadFile(fileUrl: String, fileName: String, description: String) {
        val url = Uri.parse(fileUrl).buildUpon().appendOrSetQueryParameter("token", token).build()

        val request = DownloadManager.Request(url)
        request.setDescription(description)
        request.setTitle(fileName)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            getRelativeFilePath(fileName)
        )
        (activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        requestedDownloads.add(fileName)
    }

    private fun openFile(fileName: String) {
        val fileUri = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                getRelativeFilePath(fileName)
            )
            FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.provider", file)
        } else {
            val baseContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val where = ("(${MediaStore.Downloads.RELATIVE_PATH} LIKE ?) AND " +
                "${MediaStore.Downloads.DISPLAY_NAME} = ?")
            val args = arrayOf("%${sanitizedCourseName}%", fileName)
            val orderBy = MediaStore.Downloads.RELATIVE_PATH + " ASC"
            MyApplication.getInstance().contentResolver.query(
                baseContentUri,
                projection,
                where,
                args,
                orderBy
            ).use { cursor ->
                var ret: Uri? = null
                if (cursor != null) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    if (cursor.moveToNext()) {
                        ret = Uri.withAppendedPath(baseContentUri, "" + cursor.getInt(idColumn))
                    }
                }
                ret
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, FileUtils.getFileMimeType(fileName))
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            intent.setDataAndType(fileUri, "application/*")
            activity.startActivity(
                Intent.createChooser(
                    intent,
                    "No Application found to open File - $fileName"
                )
            )
        }
    }

    fun shareModuleContent(content: Content) =
        shareFile(content.fileName)

    fun shareDiscussionAttachment(attachment: Attachment) =
        shareFile(attachment.fileName)

    private fun shareFile(fileName: String) {
        val fileUri = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                    + getRelativeFilePath(fileName)
            )
            FileProvider.getUriForFile(
                activity,
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
        } else {
            val baseContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val where = ("(${MediaStore.Downloads.RELATIVE_PATH} LIKE ?) AND " +
                "${MediaStore.Downloads.DISPLAY_NAME} = ?")
            val args = arrayOf("%${sanitizedCourseName}%", fileName)
            val orderBy = MediaStore.Downloads.RELATIVE_PATH + " ASC"
            MyApplication.getInstance().contentResolver.query(
                baseContentUri,
                projection,
                where,
                args,
                orderBy
            ).use { cursor ->
                var ret: Uri? = null
                if (cursor != null) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    if (cursor.moveToNext()) {
                        ret =  Uri.withAppendedPath(baseContentUri, "" + cursor.getInt(idColumn))
                    }
                }
                ret
            }
        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.type = "application/*"
        try {
            activity.startActivity(Intent.createChooser(sendIntent, "Share File"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                "No app found to share the file - $fileName",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteExistingModuleContent(content: Content) = deleteExistingFile(content.fileName)

    private fun deleteExistingDiscussionAttachment(attachment: Attachment) =
        deleteExistingFile(attachment.fileName)

    private fun deleteExistingFile(fileName: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                    + getRelativeFilePath(fileName))
            if (file.exists()) {
                file.delete()
            }
        } else {
            val where = ("(${MediaStore.Downloads.RELATIVE_PATH} LIKE ?) AND " +
                "${MediaStore.Downloads.DISPLAY_NAME} = ?")
            val args = arrayOf("%${sanitizedCourseName}%", fileName)
            MyApplication.getInstance().contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                where,
                args
            )
        }
    }

    fun reloadFileList() {
        fileList.clear()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val path = (Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                + sanitizedCourseName)
            val courseDir = File(path)
            if (courseDir.isDirectory) {
                val files = courseDir.list()
                if (files != null) {
                    fileList.addAll(listOf(*files))
                }
            }
        } else {
            // MediaStore is backed by an SQLite database. We simply construct
            // an SQL query clauses which the API will run on the database.
            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
            val where = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
            val args = arrayOf("%${sanitizedCourseName}%")
            val orderBy = "${MediaStore.Downloads.RELATIVE_PATH} ASC"
            MyApplication.getInstance().contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                where,
                args,
                orderBy
            ).use { cursor ->
                if (cursor != null) {
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        fileList.add(cursor.getString(nameColumn))
                    }
                }
            }
        }
    }

    private fun getRelativeFilePath(filename: String) =
        ROOT_FOLDER + File.separator + sanitizedCourseName + File.separator + filename

    fun isModuleContentDownloaded(content: Content) = isFileDownloaded(content.fileName)

    fun isDiscussionAttachmentDownloaded(attachment: Attachment) = isFileDownloaded(attachment.fileName)

    private fun isFileDownloaded(fileName: String): Boolean {
        if (fileList.isEmpty()) {
            reloadFileList()
        }
        return fileList.contains(fileName)
    }

    fun registerDownloadReceiver() =
        activity.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    fun unregisterDownloadReceiver() =
        activity.unregisterReceiver(onComplete)

    companion object {
        /**
         * The folder into which files will be downloaded. This root folder will
         * itself be inside [MediaStore.Downloads] or
         * [Environment.DIRECTORY_DOWNLOADS]
         */
        private const val ROOT_FOLDER = "CMS"
    }
}
