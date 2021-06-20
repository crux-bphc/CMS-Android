package crux.bphc.cms.background

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import crux.bphc.cms.models.UserAccount
import java.io.File

class MigrateDataWorker(private val appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val oldStorageLocation = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path, "CMS")
        val newStorageLocation = appContext.externalMediaDirs[0]

        if (oldStorageLocation.exists()) {
            val copiedSuccessfully = oldStorageLocation.copyRecursively(
                    newStorageLocation, true
            )
            val deletedSuccessfully = if (copiedSuccessfully)
                oldStorageLocation.deleteRecursively() else false

            return if (copiedSuccessfully && deletedSuccessfully) {
                UserAccount.hasMigratedData = true
                Result.success()
            } else {
                UserAccount.hasMigratedData = false
                Result.failure()
            }
        }

        UserAccount.hasMigratedData = true
        return Result.success()
    }
}