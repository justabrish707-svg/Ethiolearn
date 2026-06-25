package com.example.data.model

import androidx.room.*

@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey val id: Int,
    val name: String
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(entity = Grade::class, parentColumns = ["id"], childColumns = ["grade_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("grade_id")]
)
data class Subject(
    @PrimaryKey val id: Int,
    val grade_id: Int,
    val name: String
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(entity = Subject::class, parentColumns = ["id"], childColumns = ["subject_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("subject_id")]
)
data class UnitTable(
    @PrimaryKey val id: Int,
    val subject_id: Int,
    val unit_number: Int,
    val title: String
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(entity = UnitTable::class, parentColumns = ["id"], childColumns = ["unit_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("unit_id")]
)
data class Topic(
    @PrimaryKey val id: Int,
    val unit_id: Int,
    val section_number: String,
    val title: String
)

@Entity(
    tableName = "learning_objectives",
    foreignKeys = [
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topic_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("topic_id")]
)
data class LearningObjective(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic_id: Int,
    val objective_text: String
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topic_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("topic_id")]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic_id: Int,
    val content: String,
    val video_url: String? = null
)

@Entity(
    tableName = "examples",
    foreignKeys = [
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topic_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("topic_id")]
)
data class Example(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic_id: Int,
    val title: String,
    val description: String,
    val solution: String? = null
)

enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topic_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("topic_id")]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic_id: Int,
    val question_text: String,
    val options: List<String>,
    val correct_option_index: Int,
    val explanation: String,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val learning_objective_id: Int? = null
)

@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(entity = Topic::class, parentColumns = ["id"], childColumns = ["topic_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("topic_id")]
)
data class Progress(
    @PrimaryKey @ColumnInfo(name = "topic_id")
    val topic_id: Int,
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED
    val completed_lessons: Boolean = false,
    val quiz_score: Int = 0,
    val last_position: String? = null,
    val last_study_date: Long = System.currentTimeMillis(),
    val mastery_score: Int = 0 // Derived from quiz performance and time
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Midnight timestamp
    val duration_minutes: Int,
    val topics_covered: List<Int>
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon_res: Int,
    val date_earned: Long? = null
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // MOCK, SUBJECT, GRADE
    val grade_id: Int,
    val subject_id: Int? = null,
    val duration_minutes: Int,
    val question_ids: List<Int>
)

@Entity(tableName = "exam_sessions")
data class ExamSession(
    @PrimaryKey val exam_id: Int,
    val answers_json: String, // Map<Int, Int> stored as JSON or simple delimited string
    val time_left_seconds: Int,
    val current_question_index: Int,
    val is_finished: Boolean
)

@Fts4
@Entity(tableName = "curriculum_search_fts")
data class CurriculumSearchFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val topic_id: Int,
    val title: String,
    val section_number: String,
    val unit_name: String,
    val content: String // Combined content from lessons, examples, etc.
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("||")

    @TypeConverter
    fun toStringList(value: String): List<String> = value.split("||")

    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> = if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
}
