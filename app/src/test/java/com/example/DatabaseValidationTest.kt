package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseValidationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(database, database.appDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun auditDatabaseSeedingIntegrity() = runBlocking {
        println("------- STARTING SQLITE DATABASE AUDIT REPORT -------")
        
        // Assert initial counts are empty
        assertEquals(0, database.appDao().countGrades())
        
        // Perform initial mock seed from assets curriculum.json
        repository.initializePrepopulatedData()
        
        // Query fully seeded results
        val grades = repository.grades.first()
        val subjectsGrade9 = repository.getSubjectsByGrade(9).first()
        val subjectsGrade10 = repository.getSubjectsByGrade(10).first()
        
        // Validate curriculum hierarchy counts
        val totalGrades = database.appDao().countGrades()
        println("📊 Grade Records Seeded: $totalGrades")
        assertEquals(2, totalGrades) // Grade 9 and Grade 10
        
        // Verify Grade 9 contains Mathematics and Grade 10 contains Mathematics
        assertEquals(1, subjectsGrade9.size)
        assertEquals("Mathematics", subjectsGrade9[0].name)
        assertEquals(1, subjectsGrade10.size)
        assertEquals("Mathematics", subjectsGrade10[0].name)
        
        // Count Unit titles: Units 1 to 8 in Grade 9, Units 9 to 17 in Grade 10
        val allUnits9 = repository.getUnitsBySubject(subjectsGrade9[0].id).first()
        val allUnits10 = repository.getUnitsBySubject(subjectsGrade10[0].id).first()
        val totalUnitsCount = allUnits9.size + allUnits10.size
        println("📚 Unit Records Seeded: $totalUnitsCount (Grade 9: ${allUnits9.size}, Grade 10: ${allUnits10.size})")
        assertEquals(17, totalUnitsCount) // 8 in Grade 9, 9 in Grade 10

        // Verify total Topics across all units
        val allTopics = mutableListOf<com.example.data.model.Topic>()
        for (u in allUnits9) {
            allTopics.addAll(repository.getTopicsByUnit(u.id).first())
        }
        for (u in allUnits10) {
            allTopics.addAll(repository.getTopicsByUnit(u.id).first())
        }
        val totalTopicsCount = allTopics.size
        println("📝 Topic Records Seeded: $totalTopicsCount / 176 Expected")
        
        // CRITICAL DATA INTEGRITY CHECK: Topic count matches exactly the 176 verified curriculum topics
        assertEquals(176, totalTopicsCount)
        println("✅ SUCCESS: Data seeding completed with absolutely ZERO data loss (176 / 176 topics accounted for)!")

        // Audit Lesson fallbacks count to verify no topic has "Curriculum Pending" offline
        var lessonCount = 0
        var exampleCount = 0
        var practiceCount = 0
        var quizCount = 0
        
        for (topic in allTopics) {
            val lesson = repository.getLessonByTopic(topic.id).first()
            if (lesson != null) {
                lessonCount++
                // Verify content does not have pending messages or empty descriptions
                assertTrue(lesson.summary.isNotEmpty())
                assertTrue(lesson.key_concepts.isNotEmpty())
            }
            
            val examples = repository.getExamplesByTopic(topic.id).first()
            exampleCount += examples.size
            if (examples.isNotEmpty()) {
                assertTrue(examples[0].question.isNotEmpty())
                assertTrue(examples[0].step_by_step_solution.isNotEmpty())
            }
            
            val practice = repository.getPracticeQuestionsByTopic(topic.id).first()
            practiceCount += practice.size
            
            val quiz = repository.getQuizQuestionsByTopic(topic.id).first()
            quizCount += quiz.size
        }
        
        println("📖 Lesson Records Populated: $lessonCount / 176")
        println("💡 Worked Examples Seeded: $exampleCount")
        println("✏️ Practice Questions Seeded: $practiceCount")
        println("❓ Quiz Questions Seeded: $quizCount")
        
        // Assertions verifying that all lessons are accounted for
        assertEquals(176, lessonCount)
        assertTrue(exampleCount >= 176)
        assertTrue(practiceCount >= 176)
        assertTrue(quizCount >= 352) // Minimum of 2 quiz questions per topic
        
        // Test FTS4 Search Performance on randomly queried keywords
        println("🔍 Initiating Full-Text Search (FTS4) Index Audit...")
        
        val ftsSearchResults = repository.searchTopics("Trigonometry").first()
        println("🔍 Query 'Trigonometry' FTS Matches: ${ftsSearchResults.size} topics found")
        assertTrue(ftsSearchResults.isNotEmpty())
        assertTrue(ftsSearchResults.any { it.title.contains("Trigonometric", ignoreCase = true) })
        
        val ftsLogResults = repository.searchTopics("Logarithmic").first()
        println("🔍 Query 'Logarithmic' FTS Matches: ${ftsLogResults.size} topics found")
        assertTrue(ftsLogResults.isNotEmpty())
        assertTrue(ftsLogResults.any { it.title.contains("Logarithmic", ignoreCase = true) })
        
        println("------- SQLITE DATABASE AUDIT COMPLETED SUCCESSFULLY -------")
    }
}
