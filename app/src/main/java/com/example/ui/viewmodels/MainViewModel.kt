package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades = _grades.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects = _subjects.asStateFlow()

    private val _units = MutableStateFlow<List<UnitTable>>(emptyList())
    val units = _units.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics = _topics.asStateFlow()

    private val _currentLesson = MutableStateFlow<Lesson?>(null)
    val currentLesson = _currentLesson.asStateFlow()

    private val _practiceQuestions = MutableStateFlow<List<PracticeQuestion>>(emptyList())
    val practiceQuestions = _practiceQuestions.asStateFlow()

    private val _examples = MutableStateFlow<List<Example>>(emptyList())
    val examples = _examples.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializePrepopulatedData()
            repository.grades.collectLatest { 
                _grades.value = it 
            }
        }
    }

    fun loadSubjects(gradeId: Int) {
        viewModelScope.launch {
            repository.getSubjectsByGrade(gradeId).collectLatest {
                _subjects.value = it
            }
        }
    }

    fun loadUnits(subjectId: Int) {
        viewModelScope.launch {
            repository.getUnitsBySubject(subjectId).collectLatest {
                _units.value = it
            }
        }
    }

    fun loadTopics(unitId: Int) {
        viewModelScope.launch {
            repository.getTopicsByUnit(unitId).collectLatest {
                _topics.value = it
            }
        }
    }

    fun loadLesson(topicId: Int) {
        viewModelScope.launch {
            repository.getLessonByTopic(topicId).collectLatest {
                _currentLesson.value = it
            }
        }
    }

    fun loadExamples(topicId: Int) {
        viewModelScope.launch {
            repository.getExamplesByTopic(topicId).collectLatest {
                _examples.value = it
            }
        }
    }

    fun loadPracticeQuestions(topicId: Int) {
        viewModelScope.launch {
            repository.getPracticeQuestionsByTopic(topicId).collectLatest {
                _practiceQuestions.value = it
            }
        }
    }
}
