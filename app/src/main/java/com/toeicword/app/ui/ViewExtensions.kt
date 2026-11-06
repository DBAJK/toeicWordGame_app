package com.toeicword.app.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.toeicword.app.R
import java.text.NumberFormat
import java.util.Locale

private val numberFormat = NumberFormat.getIntegerInstance(Locale.KOREA)

fun Int.commas(): String = numberFormat.format(this)

fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

// 배속 표시(×1.3). 소수점이 쉼표가 되는 언어 설정에서도 점으로 쓴다.
fun Context.speedLabel(speed: Double): String =
    getString(R.string.speed_value, String.format(Locale.ROOT, "%.1f", speed))

// filter: grayscale(1)
val GrayscalePaint = Paint().apply {
    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
}

fun View.setGrayscale(gray: Boolean) {
    setLayerType(if (gray) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE, if (gray) GrayscalePaint else null)
}

// 틀렸을 때 입력창을 좌우로 흔든다.
fun View.shake() {
    val unit = context.dp(1f)
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, -8f * unit, 8f * unit, -4f * unit, 0f).apply {
        duration = 300
        interpolator = LinearInterpolator()
    }.start()
}

// 키보드의 완료·이동 키와 외부 키보드의 Enter를 onSubmit으로 넘긴다. 키보드는 내리지 않는다.
fun EditText.onSubmit(action: () -> Unit) {
    setOnEditorActionListener { _, actionId, event ->
        // 외부 키보드는 누를 때와 뗄 때 두 번 들어오므로 누를 때만 처리한다.
        if (event != null && event.action != KeyEvent.ACTION_DOWN) return@setOnEditorActionListener true
        val enterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER || event?.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (actionId == EditorInfo.IME_NULL && !enterKey) return@setOnEditorActionListener false
        action()
        true
    }
}

// 조합 중이던 글자가 키보드 앱에 남지 않도록 입력 연결을 다시 시작한다.
fun EditText.clearInput() {
    text.clear()
    context.getSystemService<InputMethodManager>()?.restartInput(this)
}

fun EditText.showKeyboard(window: Window) {
    requestFocus()
    WindowCompat.getInsetsController(window, this).show(WindowInsetsCompat.Type.ime())
}

fun View.hideKeyboard(window: Window) {
    WindowCompat.getInsetsController(window, this).hide(WindowInsetsCompat.Type.ime())
}
