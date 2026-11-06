package com.toeicword.app.domain

// KR→EN은 영어 단어를, EN→KR은 뜻 중 하나를 입력해야 정답.
fun isCorrect(word: Word, mode: GameMode, input: String): Boolean {
    if (mode == GameMode.KR_EN) {
        val normalized = normalizeEn(input)
        return normalized.isNotEmpty() && normalized in word.answersEn
    }
    val normalized = normalizeKo(input)
    return normalized.isNotEmpty() && normalized in word.answersKo
}
