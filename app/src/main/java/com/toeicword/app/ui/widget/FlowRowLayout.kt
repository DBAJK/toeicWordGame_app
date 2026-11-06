package com.toeicword.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.withStyledAttributes
import androidx.core.view.isGone
import com.toeicword.app.R
import kotlin.math.max

// 자식을 가로로 늘어놓다가 폭을 넘으면 다음 줄로 내린다.
class FlowRowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
    private class Row(val width: Int, val height: Int, val count: Int)

    private var horizontalGap = 0
    private var verticalGap = 0
    private var centerRows = false
    private val rows = ArrayList<Row>()

    init {
        context.withStyledAttributes(attrs, R.styleable.FlowRowLayout) {
            horizontalGap = getDimensionPixelSize(R.styleable.FlowRowLayout_horizontalGap, 0)
            verticalGap = getDimensionPixelSize(R.styleable.FlowRowLayout_verticalGap, 0)
            centerRows = getBoolean(R.styleable.FlowRowLayout_centerRows, false)
        }
    }

    private val View.fullWidth get() = measuredWidth + (layoutParams as MarginLayoutParams).run { leftMargin + rightMargin }
    private val View.fullHeight get() = measuredHeight + (layoutParams as MarginLayoutParams).run { topMargin + bottomMargin }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
            else -> MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        }
        rows.clear()
        var rowWidth = 0
        var rowHeight = 0
        var rowCount = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            if (rowCount > 0 && rowWidth + horizontalGap + child.fullWidth > available) {
                rows += Row(rowWidth, rowHeight, rowCount)
                rowWidth = 0
                rowHeight = 0
                rowCount = 0
            }
            rowWidth += child.fullWidth + if (rowCount > 0) horizontalGap else 0
            rowHeight = max(rowHeight, child.fullHeight)
            rowCount++
        }
        if (rowCount > 0) rows += Row(rowWidth, rowHeight, rowCount)

        val width = (rows.maxOfOrNull { it.width } ?: 0) + paddingLeft + paddingRight
        val height = rows.sumOf { it.height } + verticalGap * max(0, rows.size - 1) + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val inner = width - paddingLeft - paddingRight
        var index = 0
        var y = paddingTop
        for (row in rows) {
            var x = paddingLeft + if (centerRows) (inner - row.width) / 2 else 0
            var placed = 0
            while (placed < row.count) {
                val child = getChildAt(index++)
                if (child.isGone) continue
                val params = child.layoutParams as MarginLayoutParams
                val left = x + params.leftMargin
                val top = y + (row.height - child.fullHeight) / 2 + params.topMargin
                child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
                x += child.fullWidth + horizontalGap
                placed++
            }
            y += row.height + verticalGap
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams = MarginLayoutParams(context, attrs)

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams = MarginLayoutParams(p)

    override fun checkLayoutParams(p: LayoutParams?) = p is MarginLayoutParams
}
