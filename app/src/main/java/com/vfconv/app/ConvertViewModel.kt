package com.vfconv.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CompletableDeferred
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

    fun startConversion(
        context: Context,
        inputUri: Uri,
        outputUri: Uri,
        options: ConvertOptions
    ) {
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
                Log.d("VFconv", "Input size: ${inputFile.length()}")
            } catch (e: Exception) {
                _state.value = ConversionState.Error(e.message)
                return@launch
            }

            val outputFile = File(
                context.cacheDir,
                "output_${System.currentTimeMillis()}.${options.outputFormat}"
            )

            val result = withContext(Dispatchers.IO) {
                runFFmpeg(inputFile.absolutePath, outputFile.absolutePath, options)
            }

            if (result.isSuccess && outputFile.length() > 0) {
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        outputFile.inputStream().use { it.copyTo(out) }
                    } ?: throw Exception("Cannot write output")
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
                _state.value = ConversionState.Error(
                    result.exceptionOrNull()?.message ?: "Conversion failed"
                )
            }
        }
    }

    fun cancel() {
        FFmpegKit.cancel()
        _state.value = ConversionState.Idle
    }

    private suspend fun runFFmpeg(
        inputPath: String,
        outputPath: String,
        options: ConvertOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val command = mutableListOf<String>().apply {
            add("-y")
            add("-i")
            add(inputPath)

            // --- Video ---
            if (options.codec == "copy") {
                add("-c:v")
                add("copy")
            } else {
                add("-c:v")
                add(options.codec)

                if (options.codec in listOf("libx264", "libx265")) {
                    add("-preset")
                    add(options.preset)
                }

                add("-crf")
                add(options.crf.toString())

                options.resolution?.let { res ->
                    add("-vf")
                    add("scale=$res")
                }

                options.bitrate?.let { bit ->
                    add("-b:v")
                    add(bit)
                }
            }

            // --- Audio ---
            if (options.codec == "copy") {
                add("-c:a")
                add("copy")
            } else {
                add("-c:a")
                add("aac")
                add("-b:a")
                add("128k")
            }

            add(outputPath)
        }.toTypedArray()

        Log.d("VFconv", "Command: ${command.joinToString(" ")}")

        val deferred = CompletableDeferred<Result<Unit>>()

        FFmpegKit.executeAsync(
            command,
            { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    deferred.complete(Result.success(Unit))
                } else {
                    deferred.complete(Result.failure(Exception(session.failStackTrace)))
                }
            },
            { log ->
                Log.d("VFconv", log.message)
            },
            { statistics ->
                val seconds = statistics.time / 1_000_000
                _progress.value = (seconds % 100).toInt()
            }
        )

        deferred.await()
    }
}
