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

            // Copy input to cache
            val inputFile = File(context.cacheDir, "input_${System.currentTimeMillis()}.mp4")
            try {
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    inputFile.outputStream().use { output -> input.copyTo(output) }
                } ?: run {
                    _state.value = ConversionState.Error("Cannot open input stream")
                    return@launch
                }
                if (inputFile.length() == 0L) {
                    _state.value = ConversionState.Error("Input file is empty")
                    return@launch
                }
            } catch (e: Exception) {
                _state.value = ConversionState.Error(e.message)
                return@launch
            }

            val outputFile = File(context.cacheDir, "output_${System.currentTimeMillis()}.mp4")

            val result = withContext(Dispatchers.IO) {
                runFFmpeg(inputFile.absolutePath, outputFile.absolutePath)
            }

            if (result.isSuccess && outputFile.length() > 0) {
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
                _state.value = ConversionState.Error(result.exceptionOrNull()?.message ?: "Conversion failed")
            }
        }
    }

    fun cancel() {
        FFmpegKit.cancel()
    }

    private suspend fun runFFmpeg(inputPath: String, outputPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val command = arrayOf(
                "-y",
                "-i", inputPath,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                outputPath
            )
            Log.d("VFconv", "Executing: ${command.joinToString(" ")}")

            val session = FFmpegKit.executeWithArguments(command)
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                Result.success(Unit)
            } else {
                val logs = session.getAllLogsAsString()
                Log.e("VFconv", "FFmpeg failed: $logs")
                Result.failure(Exception(logs))
            }
        }
}
