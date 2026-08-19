package com.vfconv.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class LiquidBottomTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    interface OnTabSelectedListener {
        fun onTabSelected(index: Int)
    }

    private val indicator = View(context)
    private val tabs = mutableListOf<TextView>()

    private var selectedIndex = 0
    private var listener: OnTabSelectedListener? = null
    private var indicatorAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))

        background = createGlassBackground()

        indicator.background = createIndicatorBackground()
        addView(indicator, LayoutParams(0, dp(48)))
        indicator.visibility = INVISIBLE
    }

    fun setTabs(tabTitles: List<String>) {
        removeAllViews()
        tabs.clear()

        addView(indicator, LayoutParams(0, dp(48)))
        indicator.visibility = INVISIBLE

        tabTitles.forEach { title ->
            val tab = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setOnClickListener { selectTab(tabs.indexOf(this)) }
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }
                    }
                    false
                }
            }
            addView(tab, LayoutParams(0, dp(48), 1f))
            tabs.add(tab)
        }

        if (tabs.isNotEmpty()) {
            selectTab(0, animate = false)
        }
    }

    fun setOnTabSelectedListener(listener: OnTabSelectedListener) {
        this.listener = listener
    }

    fun selectTab(index: Int, animate: Boolean = true) {
        if (index !in tabs.indices || index == selectedIndex) return

        val oldIndex = selectedIndex
        selectedIndex = index

        tabs.forEachIndexed { i, tab ->
            tab.alpha = if (i == index) 1f else 0.6f
        }

        val targetX = tabs[index].left.toFloat()
        val startX = indicator.translationX
        val endX = targetX - indicator.left

        if (animate) {
            indicatorAnimator?.cancel()
            indicatorAnimator = ValueAnimator.ofFloat(startX, endX).apply {
                duration = 300
                interpolator = OvershootInterpolator()
                addUpdateListener { anim ->
                    indicator.translationX = anim.animatedValue as Float
                }
                start()
            }
        } else {
            indicator.translationX = endX
        }

        indicator.visibility = VISIBLE
        listener?.onTabSelected(index)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (tabs.isNotEmpty()) {
            indicator.translationX = tabs[selectedIndex].left - indicator.left.toFloat()
            indicator.visibility = VISIBLE
        }
    }

    private fun createGlassBackground(): android.graphics.drawable.Drawable {
        val radius = 28f * resources.displayMetrics.density
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius
            colors = intArrayOf(
                Color.argb(100, 255, 255, 255),
                Color.argb(40, 255, 255, 255),
                Color.argb(60, 255, 255, 255)
            )
            orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
            setStroke(dp(1), Color.argb(200, 255, 255, 255))
        }
    }

    private fun createIndicatorBackground(): android.graphics.drawable.Drawable {
        val radius = 24f * resources.displayMetrics.density
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius
            colors = intArrayOf(
                Color.argb(120, 255, 255, 255),
                Color.argb(60, 255, 255, 255)
            )
            orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
