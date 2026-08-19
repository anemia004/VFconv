package com.vfconv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

class SettingsDialogFragment : DialogFragment() {

    private val chooseFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            prefs.edit().putString("output_folder_uri", it.toString()).apply()
            Toast.makeText(context, "Output folder set", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_close).setOnClickListener { dismiss() }

        view.findViewById<View>(R.id.btn_clear_cache).setOnClickListener {
            val cacheDir = requireContext().cacheDir
            cacheDir.deleteRecursively()
            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_choose_folder).setOnClickListener {
            chooseFolderLauncher.launch(null)
        }

        view.findViewById<View>(R.id.btn_about).setOnClickListener {
            Toast.makeText(context, "VFconv v1.0.0", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
    }
}
