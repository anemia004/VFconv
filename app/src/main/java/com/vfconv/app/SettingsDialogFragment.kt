package com.vfconv.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class SettingsDialogFragment : DialogFragment() {

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
        view.findViewById<View>(R.id.btn_update_ffmpeg).setOnClickListener {
            // Open GitHub releases page
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yourusername/VFconv/releases"))
            startActivity(intent)
        }
        view.findViewById<View>(R.id.btn_clear_cache).setOnClickListener {
            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_choose_folder).setOnClickListener {
            Toast.makeText(context, "Folder picker would open", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
}
