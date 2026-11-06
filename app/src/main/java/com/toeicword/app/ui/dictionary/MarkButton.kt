package com.toeicword.app.ui.dictionary

import android.view.animation.PathInterpolator
import android.widget.TextView
import com.toeicword.app.domain.Word

internal fun noteOf(w: Word) = listOfNotNull(w.category, w.memo).joinToString(" · ")

private val PopEasing = PathInterpolator(0.3f, 1.6f, 0.6f, 1f)

// 별표·외움 단추. 켜지는 순간 톡 튀어나온다.
internal fun TextView.showMark(on: Boolean, animate: Boolean = true) {
    val turnedOn = on && !isSelected
    isSelected = on
    if (!turnedOn || !animate) return
    scaleX = 0.6f
    scaleY = 0.6f
    animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(PopEasing).start()
}
