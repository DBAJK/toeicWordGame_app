package com.toeicword.app.ui

import com.toeicword.app.databinding.IncludeFilterChipsBinding
import com.toeicword.app.databinding.IncludePageRangeBinding
import com.toeicword.app.domain.PageInfo
import com.toeicword.app.domain.WordSelection

// 메인 화면의 자유 연습과 사전이 함께 쓰는 범위 조작.

fun IncludePageRangeBinding.show(selection: WordSelection, pages: List<PageInfo>, onChange: (WordSelection) -> Unit) {
    val numbers = pages.map { it.page }
    val byPage = pages.associateBy { it.page }
    fromPage.bind(selection.fromPage, numbers, { "${it}p · ${byPage[it]?.first.orEmpty()}" }) {
        onChange(selection.copy(fromPage = it, toPage = maxOf(it, selection.toPage)))
    }
    toPage.bind(selection.toPage, numbers, { "${it}p · ${byPage[it]?.last.orEmpty()}" }) {
        onChange(selection.copy(toPage = it, fromPage = minOf(it, selection.fromPage)))
    }
}

fun IncludeFilterChipsBinding.show(selection: WordSelection, onChange: (WordSelection) -> Unit) {
    starredOnly.isSelected = selection.starredOnly
    starredOnly.setOnClickListener { onChange(selection.copy(starredOnly = !selection.starredOnly)) }
    hideMemorized.isSelected = selection.hideMemorized
    hideMemorized.setOnClickListener { onChange(selection.copy(hideMemorized = !selection.hideMemorized)) }
}
