package com.vfconv.app

import android.content.Context
import android.media.MediaMetadataRetriever
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
import java.util.concurrent.CountDownLatch

class ConvertViewModel : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _state = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val state: StateFlow<ConversionState> = _state

    private var currentSession: FFmpegSession? = null

    fun startConversion(context: Context, inputUri: Uri, outputUri: Uri, options: ConvertOptions) {
        viewModelScope.launch {
            _state.value = ConversionState.Running
            _progress.value = 0
            currentSession = null

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
                Log.d("VFconv", "Input file size: ${inputFile.length()} bytes")
            } catch (e: Exception) {
                _state.value = ConversionState.Error(e.message)
                return@launch
            }

            val durationMs = getVideoDurationMs(inputFile.absolutePath)
            val outputFile = File(context.cacheDir, "output_${System.currentTimeMillis()}.mp4")

            val result = withContext(Dispatchers.IO) {
                runFFmpegAsync(inputFile.absolutePath, outputFile.absolutePath, options, durationMs)
            }

            if (result.isSuccess && outputFile.length() > 0) {
                Log.d("VFconv", "Output file size: ${outputFile.length()} bytes")
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        outputFile.inputStream().use { it.copyTo(out) }
                    } ?: throw Exception("Cannot open output stream")
                    outputFile.delete()
                    inputFile.delete()
                    _state.value = ConversionState.Success
                } catch (e: SecurityException) {
                    Log.e("VFconv", "SecurityException while writing output", e)
                    outputFile.delete()
                    inputFile.delete()
                    _state.value = ConversionState.Error("Permission denied: ${e.message}")
                } catch (e: Exception) {
                    outputFile.delete()
                    inputFile.delete()
                    _state.value = ConversionState.Error(e.message)
                }
            } else {
                Log.e("VFconv", "Conversion failed or empty output")
                outputFile.delete()
                inputFile.delete()
                _state.value = ConversionState.Error(result.exceptionOrNull()?.message ?: "Conversion failed")
            }
        }
    }

    fun cancel() {
        currentSession?.cancel()
        FFmpegKit.cancel()
    }

    private suspend fun runFFmpegAsync(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions,
        durationMs: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val command = mutableListOf<String>()

        if (options.codec == "copy") {
            command.addAll(listOf(
                "-y",
                "-i", inputPath,
                "-c:v", "copy",
                "-c:a", "copy",
                outputPath
            ))
        } else {
            command.addAll(listOf(
                "-y",
                "-err_detect", "ignore_err",
                "-fflags", "+genpts+discardcorrupt",
                "-i", inputPath,
                "-c:v", options.codec,
                "-preset", "ultrafast",
                "-crf", options.crf.toString(),
                "-threads", "0"
            ))

            if (options.codec == "libx264") {
                command.addAll(listOf("-tune", "zerolatency"))
            }

            options.resolution?.let { res -> command.addAll(listOf("-vf", "scale=$res")) }
            options.bitrate?.let { bitrate -> command.addAll(listOf("-b:v", bitrate)) }

            if (options.codec == "libvpx-vp9") {
                command.addAll(listOf("-deadline", "realtime", "-cpu-used", "8"))
            }

            command.addAll(listOf("-c:a", "aac", "-b:a", "128k"))
            command.add(outputPath)
        }

        // Build command string with quotes for paths
        val commandString = command.joinToString(" ") { arg ->
            if (arg.startsWith("/") || arg.contains(" ")) "\"$arg\"" else arg
        }
        Log.d("VFconv", "Executing: $commandString")

        val latch = CountDownLatch(1)
        var success = false
        var errorMessage: String? = null

        try {
            val session = FFmpegKit.executeAsync(
                commandString,
                { completedSession ->
                    success = ReturnCode.isSuccess(completedSession.getReturnCode())
                    if (!success) {
                        errorMessage = completedSession.getAllLogsAsString()
                    }
                    latch.countDown()
                },
                { log -> /* optional log callback */ },
                { statistics ->
                    if (durationMs > 0) {
                        val percent = ((statistics.time.toDouble() / durationMs) * 100)
                            .toInt().coerceIn(0, 100)
                        _progress.value = percent
                    }
                }
            )
            currentSession = session
            latch.await()
        } catch (e: Exception) {
            errorMessage = e.message
            latch.countDown()
        }

        if (success) Result.success(Unit)
        else Result.failure(Exception(errorMessage ?: "Conversion failed"))
    }

    private fun getVideoDurationMs(path: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("VFconv", "Failed to get duration", e)
            0L
        }
    }
}
