package com.vfconv.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView

class GlassDropdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val selectedText: TextView
    private var options: List<String> = emptyList()
    private var listener: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_glass_dropdown, this, true)
        selectedText = findViewById(R.id.dropdown_selected_text)
        selectedText.setOnClickListener { showPopup() }
    }

    fun setOptions(options: List<String>) {
        this.options = options
        if (options.isNotEmpty()) {
            setSelected(options[0])
        }
    }

    fun setSelected(option: String) {
        selectedText.text = option
    }

    fun getSelected(): String = selectedText.text.toString()

    fun setOnOptionSelectedListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    private fun showPopup() {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_dropdown, null)
        val listView = popupView.findViewById<ListView>(R.id.dropdown_list)
        val adapter = ArrayAdapter(context, R.layout.item_dropdown, options)
        listView.adapter = adapter

        val popupWindow = PopupWindow(
            popupView,
            this.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(this, 0, 8)

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = options[position]
            setSelected(selected)
            listener?.invoke(selected)
            popupWindow.dismiss()
        }
    }
}
