package com.toeicword.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toeicword.app.databinding.ActivityDictionaryBinding
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.searchWords
import com.toeicword.app.game.STAGES
import com.toeicword.app.ui.AppUiState
import com.toeicword.app.ui.BaseActivity
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.dictionary.CardStudyController
import com.toeicword.app.ui.dictionary.DictView
import com.toeicword.app.ui.dictionary.DictionaryState
import com.toeicword.app.ui.dictionary.WordListAdapter
import com.toeicword.app.ui.dictionary.WordRow
import com.toeicword.app.ui.dp
import com.toeicword.app.ui.hideKeyboard
import com.toeicword.app.ui.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 단어 사전. 목록과 카드 두 가지로 보며, 별표·외움을 표시한다.
class DictionaryActivity : BaseActivity() {
    private lateinit var binding: ActivityDictionaryBinding
    private lateinit var card: CardStudyController
    private val state: DictionaryState by viewModels()
    private val adapter = WordListAdapter { id, key -> store.toggleMark(id, key) }

    private var list: List<Word> = emptyList()
    private var searchJob: Job? = null
    private var searchedQuery = ""
    private var searchedWords: List<Word> = emptyList()

    private val ui get() = store.uiState.value
    private val query get() = binding.search.root.text.toString()
    private val searching get() = query.isNotBlank()
    private val twoPane get() = binding.panel.orientation == LinearLayout.HORIZONTAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDictionaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fitSafeArea(binding.root)

        val content = binding.dictContent
        card = CardStudyController(content.cardPane, state) { id, key -> store.toggleMark(id, key) }
        content.list.layoutManager = LinearLayoutManager(this)
        content.list.itemAnimator = null
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        content.list.adapter = adapter

        binding.header.home.setOnClickListener { finish() }
        binding.header.startGame.setOnClickListener {
            store.startFromDictionary()
            startActivity(Intent(this, GameActivity::class.java))
            // 게임을 마치면 사전이 아니라 메인 화면으로 돌아간다.
            finish()
        }
        binding.toolbar.viewList.setOnClickListener { setView(DictView.LIST) }
        binding.toolbar.viewCard.setOnClickListener { setView(DictView.CARD) }
        binding.toolbar.hideMeanings.setOnClickListener {
            state.hideMeanings = !state.hideMeanings
            adapter.clearRevealed()
            renderContent()
        }
        binding.search.root.doAfterTextChanged { refreshList() }
        binding.root.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) {
                val height = ((bottom - top) * 0.42f).coerceIn(dp(180f), dp(300f)).toInt()
                binding.root.post { card.setCardHeight(height) }
            }
        }

        observe(store.uiState, ::render)
    }

    // 외부 키보드: Space·Enter 뒤집기, ←/→ 넘기기.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val cardActive = state.view == DictView.CARD && list.isNotEmpty() && !binding.search.root.hasFocus()
        if (cardActive && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER -> card.flip()
                KeyEvent.KEYCODE_DPAD_RIGHT -> card.move(1)
                KeyEvent.KEYCODE_DPAD_LEFT -> card.move(-1)
                else -> return super.dispatchKeyEvent(event)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun render(ui: AppUiState) {
        val words = ui.words.orEmpty()
        binding.header.startGame.text = getString(R.string.dictionary_game, ui.mode.label)
        binding.header.startGame.isEnabled = ui.dictPoolCount > 0

        val controls = binding.controls
        val selection = ui.dictSelection
        controls.stage.bind(selection.stage, listOf(0) + STAGES.map { it.id }, ::stageLabel) {
            store.setDictSelection(selection.copy(stage = it))
        }
        controls.pageRange.root.isVisible = selection.stage == 0
        controls.pageRange.show(selection, ui.pages, store::setDictSelection)
        controls.filters.show(selection, store::setDictSelection)
        controls.count.text = getString(R.string.count_items, ui.dictSelected.size.commas())

        if (words.isNotEmpty()) refreshList()
    }

    private fun stageLabel(id: Int): String =
        STAGES.find { it.id == id }?.let { "★".repeat(it.stars) + " " + it.name } ?: getString(R.string.page_range_label)

    // 단어마다 정규화하므로 입력 중에 화면이 끊기지 않게 백그라운드에서 찾는다.
    private fun refreshList() {
        val query = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchedQuery = ""
            list = ui.dictSelected
            renderContent()
            return
        }
        val words = ui.words.orEmpty()
        // 별표만 바뀌었으면 다시 찾지 않는다.
        if (query == searchedQuery && words === searchedWords) {
            renderContent()
            return
        }
        searchJob = lifecycleScope.launch {
            val found = withContext(Dispatchers.Default) { searchWords(words, query) }
            searchedQuery = query
            searchedWords = words
            list = found
            renderContent()
        }
    }

    private fun setView(view: DictView) {
        state.view = view
        if (view == DictView.CARD) {
            binding.search.root.clearFocus()
            binding.root.hideKeyboard(window)
        }
        renderContent()
    }

    private fun renderContent() {
        val ui = ui
        val searching = searching
        val hide = state.hideMeanings
        val listView = state.view == DictView.LIST
        val empty = list.isEmpty()

        // 검색은 범위와 상관없으므로 검색 중에는 범위 조작을 접어 목록 자리를 넓힌다.
        binding.controls.root.isGone = searching && !twoPane

        val toolbar = binding.toolbar
        toolbar.viewList.isSelected = listView
        toolbar.viewCard.isSelected = !listView
        toolbar.hideMeanings.isVisible = listView
        toolbar.hideMeanings.isSelected = hide
        val starred = list.count { ui.marks[it.id]?.starred == true }
        val memorized = list.count { ui.marks[it.id]?.memorized == true }
        toolbar.summary.text = if (searching) {
            getString(R.string.search_summary, query.trim(), list.size, starred, memorized)
        } else {
            getString(R.string.count_summary, list.size.commas(), starred, memorized)
        }

        val content = binding.dictContent
        content.empty.isVisible = empty
        content.listPane.isVisible = listView && !empty
        content.cardPane.root.isVisible = !listView && !empty
        content.listHeader.meaningHeader.setText(if (hide) R.string.column_meaning_hidden else R.string.column_meaning)
        if (listView) {
            adapter.submitList(
                list.mapIndexed { index, word -> WordRow(index, word, ui.marks[word.id], hide, searching) },
            )
        } else {
            card.update(list, ui.marks)
        }
    }
}
