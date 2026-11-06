package com.toeicword.app.domain

import kotlinx.serialization.Serializable

// 학습·게임에 쓸 단어 고르기. 사전과 게임이 같은 규칙을 쓴다.

// stage가 1~5면 그 난이도의 단어만(무대), 0이면 페이지 범위(자유 연습).
// 기본값(페이지 1~끝, 거르기 없음)은 사전을 열 때마다 쓰는 값이기도 하다.
@Serializable
data class WordSelection(
    val stage: Int = 0,
    val fromPage: Int = 1,
    val toPage: Int = 999,
    val starredOnly: Boolean = false,
    val hideMemorized: Boolean = false,
)

typealias MarkMap = Map<Int, WordMark>

data class PageInfo(
    val page: Int,
    val first: String,
    val last: String,
    val count: Int,
)

private val HANGUL = Regex("[가-힣]")

fun pageInfos(words: List<Word>): List<PageInfo> {
    val pages = LinkedHashMap<Int, PageInfo>()
    for (w in words) {
        val info = pages[w.page]
        pages[w.page] =
            if (info != null) info.copy(last = w.word, count = info.count + 1)
            else PageInfo(w.page, w.word, w.word, 1)
    }
    return pages.values.sortedBy { it.page }
}

// 무대(난이도) 또는 페이지 범위 안에서 별표만 / 외운 단어 빼기를 적용한다.
fun selectWords(words: List<Word>, sel: WordSelection, marks: MarkMap): List<Word> {
    val lo = minOf(sel.fromPage, sel.toPage)
    val hi = maxOf(sel.fromPage, sel.toPage)
    return words.filter { w ->
        if (if (sel.stage > 0) w.level != sel.stage else w.page < lo || w.page > hi) return@filter false
        val mark = marks[w.id]
        if (sel.starredOnly && mark?.starred != true) return@filter false
        if (sel.hideMemorized && mark?.memorized == true) return@filter false
        true
    }
}

// 게임 기록에 남길 범위 이름. 예: "★3 과자 상자", "1~3p · 별표만 · 외움 제외"
fun selectionLabel(sel: WordSelection, lastPage: Int, stageName: String = ""): String {
    val lo = minOf(sel.fromPage, sel.toPage)
    val hi = maxOf(sel.fromPage, sel.toPage)
    val range = if (lo == 1 && hi >= lastPage) "전체" else if (lo == hi) "${lo}p" else "$lo~${hi}p"
    val parts = mutableListOf(if (sel.stage > 0) "★${sel.stage} $stageName".trim() else range)
    if (sel.starredOnly) parts += "별표만"
    if (sel.hideMemorized) parts += "외움 제외"
    return parts.joinToString(" · ")
}

// 검색은 범위와 상관없이 전체에서 찾는다. 한글이 있으면 뜻에서, 없으면 영어 단어와 뜻 모두에서.
fun searchWords(words: List<Word>, query: String): List<Word> {
    val ko = normalizeKo(query)
    if (HANGUL.containsMatchIn(query)) {
        return words.filter { normalizeKo(it.meanings.joinToString(" ")).contains(ko) }
    }
    val en = normalizeEn(query)
    if (en.isEmpty()) return emptyList()
    return words.filter {
        normalizeEn(it.word).contains(en) || normalizeKo(it.meanings.joinToString(" ")).contains(ko)
    }
}
