package com.toeicword.app.ui.dictionary

import androidx.lifecycle.ViewModel

enum class DictView { LIST, CARD }

// 화면을 회전해 Activity가 다시 만들어져도 유지할 사전 화면의 상태.
class DictionaryState : ViewModel() {
    var view = DictView.LIST
    var hideMeanings = false

    // 카드 학습
    var wordFirst = true
    var seed = 0 // 0이면 순서대로
    var index = 0
    var flipped = false
    var moveNext = true
}
