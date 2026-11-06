package com.toeicword.app.game

// 무대 배경. 순서는 Stages.kt의 무대 순서(오븐 → 식탁)와 같다. x·y는 보드 기준 %, size는 dp.
data class Decoration(
    val emoji: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotate: Float = 0f,
)

enum class BoardBackground { OVEN, SINK, BOX, FRIDGE, TABLE }

data class Theme(
    val bg: BoardBackground,
    val monster: String,
    val thumb: String, // 무대 카드 미리보기에 올릴 장식
    val decorations: List<Decoration>,
)

val THEMES = listOf(
    Theme(
        BoardBackground.OVEN,
        "👾",
        "🥐🍪",
        listOf(
            Decoration("🥐", 40f, 4f, 70f, -14f),
            Decoration("🍞", 64f, 3f, 60f, 10f),
            Decoration("🍪", 86f, 8f, 50f, 20f),
            Decoration("🧇", 26f, 8f, 40f, -8f),
            Decoration("🔥", 6f, 92f, 44f),
            Decoration("🔥", 94f, 92f, 44f),
        ),
    ),
    Theme(
        BoardBackground.SINK,
        "🐙",
        "🫧🍽️",
        listOf(
            Decoration("🍽️", 44f, 3f, 80f, -6f),
            Decoration("🧽", 68f, 6f, 48f, 18f),
            Decoration("🫧", 86f, 10f, 46f),
            Decoration("🥄", 26f, 6f, 40f, -30f),
            Decoration("🫧", 6f, 86f, 50f),
            Decoration("💧", 95f, 88f, 38f),
        ),
    ),
    Theme(
        BoardBackground.BOX,
        "👻",
        "🍬🍫",
        listOf(
            Decoration("🍎", 93f, 3f, 120f, -12f),
            Decoration("🍫", 44f, 0f, 70f, 14f),
            Decoration("🍒", 60f, 6f, 42f, 8f),
            Decoration("🍬", 30f, 4f, 34f, -24f),
            Decoration("🍬", 74f, 10f, 30f, 32f),
            Decoration("🍭", 5f, 90f, 58f, -28f),
            Decoration("🍒", 95f, 88f, 44f, 16f),
        ),
    ),
    Theme(
        BoardBackground.FRIDGE,
        "👾",
        "🥛🧀",
        listOf(
            Decoration("🥛", 42f, 3f, 70f, -6f),
            Decoration("🧀", 64f, 6f, 56f, 12f),
            Decoration("🥚", 84f, 8f, 40f, -16f),
            Decoration("🍦", 26f, 6f, 44f, 10f),
            Decoration("🧊", 6f, 88f, 50f, -10f),
            Decoration("🧊", 94f, 90f, 42f, 14f),
        ),
    ),
    Theme(
        BoardBackground.TABLE,
        "👻",
        "🎂🍰",
        listOf(
            Decoration("🎂", 46f, 2f, 130f, -6f),
            Decoration("🍩", 88f, 6f, 64f, 18f),
            Decoration("🍪", 28f, 6f, 46f, -12f),
            Decoration("🍫", 70f, 3f, 40f, 30f),
            Decoration("🍪", 6f, 86f, 50f, 20f),
            Decoration("🍩", 94f, 90f, 46f, -16f),
        ),
    ),
)

// 강한 적의 모습은 무대와 상관없이 같다.
fun specialMonster(kind: EnemyKind): String? = when (kind) {
    EnemyKind.ROBOT -> "🤖"
    EnemyKind.BOSS -> "👹"
    EnemyKind.NORMAL -> null
}

// 고정 배경이 없으면(자유 연습) 단계마다 무대를 돌아가며 바꾼다.
fun themeFor(fixed: Int?, stage: Int): Theme = THEMES[fixed ?: ((stage - 1) % THEMES.size)]
