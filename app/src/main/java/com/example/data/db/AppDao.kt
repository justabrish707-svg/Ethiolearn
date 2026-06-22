package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Example
import com.example.data.model.Grade
import com.example.data.model.Lesson
import com.example.data.model.PracticeQuestion
import com.example.data.model.Progress
import com.example.data.model.QuizQuestion
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UnitTable
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM grades")
    fun getGrades(): Flow<List<Grade>>

    @Query("SELECT * FROM subjects WHERE grade_id = :gradeId")
    fun getSubjectsByGrade(gradeId: Int): Flow<List<Subject>>

    @Query("SELECT * FROM units WHERE subject_id = :subjectId ORDER BY unit_number ASC")
    fun getUnitsBySubject(subjectId: Int): Flow<List<UnitTable>>

    @Query("SELECT * FROM topics WHERE unit_id = :unitId")
    fun getTopicsByUnit(unitId: Int): Flow<List<Topic>>

    @Query("SELECT topics.* FROM topics INNER JOIN units ON topics.unit_id = units.id WHERE topics.title LIKE '%' || :query || '%' OR topics.section LIKE '%' || :query || '%' OR units.title LIKE '%' || :query || '%'")
    fun searchTopics(query: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: Int): Topic?

    @Query("SELECT * FROM units WHERE id = :unitId LIMIT 1")
    suspend fun getUnitById(unitId: Int): UnitTable?

    @Query("SELECT * FROM lessons WHERE topic_id = :topicId LIMIT 1")
    fun getLessonByTopic(topicId: Int): Flow<Lesson?>

    @Query("SELECT * FROM examples WHERE topic_id = :topicId")
    fun getExamplesByTopic(topicId: Int): Flow<List<Example>>

    @Query("SELECT * FROM practice_questions WHERE topic_id = :topicId")
    fun getPracticeQuestionsByTopic(topicId: Int): Flow<List<PracticeQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE topic_id = :topicId")
    fun getQuizQuestionsByTopic(topicId: Int): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM progress WHERE topic_id = :topicId LIMIT 1")
    fun getProgressByTopic(topicId: Int): Flow<Progress?>

    @Query("SELECT * FROM progress")
    fun getAllProgress(): Flow<List<Progress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: Progress)

    // Prepopulation helpers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<Grade>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitTable>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<Lesson>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamples(examples: List<Example>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeQuestions(questions: List<PracticeQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)
    
    @Query("SELECT COUNT(*) FROM grades")
    suspend fun countGrades(): Int

    @Query("SELECT * FROM progress WHERE topic_id = :topicId LIMIT 1")
    suspend fun getProgressByTopicDirect(topicId: Int): Progress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchFts(ftsItems: List<com.example.data.model.CurriculumSearchFts>)

    @Query("""
        SELECT t.* FROM topics t
        INNER JOIN curriculum_search_fts ON t.id = curriculum_search_fts.topic_id
        WHERE curriculum_search_fts MATCH :query
    """)
    fun searchTopicsFts(query: String): Flow<List<Topic>>

    @Query("SELECT * FROM units ORDER BY id ASC")
    fun getAllUnits(): Flow<List<UnitTable>>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    fun getAllTopics(): Flow<List<Topic>>
}
