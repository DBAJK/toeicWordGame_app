package com.toeicword.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import com.toeicword.app.R
import com.toeicword.app.ui.dp
import kotlin.math.ceil
import kotlin.math.min

// <select>에 해당하는 드롭다운.
class SelectBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {
    private var labels: List<String> = emptyList()
    private var pick: (Int) -> Unit = {}

    init {
        setOnClickListener { open() }
    }

    fun <T> bind(value: T, options: List<T>, label: (T) -> String, onSelect: (T) -> Unit) {
        text = context.getString(R.string.select_value, label(value))
        labels = options.map(label)
        pick = { onSelect(options[it]) }
    }

    private fun open() {
        if (labels.isEmpty()) return
        val widest = ceil(labels.maxOf { paint.measureText(it) }).toInt() + context.dp(40f).toInt()
        val popup = ListPopupWindow(context)
        popup.anchorView = this
        popup.isModal = true
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_popup))
        popup.setAdapter(ArrayAdapter(context, R.layout.item_select_option, labels))
        popup.setContentWidth(maxOf(width, widest))
        popup.height = min(context.dp(320f).toInt(), labels.size * context.dp(48f).toInt())
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            pick(position)
        }
        popup.show()
    }
}
