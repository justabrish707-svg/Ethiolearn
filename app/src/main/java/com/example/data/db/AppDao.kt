package com.example.data.db

import androidx.room.*
import com.example.data.model.*
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

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: Int): Topic?

    @Query("SELECT * FROM units WHERE id = :unitId LIMIT 1")
    suspend fun getUnitById(unitId: Int): UnitTable?

    @Query("SELECT * FROM lessons WHERE topic_id = :topicId LIMIT 1")
    fun getLessonByTopic(topicId: Int): Flow<Lesson?>

    @Query("SELECT * FROM learning_objectives WHERE topic_id = :topicId")
    fun getObjectivesByTopic(topicId: Int): Flow<List<LearningObjective>>

    @Query("SELECT * FROM examples WHERE topic_id = :topicId")
    fun getExamplesByTopic(topicId: Int): Flow<List<Example>>

    @Query("SELECT * FROM quiz_questions WHERE topic_id = :topicId")
    fun getQuizQuestionsByTopic(topicId: Int): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM progress WHERE topic_id = :topicId LIMIT 1")
    fun getProgressByTopic(topicId: Int): Flow<Progress?>

    @Query("SELECT * FROM progress")
    fun getAllProgress(): Flow<List<Progress>>

    // Weak Topic Detection
    @Query("SELECT * FROM topics WHERE id IN (SELECT topic_id FROM progress WHERE mastery_score < 60) LIMIT 10")
    fun getWeakTopics(): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: Progress)

    // Retention & Exams
    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getStudySessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession)

    @Query("SELECT * FROM achievements")
    fun getAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Query("SELECT * FROM exams WHERE type = :type")
    fun getExamsByType(type: String): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam)

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
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectives(objectives: List<LearningObjective>)
    
    @Query("SELECT COUNT(*) FROM topics")
    suspend fun countTopics(): Int

    @Query("SELECT * FROM quiz_questions WHERE id IN (:questionIds)")
    suspend fun getQuestionsByIds(questionIds: List<Int>): List<QuizQuestion>

    @Query("""
        SELECT q.* FROM quiz_questions q
        INNER JOIN topics t ON q.topic_id = t.id
        INNER JOIN units u ON t.unit_id = u.id
        WHERE u.subject_id = :subjectId
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsBySubject(subjectId: Int, limit: Int): List<QuizQuestion>

    @Query("""
        SELECT q.* FROM quiz_questions q
        INNER JOIN topics t ON q.topic_id = t.id
        INNER JOIN units u ON t.unit_id = u.id
        INNER JOIN subjects s ON u.subject_id = s.id
        WHERE s.grade_id = :gradeId
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsByGrade(gradeId: Int, limit: Int): List<QuizQuestion>

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: Int): Exam?

    @Query("SELECT * FROM exams")
    fun getAllExams(): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExamSession(session: ExamSession)

    @Query("SELECT * FROM exam_sessions WHERE exam_id = :examId LIMIT 1")
    suspend fun getExamSession(examId: Int): ExamSession?

    @Query("SELECT * FROM progress WHERE topic_id = :topicId LIMIT 1")
    suspend fun getProgressByTopicDirect(topicId: Int): Progress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchFts(ftsItems: List<CurriculumSearchFts>)

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

    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<Subject>>
    
    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectById(subjectId: Int): Subject?

    @Query("DELETE FROM grades")
    suspend fun clearGrades()

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM units")
    suspend fun clearUnits()

    @Query("DELETE FROM topics")
    suspend fun clearTopics()

    @Query("DELETE FROM lessons")
    suspend fun clearLessons()

    @Query("DELETE FROM examples")
    suspend fun clearExamples()

    @Query("DELETE FROM quiz_questions")
    suspend fun clearQuizQuestions()

    @Query("DELETE FROM learning_objectives")
    suspend fun clearObjectives()

    @Query("DELETE FROM curriculum_search_fts")
    suspend fun clearFts()
}
