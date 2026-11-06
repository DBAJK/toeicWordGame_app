package com.toeicword.app

import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.Word
import com.toeicword.app.game.GameState
import com.toeicword.app.game.SPEED_STEPS
import com.toeicword.app.game.STAGES
import com.toeicword.app.game.clampSpeed
import com.toeicword.app.game.isBaseSpeed
import com.toeicword.app.game.speedAt
import com.toeicword.app.game.speedStepOf
import com.toeicword.app.game.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 배속은 낙하 시간과 다음 단어까지의 간격을 정확히 그 배수만큼 바꿔야 한다. 기준은 1.0.
class SpeedTest {
    private val pool = listOf(
        Word(
            id = 1,
            page = 1,
            level = 1,
            word = "price",
            meanings = listOf("값"),
            answersEn = listOf("price"),
            answersKo = listOf("값"),
            keysEn = 6,
            keysKo = 3,
        ),
    )

    // 첫 단어가 나온 순간의 낙하 시간과, 그다음 단어가 나올 때까지의 간격
    private fun firstSpawn(speed: Double): Pair<Double, Double> {
        val state = GameState(GameMode.KR_EN, STAGES[0].settings, speed)
        while (state.items.isEmpty()) step(state, 0.01, pool) { 0.0 }
        return state.items[0].duration to state.nextSpawnAt - state.time
    }

    @Test
    fun speed_dividesFallAndSpawnTimes() {
        val (fall, gap) = firstSpawn(1.0)
        for (speed in listOf(0.5, 1.5)) {
            val (scaledFall, scaledGap) = firstSpawn(speed)
            assertEquals(fall / speed, scaledFall, 1e-9)
            assertEquals(gap / speed, scaledGap, 1e-9)
        }
    }

    @Test
    fun defaultSpeed_isBase() {
        assertEquals(1.0, GameState(GameMode.KR_EN, STAGES[0].settings).speed, 0.0)
    }

    @Test
    fun speedSteps_coverHalfToOnePointFive_withBaseInMiddle() {
        assertEquals(10, SPEED_STEPS)
        assertEquals(
            listOf(0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5),
            (0..SPEED_STEPS).map(::speedAt),
        )
        assertEquals(SPEED_STEPS / 2, speedStepOf(1.0))
        assertTrue(isBaseSpeed(1.0))
        assertFalse(isBaseSpeed(0.9))
        assertFalse(isBaseSpeed(1.1))
    }

    @Test
    fun clampSpeed_snapsToNearestStep() {
        assertEquals(0.5, clampSpeed(0.1), 0.0)
        assertEquals(1.5, clampSpeed(3.0), 0.0)
        assertEquals(0.7, clampSpeed(0.699999988079071), 0.0) // Float로 저장했다 읽은 값
        assertEquals(1.2, clampSpeed(1.2000000476837158), 0.0)
    }
}
