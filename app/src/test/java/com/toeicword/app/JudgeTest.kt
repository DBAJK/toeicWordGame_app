package com.toeicword.app

import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.isCorrect
import com.toeicword.app.domain.normalizeEn
import com.toeicword.app.domain.normalizeKo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 판정 규칙을 확인하는 대조 예제.
class JudgeTest {
    private val hypothesis = Word(
        id = 1,
        page = 1,
        level = 3,
        word = "hypothesis",
        meanings = listOf("가설"),
        answersEn = listOf("hypothesis"),
        answersKo = listOf("가설"),
        keysEn = 11,
        keysKo = 6,
    )

    @Test
    fun normalizeEn_normalizesInput() {
        assertEquals("hypothesis", normalizeEn("  Hypothesis "))
        assertEquals("cafe", normalizeEn("Café"))
        assertEquals("don't", normalizeEn("don’t"))
        assertEquals("well known", normalizeEn("well-known"))
        assertEquals("fill in", normalizeEn("fill   in!"))
    }

    @Test
    fun normalizeKo_normalizesInput() {
        assertEquals("가설", normalizeKo("가 설"))
        assertEquals("남용하다", normalizeKo("남용(하다)"))
        assertEquals("abc1", normalizeKo("A b-C 1"))
    }

    @Test
    fun isCorrect_matchesAnswerCandidates() {
        assertTrue(isCorrect(hypothesis, GameMode.KR_EN, "  Hypothesis "))
        assertTrue(isCorrect(hypothesis, GameMode.EN_KR, "가 설"))
        assertFalse(isCorrect(hypothesis, GameMode.KR_EN, "hypothes"))
        assertFalse(isCorrect(hypothesis, GameMode.EN_KR, "   "))
    }
}
