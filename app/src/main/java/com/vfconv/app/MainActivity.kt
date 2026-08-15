package com.vfconv.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vfconv.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel = ConvertViewModel()

    private var inputUri: Uri? = null

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            inputUri = it
            binding.tvSelectedFile.text = "Selected: ${it.lastPathSegment}"
            binding.btnConvert.isEnabled = true
        }
    }

    private val createOutputLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("video/*")
    ) { uri ->
        uri?.let { outputUri ->
            startConversion(outputUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupDropdowns() {
        binding.dropdownFormat.setOptions(listOf("MP4", "MKV", "WebM"))
        binding.dropdownCodec.setOptions(listOf("H.264", "H.265", "VP9", "AV1"))
        binding.dropdownResolution.setOptions(listOf("Original", "1080p", "720p", "480p", "360p"))
        binding.dropdownBitrate.setOptions(listOf("Default", "1 Mbps", "2 Mbps", "4 Mbps", "8 Mbps"))
    }

    private fun setupClickListeners() {
        binding.btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch(arrayOf("video/*"))
        }
        binding.btnConvert.setOnClickListener {
            if (inputUri == null) {
                Toast.makeText(this, "Select a video first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val extension = when (binding.dropdownFormat.getSelected()) {
                "MKV" -> "mkv"
                "WebM" -> "webm"
                else -> "mp4"
            }
            createOutputLauncher.launch("converted_video.$extension")
        }
        binding.btnCancel.setOnClickListener { viewModel.cancel() }
        binding.btnSettings.setOnClickListener {
            SettingsDialogFragment().show(supportFragmentManager, "settings")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.progress.collect { p ->
                binding.progressBar.progress = p
                binding.tvProgress.text = "$p%"
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    ConversionState.Running -> {
                        binding.btnConvert.isEnabled = false
                        binding.btnCancel.isEnabled = true
                        binding.progressCard.visibility = android.view.View.VISIBLE
                    }
                    ConversionState.Success -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        Toast.makeText(this@MainActivity, "Conversion completed!", Toast.LENGTH_LONG).show()
                    }
                    ConversionState.Error -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        Toast.makeText(this@MainActivity, "Conversion failed: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startConversion(outputUri: Uri) {
        val inputUri = inputUri ?: return
        val options = ConvertOptions(
            codec = getCodecValue(),
            resolution = getResolutionValue(),
            bitrate = getBitrateValue(),
            outputFormat = getExtensionFromFormat()
        )
        viewModel.startConversion(this, inputUri, outputUri, options)
    }

    private fun getExtensionFromFormat(): String {
        return when (binding.dropdownFormat.getSelected()) {
            "MKV" -> "mkv"
            "WebM" -> "webm"
            else -> "mp4"
        }
    }

    private fun getCodecValue(): String {
        return when (binding.dropdownCodec.getSelected()) {
            "H.265" -> "hevc"
            "VP9" -> "vp9"
            "AV1" -> "av01"
            else -> "avc"
        }
    }

    private fun getResolutionValue(): String? {
        return when (binding.dropdownResolution.getSelected()) {
            "Original" -> null
            "1080p" -> "1920x1080"
            "720p" -> "1280x720"
            "480p" -> "854x480"
            "360p" -> "640x360"
            else -> null
        }
    }

    private fun getBitrateValue(): String? {
        return when (binding.dropdownBitrate.getSelected()) {
            "Default" -> null
            "1 Mbps" -> "1M"
            "2 Mbps" -> "2M"
            "4 Mbps" -> "4M"
            "8 Mbps" -> "8M"
            else -> null
        }
    }
}
