package com.toeicword.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.toeicword.app.R
import kotlin.math.ceil

private val LOGO_COLORS = intArrayOf(
    0xFFFFF6A8.toInt(),
    0xFFFFD23A.toInt(),
    0xFFFF9A1A.toInt(),
    0xFFD9600A.toInt(),
)
private val LOGO_STOPS = floatArrayOf(0f, 0.38f, 0.7f, 1f)

// -webkit-text-stroke + paint-order: stroke fill. 테두리를 먼저 그리고 그 위에 글자를 덮는다.
// 점수·로고·단계 표시처럼 한 줄짜리 글자에 쓴다.
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val fill = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private var depth = 0f
    private var logoFill = false

    var text: String = ""
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
            invalidate()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.OutlinedTextView) {
            text = getString(R.styleable.OutlinedTextView_android_text).orEmpty()
            setTextSizePx(getDimension(R.styleable.OutlinedTextView_android_textSize, fill.textSize))
            fill.color = getColor(R.styleable.OutlinedTextView_android_textColor, Color.BLACK)
            stroke.color = getColor(R.styleable.OutlinedTextView_outlineColor, Color.BLACK)
            stroke.strokeWidth = getDimension(R.styleable.OutlinedTextView_outlineWidth, 0f)
            depth = getDimension(R.styleable.OutlinedTextView_textDepth, 0f)
            logoFill = getBoolean(R.styleable.OutlinedTextView_logoFill, false)
            if (getBoolean(R.styleable.OutlinedTextView_blackWeight, false)) {
                val black = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Typeface.create(Typeface.DEFAULT, 900, false)
                } else {
                    Typeface.DEFAULT_BOLD
                }
                fill.typeface = black
                stroke.typeface = black
            }
        }
    }

    fun setTextSizePx(size: Float) {
        fill.textSize = size
        stroke.textSize = size
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val metrics = fill.fontMetrics
        val width = ceil(fill.measureText(text) + stroke.strokeWidth).toInt() + paddingLeft + paddingRight
        val height = ceil(metrics.descent - metrics.ascent + stroke.strokeWidth + depth).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (!logoFill) return
        val metrics = fill.fontMetrics
        val top = paddingTop + stroke.strokeWidth / 2
        fill.shader = LinearGradient(
            0f,
            top,
            0f,
            top + metrics.descent - metrics.ascent,
            LOGO_COLORS,
            LOGO_STOPS,
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val x = paddingLeft + stroke.strokeWidth / 2
        val y = paddingTop + stroke.strokeWidth / 2 - fill.fontMetrics.ascent
        if (stroke.strokeWidth > 0f) {
            // text-shadow: 0 Npx 0 같은 입체 두께
            if (depth > 0f) canvas.drawText(text, x, y + depth, stroke)
            canvas.drawText(text, x, y, stroke)
        }
        canvas.drawText(text, x, y, fill)
    }
}
