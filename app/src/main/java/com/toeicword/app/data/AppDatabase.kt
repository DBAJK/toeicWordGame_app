package com.toeicword.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 숙련도는 표만 준비해 두었다. 저장·출제는 아직 구현 전이다.
@Entity(tableName = "progress", indices = [Index("mastery"), Index("lastStudiedAt")])
data class ProgressEntity(
    @PrimaryKey val wordId: Int,
    val correct: Int,
    val wrong: Int,
    val mastery: Int,
    val lastStudiedAt: Long,
)

@Entity(tableName = "game_records", indices = [Index("playedAt")])
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val stage: Int,
    val score: Int,
    val playedAt: Long,
    val range: String?,
    @ColumnInfo(defaultValue = "1.0") val speed: Double,
)

@Entity(tableName = "marks")
data class WordMarkEntity(
    @PrimaryKey val wordId: Int,
    val starred: Boolean,
    val memorized: Boolean,
)

@Dao
interface GameRecordDao {
    @Insert
    suspend fun insert(record: GameRecordEntity): Long

    @Query("SELECT * FROM game_records ORDER BY score DESC, playedAt ASC")
    suspend fun ranked(): List<GameRecordEntity>
}

@Dao
interface WordMarkDao {
    @Query("SELECT * FROM marks")
    suspend fun all(): List<WordMarkEntity>

    @Upsert
    suspend fun put(mark: WordMarkEntity)

    @Query("DELETE FROM marks WHERE wordId = :wordId")
    suspend fun delete(wordId: Int)
}

// 1 → 2: 게임 기록에 배속 칸 추가. 이전 기록은 1.0배로 남는다.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE game_records ADD COLUMN speed REAL NOT NULL DEFAULT 1.0")
    }
}

@Database(
    entities = [ProgressEntity::class, GameRecordEntity::class, WordMarkEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameRecords(): GameRecordDao
    abstract fun marks(): WordMarkDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "toeicword.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
