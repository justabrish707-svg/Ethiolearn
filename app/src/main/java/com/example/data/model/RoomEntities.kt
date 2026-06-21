package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "grades")
@Serializable
data class Grade(
    @PrimaryKey val id: Int,
    val name: String
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = Grade::class,
            parentColumns = ["id"],
            childColumns = ["grade_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Subject(
    @PrimaryKey val id: Int,
    val grade_id: Int,
    val name: String
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class UnitTable(
    @PrimaryKey val id: Int,
    val subject_id: Int,
    val unit_number: Int,
    val title: String
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = UnitTable::class,
            parentColumns = ["id"],
            childColumns = ["unit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Topic(
    @PrimaryKey val id: Int,
    val unit_id: Int,
    val section: String = "",
    val title: String
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Lesson(
    @PrimaryKey val id: Int,
    val topic_id: Int,
    val summary: String,
    val key_concepts: String,
    val important_notes: String,
    val formulas: String
)

@Entity(
    tableName = "examples",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Example(
    @PrimaryKey val id: Int,
    val topic_id: Int,
    val question: String,
    val step_by_step_solution: String
)

@Entity(
    tableName = "practice_questions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class PracticeQuestion(
    @PrimaryKey val id: Int,
    val topic_id: Int,
    val difficulty: String,
    val question: String,
    val correct_answer: String,
    val explanation: String
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class QuizQuestion(
    @PrimaryKey val id: Int,
    val topic_id: Int,
    val question: String,
    val options_json: String,
    val correct_option_index: Int
)

@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Progress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic_id: Int,
    val completed_lessons: Boolean = false,
    val quiz_score: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
