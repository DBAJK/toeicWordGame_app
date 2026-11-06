package com.toeicword.app.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlin.math.ceil

const val EMOJI_SHADOW = 0x4D000000

// 미리 그려 둔 이모지. width·height는 그림자를 뺀 글자 칸의 크기다.
class EmojiImage(private val bitmap: Bitmap, val width: Float, val height: Float, private val margin: Float) {
    fun draw(canvas: Canvas, left: Float, top: Float, paint: Paint?) {
        canvas.drawBitmap(bitmap, left - margin, top - margin, paint)
    }

    fun drawCentered(canvas: Canvas, x: Float, y: Float, paint: Paint?) = draw(canvas, x - width / 2, y - height / 2, paint)
}

// 게임 보드는 매 프레임 그려지므로 이모지를 비트맵으로 만들어 두고 변환만 적용한다.
object Emojis {
    private val cache = LruCache<String, EmojiImage>(96)

    fun get(text: String, sizePx: Float, density: Float, shadowColor: Int = EMOJI_SHADOW): EmojiImage {
        val key = "$text/${sizePx.toInt()}/$shadowColor"
        return cache[key] ?: render(text, sizePx, density, shadowColor).also { cache.put(key, it) }
    }

    private fun render(text: String, sizePx: Float, density: Float, shadowColor: Int): EmojiImage {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx
            color = Color.BLACK
            if (shadowColor != Color.TRANSPARENT) setShadowLayer(4 * density, 0f, 4 * density, shadowColor)
        }
        val metrics = paint.fontMetrics
        val width = ceil(paint.measureText(text))
        val height = ceil(metrics.descent - metrics.ascent)
        val margin = 12 * density
        val bitmap = createBitmap((width + margin * 2).toInt().coerceAtLeast(1), (height + margin * 2).toInt().coerceAtLeast(1))
        Canvas(bitmap).drawText(text, margin, margin - metrics.ascent, paint)
        return EmojiImage(bitmap, width, height, margin)
    }
}
