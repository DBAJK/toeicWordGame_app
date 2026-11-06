package com.toeicword.app.ui.game

import kotlin.math.abs

// 애니메이션 키프레임. 앱에서는 게임 시간으로 위치를 계산하기 때문에,
// 진행률(0~1)을 넣으면 값이 나오는 표로 만들었다.

internal fun interface Easing {
    fun transform(fraction: Float): Float
}

// cubic-bezier(a, b, c, d). x가 fraction이 되는 곡선 위의 점을 이분 탐색으로 찾는다.
internal class CubicBezierEasing(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float,
) : Easing {
    private fun curve(p1: Float, p2: Float, t: Float): Float {
        val rest = 1 - t
        return 3 * rest * rest * t * p1 + 3 * rest * t * t * p2 + t * t * t
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        var low = 0f
        var high = 1f
        repeat(MAX_STEPS) {
            val middle = (low + high) / 2
            val x = curve(a, c, middle)
            if (abs(x - fraction) < PRECISION) return curve(b, d, middle)
            if (x < fraction) low = middle else high = middle
        }
        return curve(b, d, (low + high) / 2)
    }

    private companion object {
        const val MAX_STEPS = 20
        const val PRECISION = 0.0005f
    }
}

private val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val Linear = Easing { it }

internal class Track(
    private val stops: FloatArray,
    private val values: FloatArray,
    private val easings: List<Easing>,
) {
    fun at(progress: Float): Float {
        val p = progress.coerceIn(stops.first(), stops.last())
        var i = 0
        while (i < stops.size - 2 && p >= stops[i + 1]) i++
        val span = stops[i + 1] - stops[i]
        val local = if (span == 0f) 1f else (p - stops[i]) / span
        val eased = easings[i.coerceAtMost(easings.lastIndex)].transform(local)
        return values[i] + (values[i + 1] - values[i]) * eased
    }
}

private fun track(easing: Easing, vararg frames: Pair<Float, Float>) = Track(
    FloatArray(frames.size) { frames[it].first },
    FloatArray(frames.size) { frames[it].second },
    listOf(easing),
)

private fun shared(stops: List<Float>, values: List<Float>, easings: List<Easing>) =
    Track(stops.toFloatArray(), values.toFloatArray(), easings)

// 통통: 착지할 때 납작, 뛰어오를 때 길쭉. 좌우로 번갈아 기울며 내려온다.
private val HopStops = listOf(0f, 0.08f, 0.25f, 0.45f, 0.5f, 0.58f, 0.75f, 0.95f, 1f)
private val HopUp = CubicBezierEasing(0.3f, 0.7f, 0.5f, 1f)
private val HopDown = CubicBezierEasing(0.5f, 0f, 0.7f, 0.4f)
private val HopEasings = listOf(HopUp, HopUp, HopDown, Ease, HopUp, HopUp, HopDown, Ease)
internal val HopY = shared(HopStops, listOf(0f, -2f, -11f, -1f, 0f, -2f, -11f, -1f, 0f), HopEasings)
internal val HopScaleX = shared(HopStops, listOf(1.12f, 0.94f, 1f, 0.96f, 1.12f, 0.94f, 1f, 0.96f, 1.12f), HopEasings)
internal val HopScaleY = shared(HopStops, listOf(0.88f, 1.08f, 1f, 1.05f, 0.88f, 1.08f, 1f, 1.05f, 0.88f), HopEasings)
internal val HopRotate = shared(HopStops, listOf(0f, -2f, -6f, -2f, 0f, 2f, 6f, 2f, 0f), HopEasings)

// 단어판은 몬스터에 매달린 듯 한 박자 늦게 살짝 흔들린다.
internal val SwayY = track(EaseInOut, 0f to 1f, 0.25f to -3f, 0.5f to 1f, 0.75f to -3f, 1f to 1f)
internal val SwayRotate = track(EaseInOut, 0f to 0f, 0.25f to -2f, 0.5f to 0f, 0.75f to 2f, 1f to 0f)

// 맞음: 납작하게 찌그러짐 → 바깥쪽으로 포물선을 그리며 빙글빙글 튕겨 나감.
private val ImpactStops = listOf(0f, 0.1f, 0.2f, 1f)
private val ImpactEasings = listOf(Ease, Ease, CubicBezierEasing(0.3f, 0.5f, 0.6f, 1f))
internal val ImpactScaleX = shared(ImpactStops, listOf(1f, 1.35f, 0.88f, 0.75f), ImpactEasings)
internal val ImpactScaleY = shared(ImpactStops, listOf(1f, 0.68f, 1.18f, 0.75f), ImpactEasings)
internal val ImpactRotate = shared(ImpactStops, listOf(0f, 0f, 20f, 600f), ImpactEasings)
internal val FlyX = track(Linear, 0f to 0f, 0.15f to 0f, 1f to 170f)
internal val FlyY = shared(
    listOf(0f, 0.15f, 0.5f, 1f),
    listOf(0f, 0f, -80f, 90f),
    listOf(Ease, CubicBezierEasing(0.25f, 0.6f, 0.45f, 1f), CubicBezierEasing(0.55f, 0f, 0.75f, 0.4f)),
)
internal val FadeLate = track(Ease, 0f to 1f, 0.7f to 1f, 1f to 0f)
internal val CreamIn: Easing = CubicBezierEasing(0.3f, 1.6f, 0.6f, 1f)
internal val PlatePopX = track(EaseIn, 0f to 1f, 0.3f to 1.18f, 1f to 0.2f)
internal val PlatePopY = track(EaseIn, 0f to 1f, 0.3f to 1.1f, 1f to 0.2f)
internal val PlatePopRotate = track(EaseIn, 0f to 0f, 0.3f to 0f, 1f to -25f)
internal val PlatePopAlpha = track(EaseIn, 0f to 1f, 1f to 0f)
internal val Crumb: Easing = CubicBezierEasing(0.2f, 0.7f, 0.4f, 1f)

internal val PointsY = track(EaseOut, 0f to 0f, 0.18f to -14f, 0.32f to -18f, 1f to -60f)
internal val PointsScale = track(EaseOut, 0f to 0.3f, 0.18f to 1.35f, 0.32f to 1f, 1f to 1f)
internal val PointsAlpha = track(EaseOut, 0f to 0f, 0.18f to 1f, 0.75f to 1f, 1f to 0f)

// 놓침: 좌우로 쿵쿵거리다가 흐려진다.
internal val StompX = track(EaseOut, 0f to 0f, 0.05f to -6f, 0.1f to 6f, 0.15f to -4f, 0.2f to 4f, 0.25f to 0f, 1f to 0f)
internal val StompAlpha = track(EaseOut, 0f to 1f, 0.8f to 1f, 1f to 0f)

internal val BannerScale = track(EaseOut, 0f to 0.3f, 0.15f to 1.15f, 0.25f to 1f, 1f to 1f)
internal val BannerAlpha = track(EaseOut, 0f to 0f, 0.15f to 1f, 0.8f to 1f, 1f to 0f)
internal val BannerShift = track(EaseOut, 0f to 0f, 0.8f to 0f, 1f to -0.2f)

// 던지는 순간 길쭉해지며 목표 쪽으로 기울고, 반대로 살짝 출렁인 뒤 제자리.
private val ThrowStops = listOf(0f, 0.25f, 0.55f, 0.8f, 1f)
internal val ThrowY = shared(ThrowStops, listOf(0f, -0.09f, 0.01f, -0.01f, 0f), listOf(EaseOut))
internal val ThrowRotate = shared(ThrowStops, listOf(0f, 14f, -5f, 2f, 0f), listOf(EaseOut))
internal val ThrowScaleX = shared(ThrowStops, listOf(1.08f, 0.92f, 1.05f, 0.99f, 1f), listOf(EaseOut))
internal val ThrowScaleY = shared(ThrowStops, listOf(0.9f, 1.12f, 0.95f, 1.01f, 1f), listOf(EaseOut))

internal val HeartScale = track(Ease, 0f to 1f, 0.3f to 1.6f, 1f to 1f)
internal val HeartRotate = track(Ease, 0f to 0f, 0.3f to -15f, 1f to 0f)
