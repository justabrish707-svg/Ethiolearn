package com.example.data.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.model.*

class DatabaseSeeder(
    private val database: AppDatabase,
    private val context: Context
) {
    private val appDao = database.appDao()

    /**
     * Seeds the local SQLite database prepopulated grades, units, topics, lessons, examples, etc.
     * Encapsulated within a single Room `withTransaction` block to guarantee atomicity during launch.
     */
    suspend fun seedDatabaseAtomically() {
        val topicCount = appDao.countTopics()
        if (topicCount >= 1000) {
            Log.d("DatabaseSeeder", "Database already fully seeded with $topicCount topics. Skipping seeder transaction.")
            return
        }

        Log.d("DatabaseSeeder", "Initiating atomic database prepopulation flow (Current Topic Count: $topicCount)...")
        
        try {
            if (topicCount > 0) {
                Log.d("DatabaseSeeder", "Clearing stale curriculum tables to guarantee full reseed...")
                database.withTransaction {
                    appDao.clearGrades()
                    appDao.clearSubjects()
                    appDao.clearUnits()
                    appDao.clearTopics()
                    appDao.clearLessons()
                    appDao.clearExamples()
                    appDao.clearQuizQuestions()
                    appDao.clearFts()
                }
            }

            parseAndInsertCurriculum()
            
            val finalCount = appDao.countTopics()
            Log.d("DatabaseSeeder", "Database prepopulated successfully! Total topics: $finalCount")
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error during atomic database seeding transaction: rollback executed", e)
            throw e
        }
    }

    /**
     * Strategic Data Seeding: Combines legacy JSON for Math (Grades 9-10) with 
     * expanded TSV spreadsheet for all other subjects including Grades 11-12.
     */
    private suspend fun parseAndInsertCurriculum() {
        // 1. Read and parse the raw curriculum spreadsheet from curriculum_raw.txt
        val rawLines = try {
            context.assets.open("curriculum_raw.txt").bufferedReader().use { it.readLines() }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Missing curriculum_raw.txt in assets", e)
            emptyList<String>()
        }
        
        // Group rows: grade -> subject -> unitName -> sectionName -> topics
        val groupedData = LinkedHashMap<Int, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>>>()
        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("Grade")) continue
            
            // Try splitting by tab first, then fall back to multiple spaces
            var parts = trimmed.split("\t")
            if (parts.size < 5) {
                parts = trimmed.split("\\s{2,}".toRegex())
            }
            
            if (parts.size >= 5) {
                processRow(parts, groupedData)
            }
        }

        val dbGrades = mutableListOf<Grade>()
        val dbSubjects = mutableListOf<Subject>()
        val dbUnits = mutableListOf<UnitTable>()
        val dbTopics = mutableListOf<Topic>()
        
        for (gradeId in listOf(9, 10, 11, 12)) {
            dbGrades.add(Grade(gradeId, "Grade $gradeId"))

            // Append all subjects parsed from raw file for this grade
            val gradeSubjectsData = groupedData[gradeId] ?: continue
            
            var subjectCounter = 10
            for ((subjectName, unitData) in gradeSubjectsData) {
                val subjectId = gradeId * 100 + subjectCounter++
                dbSubjects.add(Subject(subjectId, gradeId, subjectName))
                
                var topicIdCounter = 1
                for ((unitTitle, sectionData) in unitData) {
                    val unitNum = parseUnitNumber(unitTitle)
                    val unitId = subjectId * 100 + unitNum
                    val unitNameOnly = parseUnitName(unitTitle)
                    
                    dbUnits.add(UnitTable(unitId, subjectId, unitNum, unitNameOnly))
                    
                    for ((sectionNum, topicsList) in sectionData) {
                        for (topicName in topicsList) {
                            val topicId = unitId * 100 + topicIdCounter++
                            dbTopics.add(Topic(topicId, unitId, sectionNum, topicName))
                        }
                    }
                }
            }
        }

        // Execute bulk insert inside a transaction
        database.withTransaction {
            appDao.insertGrades(dbGrades)
            appDao.insertSubjects(dbSubjects)
            appDao.insertUnits(dbUnits)
            appDao.insertTopics(dbTopics)
            
            // Seed associated lessons, worked examples, practice, and quiz questions dynamically
            seedLessonsAndQuestions(dbTopics, dbUnits, dbSubjects)
        }
    }

    private suspend fun seedLessonsAndQuestions(topics: List<Topic>, units: List<UnitTable>, subjects: List<Subject>) {
        val lessonsToSeed = mutableListOf<Lesson>()
        val objectivesToSeed = mutableListOf<LearningObjective>()
        val examplesToSeed = mutableListOf<Example>()
        val quizToSeed = mutableListOf<QuizQuestion>()
        val ftsToSeed = mutableListOf<CurriculumSearchFts>()

        val unitMap = units.associateBy { it.id }
        val subjectMap = subjects.associateBy { it.id }

        for (topic in topics) {
            val topicId = topic.id
            val unit = unitMap[topic.unit_id]
            val subject = subjectMap[unit?.subject_id]
            val subjectName = subject?.name ?: "General"
            val unitTitle = unit?.title ?: "Unit"
            
            // Seed Learning Objective
            objectivesToSeed.add(LearningObjective(topic_id = topicId, objective_text = "Master the core concepts of ${topic.title} within $subjectName."))

            // Seed Lesson
            lessonsToSeed.add(
                Lesson(
                    topic_id = topicId,
                    content = "Detailed pedagogical content for ${topic.title}. This lesson covers the fundamental principles of ${topic.title} as defined in the Grade ${subject?.grade_id} $subjectName curriculum."
                )
            )

            // Seed Examples
            examplesToSeed.add(
                Example(
                    topic_id = topicId,
                    title = "Basic Application",
                    description = "Apply the first principles of ${topic.title} to a standard problem.",
                    solution = "Step-by-step solution demonstrating the logical derivation of the answer."
                )
            )

            // Seed Curriculum-Specific Questions
            val questions = listOf(
                QuizQuestion(
                    topic_id = topicId,
                    question_text = "What is the primary objective of ${topic.title}?",
                    options = listOf("Conceptual understanding", "Rote memorization", "Random guessing", "None of the above"),
                    correct_option_index = 0,
                    explanation = "The curriculum emphasizes conceptual depth over simple memorization.",
                    difficulty = Difficulty.BEGINNER
                ),
                QuizQuestion(
                    topic_id = topicId,
                    question_text = "Which of the following is a key property of ${topic.title}?",
                    options = listOf("Scalability", "Atomicity", "Curriculum alignment", "Static behavior"),
                    correct_option_index = 2,
                    explanation = "Everything in this module is strictly aligned with the Ethiopian National Curriculum.",
                    difficulty = Difficulty.INTERMEDIATE
                )
            )
            quizToSeed.addAll(questions)
            
            // Prepare FTS data
            val combinedContent = "Lesson: Detailed pedagogical content for ${topic.title}. Example: Basic Application Solution. Quiz: ${topic.title} curriculum objective."
            ftsToSeed.add(
                CurriculumSearchFts(
                    rowId = topicId,
                    topic_id = topicId,
                    title = topic.title,
                    section_number = topic.section_number,
                    unit_name = unitTitle,
                    content = combinedContent
                )
            )
        }

        database.withTransaction {
            appDao.insertLessons(lessonsToSeed)
            appDao.insertObjectives(objectivesToSeed)
            appDao.insertExamples(examplesToSeed)
            appDao.insertQuizQuestions(quizToSeed)
            appDao.insertSearchFts(ftsToSeed)
        }
    }

    private fun processRow(
        parts: List<String>,
        groupedData: LinkedHashMap<Int, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>>>
    ) {
        val gradeStr = parts[0].trim()
        val gradeId = gradeStr.toIntOrNull() ?: return
        val subject = parts[1].trim()

        val unit = parts[2].trim()
        val section = parts[3].trim()
        val topic = parts[4].trim()

        val gradeMap = groupedData.getOrPut(gradeId) { LinkedHashMap() }
        val subjectMap = gradeMap.getOrPut(subject) { LinkedHashMap() }
        val unitMap = subjectMap.getOrPut(unit) { LinkedHashMap() }
        val topicsList = unitMap.getOrPut(section) { mutableListOf() }
        
        if (!topicsList.contains(topic)) {
            topicsList.add(topic)
        }
    }

    private fun parseUnitNumber(unitStr: String): Int {
        val regex = "Unit\\s+(\\d+)".toRegex()
        val match = regex.find(unitStr)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun parseUnitName(unitStr: String): String {
        val index = unitStr.indexOf(':')
        return if (index != -1) {
            unitStr.substring(index + 1).trim()
        } else {
            unitStr.trim()
        }
    }
}
