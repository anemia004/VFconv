package com.vfconv.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.min

class LiquidGlassButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    var label: String = "Button"
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }

    private var scaleX = 1f
    private var scaleY = 1f
    private var glowAlpha = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true

        context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassButton).apply {
            label = getString(R.styleable.LiquidGlassButton_label) ?: "Button"
            recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2

        canvas.save()
        canvas.scale(scaleX, scaleY, cx, cy)

        val rect = RectF(0f, 0f, w, h)
        val radius = h / 2

        // Main glass fill
        val gradient = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(70, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Color.argb(40, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Stroke
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(200, 255, 255, 255)
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Highlight top
        val highlightGradient = LinearGradient(
            0f, 0f, 0f, h * 0.4f,
            Color.argb(120, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        paint.shader = highlightGradient
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius, radius, paint)

        // Glow effect when pressed
        if (glowAlpha > 0f) {
            glowPaint.shader = RadialGradient(
                cx, cy, min(w, h) * 0.6f,
                Color.argb((glowAlpha * 80).toInt(), 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, radius, radius, glowPaint)
        }

        // Draw label
        paint.shader = null
        textPaint.color = Color.WHITE
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, cx, textY, textPaint)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(150)
                    .start()
                glowAlpha = 1f
                invalidate()
                performClick()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator())
                    .start()
                glowAlpha = 0f
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
