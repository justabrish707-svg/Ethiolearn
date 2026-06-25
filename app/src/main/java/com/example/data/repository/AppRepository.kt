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

    fun getLessonByTopic(topicId: Int): Flow<Lesson?> = appDao.getLessonByTopic(topicId)

    fun getExamplesByTopic(topicId: Int): Flow<List<Example>> = appDao.getExamplesByTopic(topicId)

    fun getPracticeQuestionsByTopic(topicId: Int): Flow<List<PracticeQuestion>> = appDao.getPracticeQuestionsByTopic(topicId)

    fun getQuizQuestionsByTopic(topicId: Int): Flow<List<QuizQuestion>> = appDao.getQuizQuestionsByTopic(topicId)
    
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
        if (currentProgress != null) {
            appDao.insertProgress(currentProgress.copy(completed_lessons = true, timestamp = System.currentTimeMillis()))
        } else {
            appDao.insertProgress(Progress(topic_id = topicId, completed_lessons = true))
        }
    }

    suspend fun saveQuizScore(topicId: Int, score: Int, remainingTimeSeconds: Int) {
        val currentProgress = appDao.getProgressByTopicDirect(topicId)
        if (currentProgress != null) {
            val maxScore = maxOf(currentProgress.quiz_score, score)
            // Save the best remaining time if scores are equal, or the new remaining time if score is better
            val newRemainingTime = if (score > currentProgress.quiz_score) remainingTimeSeconds 
                                    else if (score == currentProgress.quiz_score) maxOf(currentProgress.last_quiz_remaining_time_seconds, remainingTimeSeconds)
                                    else currentProgress.last_quiz_remaining_time_seconds

            appDao.insertProgress(
                currentProgress.copy(
                    quiz_score = maxScore, 
                    last_quiz_remaining_time_seconds = newRemainingTime,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            appDao.insertProgress(Progress(topic_id = topicId, quiz_score = score, last_quiz_remaining_time_seconds = remainingTimeSeconds))
        }
    }

    suspend fun insertLesson(lesson: Lesson) {
        appDao.insertLessons(listOf(lesson))
    }

    suspend fun insertExamples(examples: List<Example>) {
        appDao.insertExamples(examples)
    }

    suspend fun insertPracticeQuestions(questions: List<PracticeQuestion>) {
        appDao.insertPracticeQuestions(questions)
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
