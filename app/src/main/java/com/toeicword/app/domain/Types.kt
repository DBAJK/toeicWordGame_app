package com.toeicword.app.domain

import kotlinx.serialization.Serializable

// words.json 한 항목. 정답 후보(answersEn/answersKo)는 미리 정규화해서 words.json에 넣어 둔다.
@Serializable
data class Word(
    val id: Int,
    val page: Int,
    val level: Int, // 1~5, 영어 정답 길이 기준
    val word: String,
    val meanings: List<String>,
    val category: String? = null,
    val memo: String? = null,
    val answersEn: List<String>,
    val answersKo: List<String>,
    val keysEn: Int, // 가장 짧은 영어 정답 + Enter의 타수
    val keysKo: Int, // 가장 짧은 한글 정답 + Enter의 타수 (두벌식, 정답 없으면 0)
)

enum class GameMode(val label: String) {
    KR_EN("KR→EN"),
    EN_KR("EN→KR"),
}

data class GameRecord(
    val id: Long = 0,
    val mode: GameMode,
    val stage: Int,
    val score: Int,
    val playedAt: Long,
    val range: String? = null,
    val speed: Double = 1.0, // 배속. 점수 계산에는 쓰지 않고 표에 표시만 한다.
)

// 사전에서 사용자가 직접 표시하는 별표·외움.
data class WordMark(
    val wordId: Int,
    val starred: Boolean,
    val memorized: Boolean,
)
