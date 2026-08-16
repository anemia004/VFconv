package com.vfconv.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ConvertViewModel : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _state = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val state: StateFlow<ConversionState> = _state

    fun startConversion(context: Context, inputUri: Uri, outputUri: Uri, options: ConvertOptions) {
        viewModelScope.launch {
            _state.value = ConversionState.Running
            _progress.value = 0

            // Copy input to cache (FFmpeg needs a file path)
            val inputFile = File(context.cacheDir, "input_${System.currentTimeMillis()}.mp4")
            try {
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    inputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    _state.value = ConversionState.Error("Cannot open input stream")
                    return@launch
                }
            } catch (e: Exception) {
                _state.value = ConversionState.Error(e.message)
                return@launch
            }

            val outputFile = File(context.cacheDir, "output_${System.currentTimeMillis()}.${options.outputFormat}")

            val result = withContext(Dispatchers.IO) {
                runFFmpeg(inputFile.absolutePath, outputFile.absolutePath, options)
            }

            if (result.isSuccess) {
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        outputFile.inputStream().use { it.copyTo(out) }
                    } ?: throw Exception("Cannot open output stream")
                    outputFile.delete()
                    inputFile.delete()
                    _state.value = ConversionState.Success
                } catch (e: Exception) {
                    outputFile.delete()
                    inputFile.delete()
                    _state.value = ConversionState.Error(e.message)
                }
            } else {
                outputFile.delete()
                inputFile.delete()
                _state.value = ConversionState.Error(result.exceptionOrNull()?.message)
            }
        }
    }

    fun cancel() {
        FFmpegKit.cancel()
    }

    private fun runFFmpeg(inputPath: String, outputPath: String, options: ConvertOptions): Result<Unit> {
        val command = buildFFmpegCommand(inputPath, outputPath, options)
        Log.d("VFconv", "Executing: $command")

        val session = FFmpegKit.execute(command)
        return if (ReturnCode.isSuccess(session.returnCode)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(session.allLogsAsString))
        }
    }

    private fun buildFFmpegCommand(inputPath: String, outputPath: String, options: ConvertOptions): String {
        return "-y -i \"$inputPath\" -c:v ${options.codec} -preset ${options.preset} -crf ${options.crf} " +
            (if (options.resolution != null) "-vf scale=${options.resolution} " else "") +
            (if (options.bitrate != null) "-b:v ${options.bitrate} " else "") +
            when (options.outputFormat) {
                "webm" -> "-c:a libopus -b:a 128k "
                else -> "-c:a aac -b:a 128k "
            } +
            "-f ${options.outputFormat} \"$outputPath\""
    }
}
