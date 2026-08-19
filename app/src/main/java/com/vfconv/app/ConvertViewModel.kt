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
                if (inputFile.length() == 0L) {
                    _state.value = ConversionState.Error("Input file is empty")
                    return@launch
                }
                Log.d("VFconv", "Input file size: ${inputFile.length()} bytes")
            } catch (e: Exception) {
                _state.value = ConversionState.Error(e.message)
                return@launch
            }

            val outputFile = File(context.cacheDir, "output_${System.currentTimeMillis()}.mp4")

            val result = withContext(Dispatchers.IO) {
                runFFmpeg(inputFile.absolutePath, outputFile.absolutePath, options)
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
        FFmpegKit.cancel()
    }

    private suspend fun runFFmpeg(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Build command with optional parameters
        val command = mutableListOf(
            "-y",
            "-i", inputPath,
            "-c:v", options.codec,
            "-preset", options.preset,
            "-crf", options.crf.toString()
        )

        // Add resolution scale if specified
        options.resolution?.let { res ->
            command.addAll(listOf("-vf", "scale=$res"))
        }

        // Add bitrate if specified
        options.bitrate?.let { bitrate ->
            command.addAll(listOf("-b:v", bitrate))
        }

        // Always add audio codec
        command.addAll(listOf("-c:a", "aac", "-b:a", "128k"))
        command.add(outputPath)

        Log.d("VFconv", "Executing: ${command.joinToString(" ")}")

        try {
            val session = FFmpegKit.executeWithArguments(command.toTypedArray())
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                Log.d("VFconv", "FFmpeg success")
                Result.success(Unit)
            } else {
                val logs = session.getAllLogsAsString()
                Log.e("VFconv", "FFmpeg failed: $logs")
                Result.failure(Exception(logs))
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.e("VFconv", "Native library missing (UnsatisfiedLinkError)", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("VFconv", "FFmpeg execution error", e)
            Result.failure(e)
        }
    }
}
