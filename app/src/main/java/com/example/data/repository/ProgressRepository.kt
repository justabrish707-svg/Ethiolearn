package com.example.data.repository

import com.example.data.db.AppDao
import com.example.data.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TopicProgressMetrics(
    val topicId: Int,
    val isCompleted: Boolean,
    val highestQuizScore: Int,
    val lastAttemptTimestamp: Long
)

class ProgressRepository(private val appDao: AppDao) {

    /**
     * Observable stream of all progress records in the SQLite database block.
     */
    val allProgress: Flow<List<Progress>> = appDao.getAllProgress()

    /**
     * Gets progress record associated with a single topic.
     */
    fun getProgressByTopic(topicId: Int): Flow<Progress?> {
        return appDao.getProgressByTopic(topicId)
    }

    /**
     * Direct synchronous/suspend query fetching progress for updates.
     */
    suspend fun getProgressByTopicDirect(topicId: Int): Progress? {
        return appDao.getProgressByTopicDirect(topicId)
    }

    /**
     * Updates the completion status flag of lessons for a specific topic in SQLite.
     */
    suspend fun updateCompletionStatus(topicId: Int, completed: Boolean) {
        val current = appDao.getProgressByTopicDirect(topicId)
        if (current != null) {
            appDao.insertProgress(current.copy(
                completed_lessons = completed,
                last_study_date = System.currentTimeMillis()
            ))
        } else {
            appDao.insertProgress(Progress(
                topic_id = topicId,
                completed_lessons = completed,
                last_study_date = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Updates and logs quiz scores in the SQLite progress table, keeping the highest.
     */
    suspend fun saveQuizScore(topicId: Int, score: Int) {
        val current = appDao.getProgressByTopicDirect(topicId)
        if (current != null) {
            val maxScore = maxOf(current.quiz_score, score)
            appDao.insertProgress(current.copy(
                quiz_score = maxScore,
                last_study_date = System.currentTimeMillis()
            ))
        } else {
            appDao.insertProgress(Progress(
                topic_id = topicId,
                quiz_score = score,
                last_study_date = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Fetches aggregate quiz performance and completion metrics per topic from database rows.
     */
    fun getAggregateMetricsPerTopic(): Flow<List<TopicProgressMetrics>> {
        return appDao.getAllProgress().map { progressList ->
            progressList.map { p ->
                TopicProgressMetrics(
                    topicId = p.topic_id,
                    isCompleted = p.completed_lessons,
                    highestQuizScore = p.quiz_score,
                    lastAttemptTimestamp = p.last_study_date
                )
            }
        }
    }
}
