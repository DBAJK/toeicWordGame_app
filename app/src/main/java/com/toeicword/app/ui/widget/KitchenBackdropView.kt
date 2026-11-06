package com.toeicword.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

// 메인 화면의 부엌 배경과 소품.
class KitchenBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density

    private fun drawProp(canvas: Canvas, emoji: String, x: Float, y: Float, size: Float) {
        Emojis.get(emoji, size * density, density).drawCentered(canvas, width * x, height * y, null)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawKitchen(width.toFloat(), height.toFloat(), density)
        drawProp(canvas, "🍳", 0.06f, 0.5f, 36f)
        drawProp(canvas, "🥣", 0.14f, 0.51f, 36f)
        drawProp(canvas, "🗑️", 0.04f, 0.78f, 46f)
        drawProp(canvas, "🎁", 0.84f, 0.86f, 44f)
    }
}
