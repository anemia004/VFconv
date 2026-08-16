package com.vfconv.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
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
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                inputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val outputFile = File(context.cacheDir, "output_${System.currentTimeMillis()}.${options.outputFormat}")

            val result = withContext(Dispatchers.IO) {
                runFFmpegConversion(context, inputFile.absolutePath, outputFile.absolutePath, options) { p ->
                    _progress.value = p
                }
            }

            if (result.isSuccess) {
                // Copy output to user-selected location
                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    outputFile.inputStream().use { it.copyTo(out) }
                }
                outputFile.delete()
                inputFile.delete()
                _state.value = ConversionState.Success
            } else {
                outputFile.delete()
                inputFile.delete()
                _state.value = ConversionState.Error(result.exceptionOrNull()?.message)
            }
        }
    }

    fun cancel() {
        // Not implemented
    }

    private fun runFFmpegConversion(
        context: Context,
        inputPath: String,
        outputPath: String,
        options: ConvertOptions,
        onProgress: (Int) -> Unit
    ): Result<Unit> {
        return try {
            val ffmpeg = FFmpeg.getInstance(context)
            loadFFmpegBinary(ffmpeg)

            val command = buildFFmpegCommand(inputPath, outputPath, options)
            Log.d("VFconv", "Executing: $command")

            var completed = false
            var failed = false
            var errorMessage: String? = null

            ffmpeg.execute(command, object : FFmpegExecuteResponseHandler {
                override fun onSuccess(message: String?) {
                    completed = true
                }

                override fun onProgress(progress: String?) {
                    val timeInSeconds = parseTime(progress)
                    if (timeInSeconds > 0) {
                        val percent = (timeInSeconds / 60) * 100
                        onProgress(percent.toInt().coerceIn(0, 100))
                    }
                }

                override fun onFailure(message: String?) {
                    failed = true
                    errorMessage = message
                }

                override fun onStart() {}
                override fun onFinish() {}
            })

            while (!completed && !failed) {
                Thread.sleep(100)
            }

            if (completed) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(errorMessage ?: "FFmpeg failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadFFmpegBinary(ffmpeg: FFmpeg) {
        try {
            ffmpeg.loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onStart() {}
                override fun onFailure() {}
                override fun onSuccess() {}
                override fun onFinish() {}
            })
        } catch (e: FFmpegNotSupportedException) {
            throw e
        }
    }

    private fun buildFFmpegCommand(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions
    ): String {
        val cmd = mutableListOf<String>()
        cmd.add("-y")
        cmd.add("-i")
        cmd.add(inputPath)
        cmd.add("-c:v")
        cmd.add(options.codec)
        cmd.add("-preset")
        cmd.add(options.preset)
        cmd.add("-crf")
        cmd.add(options.crf.toString())
        if (options.resolution != null) {
            cmd.add("-vf")
            cmd.add("scale=${options.resolution}")
        }
        if (options.bitrate != null) {
            cmd.add("-b:v")
            cmd.add(options.bitrate)
        }
        when (options.outputFormat) {
            "webm" -> {
                cmd.add("-c:a")
                cmd.add("libopus")
                cmd.add("-b:a")
                cmd.add("128k")
            }
            else -> {
                cmd.add("-c:a")
                cmd.add("aac")
                cmd.add("-b:a")
                cmd.add("128k")
            }
        }
        cmd.add("-f")
        cmd.add(options.outputFormat)
        cmd.add(outputPath)
        return cmd.joinToString(" ")
    }

    private fun parseTime(progress: String?): Double {
        if (progress == null) return 0.0
        val regex = Regex("time=(\\d+):(\\d+):(\\d+\\.?\\d*)")
        val match = regex.find(progress) ?: return 0.0
        val hours = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
        val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
        return hours * 3600 + minutes * 60 + seconds
    }
}
