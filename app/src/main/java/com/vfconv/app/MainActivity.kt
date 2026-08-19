package com.vfconv.app

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
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

        setupSpinners()
        setupListeners()
        observeViewModel()
    }

    private fun setupSpinners() {
        // Format
        ArrayAdapter.createFromResource(
            this,
            R.array.format_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFormat.adapter = adapter
        }

        // Codec
        ArrayAdapter.createFromResource(
            this,
            R.array.codec_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCodec.adapter = adapter
        }

        // Resolution
        ArrayAdapter.createFromResource(
            this,
            R.array.resolution_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerResolution.adapter = adapter
        }

        // Bitrate
        ArrayAdapter.createFromResource(
            this,
            R.array.bitrate_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerBitrate.adapter = adapter
        }
    }

    private fun setupListeners() {
        binding.btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch(arrayOf("video/*"))
        }

        binding.btnConvert.setOnClickListener {
            if (inputUri == null) {
                Toast.makeText(this, "Select a video first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val extension = when (binding.spinnerFormat.selectedItem.toString()) {
                "MKV" -> "mkv"
                "WebM" -> "webm"
                else -> "mp4"
            }
            createOutputLauncher.launch("VFconv_${System.currentTimeMillis()}.$extension")
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancel()
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
                    is ConversionState.Running -> {
                        binding.btnConvert.isEnabled = false
                        binding.btnCancel.isEnabled = true
                        binding.cardProgress.visibility = android.view.View.VISIBLE
                    }
                    is ConversionState.Success -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        Toast.makeText(this@MainActivity, "Conversion complete!", Toast.LENGTH_LONG).show()
                    }
                    is ConversionState.Error -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        Toast.makeText(this@MainActivity, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startConversion(outputUri: Uri) {
        val input = inputUri ?: return
        val options = ConvertOptions(
            codec = getCodecValue(),
            resolution = getResolutionValue(),
            bitrate = getBitrateValue(),
            outputFormat = getOutputFormat()
        )
        viewModel.startConversion(this, input, outputUri, options)
    }

    private fun getCodecValue(): String {
        return when (binding.spinnerCodec.selectedItem.toString()) {
            "Copy (no re-encode)" -> "copy"
            "H.265" -> "libx265"
            "VP9" -> "libvpx-vp9"
            "AV1" -> "libaom-av1"
            else -> "libx264"
        }
    }

    private fun getResolutionValue(): String? {
        return when (binding.spinnerResolution.selectedItem.toString()) {
            "1080p" -> "1920x1080"
            "720p" -> "1280x720"
            "480p" -> "854x480"
            "360p" -> "640x360"
            else -> null
        }
    }

    private fun getBitrateValue(): String? {
        return when (binding.spinnerBitrate.selectedItem.toString()) {
            "1 Mbps" -> "1M"
            "2 Mbps" -> "2M"
            "4 Mbps" -> "4M"
            "8 Mbps" -> "8M"
            else -> null
        }
    }

    private fun getOutputFormat(): String {
        return when (binding.spinnerFormat.selectedItem.toString()) {
            "MKV" -> "mkv"
            "WebM" -> "webm"
            else -> "mp4"
        }
    }
}
