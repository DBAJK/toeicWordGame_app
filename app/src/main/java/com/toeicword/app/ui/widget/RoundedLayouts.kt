package com.toeicword.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout

// 자식을 배경의 둥근 모서리에 맞춰 잘라 낸다. XML의 clipToOutline은 Android 12부터라서 코드로 켠다.
class RoundedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        clipToOutline = true
    }
}

class RoundedFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    init {
        clipToOutline = true
    }
}
