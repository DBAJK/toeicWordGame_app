package com.toeicword.app.data

import android.content.Context
import androidx.core.content.edit
import com.toeicword.app.domain.GameMode
import com.toeicword.app.domain.GameRecord
import com.toeicword.app.domain.MarkMap
import com.toeicword.app.domain.Word
import com.toeicword.app.domain.WordMark
import com.toeicword.app.domain.WordSelection
import com.toeicword.app.game.Config
import com.toeicword.app.game.clampSpeed
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val MISSING_WORDS_MESSAGE =
    "assets/words.json이 없습니다. words.json을 app/src/main/assets에 넣은 뒤 다시 빌드하세요."

private val json = Json { ignoreUnknownKeys = true }

class WordRepository(private val context: Context) {
    suspend fun loadWords(): List<Word> = withContext(Dispatchers.IO) {
        val text = try {
            context.assets.open("words.json").bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            throw IllegalStateException(MISSING_WORDS_MESSAGE)
        }
        json.decodeFromString<List<Word>>(text)
    }
}

class RecordRepository(private val dao: GameRecordDao) {
    suspend fun saveRecord(record: GameRecord): Long = dao.insert(
        GameRecordEntity(
            mode = record.mode.name,
            stage = record.stage,
            score = record.score,
            playedAt = record.playedAt,
            range = record.range,
            speed = record.speed,
        ),
    )

    suspend fun rankedRecords(): List<GameRecord> = dao.ranked().map {
        GameRecord(it.id, GameMode.valueOf(it.mode), it.stage, it.score, it.playedAt, it.range, it.speed)
    }
}

class MarkRepository(private val dao: WordMarkDao) {
    suspend fun loadMarks(): MarkMap = dao.all().associate {
        it.wordId to WordMark(it.wordId, it.starred, it.memorized)
    }

    // 별표도 외움도 아니면 지워서 테이블을 가볍게 유지한다.
    suspend fun saveMark(mark: WordMark) {
        if (!mark.starred && !mark.memorized) dao.delete(mark.wordId)
        else dao.put(WordMarkEntity(mark.wordId, mark.starred, mark.memorized))
    }
}

// 메인 화면에서 마지막으로 고른 범위와 배속을 기억한다(편의 기능이라 실패해도 기본값으로).
class SelectionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("toeicword", Context.MODE_PRIVATE)

    fun load(): WordSelection = try {
        prefs.getString(KEY, null)?.let { json.decodeFromString<WordSelection>(it) } ?: WordSelection()
    } catch (_: IllegalArgumentException) {
        WordSelection()
    }

    fun save(selection: WordSelection) {
        prefs.edit { putString(KEY, json.encodeToString(selection)) }
    }

    fun loadSpeed(): Double = clampSpeed(prefs.getFloat(KEY_SPEED, Config.SPEED_BASE.toFloat()).toDouble())

    fun saveSpeed(speed: Double) {
        prefs.edit { putFloat(KEY_SPEED, speed.toFloat()) }
    }

    private companion object {
        const val KEY = "selection"
        const val KEY_SPEED = "speed"
    }
}
