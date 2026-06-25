package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class AppRepository(
    val database: com.example.data.db.AppDatabase,
    val appDao: AppDao,
    val context: Context
) {

    val grades: Flow<List<Grade>> = appDao.getGrades()

    fun getSubjectsByGrade(gradeId: Int): Flow<List<Subject>> = appDao.getSubjectsByGrade(gradeId)

    fun getUnitsBySubject(subjectId: Int): Flow<List<UnitTable>> = appDao.getUnitsBySubject(subjectId)

    fun getTopicsByUnit(unitId: Int): Flow<List<Topic>> = appDao.getTopicsByUnit(unitId)

    fun searchTopics(query: String): Flow<List<Topic>> {
        val sanitizedQuery = query.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .joinToString(" ") { "$it*" }
        return if (sanitizedQuery.isEmpty()) {
            flowOf(emptyList())
        } else {
            appDao.searchTopicsFts(sanitizedQuery)
        }
    }

    suspend fun getTopicById(topicId: Int): Topic? = appDao.getTopicById(topicId)
    
    suspend fun getUnitById(unitId: Int): UnitTable? = appDao.getUnitById(unitId)

    fun getObjectivesByTopic(topicId: Int): Flow<List<LearningObjective>> = appDao.getObjectivesByTopic(topicId)

    fun getLessonByTopic(topicId: Int): Flow<Lesson?> = appDao.getLessonByTopic(topicId)

    fun getExamplesByTopic(topicId: Int): Flow<List<Example>> = appDao.getExamplesByTopic(topicId)

    fun getQuizQuestionsByTopic(topicId: Int): Flow<List<QuizQuestion>> = appDao.getQuizQuestionsByTopic(topicId)
    
    fun getWeakTopics(): Flow<List<Topic>> = appDao.getWeakTopics()

    fun getStudySessions(): Flow<List<StudySession>> = appDao.getStudySessions()

    fun getAchievements(): Flow<List<Achievement>> = appDao.getAchievements()

    fun getExamsByType(type: String): Flow<List<Exam>> = appDao.getExamsByType(type)
    
    fun getAllExams(): Flow<List<Exam>> = appDao.getAllExams()

    suspend fun getExamById(examId: Int): Exam? = appDao.getExamById(examId)

    suspend fun saveExamSession(session: ExamSession) = appDao.saveExamSession(session)

    suspend fun getExamSession(examId: Int): ExamSession? = appDao.getExamSession(examId)

    suspend fun getQuestionsByIds(ids: List<Int>): List<QuizQuestion> = appDao.getQuestionsByIds(ids)

    suspend fun createMockExam(subjectId: Int): Exam {
        val questions = appDao.getRandomQuestionsBySubject(subjectId, 50)
        val exam = Exam(
            title = "Mock Exam: Subject $subjectId",
            type = "MOCK",
            grade_id = 12, // Defaulting for now
            subject_id = subjectId,
            duration_minutes = 60,
            question_ids = questions.map { it.id }
        )
        appDao.insertExam(exam)
        return exam
    }

    suspend fun createNationalExamSimulation(gradeId: Int): Exam {
        val questions = appDao.getRandomQuestionsByGrade(gradeId, 100)
        val exam = Exam(
            title = "National Exam Simulation: Grade $gradeId",
            type = "GRADE",
            grade_id = gradeId,
            duration_minutes = 120,
            question_ids = questions.map { it.id }
        )
        appDao.insertExam(exam)
        return exam
    }

    suspend fun saveStudySession(session: StudySession) = appDao.insertStudySession(session)

    fun getAllUnits(): Flow<List<UnitTable>> = appDao.getAllUnits()

    fun getAllTopics(): Flow<List<Topic>> = appDao.getAllTopics()

    fun getAllSubjects(): Flow<List<Subject>> = appDao.getAllSubjects()
    
    suspend fun getSubjectById(subjectId: Int): Subject? = appDao.getSubjectById(subjectId)
    
    fun getAllProgress(): Flow<List<Progress>> = appDao.getAllProgress()

    fun getProgressByTopic(topicId: Int): Flow<Progress?> = appDao.getProgressByTopic(topicId)

    suspend fun getProgressByTopicDirect(topicId: Int): Progress? = appDao.getProgressByTopicDirect(topicId)

    suspend fun updateProgress(progress: Progress) {
        appDao.insertProgress(progress)
    }

    suspend fun markTopicLessonCompleted(topicId: Int) {
        val currentProgress = appDao.getProgressByTopicDirect(topicId)
        val newProgress = currentProgress?.copy(
            completed_lessons = true,
            status = if (currentProgress.quiz_score > 0) "COMPLETED" else "IN_PROGRESS",
            last_study_date = System.currentTimeMillis()
        ) ?: Progress(topic_id = topicId, completed_lessons = true, status = "IN_PROGRESS")
        appDao.insertProgress(newProgress)
    }

    suspend fun saveQuizScore(topicId: Int, score: Int) {
        val currentProgress = appDao.getProgressByTopicDirect(topicId)
        val masteryScore = minOf(100, score * 10) // Basic heuristic
        val status = if (score >= 7) "COMPLETED" else "IN_PROGRESS"

        val newProgress = currentProgress?.copy(
            quiz_score = maxOf(currentProgress.quiz_score, score),
            mastery_score = maxOf(currentProgress.mastery_score, masteryScore),
            status = status,
            last_study_date = System.currentTimeMillis()
        ) ?: Progress(topic_id = topicId, quiz_score = score, mastery_score = masteryScore, status = status)
        
        appDao.insertProgress(newProgress)
    }

    suspend fun insertLesson(lesson: Lesson) {
        appDao.insertLessons(listOf(lesson))
    }

    suspend fun insertExamples(examples: List<Example>) {
        appDao.insertExamples(examples)
    }

    suspend fun insertQuizQuestions(questions: List<QuizQuestion>) {
        appDao.insertQuizQuestions(questions)
    }

    suspend fun initializePrepopulatedData() {
        if (appDao.countTopics() < 1000) {
            com.example.data.db.DatabaseSeeder(
                database,
                context
            ).seedDatabaseAtomically()
        }
    }
}
