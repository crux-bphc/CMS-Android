package crux.bphc.cms.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import crux.bphc.cms.R
import crux.bphc.cms.core.getDescription
import crux.bphc.cms.core.getIconResource
import crux.bphc.cms.models.Download
import crux.bphc.cms.models.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _application = application
    private val baseContentDir = application.externalMediaDirs[0]

    var nestingLevel = 0 // 0 means root, 1 means inside 1 folder, 2 means insider a folder which is inside another folder and so on
        private set

    private var currentFile = baseContentDir
        set(value) {
            field = value
            refreshFileList()
        }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: MutableLiveData<Boolean> = _isLoading

    private val _downloads = MutableLiveData<List<Download>>()
    val downloads: MutableLiveData<List<Download>> = _downloads

    private val _isListEmptyMessage = MutableLiveData<Int?>()
    val isListEmptyMessage: MutableLiveData<Int?> = _isListEmptyMessage

    private val _deletedMessage: MutableLiveData<SingleLiveEvent<Boolean>> = MutableLiveData()
    val deletedMessage: LiveData<SingleLiveEvent<Boolean>> = _deletedMessage

    fun goToDirectory(directoryPath: String) {
        nestingLevel++
        currentFile = currentFile.resolve(directoryPath)
    }

    fun goBackOneDirectory(): Boolean {
        return if (nestingLevel == 0)
            false
        else {
            --nestingLevel
            currentFile = currentFile.resolveSibling("")
            true
        }
    }

    private fun refreshFileList() {
        _isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            val files = currentFile?.listFiles()?.asSequence()?.toList() ?: emptyList()
            _downloads.postValue(files.map {
                Download(it, it.getDescription(_application), it.getIconResource())
            })
            if (files.isEmpty()) {
                _isListEmptyMessage.postValue(if (nestingLevel == 0) R.string.root_no_files else R.string.no_files)
            } else {
                _isListEmptyMessage.postValue(null)
            }
            _isLoading.postValue(false)
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            refreshFileList()
            _deletedMessage.postValue(SingleLiveEvent(deleted))
        }
    }

    init {
        refreshFileList()
    }
}