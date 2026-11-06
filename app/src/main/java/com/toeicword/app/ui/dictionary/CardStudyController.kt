package com.toeicword.app.ui.dictionary

import android.animation.ValueAnimator
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.toeicword.app.R
import com.toeicword.app.databinding.IncludeCardStudyBinding
import com.toeicword.app.domain.MarkMap
import com.toeicword.app.domain.Word
import com.toeicword.app.ui.MarkKey
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.dp
import kotlin.random.Random

private val EnterEasing = PathInterpolator(0.3f, 1.4f, 0.6f, 1f)

// 뒤집을 때 살짝 넘쳤다가 돌아오는 만화풍 탄성
private val FlipEasing = PathInterpolator(0.3f, 1.35f, 0.5f, 1f)

// 같은 seed면 항상 같은 순서. 단어가 빠져도 나머지 순서는 그대로라 보던 카드가 튀지 않는다.
private fun shuffleKey(id: Int, seed: Int): Long {
    var x = id xor (seed * 0x9e3779b1.toInt())
    x = (x xor (x ushr 16)) * 0x45d9f3b
    x = (x xor (x ushr 16)) * 0x45d9f3b
    return (x xor (x ushr 16)).toLong() and 0xffffffffL
}

// 카드 학습 화면(include_card_study.xml)의 동작.
internal class CardStudyController(
    private val binding: IncludeCardStudyBinding,
    private val state: DictionaryState,
    private val onToggleMark: (Int, MarkKey) -> Unit,
) {
    private val context = binding.root.context
    private var list: List<Word> = emptyList()
    private var marks: MarkMap = emptyMap()
    private var order: List<Word> = emptyList()
    private var shownKey: Pair<Int, Boolean>? = null
    private var angle = 0f
    private var flipAnimator: ValueAnimator? = null
    private var enterAnimator: ValueAnimator? = null

    init {
        val density = context.resources.displayMetrics
        binding.card.cameraDistance = 16f * density.density * density.densityDpi
        binding.card.setOnClickListener { flip() }
        binding.previous.setOnClickListener { move(-1) }
        binding.next.setOnClickListener { move(1) }
        binding.wordFirst.setOnClickListener { setWordFirst(true) }
        binding.meaningFirst.setOnClickListener { setWordFirst(false) }
        binding.inOrder.setOnClickListener { setSeed(0) }
        binding.shuffle.setOnClickListener { setSeed(Random.nextInt(1, 1_000_000_001)) }
    }

    fun setCardHeight(height: Int) {
        if (binding.card.layoutParams.height != height) binding.card.updateLayoutParams { this.height = height }
    }

    fun update(nextList: List<Word>, nextMarks: MarkMap) {
        if (nextList !== list) {
            list = nextList
            reorder()
        }
        marks = nextMarks
        render()
    }

    fun flip() {
        state.flipped = !state.flipped
        render()
    }

    fun move(delta: Int) {
        if (order.isEmpty()) return
        val current = state.index.coerceAtMost(order.size - 1)
        state.moveNext = delta > 0
        state.index = (current + delta + order.size) % order.size
        state.flipped = false
        render()
    }

    private fun setWordFirst(wordFirst: Boolean) {
        state.wordFirst = wordFirst
        state.flipped = false
        render()
    }

    private fun setSeed(seed: Int) {
        state.seed = seed
        state.index = 0
        state.flipped = false
        reorder()
        render()
    }

    private fun reorder() {
        val seed = state.seed
        order = if (seed == 0) list else list.sortedBy { shuffleKey(it.id, seed) }
    }

    private fun render() {
        val i = state.index.coerceAtMost((order.size - 1).coerceAtLeast(0))
        val word = order.getOrNull(i) ?: return
        val mark = marks[word.id]

        val key = word.id to state.wordFirst
        val newCard = key != shownKey
        if (newCard) {
            val firstCard = shownKey == null
            shownKey = key
            showFaces(word)
            flipAnimator?.cancel()
            setAngle(if (state.flipped) 180f else 0f)
            if (!firstCard) animateEnter()
        } else {
            animateFlip(if (state.flipped) 180f else 0f)
        }

        val markText = (if (mark?.starred == true) "★" else "") + (if (mark?.memorized == true) "✓" else "")
        binding.frontMarks.text = markText
        binding.position.text = context.getString(R.string.card_position, i + 1, order.size)
        binding.star.showMark(mark?.starred == true, animate = !newCard)
        binding.memorized.showMark(mark?.memorized == true, animate = !newCard)
        binding.star.setOnClickListener { onToggleMark(word.id, MarkKey.STARRED) }
        binding.memorized.setOnClickListener { onToggleMark(word.id, MarkKey.MEMORIZED) }
        binding.wordFirst.isSelected = state.wordFirst
        binding.meaningFirst.isSelected = !state.wordFirst
        binding.inOrder.isSelected = state.seed == 0
        binding.shuffle.isSelected = state.seed != 0
        binding.shuffle.setText(if (state.seed != 0) R.string.card_reshuffle else R.string.card_shuffle)
    }

    private fun showFaces(word: Word) {
        val meanings = word.meanings.joinToString(", ")
        val question = if (state.wordFirst) word.word else meanings
        val answer = if (state.wordFirst) meanings else word.word
        val note = noteOf(word)

        binding.frontMeta.text = context.getString(R.string.card_meta, word.page, word.id, "★".repeat(word.level))
        binding.frontMain.showMain(question)
        binding.backMeta.text = question
        binding.backMain.showMain(answer)
        binding.backNote.text = note
        binding.backNote.isGone = note.isEmpty()
    }

    private fun TextView.showMain(value: String) {
        val long = value.length > 18
        text = value
        textSize = if (long) 21f else 32f
    }

    private fun setAngle(value: Float) {
        angle = value
        binding.card.rotationY = value
        // 절반을 넘으면 뒷면으로 바꾼다. 뒷면은 XML에서 180도 돌려 두어 글자가 뒤집히지 않는다.
        val showBack = value % 360f > 90f
        binding.back.isVisible = showBack
        binding.front.isVisible = !showBack
    }

    private fun animateFlip(target: Float) {
        if (flipAnimator?.isRunning != true && angle == target) return
        flipAnimator?.cancel()
        flipAnimator = ValueAnimator.ofFloat(angle, target).apply {
            duration = 550
            interpolator = FlipEasing
            addUpdateListener { setAngle(it.animatedValue as Float) }
            start()
        }
    }

    private fun animateEnter() {
        val card = binding.card
        val direction = if (state.moveNext) 1f else -1f
        val slide = context.dp(70f)
        enterAnimator?.cancel()
        enterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = EnterEasing
            addUpdateListener {
                val value = it.animatedValue as Float
                val rest = 1f - value
                card.translationX = slide * rest * direction
                card.rotation = 5f * rest * direction
                card.scaleX = 0.9f + 0.1f * value
                card.scaleY = card.scaleX
                card.alpha = value.coerceIn(0f, 1f)
            }
            start()
        }
    }
}
