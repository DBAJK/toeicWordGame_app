package com.toeicword.app.ui

import com.toeicword.app.R
import com.toeicword.app.ToeicApplication
import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.GameRecord
import com.toeicword.app.domain.MarkMap
import com.toeicword.app.domain.PageInfo
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.WordMark
import com.toeicword.app.domain.WordSelection
import com.toeicword.app.domain.pageInfos
import com.toeicword.app.domain.selectWords
import com.toeicword.app.domain.selectionLabel
import com.toeicword.app.game.Config
import com.toeicword.app.game.FREE_SETTINGS
import com.toeicword.app.game.STAGES
import com.toeicword.app.game.clampSpeed
import com.toeicword.app.game.playableWords
import com.toeicword.app.game.stageById
import com.toeicword.app.ui.game.GameSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MarkKey { STARRED, MEMORIZED }

data class AppUiState(
    val words: List<Word>? = null,
    val error: String = "",
    val records: List<GameRecord> = emptyList(),
    val marks: MarkMap = emptyMap(),
    val mode: GameMode = GameMode.KR_EN,
    val speed: Double = Config.SPEED_BASE,
    val pages: List<PageInfo> = emptyList(),
    val selection: WordSelection = WordSelection(),
    val dictSelection: WordSelection = WordSelection(),
    val dictSelected: List<Word> = emptyList(),
    val dictPoolCount: Int = 0,
    val stageCounts: List<Int> = List(STAGES.size) { 0 },
    val freeCount: Int = 0,
)

data class GameResult(
    val recordId: Long,
    val score: Int,
    val stage: Int,
    val records: List<GameRecord>,
)

// 앱 전체의 상태. 메인·게임·사전 Activity가 같은 상태를 보도록 앱 전체에서 하나만 둔다.
// 메인 화면 선택(기억됨), 사전 선택(열 때마다 1p~끝), 지금 게임에 쓰는 선택은 서로 따로 움직인다.
class AppStore(private val app: ToeicApplication) {
    private data class Source(
        val words: List<Word>? = null,
        val error: String = "",
        val records: List<GameRecord> = emptyList(),
        val marks: MarkMap = emptyMap(),
        val selection: WordSelection = WordSelection(),
        val dictSelection: WordSelection = WordSelection(),
        val playSelection: WordSelection = WordSelection(),
        val mode: GameMode = GameMode.KR_EN,
        val speed: Double = Config.SPEED_BASE,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val source = MutableStateFlow(
        app.selectionPrefs.load().let {
            Source(selection = it, playSelection = it, speed = app.selectionPrefs.loadSpeed())
        },
    )

    val uiState: StateFlow<AppUiState> = source
        .map(::derive)
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, AppUiState())

    private val currentSession = MutableStateFlow<GameSession?>(null)
    val session: StateFlow<GameSession?> = currentSession.asStateFlow()

    private val currentResult = MutableStateFlow<GameResult?>(null)
    val result: StateFlow<GameResult?> = currentResult.asStateFlow()

    init {
        scope.launch {
            try {
                val words = app.words.loadWords()
                source.update { it.copy(words = words) }
            } catch (e: Exception) {
                source.update { it.copy(error = e.message ?: app.getString(R.string.load_failed)) }
            }
        }
        scope.launch {
            val records = app.records.rankedRecords()
            source.update { it.copy(records = records) }
        }
        scope.launch {
            val marks = app.marks.loadMarks()
            source.update { it.copy(marks = marks) }
        }
    }

    private fun lastPageOf(pages: List<PageInfo>) = pages.lastOrNull()?.page ?: 1

    // 범위가 실제 페이지 수를 넘으면 마지막 페이지로 맞춘다(기본값 999 → 31).
    private fun WordSelection.clamped(lastPage: Int) =
        copy(fromPage = minOf(fromPage, lastPage), toPage = minOf(toPage, lastPage))

    private fun derive(s: Source): AppUiState {
        val words = s.words
            ?: return AppUiState(error = s.error, records = s.records, marks = s.marks, mode = s.mode, speed = s.speed)
        val pages = pageInfos(words)
        val lastPage = lastPageOf(pages)
        val selection = s.selection.clamped(lastPage)
        val dictSelection = s.dictSelection.clamped(lastPage)
        val dictSelected = selectWords(words, dictSelection, s.marks)
        // 메인 화면 카드에 보여 줄 무대별 / 자유 연습 단어 수 (현재 모드·거르기 기준).
        fun countFor(stage: Int) =
            playableWords(selectWords(words, selection.copy(stage = stage), s.marks), s.mode).size
        return AppUiState(
            words = words,
            error = s.error,
            records = s.records,
            marks = s.marks,
            mode = s.mode,
            speed = s.speed,
            pages = pages,
            selection = selection,
            dictSelection = dictSelection,
            dictSelected = dictSelected,
            dictPoolCount = playableWords(dictSelected, s.mode).size,
            stageCounts = STAGES.map { countFor(it.id) },
            freeCount = countFor(0),
        )
    }

    fun setMode(mode: GameMode) = source.update { it.copy(mode = mode) }

    // 무대·자유 연습·사전에서 시작하는 게임 모두 이 배속을 쓴다.
    fun setSpeed(speed: Double) {
        val next = clampSpeed(speed)
        if (next == source.value.speed) return
        source.update { it.copy(speed = next) }
        app.selectionPrefs.saveSpeed(next)
    }

    fun setSelection(selection: WordSelection) {
        source.update { it.copy(selection = selection) }
        app.selectionPrefs.save(selection)
    }

    fun setDictSelection(selection: WordSelection) = source.update { it.copy(dictSelection = selection) }

    // 사전은 열 때마다 페이지 1p~끝, 거르기 없음으로 시작한다.
    fun openDictionary() = source.update { it.copy(dictSelection = WordSelection()) }

    fun toggleMark(wordId: Int, key: MarkKey) {
        var next: WordMark? = null
        source.update { s ->
            val current = s.marks[wordId] ?: WordMark(wordId, starred = false, memorized = false)
            val changed = when (key) {
                MarkKey.STARRED -> current.copy(starred = !current.starred)
                MarkKey.MEMORIZED -> current.copy(memorized = !current.memorized)
            }
            next = changed
            s.copy(marks = s.marks + (wordId to changed))
        }
        next?.let { scope.launch { app.marks.saveMark(it) } }
    }

    // 메인 화면에서 무대(0이면 자유 연습)를 골라 시작. 메인 선택도 함께 기억한다.
    fun startStage(stage: Int) {
        val next = source.value.selection.copy(stage = stage)
        setSelection(next)
        startGame(next)
    }

    // 사전에서 고른 범위로 시작. 메인 화면 선택은 그대로 둔다.
    fun startFromDictionary() = startGame(source.value.dictSelection)

    fun retry() = startGame(source.value.playSelection)

    private fun startGame(selection: WordSelection) {
        source.update { it.copy(playSelection = selection) }
        val s = source.value
        val words = s.words ?: return
        val play = selection.clamped(lastPageOf(pageInfos(words)))
        val stage = stageById(play.stage)
        currentSession.value = GameSession(
            mode = s.mode,
            settings = stage?.settings ?: FREE_SETTINGS,
            stageName = stage?.name ?: app.getString(R.string.free_stage_name),
            pool = playableWords(selectWords(words, play, s.marks), s.mode),
            bestScore = s.records.filter { it.mode == s.mode }.maxOfOrNull { it.score } ?: 0,
            speed = s.speed,
        )
        currentResult.value = null
    }

    fun gameOver(finished: GameSession) {
        val game = finished.game
        val s = source.value
        val words = s.words ?: return
        val lastPage = lastPageOf(pageInfos(words))
        val play = s.playSelection.clamped(lastPage)
        scope.launch {
            val recordId = app.records.saveRecord(
                GameRecord(
                    mode = finished.mode,
                    stage = game.stage,
                    score = game.score,
                    playedAt = System.currentTimeMillis(),
                    range = selectionLabel(play, lastPage, stageById(play.stage)?.name ?: ""),
                    speed = finished.speed,
                ),
            )
            val records = app.records.rankedRecords()
            source.update { it.copy(records = records) }
            if (currentSession.value === finished) {
                currentResult.value = GameResult(recordId, game.score, game.stage, records)
            }
        }
    }
}
