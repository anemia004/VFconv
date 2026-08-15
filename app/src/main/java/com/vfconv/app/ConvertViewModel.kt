package com.vfconv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    fun startConversion(inputPath: String, outputPath: String, options: ConvertOptions) {
        viewModelScope.launch {
            _state.value = ConversionState.Running
            _progress.value = 0

            val result = withContext(Dispatchers.IO) {
                simulateConversion(inputPath, outputPath) { p ->
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
        // No FFmpeg to cancel, but we can simulate cancellation by checking a flag
        // For now, this is a no-op
    }

    private suspend fun simulateConversion(
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)
            // Simple copy to simulate conversion
            inputFile.copyTo(outputFile, overwrite = true)
            // Simulate progress over ~3 seconds
            for (i in 1..100) {
                delay(30)
                onProgress(i)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
