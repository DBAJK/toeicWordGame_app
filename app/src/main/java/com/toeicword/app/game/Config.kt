package com.toeicword.app.game

// 모든 무대에 공통인 게임 수치. 무대별 수치는 Stages.kt. 시간 단위는 초.
object Config {
    const val LIVES = 5
    const val CORRECT_PER_STAGE = 10
    const val LANES = 5

    // 같은 줄에 새 단어를 내려면 앞 단어가 이만큼은 내려가 있어야 한다(0~1).
    const val LANE_CLEAR_PROGRESS = 0.3

    // 단어 처리 시간 = 읽고 떠올리는 시간 + 타이핑 시간(타수 ÷ 기준 타속).
    // 낙하 시간·출현 간격은 이 처리 시간에 무대별 여유 배수를 곱해 단어마다 정한다.
    const val THINK_SECONDS = 4.0
    const val EN_KEYS_PER_MINUTE = 250.0
    const val KO_KEYS_PER_MINUTE = 400.0

    // 단계가 오를 때마다 낙하·출현이 8%씩 빨라지되, 처리 시간의 이 배수 아래로는 내려가지 않는다.
    const val STAGE_SPEEDUP = 0.92
    const val MIN_FALL_FACTOR = 1.3
    const val MIN_SPAWN_FACTOR = 0.5

    const val FIRST_SPAWN_AT = 1.2
    const val SPAWN_RETRY_SECONDS = 0.2

    const val POINTS_PER_STAGE = 10

    // 배속. 낙하 시간과 출현 간격을 이 값으로 나눈다.
    // 기준(기본값)은 1.0이고, 0.5배까지 느리게, 1.5배까지 빠르게 할 수 있다.
    const val SPEED_BASE = 1.0
    const val SPEED_MIN = 0.5
    const val SPEED_MAX = 1.5
    const val SPEED_STEP = 0.1

    const val CUPCAKE_FLIGHT_SECONDS = 0.3
    const val HIT_VANISH_SECONDS = 0.9
    const val MISS_SHOW_SECONDS = 1.8
    const val GAME_OVER_DELAY_SECONDS = 1.4
}
