package com.toeicword.app.ui.game

import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.Word
import com.toeicword.app.game.Config
import com.toeicword.app.game.GameSettings
import com.toeicword.app.game.GameState
import com.toeicword.app.game.SubmitResult
import com.toeicword.app.game.step
import com.toeicword.app.game.submit

// 한 판의 상태. AppStore가 들고 있어서 화면을 회전해 Activity가 다시 만들어져도 판이 이어진다.
class GameSession(
    val mode: GameMode,
    settings: GameSettings,
    val stageName: String,
    val pool: List<Word>,
    val bestScore: Int,
    val speed: Double,
) {
    val game = GameState(mode, settings, speed)
    var paused = false
    var finished = false
        private set

    // 게임 오버 연출이 끝난 첫 프레임에 true를 돌려준다.
    fun tick(dt: Double): Boolean {
        step(game, dt, pool)
        val over = game.overAt
        if (over == null || game.time - over < Config.GAME_OVER_DELAY_SECONDS || finished) return false
        finished = true
        return true
    }

    fun answer(input: String): SubmitResult = submit(game, input)
}
