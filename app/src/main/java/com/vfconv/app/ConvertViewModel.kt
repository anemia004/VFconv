package com.vfconv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
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

    fun startConversion(inputPath: String, outputPath: String, options: ConvertOptions) {
        viewModelScope.launch {
            _state.value = ConversionState.Running
            _progress.value = 0

            val result = withContext(Dispatchers.IO) {
                convertVideo(inputPath, outputPath, options) { p ->
                    _progress.value = p
                }
            }

            _state.value = if (result.isSuccess) {
                ConversionState.Success
            } else {
                ConversionState.Error(result.exceptionOrNull()?.message)
            }
        }
    }

    fun cancel() {
        FFmpegKit.cancel()
    }

    private suspend fun convertVideo(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions,
        onProgress: (Int) -> Unit
    ): Result<Unit> {
        val cmd = buildString {
            append("-y -i ")
            append("\"$inputPath\" ")
            append("-c:v ${options.codec} ")
            append("-preset ${options.preset} ")
            append("-crf ${options.crf} ")
            if (options.resolution != null) append("-vf scale=${options.resolution} ")
            if (options.bitrate != null) append("-b:v ${options.bitrate} ")
            when (options.outputFormat) {
                "webm" -> append("-c:a libopus -b:a 128k ")
                else -> append("-c:a aac -b:a 128k ")
            }
            append("-f ${options.outputFormat} ")
            append("\"$outputPath\"")
        }

        val session = FFmpegKit.executeAsync(cmd) { ffmpegSession ->
            val duration = ffmpegSession?.duration ?: 0
            val time = ffmpegSession?.time ?: 0
            if (duration > 0) {
                val percent = (time.toDouble() / duration * 100).toInt().coerceIn(0, 100)
                onProgress(percent)
            }
        }

        session.returnCode
        return if (ReturnCode.isSuccess(session.returnCode)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(session.allLogsAsString))
        }
    }
}
