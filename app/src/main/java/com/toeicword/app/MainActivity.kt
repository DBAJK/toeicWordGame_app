package com.toeicword.app

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.toeicword.app.databinding.ActivityMainBinding
import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.normalizeKo
import com.toeicword.app.game.SPEED_STEPS
import com.toeicword.app.game.STAGES
import com.toeicword.app.game.speedAt
import com.toeicword.app.game.speedStepOf
import com.toeicword.app.ui.AppUiState
import com.toeicword.app.ui.BaseActivity
import com.toeicword.app.ui.clearInput
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.onSubmit
import com.toeicword.app.ui.shake
import com.toeicword.app.ui.show
import com.toeicword.app.ui.speedLabel
import java.util.Locale

private val FREE_COMMAND = Regex("^(자유|연습|자유연습|0)$")
private const val KEY_SHOW_RECORDS = "showRecords"

// 한컴타자연습 '케이크 던지기' 메인 화면처럼: 부엌 배경, 큰 제목, 무대 카드, 가운데 안내판, 아래 입력창.
// 화면 크기별 배치는 res/layout(세로), layout-w600dp(가로), layout-w860dp-h640dp(넓은 화면)에 있다.
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    private val recordsBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = showRecords(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fitSafeArea(binding.root)
        onBackPressedDispatcher.addCallback(this, recordsBack)

        binding.options.modeKrEn.setOnClickListener { store.setMode(GameMode.KR_EN) }
        binding.options.modeEnKr.setOnClickListener { store.setMode(GameMode.EN_KR) }
        binding.options.speed.max = SPEED_STEPS
        binding.options.speed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) store.setSpeed(speedAt(progress))
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
        binding.free.start.setOnClickListener { startStage(0) }
        tiles().forEach { tile -> tile.setOnClickListener { startStage(tile.stageId) } }
        binding.bar.openDictionary.setOnClickListener { openDictionary() }
        binding.bar.showRecords.setOnClickListener { showRecords(true) }
        binding.bar.input.field.onSubmit(::runCommand)
        binding.recordsOverlay.close.setOnClickListener { showRecords(false) }

        showRecords(savedInstanceState?.getBoolean(KEY_SHOW_RECORDS) == true)
        observe(store.uiState, ::render)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHOW_RECORDS, binding.recordsOverlay.root.isVisible)
    }

    private fun tiles() = listOf(binding.tile1, binding.tile2, binding.tile3, binding.tile4, binding.tile5)

    private fun render(ui: AppUiState) {
        val loading = ui.error.isNotEmpty() || ui.words == null
        binding.messageOverlay.root.isVisible = loading
        binding.messageOverlay.message.text = ui.error.ifEmpty { getString(R.string.loading_words) }

        binding.options.modeKrEn.isSelected = ui.mode == GameMode.KR_EN
        binding.options.modeEnKr.isSelected = ui.mode == GameMode.EN_KR
        binding.options.speed.progress = speedStepOf(ui.speed)
        binding.options.speedValue.text = speedLabel(ui.speed)
        ViewCompat.setStateDescription(
            binding.options.speed,
            getString(R.string.speed_description, String.format(Locale.ROOT, "%.1f", ui.speed)),
        )
        binding.options.filters.show(ui.selection, store::setSelection)
        binding.free.pageRange.show(ui.selection, ui.pages, store::setSelection)
        binding.free.start.text = getString(R.string.free_start, ui.freeCount.commas())
        binding.free.start.isEnabled = ui.freeCount > 0
        tiles().forEach { it.showCount(ui.stageCounts[it.stageId - 1]) }
        if (binding.recordsOverlay.root.isVisible) binding.recordsOverlay.records.show(ui.records)
    }

    // 입력창에 무대 이름이나 번호(1~5)를 쳐서 고른다. '자유', '사전', '점수'도 받는다.
    private fun runCommand() {
        val ui = store.uiState.value
        val input = binding.bar.input.field
        val text = normalizeKo(input.text.toString())
        if (text.isEmpty()) return
        val stage = STAGES.find { text == it.id.toString() || text == normalizeKo(it.name) }
        when {
            stage != null && ui.stageCounts[stage.id - 1] > 0 -> startStage(stage.id)
            FREE_COMMAND.matches(text) && ui.freeCount > 0 -> startStage(0)
            text == "사전" || text == "단어사전" -> openDictionary()
            text == "점수" || text == "점수보기" -> showRecords(true)
            else -> {
                binding.bar.input.root.shake()
                return
            }
        }
        input.clearInput()
    }

    private fun startStage(stage: Int) {
        store.startStage(stage)
        startActivity(Intent(this, GameActivity::class.java))
    }

    private fun openDictionary() {
        store.openDictionary()
        startActivity(Intent(this, DictionaryActivity::class.java))
    }

    private fun showRecords(visible: Boolean) {
        if (visible) binding.recordsOverlay.records.show(store.uiState.value.records)
        binding.recordsOverlay.root.isVisible = visible
        recordsBack.isEnabled = visible
    }
}
