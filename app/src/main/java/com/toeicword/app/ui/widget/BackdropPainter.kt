package com.toeicword.app.ui.widget

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import androidx.core.graphics.withScale
import com.toeicword.app.game.BoardBackground
import kotlin.math.hypot

// 무대 배경과 부엌을 Canvas로 그린다. 값은 dp로 읽는다.

private fun verticalGradient(top: Float, bottom: Float, vararg stops: Pair<Float, Long>): Shader = LinearGradient(
    0f,
    top,
    0f,
    bottom,
    IntArray(stops.size) { stops[it].second.toInt() },
    FloatArray(stops.size) { stops[it].first },
    Shader.TileMode.CLAMP,
)

private fun horizontalGradient(left: Float, right: Float, vararg stops: Pair<Float, Long>): Shader = LinearGradient(
    left,
    0f,
    right,
    0f,
    IntArray(stops.size) { stops[it].second.toInt() },
    FloatArray(stops.size) { stops[it].first },
    Shader.TileMode.CLAMP,
)

private class Painter(val canvas: Canvas, val w: Float, val h: Float, val u: Float) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun rect(color: Long, x: Float = 0f, y: Float = 0f, width: Float = w, height: Float = h) {
        paint.shader = null
        paint.color = color.toInt()
        canvas.drawRect(x, y, x + width, y + height, paint)
    }

    fun rect(shader: Shader, x: Float = 0f, y: Float = 0f, width: Float = w, height: Float = h) {
        paint.shader = shader
        canvas.drawRect(x, y, x + width, y + height, paint)
        paint.shader = null
    }

    fun circle(color: Long, x: Float, y: Float, radius: Float) {
        paint.color = color.toInt()
        canvas.drawCircle(x, y, radius, paint)
    }

    fun oval(color: Long, x: Float, y: Float, width: Float, height: Float) {
        paint.color = color.toInt()
        canvas.drawOval(x, y, x + width, y + height, paint)
    }

    fun path(path: Path, color: Long) {
        paint.color = color.toInt()
        canvas.drawPath(path, paint)
    }

    fun path(path: Path, shader: Shader) {
        paint.shader = shader
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    fun roundRect(color: Long, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        paint.color = color.toInt()
        canvas.drawRoundRect(x, y, x + width, y + height, radius, radius, paint)
    }

    // radial-gradient(ellipse rx ry at cx cy, color, transparent 70%)
    fun glow(cx: Float, cy: Float, rx: Float, ry: Float, color: Long) {
        if (rx <= 0f || ry <= 0f) return
        canvas.withScale(1f, ry / rx, cx, cy) {
            paint.shader = RadialGradient(
                cx,
                cy,
                rx,
                intArrayOf(color.toInt(), (color and 0x00FFFFFF).toInt()),
                floatArrayOf(0f, 0.7f),
                Shader.TileMode.CLAMP,
            )
            drawCircle(cx, cy, rx, paint)
            paint.shader = null
        }
    }

    // repeating-linear-gradient 대각선 띠.
    fun bands(angle: Float, band: Float, period: Float, color: Long) {
        val reach = hypot(w, h)
        val cx = w / 2
        val cy = h / 2
        canvas.withRotation(angle, cx, cy) {
            var y = cy - reach
            while (y < cy + reach) {
                rect(color, cx - reach, y, reach * 2, band)
                y += period
            }
        }
    }
}

internal fun Canvas.drawBoardBackground(bg: BoardBackground, w: Float, h: Float, density: Float) {
    withClip(0f, 0f, w, h) {
        val painter = Painter(this, w, h, density)
        when (bg) {
            BoardBackground.OVEN -> painter.drawOven()
            BoardBackground.SINK -> painter.drawSink()
            BoardBackground.BOX -> painter.drawBox()
            BoardBackground.FRIDGE -> painter.drawFridge()
            BoardBackground.TABLE -> painter.drawTable()
        }
    }
}

private fun Painter.drawOven() {
    rect(0xFF3B2A22)
    rect(
        horizontalGradient(
            0f,
            w,
            0f to 0xFF2B1E18,
            0.15f to 0xFF4A352A,
            0.85f to 0xFF4A352A,
            1f to 0xFF2B1E18,
        ),
    )
    // 가로 선반 철망
    var base = h
    while (base > 0f) {
        rect(0xFF8D8D8D, 0f, base - 89 * u, w, 3 * u)
        rect(0xFF4A4A4A, 0f, base - 92 * u, w, 3 * u)
        base -= 92 * u
    }
    // 아래 열선
    rect(verticalGradient(0f, h, 0.8f to 0x00FF6E28, 0.92f to 0x80FF6E28, 1f to 0xBFFF5014))
    glow(w * 0.5f, h * 0.45f, w * 0.7f, h * 0.55f, 0x73FFAA50)
}

private fun Painter.drawSink() {
    rect(verticalGradient(0f, h, 0f to 0xFFD3DBE1, 1f to 0xFFA9B4BD))
    var y = 0f
    while (y < h) {
        rect(0x1FFFFFFF, 0f, y, w, u)
        y += 4 * u
    }
    glow(w * 0.4f, h * 0.3f, w * 0.8f, h * 0.6f, 0x80FFFFFF)
    val drainX = w * 0.5f
    val drainY = h * 0.7f
    circle(0x14000000, drainX, drainY, 34 * u)
    circle(0xFF9AA5AE, drainX, drainY, 30 * u)
    circle(0xFF6F7A83, drainX, drainY, 18 * u)
}

private fun Painter.drawBox() {
    rect(0xFFF1E4CA)
    var x = 0f
    while (x < w) {
        rect(0xFFDCC59F, x + 34 * u, 0f, 3 * u, h)
        rect(0xFFFBF3E2, x + 37 * u, 0f, 3 * u, h)
        x += 74 * u
    }
    glow(w * 0.5f, h * 0.35f, w * 0.75f, h * 0.6f, 0x80FFFFFF)
}

private fun Painter.drawFridge() {
    rect(verticalGradient(0f, h, 0f to 0xFFF4FBFE, 1f to 0xFFCDE4EF))
    var base = h
    while (base > 0f) {
        rect(0x8C8CC3DE, 0f, base - 154 * u, w, 4 * u)
        rect(0xF2FFFFFF, 0f, base - 156 * u, w, 2 * u)
        base -= 156 * u
    }
    glow(0f, 0f, w * 0.5f, h * 0.35f, 0xF2FFFFFF)
    glow(w, 0f, w * 0.5f, h * 0.35f, 0xD9FFFFFF)
}

private fun Painter.drawTable() {
    rect(0xFFD95C63)
    bands(60f, 46 * u, 120 * u, 0x2BFFFFFF)
    bands(-30f, 46 * u, 120 * u, 0x2BFFFFFF)
    glow(w * 0.5f, h * 0.4f, w * 0.8f, h * 0.7f, 0x1FFFFFFF)
}

// ---------- 메인 화면 부엌 ----------

internal fun Canvas.drawKitchen(w: Float, h: Float, density: Float) {
    withClip(0f, 0f, w, h) { Painter(this, w, h, density).drawKitchen() }
}

private fun Painter.drawKitchen() {
    rect(0xFFE8CFA8)

    // 찬장
    val cabinets = h * 0.3f
    rect(
        verticalGradient(
            0f,
            cabinets,
            0f to 0xFFCF7330,
            0.84f to 0xFFA8521D,
            0.84f to 0xFF7A3A15,
            0.9f to 0xFF7A3A15,
            0.9f to 0xFF5A2A0E,
            1f to 0xFF5A2A0E,
        ),
        height = cabinets,
    )
    var x = 0f
    while (x < w) {
        rect(0x14FFFFFF, x + 18 * u, 0f, 104 * u, cabinets)
        rect(0xFF7A3A15, x, 0f, 4 * u, cabinets)
        x += 140 * u
    }
    rect(
        verticalGradient(cabinets, cabinets + 14 * u, 0f to 0x4D000000, 1f to 0x00000000),
        0f,
        cabinets,
        w,
        14 * u,
    )

    // 후드
    val hoodTop = h * 0.24f
    val hoodHeight = h * 0.15f
    val hoodLeft = w * 0.03f
    val hoodWidth = w * 0.22f
    path(
        Path().apply {
            moveTo(hoodLeft + hoodWidth * 0.2f, hoodTop)
            lineTo(hoodLeft + hoodWidth * 0.8f, hoodTop)
            lineTo(hoodLeft + hoodWidth, hoodTop + hoodHeight)
            lineTo(hoodLeft, hoodTop + hoodHeight)
            close()
        },
        verticalGradient(hoodTop, hoodTop + hoodHeight, 0f to 0xFFF1F3F5, 1f to 0xFFB4BAC0),
    )

    // 타일 벽
    val wallTop = h * 0.3f
    val wallHeight = h * 0.32f
    rect(
        verticalGradient(wallTop, wallTop + wallHeight, 0f to 0xFFF6EAD6, 1f to 0xFFE6D2B4),
        0f,
        wallTop,
        w,
        wallHeight,
    )
    canvas.withClip(0f, wallTop, w, wallTop + wallHeight) {
        var tile = wallTop + wallHeight
        while (tile > wallTop) {
            rect(0x59A08264, 0f, tile - 2 * u, w, 2 * u)
            tile -= 40 * u
        }
        var column = 0f
        while (column < w) {
            rect(0x59A08264, column, wallTop, 2 * u, wallHeight)
            column += 40 * u
        }
    }

    // 조리대
    val counterTop = h * 0.54f
    val counterHeight = h * 0.11f
    rect(
        verticalGradient(
            counterTop,
            counterTop + counterHeight,
            0f to 0xFFF4F4F4,
            0.18f to 0xFFF4F4F4,
            0.18f to 0xFFAEB2B6,
            0.24f to 0xFFAEB2B6,
            0.24f to 0xFFD98A4A,
            1f to 0xFFB05A28,
        ),
        0f,
        counterTop,
        w * 0.32f,
        counterHeight,
    )

    // 냉장고
    val fridgeWidth = w * 0.13f
    val fridgeLeft = w - fridgeWidth
    val fridgeTop = h * 0.08f
    val fridgeHeight = h * 0.72f
    rect(
        horizontalGradient(fridgeLeft - 14 * u, fridgeLeft, 0f to 0x00000000, 1f to 0x33000000),
        fridgeLeft - 14 * u,
        fridgeTop,
        14 * u,
        fridgeHeight,
    )
    rect(
        horizontalGradient(fridgeLeft, w, 0f to 0xFFC9CFD4, 0.4f to 0xFFF3F5F7, 1f to 0xFFD5DADE),
        fridgeLeft,
        fridgeTop,
        fridgeWidth,
        fridgeHeight,
    )
    rect(0xFF9AA1A7, fridgeLeft, fridgeTop, 3 * u, fridgeHeight)
    roundRect(
        0xFF8D949A,
        fridgeLeft + fridgeWidth * 0.14f,
        fridgeTop + fridgeHeight * 0.18f,
        6 * u,
        fridgeHeight * 0.18f,
        3 * u,
    )
    rect(0xFF9AA1A7, fridgeLeft, fridgeTop + fridgeHeight * 0.42f, fridgeWidth, 3 * u)

    drawFloor(top = h * 0.62f, height = h * 0.38f)
    drawKitchenTable(RectF(w * 0.27f, h * 0.66f, w * 0.73f, h * 0.86f))
}

// perspective(600) rotateX(60deg)로 눕힌 체크무늬를 직접 투영한다.
private fun Painter.drawFloor(top: Float, height: Float) {
    val cell = 55 * u
    val perspective = 600 * u
    val sin60 = 0.866f
    val cos60 = 0.5f
    fun scaleAt(depth: Float) = perspective / (perspective - depth * sin60)

    canvas.withClip(0f, top, w, top + height) {
        rect(0xFFE9E3DA, 0f, top, w, height)
        val centerX = w / 2
        val columns = ((w * 1.1f) / cell).toInt() + 2
        val path = Path()
        var row = 0
        while (true) {
            val near = row * cell
            val far = near + cell
            val s0 = scaleAt(near)
            val s1 = scaleAt(far)
            val y0 = top + near * cos60 * s0
            if (y0 > top + height || far * sin60 >= perspective * 0.95f) break
            val y1 = top + far * cos60 * s1
            for (column in -columns..columns) {
                if ((row + column) and 1 != 0) continue
                val left = column * cell
                val right = left + cell
                path.moveTo(centerX + left * s0, y0)
                path.lineTo(centerX + right * s0, y0)
                path.lineTo(centerX + right * s1, y1)
                path.lineTo(centerX + left * s1, y1)
                path.close()
            }
            row++
        }
        path(path, 0xFF9D968E)
        rect(
            verticalGradient(
                top,
                top + height,
                0f to 0x47000000,
                0.3f to 0x00000000,
                0.7f to 0x1FFFFFFF,
                1f to 0x1F000000,
            ),
            0f,
            top,
            w,
            height,
        )
    }
}

private fun Painter.drawKitchenTable(bounds: RectF) {
    glow(bounds.centerX(), bounds.centerY() + 32 * u, bounds.width() * 0.7f, bounds.height() * 0.8f, 0x59000000)
    oval(0xFF8F2A30, bounds.left + 4 * u, bounds.top + 22 * u, bounds.width() - 8 * u, bounds.height() - 8 * u)
    oval(0xFFC8434A, bounds.left, bounds.top, bounds.width(), bounds.height())
    val top = Path().apply { addOval(bounds, Path.Direction.CW) }
    canvas.withClip(top) {
        bands(45f, 18 * u, 46 * u, 0x26FFFFFF)
        bands(-45f, 18 * u, 46 * u, 0x26FFFFFF)
    }
}
