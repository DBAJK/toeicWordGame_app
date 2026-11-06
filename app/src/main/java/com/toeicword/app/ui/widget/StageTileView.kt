package com.toeicword.app.ui.widget

import android.animation.AnimatorInflater
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.toeicword.app.R
import com.toeicword.app.databinding.ViewStageTileBinding
import com.toeicword.app.game.STAGES
import com.toeicword.app.game.StageDef
import com.toeicword.app.game.THEMES
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.setGrayscale

// 무대 카드: 회색 금속 테두리, 위쪽 손잡이, 미리보기 · 별 · 이름
class StageTileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {
    private val binding = ViewStageTileBinding.inflate(LayoutInflater.from(context), this)
    private var stage: StageDef = STAGES[0]

    init {
        context.withStyledAttributes(attrs, R.styleable.StageTileView) {
            stage = STAGES[getInt(R.styleable.StageTileView_stageNumber, 1) - 1]
        }
        val theme = THEMES[stage.settings.theme ?: 0]
        binding.thumbBackdrop.show(theme.bg)
        binding.thumbText.text = theme.thumb
        binding.thumbMonster.text = theme.monster
        binding.stars.text = "★".repeat(stage.stars)
        binding.name.text = stage.name
        contentDescription = context.getString(R.string.stage_start, stage.name)
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.stage_tile)
        isClickable = true
        isFocusable = true
    }

    val stageId: Int get() = stage.id

    fun showCount(count: Int) {
        binding.count.text = context.getString(R.string.stage_words, count.commas())
        val available = count > 0
        isEnabled = available
        setGrayscale(!available)
    }
}
