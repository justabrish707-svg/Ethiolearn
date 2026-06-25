package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.*

@Database(
    entities = [
        Grade::class,
        Subject::class,
        UnitTable::class,
        Topic::class,
        Lesson::class,
        Example::class,
        QuizQuestion::class,
        Progress::class,
        CurriculumSearchFts::class,
        LearningObjective::class,
        StudySession::class,
        Achievement::class,
        Exam::class,
        ExamSession::class,
        StudentProfile::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
                .createFromAsset("database/curriculum.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
