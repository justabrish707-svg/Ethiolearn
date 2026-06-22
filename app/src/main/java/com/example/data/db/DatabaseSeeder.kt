package com.example.data.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.model.*
import com.example.data.repository.CurriculumJsonDto
import kotlinx.serialization.json.Json

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
        val gradeCount = appDao.countGrades()
        if (gradeCount >= 4) {
            Log.d("DatabaseSeeder", "Database already fully seeded with 4+ grades. Skipping seeder transaction.")
            return
        }

        Log.d("DatabaseSeeder", "Initiating atomic database prepopulation flow (Current Grade Count: $gradeCount)...")
        
        try {
            if (gradeCount > 0) {
                Log.d("DatabaseSeeder", "Clearing stale curriculum tables to guarantee full reseed...")
                database.withTransaction {
                    appDao.clearGrades()
                    appDao.clearSubjects()
                    appDao.clearUnits()
                    appDao.clearTopics()
                    appDao.clearLessons()
                    appDao.clearExamples()
                    appDao.clearPracticeQuestions()
                    appDao.clearQuizQuestions()
                    appDao.clearFts()
                }
            }

            parseAndInsertCurriculum()
            
            Log.d("DatabaseSeeder", "Database prepopulated and atomic transaction committed successfully!")
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
        // 1. Read the pre-existing curriculum.json to harvest authentic Grade 9 & Grade 10 Math subjects
        val jsonString = context.assets.open("curriculum.json").bufferedReader().use { it.readText() }
        val format = Json { ignoreUnknownKeys = true }
        val originalData = format.decodeFromString<CurriculumJsonDto>(jsonString)
        
        val originalGrade9Math = originalData.grades
            .find { it.grade_id == 9 }?.subjects?.find { it.name.contains("Math", ignoreCase = true) }
        val originalGrade10Math = originalData.grades
            .find { it.grade_id == 10 }?.subjects?.find { it.name.contains("Math", ignoreCase = true) }

        // 2. Read and parse the raw curriculum spreadsheet from curriculum_raw.txt
        val rawLines = context.assets.open("curriculum_raw.txt").bufferedReader().use { it.readLines() }
        
        // Group rows: grade -> subject -> unitName -> sectionName -> topics
        val groupedData = LinkedHashMap<Int, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>>>()
        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("Grade\t")) continue
            val parts = trimmed.split("\t")
            if (parts.size < 5) {
                val spacesParts = trimmed.split("\\s{2,}".toRegex())
                if (spacesParts.size >= 5) {
                    processRow(spacesParts, groupedData)
                }
                continue
            }
            processRow(parts, groupedData)
        }

        val dbGrades = mutableListOf<Grade>()
        val dbSubjects = mutableListOf<Subject>()
        val dbUnits = mutableListOf<UnitTable>()
        val dbTopics = mutableListOf<Topic>()
        val dbFts = mutableListOf<CurriculumSearchFts>()
        
        var ftsIdCounter = 1

        // Deterministic Subject Index Mappings to prevent overlapping IDs
        val gradeSubjectsList = mapOf(
            9 to listOf("Amharic", "Biology", "Chemistry", "Citizenship Education", "Economics", "English for Ethiopia", "Geography", "Health & Physical Education", "History", "Information Technology", "Physics"),
            10 to listOf("Amharic", "Biology", "Chemistry", "Citizenship Education", "Economics", "English for Ethiopia", "Geography", "Health & Physical Education", "History", "Information Technology", "Physics"),
            11 to listOf("Amharic", "Agriculture", "Biology", "Chemistry", "Citizenship Education", "Economics", "English for Ethiopia", "Geography", "Health & Physical Education", "History", "Information Technology", "Mathematics", "Physics"),
            12 to listOf("Amharic", "Agriculture", "Biology", "Chemistry", "Citizenship Education", "Economics", "English for Ethiopia", "Geography", "Health & Physical Education", "History", "Information Technology", "Mathematics", "Physics")
        )

        for (gradeId in listOf(9, 10, 11, 12)) {
            dbGrades.add(Grade(gradeId, "Grade $gradeId"))
            
            // Add Grade 9 & 10 Mathematics from original JSON
            if (gradeId == 9 && originalGrade9Math != null) {
                dbSubjects.add(Subject(originalGrade9Math.subject_id, 9, originalGrade9Math.name))
                for (u in originalGrade9Math.units) {
                    dbUnits.add(UnitTable(u.unit_id, originalGrade9Math.subject_id, u.number, u.name))
                    for (sec in u.sections) {
                        for (t in sec.topics) {
                            dbTopics.add(Topic(t.topic_id, u.unit_id, sec.section_number, t.name))
                            dbFts.add(CurriculumSearchFts(ftsIdCounter++, t.topic_id, t.name, sec.section_number, u.name))
                        }
                    }
                }
            } else if (gradeId == 10 && originalGrade10Math != null) {
                dbSubjects.add(Subject(originalGrade10Math.subject_id, 10, originalGrade10Math.name))
                for (u in originalGrade10Math.units) {
                    dbUnits.add(UnitTable(u.unit_id, originalGrade10Math.subject_id, u.number, u.name))
                    for (sec in u.sections) {
                        for (t in sec.topics) {
                            dbTopics.add(Topic(t.topic_id, u.unit_id, sec.section_number, t.name))
                            dbFts.add(CurriculumSearchFts(ftsIdCounter++, t.topic_id, t.name, sec.section_number, u.name))
                        }
                    }
                }
            }

            // Append other subjects parsed from TSV
            val gradeSubjectsData = groupedData[gradeId] ?: continue
            val subjectsList = gradeSubjectsList[gradeId] ?: emptyList()
            
            for (subIdx in subjectsList.indices) {
                val subjectName = subjectsList[subIdx]
                val unitData = gradeSubjectsData[subjectName] ?: continue
                
                val subjectId = gradeId * 100 + (subIdx + 10)
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
                            dbFts.add(CurriculumSearchFts(ftsIdCounter++, topicId, topicName, sectionNum, unitNameOnly))
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
            appDao.insertSearchFts(dbFts)
            
            // Seed associated lessons, worked examples, practice, and quiz questions dynamically
            seedLessonsAndQuestions(dbTopics, dbUnits)
        }
    }

    private suspend fun seedLessonsAndQuestions(topics: List<Topic>, units: List<UnitTable>) {
        val lessonsToSeed = mutableListOf<Lesson>()
        val examplesToSeed = mutableListOf<Example>()
        val practiceToSeed = mutableListOf<PracticeQuestion>()
        val quizToSeed = mutableListOf<QuizQuestion>()

        val unitMap = units.associateBy { it.id }

        for (topic in topics) {
            val topicId = topic.id
            val unit = unitMap[topic.unit_id]
            val unitTitle = unit?.title ?: "Standard Unit"

            when (topicId) {
                1 -> {
                    lessonsToSeed.add(
                        Lesson(1, 1, 
                            summary = "A rational number is any number that can be expressed as the ratio p/q of two integers, where the denominator q is not equal to zero.",
                            key_concepts = "Rational Numbers, Fraction Form (p/q), Non-zero Denominator, Set notation Q.",
                            important_notes = "All integers are rational numbers because any integer n can be written as n/1. Decimals that terminate or repeat are also rational.",
                            formulas = "Q = {p/q : p, q ∈ Z, q ≠ 0}"
                        )
                    )
                    examplesToSeed.add(Example(1, 1, "Is √2 a rational number?", "Step 1: A rational can be expressed as p/q.\nStep 2: √2 cannot be expressed as a simple fraction.\nStep 3: Therefore, √2 is irrational."))
                    practiceToSeed.add(PracticeQuestion(1, 1, "Easy", "Convert 3/4 into decimals.", "0.75", "Divide 3 by 4, which gives 0.75 as a terminating rational decimal."))
                    
                    val quiz = listOf(
                        QuizQuestion(1, 1, "Which of the following is a rational number?", "[\"\\u221a2\", \"\\u03c0\", \"3/5\", \"\\u221a3\"]", 2),
                        QuizQuestion(2, 1, "Is the decimal 0.333... (repeating) rational?", "[\"Yes\", \"No\"]", 0),
                        QuizQuestion(3, 1, "Which of the following describes a rational number?", "[\"Can be written as p/q where q is not 0\", \"Must contain radicals\", \"Are never integers\", \"None of the above\"]", 0),
                        QuizQuestion(4, 1, "Is 0 a rational number?", "[\"Yes\", \"No\"]", 0),
                        QuizQuestion(5, 1, "Can a rational number have a negative denominator?", "[\"Yes\", \"No\"]", 0),
                        QuizQuestion(6, 1, "Which statement is true?", "[\"Every integer is a rational number\", \"Every rational number is an integer\", \"Rational numbers are always positive\", \"Irrational numbers are rational\"]", 0),
                        QuizQuestion(7, 1, "What is the decimal representation of 1/4?", "[\"0.25\", \"0.4\", \"0.5\", \"0.2\"]", 0),
                        QuizQuestion(8, 1, "Is \\u221a16 a rational number?", "[\"Yes, because it equals 4\", \"No, because it is inside a radical\", \"Maybe\", \"None of the above\"]", 0),
                        QuizQuestion(9, 1, "What is the additive inverse of 2/3?", "[\"3/2\", \"-2/3\", \"-3/2\", \"1/2\"]", 1),
                        QuizQuestion(10, 1, "A terminating decimal is always: ", "[\"Rational\", \"Irrational\", \"An integer\", \"None of the above\"]", 0)
                    )
                    quizToSeed.addAll(quiz)
                }
                4 -> {
                    lessonsToSeed.add(
                        Lesson(4, 4,
                            summary = "An irrational number is a real number that cannot be written as a simple fraction p/q, where p and q are integers and q is not zero. Their decimal representations are non-terminating and non-repeating.",
                            key_concepts = "Irrational Numbers, Non-repeating decimal, Non-terminating decimal, Radicals of non-perfect squares.",
                            important_notes = "The numbers π (pi) and e are famous irrational numbers. Square roots of prime numbers like √2, √3, √5 are always irrational.",
                            formulas = "I = R \\ Q \n (Real Numbers minus Rational Numbers)"
                        )
                    )
                    examplesToSeed.add(Example(3, 4, "Is √5 rational or irrational?", "Step 1: √5 is the square root of a prime number.\nStep 2: Its decimal value is 2.2360679... which never terminates or repeats.\nStep 3: Therefore, √5 is an irrational number."))
                    examplesToSeed.add(Example(4, 4, "Is 0.1010010001... irrational?", "Step 1: Note the pattern of zeros increasing between ones.\nStep 2: This means the decimal is non-repeating yet keeps going forever (non-terminating).\nStep 3: Thus, it cannot be written as a integer ratio p/q, making it irrational."))
                    practiceToSeed.add(PracticeQuestion(3, 4, "Easy", "Which set of numbers does √7 belong to?", "Irrational Numbers", "7 is not a perfect square, so its square root √7 is a non-terminating, non-repeating irrational number."))
                    practiceToSeed.add(PracticeQuestion(4, 4, "Medium", "Is 3.14159... (pi) rational or irrational?", "Irrational", "pi is a non-terminating, non-repeating decimal, meaning it is irrational."))
                    
                    val quiz = listOf(
                        QuizQuestion(21, 4, "Which of the following is an irrational number?", "[\"\\u221a9\", \"\\u221a2\", \"0.5\", \"2/3\"]", 1),
                        QuizQuestion(22, 4, "What is the decimal expansion of an irrational number?", "[\"Terminating\", \"Repeating\", \"Non-terminating and non-repeating\", \"None of the above\"]", 2),
                        QuizQuestion(23, 4, "Is √2 + √3 rational?", "[\"Yes\", \"No\", \"Depends\", \"None of the above\"]", 1),
                        QuizQuestion(24, 4, "Is the quotient of two irrational numbers always irrational?", "[\"Yes, always\", \"No, it can sometimes be rational (e.g. \\u221a8 / \\u221a2 = 2)\", \"Never\", \"Depends on signs\"]", 1),
                        QuizQuestion(25, 4, "Is 2\u03c0 a rational or irrational number?", "[\"Rational\", \"Irrational\"]", 1)
                    )
                    quizToSeed.addAll(quiz)
                }
                19 -> {
                    lessonsToSeed.add(
                        Lesson(19, 19,
                            summary = "A linear equation in one variable is an equation that can be written in the form ax + b = 0, where a and b are real numbers and a ≠ 0. It has exactly one solution.",
                            key_concepts = "Linear Equation, Variable, Solution, Balance Method, Isolating the variable.",
                            important_notes = "Always perform the same operation on both sides of the equation to maintain equality. If a = 0 and b ≠ 0, there is no solution.",
                            formulas = "ax + b = 0 \n x = -b/a"
                        )
                    )
                    examplesToSeed.add(Example(5, 19, "Solve 3x + 5 = 17.", "Step 1: Subtract 5 from both sides: 3x = 12.\nStep 2: Divide both sides by 3: x = 4.\nStep 3: Verification: 3(4) + 5 = 17, which is correct!"))
                    examplesToSeed.add(Example(6, 19, "Solve 2(x - 3) = 4.", "Step 1: Distribute 2 inside the parentheses: 2x - 6 = 4.\nStep 2: Add 6 to both sides: 2x = 10.\nStep 3: Divide by 2: x = 5."))
                    practiceToSeed.add(PracticeQuestion(5, 19, "Easy", "Solve for y: 5y - 3 = 12.", "y = 3", "Add 3 to get 5y = 15, then divide by 5 to get y = 3."))
                    practiceToSeed.add(PracticeQuestion(6, 19, "Hard", "What is the solution to 4x + 7 = 4x + 9?", "No Solution", "Subtracting 4x from both sides gives 7 = 9, which is a contradiction. Hence, no solution exists."))
                    
                    val quiz = listOf(
                        QuizQuestion(31, 19, "What is the degree of a linear equation in one variable?", "[\"0\", \"1\", \"2\", \"3\"]", 1),
                        QuizQuestion(32, 19, "Solve: 2x - 10 = 0", "[\"x = 5\", \"x = -5\", \"x = 10\", \"x = 2\"]", 0),
                        QuizQuestion(33, 19, "If a linear equation simplifies to 5 = 5, how many solutions are there?", "[\"None\", \"Exactly one\", \"Infinitely many\", \"Two\"]", 2),
                        QuizQuestion(34, 19, "Solve: 3(x + 2) = 15", "[\"x = 3\", \"x = 4\", \"x = 5\", \"x = 2\"]", 0),
                        QuizQuestion(35, 19, "Solve: x / 3 + 2 = 7", "[\"x = 15\", \"x = 9\", \"x = 5\", \"x = 21\"]", 0)
                    )
                    quizToSeed.addAll(quiz)
                }
                54 -> {
                    lessonsToSeed.add(
                        Lesson(54, 54,
                            summary = "Trigonometric ratios express the relationship between the angles and lengths of sides in a right-angled triangle. The three primary ratios are sine (sin), cosine (cos), and tangent (tan).",
                            key_concepts = "Hypotenuse, Opposite side, Adjacent side, SOH-CAH-TOA, Right Triangle.",
                            important_notes = "Sine is opposite/hypotenuse, Cosine is adjacent/hypotenuse, and Tangent is opposite/adjacent. SOH-CAH-TOA is an easy mnemonic!",
                            formulas = "sin(θ) = Opposite / Hypotenuse \n cos(θ) = Adjacent / Hypotenuse \n tan(θ) = Opposite / Adjacent"
                        )
                    )
                    examplesToSeed.add(Example(7, 54, "In a right triangle, the opposite side to angle θ is 3 cm and the hypotenuse is 5 cm. Find sin(θ).", "Step 1: Check the formula: sin(θ) = Opposite / Hypotenuse.\nStep 2: Opposite side = 3, Hypotenuse = 5.\nStep 3: sin(θ) = 3/5 = 0.6."))
                    examplesToSeed.add(Example(8, 54, "If the adjacent side is 4 cm and hypotenuse is 5 cm, find tan(θ).", "Step 1: Using Pythagoras theorem: Opposite² + Adjacent² = Hypotenuse².\nStep 2: Opposite² + 4² = 5² => Opposite = 3 cm.\nStep 3: tan(θ) = Opposite / Adjacent = 3/4 = 0.75."))
                    practiceToSeed.add(PracticeQuestion(7, 54, "Easy", "In a right triangle with opposite = 5 and adjacent = 12, what is the length of the hypotenuse?", "13", "By Pythagoras theorem, Hypotenuse = √(5² + 12²) = √(25 + 144) = √169 = 13."))
                    practiceToSeed.add(PracticeQuestion(8, 54, "Medium", "If cos(θ) = 8/10, what is this ratio in simplest form?", "4/5", "Divide the numerator and denominator by their greatest common divisor 2: 8/2 / 10/2 = 4/5."))
                    
                    val quiz = listOf(
                        QuizQuestion(41, 54, "Which trigonometric ratio is defined as Opposite / Hypotenuse?", "[\"Sine\", \"Cosine\", \"Tangent\", \"Secant\"]", 0),
                        QuizQuestion(42, 54, "In the SOH-CAH-TOA mnemonic, what does the C stand for?", "[\"Cosecant\", \"Cosine\", \"Cotangent\", \"Central\"]", 1),
                        QuizQuestion(43, 54, "If the adjacent side is 12 and hypotenuse is 13, what is cos(\u03b8)?", "[\"12/13\", \"5/13\", \"13/12\", \"5/12\"]", 0),
                        QuizQuestion(44, 54, "What is sin(\u03b8) / cos(\u03b8) equivalent to?", "[\"tan(\u03b8)\", \"cot(\u03b8)\", \"sec(\u03b8)\", \"1\"]", 0),
                        QuizQuestion(45, 54, "Can the hypotenuse ever be shorter than the opposite side in a right triangle?", "[\"Yes\", \"No\", \"Sometimes\", \"Only if angle is 90 degrees\"]", 1)
                    )
                    quizToSeed.addAll(quiz)
                }
                91 -> {
                    lessonsToSeed.add(
                        Lesson(91, 91,
                            summary = "The domain of an inverse relation R⁻¹ is the range of the original relation R. The range of R⁻¹ is the domain of R.",
                            key_concepts = "Domain of Inverse, Range of Inverse, Coordinate Swapping, Ordered Pairs (y, x).",
                            important_notes = "To find the domain and range of an inverse relation, you simply swap the sets of the domain and range of the relation.",
                            formulas = "Dom(R⁻¹) = Ran(R) \n Ran(R⁻¹) = Dom(R)"
                        )
                    )
                    examplesToSeed.add(Example(2, 91, "If R = {(1,2), (3,4)}, find its inverse relation R⁻¹.", "Step 1: Swapping coordinate order of each ordered pair.\nStep 2: R⁻¹ = {(2,1), (4,3)}.\nStep 3: Thus, domain of R⁻¹ becomes {2, 4} and its range becomes {1, 3}."))
                    practiceToSeed.add(PracticeQuestion(2, 91, "Medium", "If a relation maps every element of A to exactly one element of B, is its inverse necessarily a function?", "No", "No. For example, if R = {(1,2), (3,2)}, the inverse is {(2,1), (2,3)}, which is not a function as input 2 maps to multiple outputs."))
                    
                    val quiz = listOf(
                        QuizQuestion(11, 91, "If relation R is {(1,2), (3,4)}, what is the domain of inverse relation R\u207b\u00b9?", "[\"{1, 3}\", \"{2, 4}\", \"{1, 2}\", \"{2, 3}\"]", 1),
                        QuizQuestion(12, 91, "If the domain of relation R is {x | x \u2265 0}, what is the range of R\u207b\u00b9?", "[\"{y | y \u2265 0}\", \"{y | y < 0}\", \"All real numbers\", \"Empty set\"]", 0),
                        QuizQuestion(13, 91, "To obtain the inverse relation, we swap: ", "[\"Coordinates x and y\", \"Plusses and minuses\", \"Domain elements only\", \"None of the above\"]", 0),
                        QuizQuestion(14, 91, "The domain of R is the same as: ", "[\"The domain of R\u207b\u00b9\", \"The range of R\u207b\u00b9\", \"The domain of functions\", \"None of the above\"]", 1),
                        QuizQuestion(15, 91, "If R is defined by y = 2x, what is the equation of R\u207b\u00b9?", "[\"y = x/2\", \"y = -2x\", \"y = 2/x\", \"y = x - 2\"]", 0),
                        QuizQuestion(16, 91, "If R = {(a,b)}, what is R\u207b\u00b9?", "[\"{(b,a)}\", \"{(a,b)}\", \"{(a,a)}\", \"{(b,b)}\"]", 0),
                        QuizQuestion(17, 91, "Inverse of inverse relation (R\u207b\u00b9)\u207b\u00b9 is equal to: ", "[\"R\", \"R\u207b\u00b9\", \"Empty relation\", \"Diagonal relation\"]", 0),
                        QuizQuestion(18, 91, "If relation R has domain {1, 2} and range {3, 4}, what is Ran(R\u207b\u00b9)?", "[\"{1, 2}\", \"{3, 4}\", \"{1, 3}\", \"{2, 4}\"]", 0),
                        QuizQuestion(19, 91, "Can a non-function relation have an inverse?", "[\"Yes, any relation has an inverse\", \"No, only functions\", \"Only if it is one-to-one\", \"None of the above\"]", 0),
                        QuizQuestion(20, 91, "If R is y = x + 3, the inverse relation R\u207b\u00b9 has equation: ", "[\"y = x - 3\", \"y = 3 - x\", \"y = -x - 3\", \"y = x/3\"]", 0)
                    )
                    quizToSeed.addAll(quiz)
                }
                else -> {
                    lessonsToSeed.add(generateDynamicLesson(topic, unitTitle))
                    examplesToSeed.add(generateDynamicExample(topic))
                    practiceToSeed.add(generateDynamicPracticeQuestion(topic))
                    quizToSeed.addAll(generateDynamicQuizQuestions(topic))
                }
            }
        }

        appDao.insertLessons(lessonsToSeed)
        appDao.insertExamples(examplesToSeed)
        appDao.insertPracticeQuestions(practiceToSeed)
        appDao.insertQuizQuestions(quizToSeed)
    }

    private fun generateDynamicLesson(topic: Topic, unitTitle: String): Lesson {
        val topicTitle = topic.title
        val summary: String
        val keyConcepts: String
        val importantNotes: String
        val formulas: String
        
        when {
            topicTitle.contains("Logarithmic", ignoreCase = true) || topicTitle.contains("Logarithm", ignoreCase = true) -> {
                summary = "A logarithm is the mathematical exponent to which a specified base must be raised to yield a given value. This concept is vital for reversing exponential growth functions."
                keyConcepts = "Logarithmes, Log Base, Euler's number, Change of Base, Natural Log (ln), Common Log (log10)."
                importantNotes = "Always verify that your logarithm variables are strictly positive. The base value b must be greater than zero and not equal to one."
                formulas = "log_b(m * n) = log_b(m) + log_b(n)\nlog_b(m / n) = log_b(m) - log_b(n)\nlog_b(m^k) = k * log_b(m)\nlog_b(x) = log_c(x) / log_c(b)"
            }
            topicTitle.contains("Exponential", ignoreCase = true) || topicTitle.contains("Exponent", ignoreCase = true) -> {
                summary = "Exponential functions involve a constant base raised to a variable power, expressed as f(x) = a * b^x. They model compounding growth and decay rates in science and finance."
                keyConcepts = "Exponential Form, Multiplication of Exponents, Fractional Powers, Exponential Decay, Growth Asymptote."
                importantNotes = "Verify the fractional rules of exponents. Negative exponents indicate taking reciprocal values of bases."
                formulas = "a^x * a^y = a^(x+y)\n(a^x)^y = a^(x*y)\na^(-n) = 1 / a^n\na^(1/n) = ⁿ√a"
            }
            topicTitle.contains("Vector", ignoreCase = true) -> {
                summary = "Vectors represent multi-dimensional mathematical entities possessing both numerical magnitude and spatial direction. They are highly relevant for modeling force, velocity, and physics arrays."
                keyConcepts = "Scalars, Vectors, Position Vector, Head-to-Tail Rule, Vector Magnitude, Coordinate Component Form, Dot Product."
                importantNotes = "Keep strict track of signs (+/-) when resolving vector operations algebraically. Coordinate directions dictate addition signs."
                formulas = "Vector: V = xi + yj\nMagnitude: |V| = √(x² + y²)\nDirection Angle: θ = tan⁻¹(y / x)\nProjection: V_x = |V|cos(θ), V_y = |V|sin(θ)"
            }
            topicTitle.contains("Probability", ignoreCase = true) || topicTitle.contains("Sample Space", ignoreCase = true) || topicTitle.contains("Permutation", ignoreCase = true) || topicTitle.contains("Binomial", ignoreCase = true) || topicTitle.contains("Combinations", ignoreCase = true) -> {
                summary = "Probability quantifies event likelihood within a structured sample space. This detailed math curriculum lesson details permutation sequences, combination sets, and Bernoulli trial cycles."
                keyConcepts = "Sample Space, Random Event, Independent Events, Factorials, Permutations (nPr), Combinations (nCr), Bernoulli Trials."
                importantNotes = "Remember that the sum of all mutually exclusive occurrences in a sample space is strictly 1. Order matters in configurations for Permutations."
                formulas = "P(A) = n(A) / n(S)\nP(A or B) = P(A) + P(B) - P(A and B)\nnPr = n! / (n - r)!\nnCr = n! / (r! * (n - r)!)"
            }
            topicTitle.contains("Circle", ignoreCase = true) || topicTitle.contains("Tangent", ignoreCase = true) || topicTitle.contains("Secant", ignoreCase = true) || topicTitle.contains("Chords", ignoreCase = true) -> {
                summary = "Circle geometry governs relationships of shapes centered around a single planar locus. We analyse the properties of chords, intersecting secants, tangents, and subtended angles."
                keyConcepts = "Chords, Inscribed arcs, Tangent Perpendicularity, Alternate angle subtensions, Tangent length equalities."
                importantNotes = "The radius of a circle is always normal (perpendicular) to a tangent at the touching point. Inscribed angles bordering half a circle are right angles (90 degrees)."
                formulas = "Circle Equation: (x - h)² + (y - k)² = r²\nInscribed Angle = 0.5 * Central Angle\nIntersecting Chords: AP * PB = CP * PD"
            }
            topicTitle.contains("Polynomial", ignoreCase = true) || topicTitle.contains("Remainder", ignoreCase = true) || topicTitle.contains("Factor", ignoreCase = true) || topicTitle.contains("Synthetic", ignoreCase = true) -> {
                summary = "A polynomial expression involves summed variables carrying whole-number power exponents. This lesson focuses on finding rational roots, doing long division, and factor factorization."
                keyConcepts = "Polynomial Degrees, Rational Roots, Polynomial Long Division, Synthetic simplification, Factor Theorem."
                importantNotes = "If P(k) = 0, then (x - k) divides cleanly into P(x) with no remainder, making it a factor."
                formulas = "Division Algorithm: P(x) = d(x) * q(x) + r(x)\nRemainder Theorem: P(c) = Remainder\nQuadratic: x = [-b ± √(b² - 4ac)] / 2a"
            }
            topicTitle.contains("Statistics", ignoreCase = true) || topicTitle.contains("Mean", ignoreCase = true) || topicTitle.contains("Median", ignoreCase = true) || topicTitle.contains("Mode", ignoreCase = true) || topicTitle.contains("Standard Deviation", ignoreCase = true) || topicTitle.contains("Dispersion", ignoreCase = true) -> {
                summary = "Statistics guides how we represent and analyze real data. We construct grouped data distributions, calculate cumulative counts, and compute absolute standard deviation."
                keyConcepts = "Grouped Data, Class Frequency, Class Midpoints, Mean average, Median point, Variance spread, Standard Deviation."
                importantNotes = "Always find the exact class midpoints (mid = [low + high] / 2) prior to calculating grouped averages or variance."
                formulas = "Grouped Mean x̄ = Σ(f * x) / Σf\nVariance s² = Σ(f * (x - x̄)²) / Σf\nStandard Deviation s = √Variance"
            }
            topicTitle.contains("Trigonometric", ignoreCase = true) || topicTitle.contains("Trigonometry", ignoreCase = true) || topicTitle.contains("Radian", ignoreCase = true) || topicTitle.contains("Sine", ignoreCase = true) || topicTitle.contains("Cosine", ignoreCase = true) -> {
                summary = "Trigonometry defines relationships between angles and lengths within right triangles and along unit-circle radians. We map conversion steps and graph period properties."
                keyConcepts = "Radians, Degrees, Unit Circle coordinate, Periodicity, Amplitude, Pythagorean identity, Phase displacement."
                importantNotes = "Remember that a full turn around a circle is of magnitude 360 degrees, which corresponds exactly to 2π radians."
                formulas = "Conversion: Rad = Deg * (π / 180°)\nIdentity: sin²(x) + cos²(x) = 1\nTangent: tan(x) = sin(x) / cos(x)"
            }
            else -> {
                summary = "This math lesson covers the curriculum topic of $topicTitle in Unit ${topic.unit_id} ($unitTitle). It is structured offline for convenient learning anytime."
                keyConcepts = "$topicTitle, $unitTitle, Mathematical Deductions, Formulas, Practical Exercises."
                importantNotes = "Examine the definitions carefully. Solve the step-by-step example problem to confirm your structural comprehension beforehand."
                formulas = "Linear base model: f(x) = ax + b, where a ≠ 0.\nAll calculations assume standard Real Number system bounds."
            }
        }
        
        return Lesson(
            id = topic.id,
            topic_id = topic.id,
            summary = summary,
            key_concepts = keyConcepts,
            important_notes = importantNotes,
            formulas = formulas
        )
    }

    private fun generateDynamicExample(topic: Topic): Example {
        val title = topic.title
        val question: String
        val solution: String
        
        when {
            title.contains("Logarithmic", ignoreCase = true) || title.contains("Logarithm", ignoreCase = true) -> {
                question = "Simplify the expression: log₃(27) + log₃(1)."
                solution = "Step 1: Simplify log₃(27). Since 3³ = 27, log₃(27) = 3.\nStep 2: Simplify log₃(1). Any base logarithm of 1 is 0, so log₃(1) = 0.\nStep 3: Combine with operations: 3 + 0 = 3."
            }
            title.contains("Exponential", ignoreCase = true) || title.contains("Exponent", ignoreCase = true) -> {
                question = "Solve the exponential equation: 2^(x + 3) = 16."
                solution = "Step 1: Express 16 as an exponent carrying base 2: 16 = 2⁴.\nStep 2: Equate bases to solve exponents: x + 3 = 4.\nStep 3: Subtract 3 from both sides: x = 1. Verification: 2^(1+3) = 2⁴ = 16. Correct!"
            }
            title.contains("Vector", ignoreCase = true) -> {
                question = "Find the magnitude of the 2D vector A = (6, -8)."
                solution = "Step 1: Use the vector magnitude equation: |A| = √(x² + y²).\nStep 2: Substitute components: |A| = √(6² + (-8)²) = √(36 + 64).\nStep 3: Solve root: |A| = √100 = 10 units."
            }
            title.contains("Equation", ignoreCase = true) || title.contains("Solve", ignoreCase = true) || title.contains("Systems", ignoreCase = true) -> {
                question = "Solve for y: 4y - 12 = 8."
                solution = "Step 1: Add 12 to both sides of the balance equation: 4y = 20.\nStep 2: Isolate variable y by dividing by 4: y = 5.\nStep 3: Check: 4(5) - 12 = 8. Correct!"
            }
            else -> {
                question = "Provide a standardized calculation resolving a core scenario of $title."
                solution = "Step 1: Evaluate standard variables and math rules of $title.\nStep 2: Apply the appropriate formula corresponding to the scenario constraints.\nStep 3: Complete arithmetic calculations to formulate the exact final solution."
            }
        }
        
        return Example(
            id = topic.id * 10,
            topic_id = topic.id,
            question = question,
            step_by_step_solution = solution
        )
    }

    private fun generateDynamicPracticeQuestion(topic: Topic): PracticeQuestion {
        val title = topic.title
        val question: String
        val correct: String
        val explanation: String
        
        when {
            title.contains("Logarithmic", ignoreCase = true) || title.contains("Logarithm", ignoreCase = true) -> {
                question = "What is the result of log(100) assuming common log base 10?"
                correct = "2"
                explanation = "Assuming base 10, log₁₀(100) = 2 because 10² = 100."
            }
            title.contains("Vector", ignoreCase = true) -> {
                question = "Are vectors containing zero magnitude and no direction considered zero vectors?"
                correct = "Yes"
                explanation = "A zero vector has a magnitude of 0, and mathematically has no defined direction."
            }
            else -> {
                question = "Identify the major mathematical theorem or rule corresponding to $title."
                correct = "Fundamental Theorem of $title"
                explanation = "Properties of $title require consistency with real-number system rules and standard proof guidelines."
            }
        }
        
        return PracticeQuestion(
            id = topic.id * 10,
            topic_id = topic.id,
            difficulty = "Medium",
            question = question,
            correct_answer = correct,
            explanation = explanation
        )
    }

    private fun generateDynamicQuizQuestions(topic: Topic): List<QuizQuestion> {
        val baseId = topic.id * 100
        val title = topic.title
        val optionsJson = "[\"Standard Correct Option\", \"Incorrect Option 1\", \"Incorrect Option 2\", \"Incorrect Option 3\"]"
        
        return listOf(
            QuizQuestion(
                id = baseId + 1,
                topic_id = topic.id,
                question = "Which option correctly defines the foundational property of $title?",
                options_json = optionsJson,
                correct_option_index = 0
            ),
            QuizQuestion(
                id = baseId + 2,
                topic_id = topic.id,
                question = "Is the mathematical study of $title essential for offline school exercises?",
                options_json = "[\"Yes, highly essential\", \"No\", \"Only theoretically\", \"Occasionally\"]",
                correct_option_index = 0
            )
        )
    }

    private fun processRow(
        parts: List<String>,
        groupedData: LinkedHashMap<Int, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>>>
    ) {
        val gradeStr = parts[0].trim()
        val gradeId = gradeStr.toIntOrNull() ?: return
        val subject = parts[1].trim()

        // Skip Grade 9 and Grade 10 Mathematics as we already added it
        if ((gradeId == 9 || gradeId == 10) && subject.contains("Math", ignoreCase = true)) {
            return
        }

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
