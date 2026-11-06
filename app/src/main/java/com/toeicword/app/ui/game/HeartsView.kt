package com.toeicword.app.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave
import com.toeicword.app.game.Config
import com.toeicword.app.ui.GrayscalePaint
import com.toeicword.app.ui.widget.EmojiImage
import com.toeicword.app.ui.widget.Emojis
import kotlin.math.ceil

// 남은 목숨. 막 잃은 하트는 커지며 기울었다가 회색으로 남는다.
class HeartsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val gap = 2 * density
    private var heart: EmojiImage = Emojis.get("❤️", 22 * density, density, Color.TRANSPARENT)
    private var lives = Config.LIVES
    private var breakProgress = 1f
    private val lostPaint = Paint(GrayscalePaint).apply { alpha = 102 }

    fun setBoardScale(scale: Float) {
        heart = Emojis.get("❤️", 22 * scale * density, density, Color.TRANSPARENT)
        requestLayout()
        invalidate()
    }

    fun show(nextLives: Int, nextBreakProgress: Float) {
        if (lives == nextLives && breakProgress == nextBreakProgress) return
        lives = nextLives
        breakProgress = nextBreakProgress
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = ceil(heart.width * Config.LIVES + gap * (Config.LIVES - 1)).toInt()
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(ceil(heart.height).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0 until Config.LIVES) {
            val p = if (i == lives) breakProgress else 1f
            val scale = HeartScale.at(p)
            val x = i * (heart.width + gap) + heart.width / 2
            val y = heart.height / 2
            canvas.withSave {
                translate(x, y)
                rotate(HeartRotate.at(p))
                scale(scale, scale)
                heart.drawCentered(this, 0f, 0f, if (i >= lives) lostPaint else null)
            }
        }
    }
}
