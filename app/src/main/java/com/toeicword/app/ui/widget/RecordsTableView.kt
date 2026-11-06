package com.toeicword.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.toeicword.app.R
import com.toeicword.app.databinding.ItemRecordHeaderBinding
import com.toeicword.app.databinding.ItemRecordRowBinding
import com.toeicword.app.databinding.ItemRecordsEmptyBinding
import com.toeicword.app.domain.GameRecord
import com.toeicword.app.game.isBaseSpeed
import com.toeicword.app.ui.commas
import com.toeicword.app.ui.dp
import com.toeicword.app.ui.speedLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHOWN = 10

private fun formatDate(ms: Long): String =
    SimpleDateFormat("yy-MM-dd HH:mm", Locale.KOREA).format(Date(ms))

// 영상의 '놀이 점수' 표. 상위 10개 + (순위 밖이면) 이번 판을 덧붙인다.
class RecordsTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
    }

    fun show(records: List<GameRecord>, currentId: Long? = null) {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        if (records.isEmpty()) {
            background = null
            setPadding(0, 0, 0, 0)
            ItemRecordsEmptyBinding.inflate(inflater, this, true)
            return
        }

        setBackgroundResource(R.drawable.bg_records)
        context.dp(8f).toInt().let { setPadding(it, it, it, it) }
        ItemRecordHeaderBinding.inflate(inflater, this, true)

        val rank = records.indexOfFirst { it.id == currentId }
        val rows = records.take(SHOWN).mapIndexed { i, r -> r to i + 1 }.toMutableList()
        if (rank >= SHOWN) rows += records[rank] to rank + 1

        val gap = context.dp(5f).toInt()
        rows.forEach { (r, position) ->
            val row = ItemRecordRowBinding.inflate(inflater, this, true)
            row.root.updateLayoutParams<LayoutParams> { topMargin = gap }
            val color = ContextCompat.getColor(context, if (r.id == currentId) R.color.hint_yellow else R.color.record_row)
            listOf(
                row.rank to position.toString(),
                row.mode to r.mode.label,
                row.range to rangeText(r),
                row.stage to r.stage.toString(),
                row.score to r.score.commas(),
                row.date to formatDate(r.playedAt),
            ).forEach { (cell, value) ->
                cell.text = value
                cell.setTextColor(color)
            }
        }
    }

    // 기준 속도(1.0)가 아닌 판은 무대·범위 뒤에 배속을 붙인다(★1 오븐 ×1.3, ★1 오븐 ×0.7).
    private fun rangeText(r: GameRecord): String {
        val range = r.range ?: context.getString(R.string.records_all)
        return if (isBaseSpeed(r.speed)) range else "$range ${context.speedLabel(r.speed)}"
    }
}
