package com.xsgrok2.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xsgrok2.app.data.dao.ChapterDao
import com.xsgrok2.app.data.dao.ChapterInstructionDao
import com.xsgrok2.app.data.dao.CharacterStateDao
import com.xsgrok2.app.data.dao.LorebookEntryDao
import com.xsgrok2.app.data.dao.NovelDao
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.ChapterInstruction
import com.xsgrok2.app.data.model.CharacterState
import com.xsgrok2.app.data.model.LorebookEntry
import com.xsgrok2.app.data.model.Novel

@Database(
    entities = [
        Novel::class, 
        Chapter::class, 
        LorebookEntry::class, 
        ChapterInstruction::class,
        CharacterState::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun lorebookEntryDao(): LorebookEntryDao
    abstract fun chapterInstructionDao(): ChapterInstructionDao
    abstract fun characterStateDao(): CharacterStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapters ADD COLUMN summary TEXT DEFAULT ''")
                database.execSQL("ALTER TABLE chapters ADD COLUMN keyEvents TEXT DEFAULT ''")
                database.execSQL("ALTER TABLE chapters ADD COLUMN characterStateSnapshot TEXT DEFAULT ''")
                database.execSQL("ALTER TABLE chapters ADD COLUMN qualityScore REAL")
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS character_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        novelId INTEGER NOT NULL,
                        characterName TEXT NOT NULL,
                        currentMood TEXT NOT NULL DEFAULT '',
                        knownInformation TEXT NOT NULL DEFAULT '',
                        relationships TEXT NOT NULL DEFAULT '',
                        currentLocation TEXT NOT NULL DEFAULT '',
                        forbiddenActions TEXT NOT NULL DEFAULT '',
                        arcProgress TEXT NOT NULL DEFAULT '',
                        chapterNumber INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(novelId) REFERENCES novels(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_character_states_novelId ON character_states(novelId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_character_states_characterName ON character_states(characterName)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xsgrok2_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
