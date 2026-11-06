package com.toeicword.app.domain

import java.text.Normalizer

// 입력과 정답 후보를 같은 형태로 맞추는 정규화. words.json의 정답 후보와 같은 규칙이어야
// 같은 판정이 나온다.

private val COMBINING_MARKS = Regex("[̀-ͯ]")
private val NOT_EN = Regex("[^a-z0-9' ]")
private val SPACES = Regex("\\s+")
private val NOT_KO = Regex("[^가-힣a-z0-9]")

// 영어: 악센트 제거, 소문자, 하이픈은 공백 취급, 영문·숫자·아포스트로피·공백 외 제거, 공백 하나로.
fun normalizeEn(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replace(COMBINING_MARKS, "")
        .lowercase()
        .replace('‘', '\'')
        .replace('’', '\'')
        .replace('-', ' ')
        .replace(NOT_EN, "")
        .replace(SPACES, " ")
        .trim()

// 한글: 띄어쓰기·기호 무시. 한글·영문·숫자만 남긴다.
fun normalizeKo(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFC)
        .lowercase()
        .replace(NOT_KO, "")
