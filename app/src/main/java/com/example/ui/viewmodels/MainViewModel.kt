package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val grades = repository.grades.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects = _subjects.asStateFlow()

    private val _units = MutableStateFlow<List<UnitTable>>(emptyList())
    val units = _units.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics = _topics.asStateFlow()

    private val _currentLesson = MutableStateFlow<Lesson?>(null)
    val currentLesson = _currentLesson.asStateFlow()

    private val _objectives = MutableStateFlow<List<LearningObjective>>(emptyList())
    val objectives = _objectives.asStateFlow()

    private val _examples = MutableStateFlow<List<Example>>(emptyList())
    val examples = _examples.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions = _quizQuestions.asStateFlow()

    private val _topicProgress = MutableStateFlow<Progress?>(null)
    val topicProgress = _topicProgress.asStateFlow()

    val allProgress = repository.getAllProgress().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allUnits = repository.getAllUnits().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTopics = repository.getAllTopics().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allSubjects = repository.getAllSubjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _weakTopics = MutableStateFlow<List<Topic>>(emptyList())
    val weakTopics = _weakTopics.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements = _achievements.asStateFlow()

    val exams = repository.getAllExams().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentExamQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val currentExamQuestions = _currentExamQuestions.asStateFlow()

    private val _currentExamSession = MutableStateFlow<ExamSession?>(null)
    val currentExamSession = _currentExamSession.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Topic>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _studentProfile = MutableStateFlow<StudentProfile?>(null)
    val studentProfile = _studentProfile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getStudentProfile().collectLatest { 
                if (it == null) {
                    val newProfile = StudentProfile()
                    repository.saveStudentProfile(newProfile)
                    _studentProfile.value = newProfile
                } else {
                    _studentProfile.value = it 
                }
            }
        }
    }

    fun loadSubjects(gradeId: Int) {
        viewModelScope.launch {
            repository.getSubjectsByGrade(gradeId).collectLatest { _subjects.value = it }
        }
    }

    fun loadUnits(subjectId: Int) {
        viewModelScope.launch {
            repository.getUnitsBySubject(subjectId).collectLatest { _units.value = it }
        }
    }

    fun loadTopics(unitId: Int) {
        viewModelScope.launch {
            repository.getTopicsByUnit(unitId).collectLatest { _topics.value = it }
        }
    }

    fun loadLesson(topicId: Int) {
        viewModelScope.launch {
            repository.getLessonByTopic(topicId).collectLatest { _currentLesson.value = it }
        }
    }

    fun loadObjectives(topicId: Int) {
        viewModelScope.launch {
            repository.getObjectivesByTopic(topicId).collectLatest { _objectives.value = it }
        }
    }

    fun loadExamples(topicId: Int) {
        viewModelScope.launch {
            repository.getExamplesByTopic(topicId).collectLatest { _examples.value = it }
        }
    }

    fun loadQuizQuestions(topicId: Int) {
        viewModelScope.launch {
            repository.getQuizQuestionsByTopic(topicId).collectLatest { _quizQuestions.value = it }
        }
    }

    fun loadProgress(topicId: Int) {
        viewModelScope.launch {
            repository.getProgressByTopic(topicId).collectLatest { _topicProgress.value = it }
        }
    }

    fun loadWeakTopics() {
        viewModelScope.launch {
            repository.getWeakTopics().collectLatest { _weakTopics.value = it }
        }
    }

    fun loadAchievements() {
        viewModelScope.launch {
            repository.getAchievements().collectLatest { _achievements.value = it }
        }
    }

    fun loadExamQuestions(examId: Int) {
        viewModelScope.launch {
            val exam = repository.getExamById(examId)
            if (exam != null) {
                val questions = repository.getQuestionsByIds(exam.question_ids)
                _currentExamQuestions.value = questions
                
                // Load existing session if it exists
                val session = repository.getExamSession(examId)
                _currentExamSession.value = session
            }
        }
    }

    fun saveExamSession(session: ExamSession) {
        viewModelScope.launch {
            repository.saveExamSession(session)
        }
    }

    fun generateMockExam(subjectId: Int) {
        viewModelScope.launch {
            repository.createMockExam(subjectId)
        }
    }

    fun generateGradeExam(gradeId: Int) {
        viewModelScope.launch {
            repository.createNationalExamSimulation(gradeId)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                repository.searchTopics(query).collectLatest { _searchResults.value = it }
            }
        }
    }

    fun submitQuizScore(topicId: Int, score: Int) {
        viewModelScope.launch {
            repository.saveQuizScore(topicId, score)
            loadWeakTopics()
            loadAchievements()
        }
    }
}
