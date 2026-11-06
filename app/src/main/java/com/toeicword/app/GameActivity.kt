package com.toeicword.app

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.LocaleList
import android.util.TypedValue
import android.view.Choreographer
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.toeicword.app.databinding.ActivityGameBinding
import com.toeicword.app.domain.GameMode
import com.toeicword.app.game.Config
import com.toeicword.app.game.SubmitResult
import com.toeicword.app.game.isBaseSpeed
import com.toeicword.app.game.themeFor
import com.toeicword.app.ui.BaseActivity
import com.toeicword.app.ui.GameResult
import com.toeicword.app.ui.clearInput
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.dp
import com.toeicword.app.ui.game.BannerAlpha
import com.toeicword.app.ui.game.BannerScale
import com.toeicword.app.ui.game.BannerShift
import com.toeicword.app.ui.game.BoardMetrics
import com.toeicword.app.ui.game.GameSession
import com.toeicword.app.ui.hideKeyboard
import com.toeicword.app.ui.onSubmit
import com.toeicword.app.ui.shake
import com.toeicword.app.ui.showKeyboard
import com.toeicword.app.ui.speedLabel
import java.util.Locale
import kotlin.math.roundToInt

private const val GAUGE_MAX = 1000
private const val RESULT_KEY_DELAY_MS = 700L

// 게임 화면. 떨어지는 단어는 GameBoardView가 그리고, 나머지는 activity_game.xml의 뷰다.
class GameActivity : BaseActivity() {
    private lateinit var binding: ActivityGameBinding
    private var session: GameSession? = null
    private var result: GameResult? = null
    private var resumed = false
    private var looping = false
    private var lastFrameNanos = 0L
    private var acceptResultKeys = false
    private var boardScale = 1f
    private var gaugeAnimator: ObjectAnimator? = null

    // 마지막으로 화면에 반영한 값. 바뀔 때만 뷰를 고친다.
    private var shownScore = -1
    private var shownBest = -1
    private var shownStage = -1
    private var shownBannerStage = -1
    private var shownGauge = -1

    private val frameCallback = Choreographer.FrameCallback(::onFrame)

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (result != null) finish() else togglePause()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fitSafeArea(binding.root)
        onBackPressedDispatcher.addCallback(this, backCallback)

        // 프로세스가 복원되어 진행 중이던 판이 사라진 경우
        val current = store.session.value
        if (current == null) {
            finish()
            return
        }

        binding.board.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val changed = right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop
            if (changed) binding.board.post { applyBoardSize(right - left, bottom - top) }
        }
        binding.gameBoard.setOnClickListener { binding.bar.answer.showKeyboard(window) }
        binding.pauseTile.setOnClickListener { setPaused(true) }
        binding.pauseOverlay.resume.setOnClickListener { setPaused(false) }
        binding.pauseOverlay.quit.setOnClickListener { finish() }
        binding.bar.exit.setOnClickListener { finish() }
        binding.bar.answer.onSubmit(::submitAnswer)
        binding.resultOverlay.retry.setOnClickListener { store.retry() }
        binding.resultOverlay.home.setOnClickListener { finish() }

        bindSession(current)
        observe(store.session) { next -> if (next != null && next !== session) bindSession(next) }
        observe(store.result, ::showResult)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        updateLoop()
        syncKeyboard()
    }

    // 다른 앱으로 나가면 자동 일시정지. 회전으로 다시 만들어질 때는 그대로 이어 간다.
    override fun onPause() {
        super.onPause()
        resumed = false
        if (result == null && !isChangingConfigurations) session?.paused = true
        if (isFinishing) binding.root.hideKeyboard(window)
        renderPause()
        updateLoop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) syncKeyboard()
    }

    // 외부 키보드: Esc는 일시정지, 결과 패널에서는 Enter 다시 하기 / Esc 처음으로.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val enter = event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            val escape = event.keyCode == KeyEvent.KEYCODE_ESCAPE
            if (result != null) {
                if (acceptResultKeys && enter) {
                    store.retry()
                    return true
                }
                if (acceptResultKeys && escape) {
                    finish()
                    return true
                }
            } else if (escape) {
                togglePause()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun bindSession(next: GameSession) {
        session = next
        binding.gameBoard.bind(next)

        val krEn = next.mode == GameMode.KR_EN
        val answer = binding.bar.answer
        val options = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        answer.imeOptions = if (krEn) options or EditorInfo.IME_FLAG_FORCE_ASCII else options
        answer.imeHintLocales = LocaleList(Locale.forLanguageTag(if (krEn) "en" else "ko"))
        answer.setHint(if (krEn) R.string.answer_hint_en else R.string.answer_hint_ko)
        answer.clearInput()

        shownScore = -1
        shownBest = -1
        shownStage = -1
        shownBannerStage = -1
        shownGauge = -1
        renderFrame()
        renderPause()
        updateLoop()
    }

    // ---------- 게임 루프 ----------

    private fun updateLoop() {
        val current = session
        val run = resumed && current != null && !current.paused && !current.finished && result == null
        if (run == looping) return
        looping = run
        val choreographer = Choreographer.getInstance()
        if (run) {
            lastFrameNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        } else {
            choreographer.removeFrameCallback(frameCallback)
        }
    }

    private fun onFrame(nanos: Long) {
        val current = session ?: return
        if (!looping) return
        // 탭 전환 등 큰 공백은 0.05초로 자른다.
        if (lastFrameNanos != 0L) {
            val dt = ((nanos - lastFrameNanos) / 1e9).coerceIn(0.0, 0.05)
            val over = current.tick(dt)
            renderFrame()
            if (over) {
                updateLoop()
                store.gameOver(current)
                return
            }
        }
        lastFrameNanos = nanos
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun submitAnswer() {
        val current = session ?: return
        if (current.paused || result != null) return
        val answer = binding.bar.answer
        val outcome = current.answer(answer.text.toString())
        if (outcome == SubmitResult.EMPTY) return
        if (outcome == SubmitResult.WRONG) answer.shake()
        answer.clearInput()
    }

    // ---------- 화면 반영 ----------

    private fun renderFrame() {
        val current = session ?: return
        val game = current.game
        val t = game.time
        binding.gameBoard.invalidate()
        binding.backdrop.show(themeFor(game.settings.theme, game.stage), boardScale)

        if (game.score != shownScore) {
            shownScore = game.score
            binding.bar.score.text = game.score.commas()
        }
        val best = maxOf(current.bestScore, game.score)
        if (best != shownBest) {
            shownBest = best
            binding.hud.bestScore.text = best.commas()
        }
        if (game.stage != shownStage) {
            shownStage = game.stage
            // 기준 속도(1.0)가 아니면 무대 이름 옆에 배속을 함께 보여 준다(오븐 ×1.3 · 단계 1).
            val speed = current.speed
            val name = if (isBaseSpeed(speed)) current.stageName else "${current.stageName} ${speedLabel(speed)}"
            binding.bar.stagePill.stageName.text = getString(R.string.stage_pill, name)
            binding.bar.stagePill.stageNumber.text = game.stage.toString()
        }
        val toNext = (game.correct % Config.CORRECT_PER_STAGE) * GAUGE_MAX / Config.CORRECT_PER_STAGE
        if (toNext != shownGauge) showGauge(toNext)

        val breakProgress = if (t - game.lastMissAt < 0.5) ((t - game.lastMissAt) / 0.5).toFloat() else 1f
        binding.hud.hearts.show(game.lives, breakProgress)
        renderBanner(game.stage, t - game.stageUpAt)
        binding.missFlash.show(((t - game.lastMissAt) / 0.45).toFloat())
    }

    private fun showGauge(value: Int) {
        val animate = shownGauge >= 0
        shownGauge = value
        gaugeAnimator?.cancel()
        if (!animate) {
            binding.bar.gauge.progress = value
            return
        }
        gaugeAnimator = ObjectAnimator.ofInt(binding.bar.gauge, "progress", value).apply {
            duration = 250
            start()
        }
    }

    private fun renderBanner(stage: Int, age: Double) {
        val banner = binding.stageBanner
        if (age >= 1.4) {
            banner.isGone = true
            return
        }
        if (stage != shownBannerStage) {
            shownBannerStage = stage
            banner.text = getString(R.string.stage_level, stage)
        }
        banner.isVisible = true
        val p = (age / 1.4).toFloat()
        val scale = BannerScale.at(p)
        banner.scaleX = scale
        banner.scaleY = scale
        banner.alpha = BannerAlpha.at(p)
        banner.translationY = binding.board.height * 0.38f - banner.height / 2f + BannerShift.at(p) * banner.height
    }

    private fun togglePause() {
        val current = session ?: return
        setPaused(!current.paused)
    }

    private fun setPaused(paused: Boolean) {
        session?.paused = paused
        renderPause()
        updateLoop()
        syncKeyboard()
    }

    private fun renderPause() {
        binding.pauseOverlay.root.isVisible = session?.paused == true && result == null
    }

    private fun syncKeyboard() {
        if (session?.paused == false && result == null && !isFinishing) binding.bar.answer.showKeyboard(window)
    }

    private fun showResult(next: GameResult?) {
        result = next
        binding.bar.answer.isEnabled = next == null
        binding.resultOverlay.root.isVisible = next != null
        renderPause()
        updateLoop()
        if (next == null) {
            acceptResultKeys = false
            syncKeyboard()
            return
        }

        val rank = next.records.indexOfFirst { it.id == next.recordId }
        binding.resultOverlay.resultScore.text = next.score.commas()
        binding.resultOverlay.resultSummary.text = getString(R.string.result_summary, next.stage, rank + 1)
        binding.resultOverlay.resultRecords.show(next.records, next.recordId)
        binding.root.hideKeyboard(window)
        // 게임 중 치던 Enter가 바로 '다시 하기'로 넘어가지 않도록 잠깐 기다린다.
        binding.root.postDelayed({ acceptResultKeys = true }, RESULT_KEY_DELAY_MS)
    }

    // 보드가 작으면 머리글도 비율대로 줄인다. 키보드를 올린 가로 화면처럼 낮은 보드에서는 한 줄로 줄인다.
    private fun applyBoardSize(width: Int, height: Int) {
        if (width == 0 || height == 0) return
        val density = resources.displayMetrics.density
        val s = BoardMetrics(width, height, density).scale
        boardScale = s
        fun scaled(value: Float) = (value * s * density).roundToInt()

        val hud = binding.hud
        val compact = height / density < 200f
        hud.root.orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        hud.root.setPadding(scaled(12f), scaled(12f), scaled(12f), scaled(12f))
        hud.avatar.isGone = compact
        hud.bestLabel.isGone = compact
        hud.avatar.updateLayoutParams {
            this.width = scaled(64f)
            this.height = scaled(64f)
        }
        hud.avatarEmoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, 38f * s * density)
        // android:minWidth는 View와 TextView가 따로 들고 있어서 둘 다 바꿔야 줄어든다.
        hud.bestScore.minimumWidth = scaled(120f)
        hud.bestScore.minWidth = scaled(120f)
        hud.hearts.setBoardScale(s)
        hud.hearts.updateLayoutParams<LinearLayout.LayoutParams> {
            leftMargin = if (compact) dp(6f).toInt() else 0
            topMargin = if (compact) 0 else scaled(6f)
        }

        binding.pauseTile.updateLayoutParams<FrameLayout.LayoutParams> {
            this.width = scaled(54f)
            this.height = scaled(54f) + dp(3f).toInt()
            setMargins(scaled(12f), scaled(12f), scaled(12f), scaled(12f))
        }
        binding.pauseEmoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30f * s * density)
        binding.stageBanner.setTextSizePx(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (68f * s).coerceAtLeast(40f), resources.displayMetrics),
        )
        renderFrame()
    }
}
