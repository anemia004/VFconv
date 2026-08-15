package com.vfconv.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class MovingBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private var time = 0f
    private val colors = intArrayOf(
        Color.parseColor("#FF6B6B"),
        Color.parseColor("#4ECDC4"),
        Color.parseColor("#45B7D1"),
        Color.parseColor("#96CEB4")
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.02f
        if (time > 1f) time = 0f

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, Shader.TileMode.MIRROR
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Overlay a moving radial gradient for extra depth (optional)
        val radial = RadialGradient(
            width * 0.5f, height * 0.5f, width * 0.7f,
            Color.argb(80, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        paint.shader = radial
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        invalidate()
    }
}
