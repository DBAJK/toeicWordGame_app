package com.toeicword.app.ui.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.toeicword.app.domain.GameMode
import com.toeicword.app.game.Config
import com.toeicword.app.game.Cupcake
import com.toeicword.app.game.EnemyKind
import com.toeicword.app.game.FallingWord
import com.toeicword.app.game.GameState
import com.toeicword.app.game.ItemStatus
import com.toeicword.app.game.answerOf
import com.toeicword.app.game.specialMonster
import com.toeicword.app.game.themeFor
import com.toeicword.app.ui.widget.EMOJI_SHADOW
import com.toeicword.app.ui.widget.EmojiImage
import com.toeicword.app.ui.widget.Emojis
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

// 보드 안 세로 위치(%). 단어는 몬스터 중심 기준으로 START_Y에서 FLOOR_Y까지 내려온다.
// 한 줄짜리 단어판의 아랫변은 몬스터 중심보다 PLATE_DROP(dp × 배율) 아래에 있다.
private const val START_Y = -14f
private const val FLOOR_Y = 68f
private const val PLATE_DROP = 58f
private const val CHEF_Y = 90f

// 몬스터 통통 주기(초). 몬스터마다 박자를 어긋나게 한다.
private const val HOP_SECONDS = 0.8

private const val SCORE_YELLOW = 0xFFFFD84A.toInt()
private const val SCORE_OUTLINE = 0xFF5A2F00.toInt()
private const val BOSS_SHADOW = 0xBFFF5028.toInt()

private fun laneX(lane: Int) = (lane + 0.5f) / Config.LANES
private fun itemY(progress: Double) = (START_Y + progress.toFloat() * (FLOOR_Y - START_Y)) / 100f

// 보드 크기(px)와 스프라이트 배율. 휴대폰 보드는 작아서 크기를 비율대로 줄인다.
internal class BoardMetrics(val width: Int, val height: Int, val density: Float) {
    private val widthDp = width / density
    private val heightDp = height / density

    val scale = min(widthDp / 640f, heightDp / 420f).coerceIn(0.6f, 1f)

    // 양옆 줄의 단어판이 겹치지 않을 만큼만 넓힌다.
    val plateMaxWidth = (max(widthDp * 0.19f, min(150f, widthDp * 0.27f)) * density).roundToInt()
}

private enum class SpriteState { FALLING, SPLAT, MISSED }

private class PlateStyle(val border: Int, val top: Int, val bottom: Int, val shadow: Int, val text: Int)

private fun plate(border: Long, top: Long, bottom: Long, shadow: Long, text: Long) =
    PlateStyle(border.toInt(), top.toInt(), bottom.toInt(), shadow.toInt(), text.toInt())

private val NormalPlate = plate(0xFF4B4F57, 0xFFFFFFFF, 0xFFE9E7E1, 0xFF33363C, 0xFF222222)
private val RobotPlate = plate(0xFF2B4A8A, 0xFFF0F5FF, 0xFFCDDCF7, 0xFF1E3566, 0xFF14284F)
private val BossPlate = plate(0xFF8A2B00, 0xFFFFF4D9, 0xFFFFD27A, 0xFF5C1D00, 0xFF4A1500)
private val MissedPlate = plate(0xFFB3202F, 0xFFFFF0F0, 0xFFFFF0F0, 0xFF7D1520, 0xFFA3121F)

private val CrumbColors = longArrayOf(
    0xFFFF8FB1, 0xFFFFD84A, 0xFFFFFFFF, 0xFF8B5A2B,
    0xFF7ED6FF, 0xFFFF8FB1, 0xFFFFD84A, 0xFFFFFFFF,
).map { it.toInt() }

// 맞은 몬스터는 화면 바깥쪽으로 튕겨 나간다.
private fun flyDir(item: FallingWord): Int {
    val mid = (Config.LANES - 1) / 2
    if (item.lane == mid) return if (item.key % 2 == 0) 1 else -1
    return if (item.lane < mid) -1 else 1
}

// 줄 높이를 글꼴과 상관없이 고정한다.
private class FixedLineHeight(private val height: Int) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt,
    ) {
        val extra = height - (fm.descent - fm.ascent)
        fm.ascent -= extra / 2
        fm.descent += extra - extra / 2
        fm.top = fm.ascent
        fm.bottom = fm.descent
    }
}

// 단어 하나의 그릴거리. 단어판 글자 배치는 비싸서 상태가 바뀔 때만 다시 만든다.
private class Sprite(
    val state: SpriteState,
    val monster: String,
    val face: EmojiImage,
    val style: PlateStyle,
    val text: StaticLayout,
    val textLeft: Float,
    val plateWidth: Float,
    val plateHeight: Float,
    val width: Int,
    val fill: Shader,
    val points: String,
)

// 떨어지는 단어와 요리사, 컵케이크를 그린다. GameActivity가 프레임마다 invalidate()를 부른다.
// GameState는 프레임마다 직접 수정되므로 그릴 때 읽은 값이 그대로 화면이 된다.
class GameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private var session: GameSession? = null
    private var metrics: BoardMetrics? = null
    private val sprites = HashMap<Int, Sprite>()

    private var chef: EmojiImage? = null
    private var thrown: EmojiImage? = null
    private var hat: EmojiImage? = null

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    private val floorPaint = Paint()
    private val floorLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0x2E000000
        strokeWidth = 2 * density
        pathEffect = DashPathEffect(floatArrayOf(6 * density, 4 * density), 0f)
    }
    private val chefShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x38000000 }
    private val pointsFill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SCORE_YELLOW
        textAlign = Paint.Align.CENTER
    }
    private val pointsStroke = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SCORE_OUTLINE
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2 * density
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private var floorTop = 0f

    fun bind(next: GameSession) {
        session = next
        sprites.clear()
        invalidate()
    }

    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w == 0 || h == 0) return
        val board = BoardMetrics(w, h, density)
        val s = board.scale
        metrics = board
        sprites.clear()
        chef = Emojis.get("🧑‍🍳", 64 * s * density, density)
        thrown = Emojis.get("🧁", 32 * s * density, density)
        hat = Emojis.get("🧁", 30 * s * density, density, Color.TRANSPARENT)
        val pointsSize = sp((26 * s).coerceAtLeast(18f))
        pointsFill.textSize = pointsSize
        pointsStroke.textSize = pointsSize
        floorTop = h * FLOOR_Y / 100f + PLATE_DROP * s * density
        floorPaint.shader = LinearGradient(0f, floorTop, 0f, h.toFloat(), 0x0A000000, 0x1F000000, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val session = session ?: return
        val board = metrics ?: return
        val game = session.game
        drawFloor(canvas, board)

        val theme = themeFor(game.settings.theme, game.stage)
        for (item in game.items) drawSprite(canvas, item, game, session.mode, theme.monster, board)
        for (cupcake in game.cupcakes) drawCupcake(canvas, cupcake, game, board)
        drawChef(canvas, game, board)
        if (sprites.size > game.items.size + 8) sprites.keys.retainAll { key -> game.items.any { it.key == key } }
    }

    private fun drawFloor(canvas: Canvas, board: BoardMetrics) {
        val w = board.width.toFloat()
        val h = board.height.toFloat()
        val s = board.scale
        if (floorTop >= h) return
        canvas.drawRect(0f, floorTop, w, h, floorPaint)
        canvas.drawLine(0f, floorTop, w, floorTop, floorLinePaint)
        // 요리사 그림자
        val shadowWidth = 70 * s * density
        val shadowHeight = 14 * s * density
        val left = (w - shadowWidth) / 2
        val top = h * CHEF_Y / 100f + shadowHeight * 1.4f
        canvas.drawOval(left, top, left + shadowWidth, top + shadowHeight, chefShadowPaint)
    }

    private fun spriteStateOf(item: FallingWord, time: Double) = when {
        item.status == ItemStatus.HIT && time - item.statusAt >= Config.CUPCAKE_FLIGHT_SECONDS -> SpriteState.SPLAT
        item.status == ItemStatus.MISSED -> SpriteState.MISSED
        else -> SpriteState.FALLING
    }

    private fun spriteFor(item: FallingWord, state: SpriteState, mode: GameMode, monster: String, board: BoardMetrics): Sprite {
        val cached = sprites[item.key]
        if (cached != null && cached.state == state && cached.monster == monster) return cached
        return buildSprite(item, state, mode, monster, board).also { sprites[item.key] = it }
    }

    // Layout.BREAK_STRATEGY_BALANCED는 lint가 보는 LineBreaker(API 29)의 상수와 값이 같다.
    @SuppressLint("WrongConstant")
    private fun buildSprite(item: FallingWord, state: SpriteState, mode: GameMode, monster: String, board: BoardMetrics): Sprite {
        val s = board.scale
        val faceText = specialMonster(item.kind) ?: monster
        val faceSize = when (item.kind) {
            EnemyKind.ROBOT -> 48f
            EnemyKind.BOSS -> 54f
            EnemyKind.NORMAL -> 42f
        } * s * density
        val face = Emojis.get(faceText, faceSize, density, if (item.kind == EnemyKind.BOSS) BOSS_SHADOW else EMOJI_SHADOW)

        val style = when {
            state == SpriteState.MISSED -> MissedPlate
            item.kind == EnemyKind.ROBOT -> RobotPlate
            item.kind == EnemyKind.BOSS -> BossPlate
            else -> NormalPlate
        }
        val fontSize = sp(((if (item.kind == EnemyKind.BOSS) 19f else 18f) * s).coerceAtLeast(13f))
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.text
            textSize = fontSize
        }
        val padX = (14 * s).coerceAtLeast(8f) * density
        val padY = 3 * density
        val label = if (state == SpriteState.MISSED) answerOf(item.word, mode) else item.prompt
        val spanned = SpannableString(label).apply {
            setSpan(FixedLineHeight((fontSize * 1.25f).roundToInt()), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        val available = (board.plateMaxWidth - padX * 2).toInt().coerceAtLeast(1)
        val desired = ceil(StaticLayout.getDesiredWidth(spanned, paint)).toInt()
        val layoutWidth = min(desired, available).coerceAtLeast(1)
        val text = StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setLineBreakConfig(
                        LineBreakConfig.Builder()
                            .setLineBreakWordStyle(LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE)
                            .build(),
                    )
                }
            }
            .build()
        val textWidth = ceil((0 until text.lineCount).maxOf { text.getLineMax(it) })
        val plateWidth = textWidth + padX * 2
        val plateHeight = text.height + padY * 2
        return Sprite(
            state = state,
            monster = monster,
            face = face,
            style = style,
            text = text,
            textLeft = padX - (layoutWidth - textWidth) / 2,
            plateWidth = plateWidth,
            plateHeight = plateHeight,
            width = ceil(max(face.width, plateWidth)).toInt(),
            fill = LinearGradient(0f, 0f, 0f, plateHeight, style.top, style.bottom, Shader.TileMode.CLAMP),
            points = "+${item.points}",
        )
    }

    private fun drawSprite(
        canvas: Canvas,
        item: FallingWord,
        game: GameState,
        mode: GameMode,
        monster: String,
        board: BoardMetrics,
    ) {
        val time = game.time
        val state = spriteStateOf(item, time)
        val sprite = spriteFor(item, state, mode, monster, board)
        val s = board.scale
        val dir = flyDir(item)
        val anchor = 21 * s * density
        val splatAt = item.statusAt + Config.CUPCAKE_FLIGHT_SECONDS

        // 양끝 줄의 단어판이 보드 밖으로 잘리지 않게 안쪽으로 민다.
        val left = (board.width * laneX(item.lane) - sprite.width / 2f).roundToInt()
            .coerceIn(0, max(0, board.width - sprite.width)).toFloat()
        val top = (board.height * itemY(item.progress) - anchor).roundToInt().toFloat()

        var alpha = 1f
        var shiftX = 0f
        when (state) {
            SpriteState.SPLAT -> alpha = FadeLate.at(((time - splatAt) / 0.9).toFloat())
            SpriteState.MISSED -> {
                val p = ((time - item.statusAt) / Config.MISS_SHOW_SECONDS).toFloat()
                shiftX = StompX.at(p) * density
                alpha = StompAlpha.at(p)
            }
            SpriteState.FALLING -> Unit
        }

        canvas.withTranslation(left + shiftX, top) {
            drawMonster(this, item, sprite, s, dir, splatAt, time, alpha)
            drawPlate(this, item, sprite, s, dir, splatAt, time, alpha)
        }
        if (state == SpriteState.SPLAT) {
            canvas.withTranslation(left, top) {
                drawCrumbs(this, sprite.width / 2f, anchor, s, ((time - splatAt) / 0.5).toFloat())
                drawPoints(this, sprite, s, ((time - splatAt) / 0.9).toFloat())
            }
        }
    }

    private fun drawMonster(
        canvas: Canvas,
        item: FallingWord,
        sprite: Sprite,
        s: Float,
        dir: Int,
        splatAt: Double,
        time: Double,
        alpha: Float,
    ) {
        val face = sprite.face
        var pivotY = face.height / 2
        var tx = 0f
        var ty = 0f
        var scaleX = 1f
        var scaleY = 1f
        var rotation = 0f
        when (sprite.state) {
            SpriteState.FALLING -> {
                val phase = (item.key * 0.37) % HOP_SECONDS
                val p = (((time + phase) / HOP_SECONDS) % 1.0).toFloat()
                pivotY = face.height
                ty = HopY.at(p) * s * density
                scaleX = HopScaleX.at(p)
                scaleY = HopScaleY.at(p)
                rotation = HopRotate.at(p)
            }
            SpriteState.SPLAT -> {
                val p = ((time - splatAt) / 0.9).toFloat()
                tx = FlyX.at(p) * dir * s * density
                ty = FlyY.at(p) * s * density
                scaleX = ImpactScaleX.at(p)
                scaleY = ImpactScaleY.at(p)
                rotation = ImpactRotate.at(p) * dir
            }
            SpriteState.MISSED -> Unit
        }

        val pivotX = face.width / 2
        imagePaint.alpha = (alpha * 255).roundToInt()
        canvas.withSave {
            translate((sprite.width - face.width) / 2 + tx + pivotX, ty + pivotY)
            rotate(rotation)
            scale(scaleX, scaleY)
            translate(-pivotX, -pivotY)
            face.draw(this, 0f, 0f, imagePaint)
            if (sprite.state == SpriteState.SPLAT) drawCream(this, face, s, ((time - splatAt) / 0.22).toFloat())
        }
    }

    // 머리 위에 뒤집혀 얹히는 컵케이크
    private fun drawCream(canvas: Canvas, face: EmojiImage, s: Float, progress: Float) {
        val image = hat ?: return
        val eased = CreamIn.transform(progress.coerceIn(0f, 1f))
        val centerX = face.width / 2
        val centerY = (-16 - 10 * (1f - eased)) * s * density + image.height / 2
        val scale = 0.3f + 0.7f * eased
        canvas.withSave {
            translate(centerX, centerY)
            rotate(170f)
            scale(scale, scale)
            image.drawCentered(this, 0f, 0f, imagePaint)
        }
    }

    private fun drawPlate(
        canvas: Canvas,
        item: FallingWord,
        sprite: Sprite,
        s: Float,
        dir: Int,
        splatAt: Double,
        time: Double,
        groupAlpha: Float,
    ) {
        val style = sprite.style
        val width = sprite.plateWidth
        val height = sprite.plateHeight
        var pivotY = height / 2
        var ty = 0f
        var scaleX = 1f
        var scaleY = 1f
        var rotation = 0f
        var alpha = groupAlpha
        when (sprite.state) {
            SpriteState.FALLING -> {
                val delay = 0.06 - (item.key * 0.37) % HOP_SECONDS
                val p = ((time - delay) / HOP_SECONDS).mod(1.0).toFloat()
                pivotY = 0f
                ty = SwayY.at(p) * s * density
                rotation = SwayRotate.at(p)
            }
            SpriteState.SPLAT -> {
                val p = ((time - splatAt) / 0.32).toFloat()
                scaleX = PlatePopX.at(p)
                scaleY = PlatePopY.at(p)
                rotation = PlatePopRotate.at(p) * dir
                alpha *= PlatePopAlpha.at(p)
            }
            SpriteState.MISSED -> Unit
        }

        val alphaInt = (alpha * 255).roundToInt()
        if (alphaInt <= 0) return
        val radius = 16 * density
        val depth = 3 * density
        val border = strokePaint.strokeWidth
        val pivotX = width / 2
        val plateTop = sprite.face.height + 2 * density
        val textTop = 3 * density
        canvas.withSave {
            translate((sprite.width - width) / 2 + pivotX, plateTop + ty + pivotY)
            rotate(rotation)
            scale(scaleX, scaleY)
            translate(-pivotX, -pivotY)

            // box-shadow: 0 3px 0
            shapePaint.color = style.shadow
            shapePaint.alpha = alphaInt
            rect.set(0f, depth, width, height + depth)
            drawRoundRect(rect, radius, radius, shapePaint)

            shapePaint.color = Color.WHITE
            shapePaint.alpha = alphaInt
            shapePaint.shader = sprite.fill
            rect.set(0f, 0f, width, height)
            drawRoundRect(rect, radius, radius, shapePaint)
            shapePaint.shader = null

            strokePaint.color = style.border
            strokePaint.alpha = alphaInt
            rect.inset(border / 2, border / 2)
            drawRoundRect(rect, radius - border / 2, radius - border / 2, strokePaint)

            sprite.text.paint.alpha = alphaInt
            withTranslation(sprite.textLeft, textTop) { sprite.text.draw(this) }
        }
    }

    // 맞은 자리에서 튀는 크림·스프링클 부스러기
    private fun drawCrumbs(canvas: Canvas, centerX: Float, centerY: Float, s: Float, progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 1f) return
        val eased = Crumb.transform(p)
        val distance = (6 + 46 * eased) * s * density
        val radius = (1.2f - 0.9f * eased) * 4 * s * density
        CrumbColors.forEachIndexed { i, color ->
            val angle = Math.toRadians(i * 45.0 + 20.0)
            shapePaint.color = color
            shapePaint.alpha = ((1 - eased) * 255).roundToInt()
            canvas.drawCircle(
                centerX + cos(angle).toFloat() * distance,
                centerY + sin(angle).toFloat() * distance,
                radius,
                shapePaint,
            )
        }
    }

    private fun drawPoints(canvas: Canvas, sprite: Sprite, s: Float, progress: Float) {
        val alpha = (PointsAlpha.at(progress) * 255).roundToInt()
        if (alpha <= 0) return
        val metrics = pointsFill.fontMetrics
        val height = metrics.descent - metrics.ascent
        val scale = PointsScale.at(progress)
        val centerY = -10 * s * density + height / 2 + PointsY.at(progress) * s * density
        pointsFill.alpha = alpha
        pointsStroke.alpha = alpha
        canvas.withSave {
            translate(sprite.width / 2f, centerY)
            scale(scale, scale)
            val baseline = -height / 2 - metrics.ascent
            drawText(sprite.points, 0f, baseline, pointsStroke)
            drawText(sprite.points, 0f, baseline, pointsFill)
        }
    }

    // 요리사 손에서 목표 몬스터 머리까지 포물선. 빠르게 출발해 감속하며, 목표가 움직여도 따라간다.
    private fun drawCupcake(canvas: Canvas, cupcake: Cupcake, game: GameState, board: BoardMetrics) {
        val image = thrown ?: return
        val target = game.items.find { it.key == cupcake.targetKey } ?: return
        val s = board.scale
        val t = ((game.time - cupcake.startAt) / Config.CUPCAKE_FLIGHT_SECONDS).toFloat().coerceAtMost(1f)
        val eased = 1 - (1 - t) * (1 - t)
        val x0 = board.width / 2f + 22 * s * density
        val y0 = board.height * CHEF_Y / 100f - 48 * s * density
        val x1 = board.width * laneX(target.lane)
        val y1 = board.height * itemY(target.progress)
        val arc = sin(PI.toFloat() * t) * hypot(x1 - x0, y1 - y0) * 0.2f
        val direction = sign(x1 - x0).takeIf { it != 0f } ?: 1f
        val scale = 1 + 0.35f * sin(PI.toFloat() * t)
        imagePaint.alpha = 255
        canvas.withSave {
            translate(x0 + (x1 - x0) * eased, y0 + (y1 - y0) * eased - arc)
            rotate(direction * 360f * eased)
            scale(scale, scale)
            image.drawCentered(this, 0f, 0f, imagePaint)
        }
    }

    private fun drawChef(canvas: Canvas, game: GameState, board: BoardMetrics) {
        val image = chef ?: return
        val left = board.width * 0.5f - image.width / 2
        val top = board.height * CHEF_Y / 100f - image.height * 0.55f
        val elapsed = game.time - game.lastThrowAt
        imagePaint.alpha = 255
        canvas.withTranslation(left, top) {
            if (elapsed < 0.4) {
                val p = (elapsed / 0.4).toFloat()
                val pivotX = image.width * 0.5f
                val pivotY = image.height * 0.9f
                translate(pivotX, ThrowY.at(p) * image.height + pivotY)
                rotate(ThrowRotate.at(p) * sign(laneX(game.lastThrowLane) - 0.5f))
                scale(ThrowScaleX.at(p), ThrowScaleY.at(p))
                translate(-pivotX, -pivotY)
            }
            image.draw(this, 0f, 0f, imagePaint)
        }
    }
}
