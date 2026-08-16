package com.vfconv.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConvertViewModel : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _state = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val state: StateFlow<ConversionState> = _state

    fun startConversion(context: Context, inputUri: Uri, outputUri: Uri, options: ConvertOptions) {
        viewModelScope.launch {
            _state.value = ConversionState.Running
            _progress.value = 0

            val result = withContext(Dispatchers.IO) {
                runMedia3Conversion(context, inputUri, outputUri, options)
            }

            _state.value = if (result.isSuccess) {
                ConversionState.Success
            } else {
                ConversionState.Error(result.exceptionOrNull()?.message)
            }
        }
    }

    fun cancel() {
        // Transformer cancellation not implemented here
    }

    private fun runMedia3Conversion(
        context: Context,
        inputUri: Uri,
        outputUri: Uri,
        options: ConvertOptions
    ): Result<Unit> {
        return try {
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(getMimeType(options.outputFormat))
                .build()

            val mediaItem = MediaItem.fromUri(inputUri)
            val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
            val sequence = EditedMediaItemSequence.Builder(editedMediaItem).build()
            val composition = Composition.Builder(sequence).build()

            var completed = false
            var failed = false
            var errorMessage: String? = null

            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    completed = true
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    failed = true
                    errorMessage = exportException.message
                }
            })

            transformer.start(composition, outputUri.toString())

            val progressHolder = ProgressHolder()
            while (!completed && !failed) {
                Thread.sleep(100)
                val progress = transformer.getProgress(progressHolder)
                _progress.value = progress
            }

            if (completed) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(errorMessage ?: "Conversion failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getMimeType(format: String): String {
        return when (format) {
            "mkv" -> MimeTypes.VIDEO_MATROSKA
            "webm" -> MimeTypes.VIDEO_WEBM
            else -> MimeTypes.VIDEO_MP4
        }
    }
}
