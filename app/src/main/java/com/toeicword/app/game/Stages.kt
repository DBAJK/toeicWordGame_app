package com.toeicword.app.game

// 게임 무대(난이도). 별이 많을수록 길고 어려운 단어가 나오고 강한 적이 섞인다.
// 무대 N은 난이도 N 단어(영어 정답 길이 기준)만 낸다.

enum class EnemyKind { NORMAL, ROBOT, BOSS }

data class SpecialEnemy(
    val kind: EnemyKind,
    val chance: Double, // 새 적이 이 종류일 확률
    val speed: Double, // 낙하 속도 배수
    val points: Int, // 점수 배수
)

// 여유 배수: 단어 처리 시간(Config.kt)에 곱한다. 클수록 느긋하다.
data class GameSettings(
    val fallSlack: Double, // 낙하 시간 = 처리 시간 × fallSlack
    val spawnSlack: Double, // 다음 단어까지 = 처리 시간 × spawnSlack
    val maxOnScreen: Int,
    val basePoints: Int,
    val specials: List<SpecialEnemy>,
    val theme: Int?, // 고정 배경(THEMES 번호). null이면 단계마다 바뀜
)

data class StageDef(
    val id: Int,
    val name: String,
    val stars: Int,
    val description: String,
    val settings: GameSettings,
)

private fun robot(chance: Double) = SpecialEnemy(EnemyKind.ROBOT, chance, speed = 1.3, points = 2)

val STAGES = listOf(
    StageDef(
        id = 1,
        name = "오븐",
        stars = 1,
        description = "5자 이하 짧은 단어. 적이 띄엄띄엄 와요.",
        settings = GameSettings(4.0, 1.3, 4, 30, emptyList(), 0),
    ),
    StageDef(
        id = 2,
        name = "개수대",
        stars = 2,
        description = "6~7자 단어. 빠른 로봇이 가끔 나타나요.",
        settings = GameSettings(3.6, 1.15, 5, 40, listOf(robot(0.1)), 1),
    ),
    StageDef(
        id = 3,
        name = "과자 상자",
        stars = 3,
        description = "8~9자 단어. 공격이 강해져요.",
        settings = GameSettings(3.3, 1.0, 6, 50, listOf(robot(0.15)), 2),
    ),
    StageDef(
        id = 4,
        name = "냉장고",
        stars = 4,
        description = "10~11자 긴 단어. 로봇이 자주 나와요.",
        settings = GameSettings(3.0, 0.9, 6, 60, listOf(robot(0.2)), 3),
    ),
    StageDef(
        id = 5,
        name = "식탁",
        stars = 5,
        description = "12자 이상 단어와 숙어. 가장 강한 대왕이 등장해요.",
        settings = GameSettings(
            2.8,
            0.8,
            6,
            70,
            listOf(robot(0.2), SpecialEnemy(EnemyKind.BOSS, chance = 0.1, speed = 1.5, points = 3)),
            4,
        ),
    ),
)

// 페이지 범위로 고르는 자유 연습. 배경은 단계마다 무대를 돌아가며 바뀐다.
val FREE_SETTINGS = GameSettings(3.4, 1.0, 6, 50, emptyList(), null)

fun stageById(id: Int): StageDef? = STAGES.find { it.id == id }
