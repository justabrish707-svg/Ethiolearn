package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.domain.AITutor
import com.example.domain.SearchService
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

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()

    private val _topicProgress = MutableStateFlow<Progress?>(null)
    val topicProgress = _topicProgress.asStateFlow()

    private val _allProgress = MutableStateFlow<List<Progress>>(emptyList())
    val allProgress = _allProgress.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Topic>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    val progressRepository = com.example.data.repository.ProgressRepository(repository.appDao)

    private val _allUnits = MutableStateFlow<List<UnitTable>>(emptyList())
    val allUnits = _allUnits.asStateFlow()

    private val _allTopics = MutableStateFlow<List<Topic>>(emptyList())
    val allTopics = _allTopics.asStateFlow()

    private val _allSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val allSubjects = _allSubjects.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializePrepopulatedData()
            repository.grades.collectLatest { 
                _grades.value = it 
            }
        }
        viewModelScope.launch {
            repository.getAllUnits().collectLatest {
                _allUnits.value = it
            }
        }
        viewModelScope.launch {
            repository.getAllTopics().collectLatest {
                _allTopics.value = it
            }
        }
        viewModelScope.launch {
            repository.getAllSubjects().collectLatest {
                _allSubjects.value = it
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

    fun loadQuizQuestions(topicId: Int) {
        viewModelScope.launch {
            repository.getQuizQuestionsByTopic(topicId).collectLatest {
                _quizQuestions.value = it
            }
        }
    }

    fun loadProgress(topicId: Int) {
        viewModelScope.launch {
            repository.getProgressByTopic(topicId).collectLatest {
                _topicProgress.value = it
            }
        }
    }

    fun loadAllProgress() {
        viewModelScope.launch {
            repository.getAllProgress().collectLatest {
                _allProgress.value = it
            }
        }
    }

    private val searchService = SearchService(repository)

    fun search(query: String) {
        viewModelScope.launch {
            if (query.trim().isEmpty()) {
                _searchResults.value = emptyList()
            } else {
                searchService.searchOfflineCurriculum(query).collectLatest {
                    _searchResults.value = it
                }
            }
        }
    }

    fun markLessonCompleted(topicId: Int) {
        viewModelScope.launch {
            repository.markTopicLessonCompleted(topicId)
        }
    }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError = _generationError.asStateFlow()

    private val aiTutor = AITutor()

    fun generateLessonForTopic(topicId: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            try {
                val topic = repository.getTopicById(topicId) ?: throw Exception("Topic not found")
                val topicTitle = topic.title
                val unitId = topic.unit_id
                
                val unit = repository.getUnitById(unitId)
                val unitTitle = unit?.title ?: "Standard Unit"
                val subjectId = unit?.subject_id ?: 0
                val subject = repository.getSubjectById(subjectId)
                val gradeId = subject?.grade_id ?: 9
                val gradeName = "Grade $gradeId"
                
                val generated = aiTutor.generateCompleteCurriculumForTopic(topicTitle, unitTitle, gradeName)
                if (generated != null) {
                    val baseId = topicId * 1000
                    
                    val lesson = Lesson(
                        id = baseId + 1,
                        topic_id = topicId,
                        summary = generated.summary,
                        key_concepts = generated.key_concepts,
                        important_notes = generated.important_notes,
                        formulas = generated.formulas
                    )
                    
                    val examplesList = generated.examples.mapIndexed { index, ex ->
                        Example(
                            id = baseId + index + 1,
                            topic_id = topicId,
                            question = ex.question,
                            step_by_step_solution = ex.step_by_step_solution
                        )
                    }
                    
                    val practiceList = generated.practice_questions.mapIndexed { index, pq ->
                        PracticeQuestion(
                            id = baseId + index + 1,
                            topic_id = topicId,
                            difficulty = pq.difficulty,
                            question = pq.question,
                            correct_answer = pq.correct_answer,
                            explanation = pq.explanation
                        )
                    }
                    
                    val json = kotlinx.serialization.json.Json
                    val quizList = generated.quiz_questions.mapIndexed { index, qq ->
                        val optionsJsonString = json.encodeToString(kotlinx.serialization.serializer<List<String>>(), qq.options)
                        QuizQuestion(
                            id = baseId + index + 1,
                            topic_id = topicId,
                            question = qq.question,
                            options_json = optionsJsonString,
                            correct_option_index = qq.correct_option_index
                        )
                    }
                    
                    repository.insertLesson(lesson)
                    repository.insertExamples(examplesList)
                    repository.insertPracticeQuestions(practiceList)
                    repository.insertQuizQuestions(quizList)
                    
                    loadLesson(topicId)
                    loadExamples(topicId)
                    loadPracticeQuestions(topicId)
                    loadQuizQuestions(topicId)
                } else {
                    _generationError.value = "Failed to generate lesson content from AI. Please check your internet connection or Gemini API key in the Secrets panel."
                }
            } catch (e: Exception) {
                _generationError.value = "Error generating content: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun submitQuizScore(topicId: Int, score: Int, remainingTimeSeconds: Int) {
        viewModelScope.launch {
            repository.saveQuizScore(topicId, score, remainingTimeSeconds)
        }
    }
}

