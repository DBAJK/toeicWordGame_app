package com.toeicword.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withRotation
import com.toeicword.app.R
import com.toeicword.app.game.BoardBackground
import com.toeicword.app.game.THEMES
import com.toeicword.app.game.Theme

// 무대 배경. 장식은 움직이지 않으므로 배경과 함께 그린다.
class BoardBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private var board = BoardBackground.BOX
    private var theme: Theme? = null
    private var scale = 1f

    init {
        context.withStyledAttributes(attrs, R.styleable.BoardBackdropView) {
            board = BoardBackground.entries[getInt(R.styleable.BoardBackdropView_board, board.ordinal)]
            val decoration = getInt(R.styleable.BoardBackdropView_decorationTheme, -1)
            if (decoration >= 0) theme = THEMES[decoration]
        }
    }

    fun show(background: BoardBackground) {
        if (board == background && theme == null) return
        board = background
        theme = null
        invalidate()
    }

    // 게임 보드처럼 장식까지 함께 보여 줄 때. scale은 장식 크기 배율.
    fun show(next: Theme, nextScale: Float) {
        if (theme === next && scale == nextScale) return
        theme = next
        board = next.bg
        scale = nextScale
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawBoardBackground(board, w, h, density)
        theme?.decorations?.forEach { d ->
            val x = w * d.x / 100f
            val y = h * d.y / 100f
            val image = Emojis.get(d.emoji, d.size * scale * density, density)
            canvas.withRotation(d.rotate, x, y) { image.drawCentered(this, x, y, null) }
        }
    }
}
