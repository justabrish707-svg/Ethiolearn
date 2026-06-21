package com.example.data.repository

import com.example.data.db.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    val grades: Flow<List<Grade>> = appDao.getGrades()

    fun getSubjectsByGrade(gradeId: Int): Flow<List<Subject>> = appDao.getSubjectsByGrade(gradeId)

    fun getUnitsBySubject(subjectId: Int): Flow<List<UnitTable>> = appDao.getUnitsBySubject(subjectId)

    fun getTopicsByUnit(unitId: Int): Flow<List<Topic>> = appDao.getTopicsByUnit(unitId)

    suspend fun getTopicById(topicId: Int): Topic? = appDao.getTopicById(topicId)
    
    suspend fun getUnitById(unitId: Int): UnitTable? = appDao.getUnitById(unitId)

    fun getLessonByTopic(topicId: Int): Flow<Lesson?> = appDao.getLessonByTopic(topicId)

    fun getExamplesByTopic(topicId: Int): Flow<List<Example>> = appDao.getExamplesByTopic(topicId)

    fun getPracticeQuestionsByTopic(topicId: Int): Flow<List<PracticeQuestion>> = appDao.getPracticeQuestionsByTopic(topicId)

    fun getProgressByTopic(topicId: Int): Flow<Progress?> = appDao.getProgressByTopic(topicId)

    suspend fun updateProgress(progress: Progress) {
        appDao.insertProgress(progress)
    }

    suspend fun initializePrepopulatedData() {
        if (appDao.countGrades() == 0) {
            val grades = listOf(
                Grade(9, "Grade 9"),
                Grade(10, "Grade 10")
            )
            appDao.insertGrades(grades)

            val subjects = listOf(
                Subject(1, 9, "Mathematics"),
                Subject(2, 10, "Mathematics")
            )
            appDao.insertSubjects(subjects)

            val units = listOf(
                UnitTable(1, 1, 1, "The Number System"),
                UnitTable(2, 1, 2, "Equations and Inequalities"),
                UnitTable(3, 2, 1, "Relations and Functions"),
                UnitTable(4, 2, 2, "Polynomial Functions")
            )
            appDao.insertUnits(units)

            val topics = listOf(
                Topic(1, 1, "Real Numbers"),
                Topic(2, 1, "Rationalization"),
                Topic(3, 3, "Introduction to Relations"),
                Topic(4, 3, "Types of Functions")
            )
            appDao.insertTopics(topics)

            val lessons = listOf(
                Lesson(1, 1, "Real numbers include all rational and irrational numbers. They can be represented on a number line.", "Rational Numbers, Irrational Numbers, Real Line", "Every point on the number line represents a unique real number.", "N ⊂ Z ⊂ Q ⊂ R"),
                Lesson(3, 3, "A relation is a subset of the Cartesian product of two sets. It pairs elements from a domain to a range.", "Domain, Range, Cartesian Product", "Not all relations are functions.", "R ⊆ A × B")
            )
            appDao.insertLessons(lessons)

            val examples = listOf(
                Example(1, 1, "Is √2 a rational number?", "Step 1: A rational can be expressed as p/q.\nStep 2: √2 cannot be expressed as a simple fraction.\nStep 3: Therefore, √2 is irrational."),
                Example(2, 3, "Find domain of R = {(1,2), (3,4)}", "Step 1: Domain is the set of all first elements.\nStep 2: D = {1, 3}.")
            )
            appDao.insertExamples(examples)

            val practiceQuestions = listOf(
                PracticeQuestion(1, 3, "Easy", "What is the domain of {(5,1), (6,2)}?", "{5, 6}", "The domain consists of the first element in each ordered pair."),
                PracticeQuestion(2, 3, "Medium", "If a relation maps every element of A to exactly one element of B, is it a function?", "Yes", "This is the exact definition of a function.")
            )
            appDao.insertPracticeQuestions(practiceQuestions)
        }
    }
}
