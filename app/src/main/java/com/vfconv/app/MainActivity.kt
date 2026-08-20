package com.vfconv.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.vfconv.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel = ConvertViewModel()

    private var inputUri: Uri? = null
    private var selectedFormat: String = "MP4"

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
        setupTabs()
        setupClickListeners()
        observeViewModel()
        updateOutputFolderLabel()
    }

    private fun setupDropdowns() {
        binding.dropdownCodec.setOptions(listOf("Copy (Fastest)", "H.264", "H.265", "VP9"))
        binding.dropdownResolution.setOptions(listOf("Original", "1080p", "720p", "480p", "360p"))
        binding.dropdownBitrate.setOptions(listOf("Default", "1 Mbps", "2 Mbps", "4 Mbps", "8 Mbps"))
    }

    private fun setupTabs() {
        binding.tabFormat.setTabs(listOf("MP4", "MKV", "WebM"))
        binding.tabFormat.setOnTabSelectedListener { index ->
            selectedFormat = when (index) {
                0 -> "MP4"
                1 -> "MKV"
                2 -> "WebM"
                else -> "MP4"
            }
        }
        selectedFormat = "MP4"
    }

    private fun setupClickListeners() {
        binding.btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch(arrayOf("video/*"))
        }
        binding.btnConvert.setOnClickListener {
            val currentInput = inputUri
            if (currentInput == null) {
                Toast.makeText(this, "Select a video first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val folderUriString = prefs.getString("output_folder_uri", null)
            if (folderUriString != null) {
                val folderUri = Uri.parse(folderUriString)
                saveOutputToFolder(folderUri, currentInput)
            } else {
                val extension = getExtensionFromFormat()
                val suggestedName = getOutputFileName(currentInput, extension)
                createOutputLauncher.launch(suggestedName)
            }
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
                    is ConversionState.Running -> {
                        binding.btnConvert.isEnabled = false
                        binding.btnCancel.isEnabled = true
                        binding.progressCard.visibility = android.view.View.VISIBLE
                    }
                    is ConversionState.Success -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        binding.progressCard.visibility = android.view.View.GONE
                        Toast.makeText(this@MainActivity, "Conversion completed!", Toast.LENGTH_LONG).show()
                    }
                    is ConversionState.Error -> {
                        binding.btnConvert.isEnabled = true
                        binding.btnCancel.isEnabled = false
                        binding.progressCard.visibility = android.view.View.GONE
                        Toast.makeText(this@MainActivity, "Conversion failed: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun saveOutputToFolder(folderUri: Uri, inputUri: Uri) {
        val extension = getExtensionFromFormat()
        val fileName = getOutputFileName(inputUri, extension)
        val outputUri = createFileInFolder(folderUri, fileName)
        if (outputUri != null) {
            startConversion(outputUri)
        } else {
            Toast.makeText(this, "Cannot create file in selected folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFileInFolder(folderUri: Uri, fileName: String): Uri? {
        val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, folderUri)
            ?: return null
        val newFile = documentFile.createFile("video/*", fileName) ?: return null
        return newFile.uri
    }

    private fun startConversion(outputUri: Uri) {
        val currentInput = inputUri ?: return
        val options = ConvertOptions(
            codec = getCodecValue(),
            resolution = getResolutionValue(),
            bitrate = getBitrateValue(),
            outputFormat = getExtensionFromFormat()
        )
        viewModel.startConversion(this, currentInput, outputUri, options)
    }

    private fun getExtensionFromFormat(): String {
        return when (selectedFormat) {
            "MKV" -> "mkv"
            "WebM" -> "webm"
            else -> "mp4"
        }
    }

    private fun getOutputFileName(inputUri: Uri, extension: String): String {
        val originalName = inputUri.lastPathSegment ?: "video"
        val baseName = originalName.substringBeforeLast('.', originalName)
        return "${baseName}_VF.$extension"
    }

    private fun getCodecValue(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val useHardware = prefs.getBoolean("use_hardware", false)

        return when (binding.dropdownCodec.getSelected()) {
            "Copy (Fastest)" -> "copy"
            "H.264" -> if (useHardware) "h264_mediacodec" else "libx264"
            "H.265" -> if (useHardware) "hevc_mediacodec" else "libx265"
            "VP9" -> "libvpx-vp9"
            else -> "libx264"
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

    private fun updateOutputFolderLabel() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val folderUriString = prefs.getString("output_folder_uri", null)
        if (folderUriString != null) {
            binding.tvOutput.text = "Output: Custom folder set"
        } else {
            binding.tvOutput.text = "Output: Ask each time"
        }
    }

    override fun onResume() {
        super.onResume()
        updateOutputFolderLabel()
    }
}
