package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Folder
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TranscriptionNote
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TranscriptionNote::class,
        TranscriptSegment::class,
        Folder::class,
        TagEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun segmentDao(): SegmentDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ses_yazici_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial folders and tags on background thread
                            CoroutineScope(Dispatchers.IO).launch {
                                val dbInstance = getInstance(context)
                                populateInitialData(dbInstance)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val initialFolders = listOf(
                Folder(name = "Toplantılar", colorHex = "#6366F1", iconName = "business"),
                Folder(name = "Mülakatlar", colorHex = "#06B6D4", iconName = "group"),
                Folder(name = "Ders Notları", colorHex = "#10B981", iconName = "school"),
                Folder(name = "Fikirler & Beyin Fırtınası", colorHex = "#F59E0B", iconName = "lightbulb")
            )
            for (folder in initialFolders) {
                db.folderDao().insertFolder(folder)
            }

            val initialTags = listOf(
                TagEntity(name = "Acil", colorHex = "#EF4444"),
                TagEntity(name = "Önemli", colorHex = "#F59E0B"),
                TagEntity(name = "Proje A", colorHex = "#6366F1"),
                TagEntity(name = "Müşteri", colorHex = "#10B981"),
                TagEntity(name = "Özetlendi", colorHex = "#8B5CF6")
            )
            for (tag in initialTags) {
                db.tagDao().insertTag(tag)
            }
        }
    }
}
