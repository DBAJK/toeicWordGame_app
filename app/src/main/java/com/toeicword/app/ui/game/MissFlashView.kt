package com.toeicword.app.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible

private const val FLASH_COLOR = 0xFFDC1E32.toInt()

// inset box-shadow로 보드 가장자리가 붉게 번쩍인다.
class MissFlashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val top = Paint()
    private val bottom = Paint()
    private val left = Paint()
    private val right = Paint()
    private var edge = 0f

    // progress 0이 번쩍이는 순간, 1이면 사라진다.
    fun show(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        isVisible = p < 1f
        if (p >= 1f) return
        val w = width.toFloat()
        val h = height.toFloat()
        val red = ColorUtils.setAlphaComponent(FLASH_COLOR, (0.55f * (1 - p) * 255).toInt())
        val clear = ColorUtils.setAlphaComponent(FLASH_COLOR, 0)
        edge = (1 - p) * 90 * density
        top.shader = LinearGradient(0f, 0f, 0f, edge, red, clear, Shader.TileMode.CLAMP)
        bottom.shader = LinearGradient(0f, h - edge, 0f, h, clear, red, Shader.TileMode.CLAMP)
        left.shader = LinearGradient(0f, 0f, edge, 0f, red, clear, Shader.TileMode.CLAMP)
        right.shader = LinearGradient(w - edge, 0f, w, 0f, clear, red, Shader.TileMode.CLAMP)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, edge, top)
        canvas.drawRect(0f, h - edge, w, h, bottom)
        canvas.drawRect(0f, 0f, edge, h, left)
        canvas.drawRect(w - edge, 0f, w, h, right)
    }
}
