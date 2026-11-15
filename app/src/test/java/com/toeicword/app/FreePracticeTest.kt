package com.toeicword.app

import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.Word
import com.toeicword.app.game.Config
import com.toeicword.app.game.FREE_SETTINGS
import com.toeicword.app.game.GameState
import com.toeicword.app.game.ItemStatus
import com.toeicword.app.game.STAGES
import com.toeicword.app.game.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 자유 연습은 단어를 놓쳐도 체력이 닳지 않아 저절로 끝나지 않는다. 무대는 그대로 체력이 닳는다.
class FreePracticeTest {
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

    // 아무것도 치지 않고 오래 흘려보낸다.
    private fun playIdle(state: GameState, seconds: Double) {
        var t = 0.0
        while (t < seconds) {
            step(state, 0.05, pool) { 0.0 }
            t += 0.05
        }
    }

    @Test
    fun freePractice_keepsLivesWhenWordsHitTheGround() {
        val state = GameState(GameMode.KR_EN, FREE_SETTINGS)
        var missed = 0
        var t = 0.0
        while (t < 120.0) {
            step(state, 0.05, pool) { 0.0 }
            missed += state.items.count { it.status == ItemStatus.MISSED }
            t += 0.05
        }
        assertTrue("놓친 단어가 있어야 이 테스트가 의미 있다", missed > 0)
        assertEquals(Config.LIVES, state.lives)
        assertNull(state.overAt)
        assertFalse(state.lastMissAt > Double.NEGATIVE_INFINITY)
    }

    @Test
    fun stage_stillLosesLivesAndEnds() {
        val state = GameState(GameMode.KR_EN, STAGES[0].settings)
        playIdle(state, 120.0)
        assertEquals(0, state.lives)
        assertNotNull(state.overAt)
    }
}
