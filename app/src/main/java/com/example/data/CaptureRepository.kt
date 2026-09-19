package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.SavedCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class CaptureRepository(private val context: Context) {

    private val capturesDir: File by lazy {
        File(context.filesDir, "waveform_captures").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _savedCaptures = MutableStateFlow<List<SavedCapture>>(emptyList())
    val savedCaptures: StateFlow<List<SavedCapture>> = _savedCaptures.asStateFlow()

    private val memoryCaptures = mutableListOf<SavedCapture>()

    suspend fun saveCapture(capture: SavedCapture): File = withContext(Dispatchers.IO) {
        memoryCaptures.add(0, capture)
        _savedCaptures.value = memoryCaptures.toList()

        val file = File(capturesDir, "${capture.title}.csv")
        file.writeText(capture.toCsv())
        file
    }

    fun deleteCapture(capture: SavedCapture) {
        memoryCaptures.removeAll { it.id == capture.id }
        _savedCaptures.value = memoryCaptures.toList()
        val file = File(capturesDir, "${capture.title}.csv")
        if (file.exists()) {
            file.delete()
        }
    }

    fun shareCaptureCsv(capture: SavedCapture) {
        val file = File(capturesDir, "${capture.title}.csv")
        if (!file.exists()) {
            file.writeText(capture.toCsv())
        }

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Waveform Capture - ${capture.formattedDate}")
            putExtra(Intent.EXTRA_TEXT, capture.toCsv())
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Waveform CSV").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
