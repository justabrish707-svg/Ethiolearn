package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CurriculumSearchFts
import com.example.data.model.Example
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.PracticeQuestion
import com.example.data.model.Progress
import com.example.data.model.QuizQuestion
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UnitTable

@Database(
    entities = [
        Grade::class,
        Subject::class,
        UnitTable::class,
        Topic::class,
        Lesson::class,
        Example::class,
        PracticeQuestion::class,
        QuizQuestion::class,
        Progress::class,
        CurriculumSearchFts::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ethiolearn_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
