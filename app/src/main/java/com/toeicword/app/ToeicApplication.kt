package com.toeicword.app

import android.app.Application
import com.toeicword.app.data.AppDatabase
import com.toeicword.app.data.MarkRepository
import com.toeicword.app.data.RecordRepository
import com.toeicword.app.data.SelectionPrefs
import com.toeicword.app.data.WordRepository
import com.toeicword.app.ui.AppStore

class ToeicApplication : Application() {
    private val database by lazy { AppDatabase.create(this) }

    val words by lazy { WordRepository(this) }
    val records by lazy { RecordRepository(database.gameRecords()) }
    val marks by lazy { MarkRepository(database.marks()) }
    val selectionPrefs by lazy { SelectionPrefs(this) }
    val store by lazy { AppStore(this) }
}
