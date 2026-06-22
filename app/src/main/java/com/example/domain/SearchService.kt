package com.example.domain

import com.example.data.model.Topic
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow

class SearchService(private val repository: AppRepository) {
    /**
     * Performs an efficient keyword query against units, sections, and topic titles.
     * Capitalizes on the SQLite FTS virtual table indexing for high-performance offline lookup.
     */
    fun searchOfflineCurriculum(query: String): Flow<List<Topic>> {
        return repository.searchTopics(query)
    }
}
