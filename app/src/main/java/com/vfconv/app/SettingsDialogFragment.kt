package com.vfconv.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
            Toast.makeText(context, "Checking for FFmpeg updates...", Toast.LENGTH_SHORT).show()
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
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
