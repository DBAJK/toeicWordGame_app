package com.toeicword.app.game

import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.isCorrect
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

// 게임 진행 로직. 화면과 분리되어 있다.
// 배속(speed)은 낙하 시간과 출현 간격을 나누는 값이고, 1.0이 기준 속도다.
// 상태 객체를 직접 수정한다(매 프레임 새 객체를 만들지 않기 위해).

enum class ItemStatus { FALLING, HIT, MISSED }

class FallingWord(
    val key: Int,
    val word: Word,
    val prompt: String,
    val lane: Int,
    var progress: Double, // 0 = 맨 위, 1 = 바닥
    val duration: Double,
    var status: ItemStatus,
    var statusAt: Double,
    var points: Int,
    val kind: EnemyKind,
)

// 날아가는 동안에도 목표가 움직이므로 위치 대신 목표 단어를 가리킨다.
class Cupcake(
    val key: Int,
    val targetKey: Int,
    val startAt: Double,
)

class GameState(
    val mode: GameMode,
    val settings: GameSettings,
    val speed: Double = Config.SPEED_BASE,
) {
    var time = 0.0
    var stage = 1
    var score = 0
    var lives = Config.LIVES
    var correct = 0
    var items: List<FallingWord> = emptyList()
    var cupcakes: List<Cupcake> = emptyList()
    var nextSpawnAt = Config.FIRST_SPAWN_AT / speed
    var nextKey = 1
    var lastThrowAt = Double.NEGATIVE_INFINITY
    var lastThrowLane = Config.LANES / 2
    var lastMissAt = Double.NEGATIVE_INFINITY
    var stageUpAt = 0.0
    var overAt: Double? = null
}

enum class SubmitResult { HIT, WRONG, EMPTY }

// KR→EN은 첫 번째 뜻만 단어판에 올린다.
fun promptOf(word: Word, mode: GameMode): String =
    if (mode == GameMode.KR_EN) word.meanings[0] else word.word

fun answerOf(word: Word, mode: GameMode): String =
    if (mode == GameMode.KR_EN) word.word else word.meanings.take(2).joinToString(", ")

fun playableWords(words: List<Word>, mode: GameMode): List<Word> =
    words.filter {
        it.meanings.isNotEmpty() &&
            (if (mode == GameMode.KR_EN) it.answersEn else it.answersKo).isNotEmpty()
    }

// 단어 하나를 읽고 떠올려 치는 데 드는 시간. 긴 단어일수록 시간을 더 준다.
fun handlingSeconds(word: Word, mode: GameMode): Double {
    val typing =
        if (mode == GameMode.KR_EN) word.keysEn / Config.EN_KEYS_PER_MINUTE * 60
        else word.keysKo / Config.KO_KEYS_PER_MINUTE * 60
    return Config.THINK_SECONDS + typing
}

private fun speedup(stage: Int) = Config.STAGE_SPEEDUP.pow(stage - 1)

fun fallDuration(s: GameSettings, stage: Int, handling: Double): Double =
    max(handling * Config.MIN_FALL_FACTOR, handling * s.fallSlack * speedup(stage))

fun spawnInterval(s: GameSettings, stage: Int, handling: Double): Double =
    max(handling * Config.MIN_SPAWN_FACTOR, handling * s.spawnSlack * speedup(stage))

fun pointsFor(s: GameSettings, stage: Int): Int = s.basePoints + (stage - 1) * Config.POINTS_PER_STAGE

// 배속 조절 칸 수(0.5~1.5를 0.1씩이면 10칸)와 칸 번호 ↔ 배속 변환. 저장값이 어긋나도 가장 가까운 칸으로 맞춘다.
val SPEED_STEPS = ((Config.SPEED_MAX - Config.SPEED_MIN) / Config.SPEED_STEP).roundToInt()

fun speedStepOf(speed: Double): Int = ((speed - Config.SPEED_MIN) / Config.SPEED_STEP).roundToInt().coerceIn(0, SPEED_STEPS)

fun speedAt(step: Int): Double = (Config.SPEED_MIN + step.coerceIn(0, SPEED_STEPS) * Config.SPEED_STEP).let {
    (it * 10).roundToInt() / 10.0
}

fun clampSpeed(speed: Double): Double = speedAt(speedStepOf(speed))

// 기준 속도(1.0)가 아니면 화면과 점수표에 배속을 붙인다.
fun isBaseSpeed(speed: Double): Boolean = speedStepOf(speed) == speedStepOf(Config.SPEED_BASE)

fun step(state: GameState, dt: Double, pool: List<Word>, random: () -> Double = Random.Default::nextDouble) {
    state.time += dt
    if (state.overAt == null) {
        for (item in state.items) {
            // 맞힌 단어도 컵케이크가 닿을 때까지는 계속 떨어진다(공중에서 멈추지 않게).
            if (item.status == ItemStatus.HIT && state.time - item.statusAt < Config.CUPCAKE_FLIGHT_SECONDS) {
                item.progress = minOf(1.0, item.progress + dt / item.duration)
                continue
            }
            if (item.status != ItemStatus.FALLING) continue
            item.progress += dt / item.duration
            if (item.progress < 1) continue
            item.progress = 1.0
            item.status = ItemStatus.MISSED
            item.statusAt = state.time
            if (!state.settings.loseLives) continue
            state.lives -= 1
            state.lastMissAt = state.time
            if (state.lives <= 0) {
                state.lives = 0
                state.overAt = state.time
            }
        }
        if (state.overAt == null && state.time >= state.nextSpawnAt) spawn(state, pool, random)
    }
    removeFinished(state)
}

fun submit(state: GameState, input: String): SubmitResult {
    if (state.overAt != null || input.isBlank()) return SubmitResult.EMPTY

    // 같은 단어가 여럿이면 바닥에 가장 가까운 것을 맞힌다.
    var target: FallingWord? = null
    for (item in state.items) {
        if (item.status != ItemStatus.FALLING || !isCorrect(item.word, state.mode, input)) continue
        if (target == null || item.progress > target.progress) target = item
    }
    if (target == null) return SubmitResult.WRONG

    val special = state.settings.specials.find { it.kind == target.kind }
    val points = pointsFor(state.settings, state.stage) * (special?.points ?: 1)
    target.status = ItemStatus.HIT
    target.statusAt = state.time
    target.points = points
    state.cupcakes += Cupcake(state.nextKey++, target.key, state.time)
    state.score += points
    state.correct += 1
    state.lastThrowAt = state.time
    state.lastThrowLane = target.lane
    if (state.correct % Config.CORRECT_PER_STAGE == 0) {
        state.stage += 1
        state.stageUpAt = state.time
    }
    return SubmitResult.HIT
}

private fun spawn(state: GameState, pool: List<Word>, random: () -> Double) {
    state.nextSpawnAt = state.time + Config.SPAWN_RETRY_SECONDS
    val falling = state.items.count { it.status == ItemStatus.FALLING }
    if (falling >= state.settings.maxOnScreen) return

    val busy = state.items.filter { it.progress < Config.LANE_CLEAR_PROGRESS }.map { it.lane }.toSet()
    val lanes = (0 until Config.LANES).filter { it !in busy }
    if (lanes.isEmpty()) return

    val word = pickWord(state, pool, random) ?: return

    val handling = handlingSeconds(word, state.mode)
    // 무대에 따라 일정 확률로 빠르고 점수가 큰 강한 적이 나온다.
    var roll = random()
    val special = state.settings.specials.find {
        roll -= it.chance
        roll < 0
    }

    state.items += FallingWord(
        key = state.nextKey++,
        word = word,
        prompt = promptOf(word, state.mode),
        lane = lanes[(random() * lanes.size).toInt()],
        progress = 0.0,
        // 배속은 하한(MIN_FALL_FACTOR)을 적용한 뒤에 나눠서 어느 단계에서나 정확히 그 배속이 되게 한다.
        duration = max(
            handling * Config.MIN_FALL_FACTOR,
            fallDuration(state.settings, state.stage, handling) / (special?.speed ?: 1.0),
        ) / state.speed,
        status = ItemStatus.FALLING,
        statusAt = state.time,
        points = 0,
        kind = special?.kind ?: EnemyKind.NORMAL,
    )
    // 다음 단어는 방금 나온 단어를 처리할 시간만큼 띄운다.
    state.nextSpawnAt = state.time + spawnInterval(state.settings, state.stage, handling) / state.speed
}

// 화면에 이미 있는 단어나 같은 뜻으로 보이는 단어는 피한다.
private fun pickWord(state: GameState, pool: List<Word>, random: () -> Double): Word? {
    val ids = state.items.map { it.word.id }.toSet()
    val prompts = state.items.map { it.prompt }.toSet()
    repeat(20) {
        val word = pool.getOrNull((random() * pool.size).toInt())
        if (word != null && word.id !in ids && promptOf(word, state.mode) !in prompts) return word
    }
    return null
}

private fun removeFinished(state: GameState) {
    val t = state.time
    state.items = state.items.filter {
        when (it.status) {
            ItemStatus.HIT -> t - it.statusAt < Config.CUPCAKE_FLIGHT_SECONDS + Config.HIT_VANISH_SECONDS
            ItemStatus.MISSED -> t - it.statusAt < Config.MISS_SHOW_SECONDS
            ItemStatus.FALLING -> true
        }
    }
    state.cupcakes = state.cupcakes.filter { t - it.startAt < Config.CUPCAKE_FLIGHT_SECONDS }
}
