package com.vfconv.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
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

    private suspend fun runFFmpeg(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val commandArray = buildFFmpegCommand(inputPath, outputPath, options)
        Log.d("VFconv", "Executing: ${commandArray.joinToString(" ")}")

        val session = FFmpegKit.executeWithArguments(commandArray)
        if (ReturnCode.isSuccess(session.returnCode)) {
            Result.success(Unit)
        } else {
            // Capture full logs for debugging
            val logs = session.allLogsAsString
            Log.e("VFconv", "FFmpeg failed: $logs")
            Result.failure(Exception(logs))
        }
    }

    private fun buildFFmpegCommand(inputPath: String, outputPath: String, options: ConvertOptions): Array<String> {
        val args = mutableListOf<String>()
        args.add("-y")
        args.add("-i")
        args.add(inputPath)
        args.add("-c:v")
        args.add(options.codec)
        args.add("-preset")
        args.add(options.preset)
        args.add("-crf")
        args.add(options.crf.toString())
        if (options.resolution != null) {
            args.add("-vf")
            args.add("scale=${options.resolution}")
        }
        if (options.bitrate != null) {
            args.add("-b:v")
            args.add(options.bitrate)
        }
        when (options.outputFormat) {
            "webm" -> {
                args.add("-c:a")
                args.add("libopus")
                args.add("-b:a")
                args.add("128k")
            }
            else -> {
                args.add("-c:a")
                args.add("aac")
                args.add("-b:a")
                args.add("128k")
            }
        }
        args.add("-f")
        args.add(options.outputFormat)
        args.add(outputPath)
        return args.toTypedArray()
    }
}
