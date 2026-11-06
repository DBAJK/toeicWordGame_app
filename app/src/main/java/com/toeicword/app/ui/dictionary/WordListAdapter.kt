package com.toeicword.app.ui.dictionary

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toeicword.app.R
import com.toeicword.app.databinding.ItemWordBinding
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.WordMark
import com.toeicword.app.ui.MarkKey
import com.toeicword.app.ui.dp

internal data class WordRow(
    val index: Int,
    val word: Word,
    val mark: WordMark?,
    val hideMeaning: Boolean,
    val showPage: Boolean,
)

private object WordRowDiff : DiffUtil.ItemCallback<WordRow>() {
    override fun areItemsTheSame(oldItem: WordRow, newItem: WordRow) = oldItem.word.id == newItem.word.id
    override fun areContentsTheSame(oldItem: WordRow, newItem: WordRow) = oldItem == newItem
}

internal class WordListAdapter(
    private val onToggleMark: (Int, MarkKey) -> Unit,
) : ListAdapter<WordRow, WordListAdapter.Holder>(WordRowDiff) {
    class Holder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {
        var boundId = -1
    }

    // '뜻 가리기' 중에 눌러서 펼친 단어.
    private val revealed = HashSet<Int>()

    fun clearRevealed() = revealed.clear()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val word = row.word
        val b = holder.binding
        val context = b.root.context
        val sameWord = holder.boundId == word.id
        holder.boundId = word.id

        val memorized = row.mark?.memorized == true
        val faded = if (memorized) 0.45f else 1f
        val note = b.note
        val stars = if (note == null) "★${word.level}" else "★".repeat(word.level)

        b.root.setBackgroundColor(
            ContextCompat.getColor(context, if (row.index % 2 == 0) R.color.word_row else R.color.word_row_alt),
        )
        b.number.text = word.id.toString()
        val level = (if (row.showPage) "${word.page}p · " else "") + stars
        b.level.text = level
        b.word.text = word.word
        b.word.alpha = faded
        b.meaning.alpha = faded
        note?.text = noteOf(word)

        val meaning = word.meanings.joinToString(", ")
        val hidden = row.hideMeaning && word.id !in revealed
        // blur는 Android 12부터 동작하므로 그 아래에서는 글자를 바꿔서 가린다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.meaning.text = meaning
            b.meaning.setBlurred(hidden)
        } else {
            b.meaning.text = if (hidden) context.getString(R.string.hidden_meaning) else meaning
        }
        if (row.hideMeaning) {
            b.meaning.setOnClickListener { toggleReveal(holder, word.id) }
        } else {
            b.meaning.setOnClickListener(null)
            b.meaning.isClickable = false
        }

        b.star.showMark(row.mark?.starred == true, animate = sameWord)
        b.memorized.showMark(memorized, animate = sameWord)
        b.star.setOnClickListener { onToggleMark(word.id, MarkKey.STARRED) }
        b.memorized.setOnClickListener { onToggleMark(word.id, MarkKey.MEMORIZED) }
    }

    private fun toggleReveal(holder: Holder, id: Int) {
        if (!revealed.add(id)) revealed.remove(id)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun View.setBlurred(blurred: Boolean) {
        val radius = context.dp(6f)
        setRenderEffect(if (blurred) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL) else null)
    }
}
