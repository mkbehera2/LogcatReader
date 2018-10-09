package com.dp.logcatapp.fragments.savedlogsviewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.dp.logcat.Log
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.util.closeQuietly
import kotlinx.coroutines.experimental.Dispatchers.IO
import kotlinx.coroutines.experimental.Dispatchers.Main
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.FileNotFoundException
import java.io.IOException

internal class SavedLogsViewerViewModel(application: Application) : AndroidViewModel(application) {
    var autoScroll = true
    var scrollPosition = 0

    lateinit var logs: LogsLiveData

    fun init(uri: Uri) {
        if (!::logs.isInitialized) {
            logs = LogsLiveData(getApplication(), uri)
        }
    }
}

internal class LogsLiveData(application: Application, uri: Uri) : LiveData<List<Log>>() {

    init {
        load(application, uri)
    }

    private fun load(application: Application, uri: Uri) {
        try {
            GlobalScope.launch(Main) {
                val logs = async(IO) {
                    val logs = mutableListOf<Log>()
                    application.contentResolver.openInputStream(uri)?.let {
                        try {
                            val reader = LogcatStreamReader(it)
                            for (log in reader) {
                                logs += log
                            }
                        } catch (e: IOException) {
                            // ignore
                        } finally {
                            it.closeQuietly()
                        }
                    }

                    logs
                }.await()
                value = logs
            }
        } catch (e: FileNotFoundException) {
            value = emptyList()
        }
    }
}