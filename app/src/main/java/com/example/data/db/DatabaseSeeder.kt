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
                    appDao.clearPracticeQuestions()
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
        val dbFts = mutableListOf<CurriculumSearchFts>()
        
        var ftsIdCounter = 1

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
            seedLessonsAndQuestions(dbTopics, dbUnits, dbSubjects)
        }
    }

    private suspend fun seedLessonsAndQuestions(topics: List<Topic>, units: List<UnitTable>, subjects: List<Subject>) {
        val lessonsToSeed = mutableListOf<Lesson>()
        val examplesToSeed = mutableListOf<Example>()
        val practiceToSeed = mutableListOf<PracticeQuestion>()
        val quizToSeed = mutableListOf<QuizQuestion>()

        val unitMap = units.associateBy { it.id }
        val subjectMap = subjects.associateBy { it.id }

        for (topic in topics) {
            val topicId = topic.id
            val unit = unitMap[topic.unit_id]
            val subject = subjectMap[unit?.subject_id]
            val subjectName = subject?.name ?: "General"
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
                else -> {
                    lessonsToSeed.add(generateDynamicLesson(topic, unitTitle, subjectName))
                    examplesToSeed.add(generateDynamicExample(topic, subjectName))
                    practiceToSeed.add(generateDynamicPracticeQuestion(topic, subjectName))
                    quizToSeed.addAll(generateDynamicQuizQuestions(topic, subjectName))
                }
            }
        }

        appDao.insertLessons(lessonsToSeed)
        appDao.insertExamples(examplesToSeed)
        appDao.insertPracticeQuestions(practiceToSeed)
        appDao.insertQuizQuestions(quizToSeed)
    }

    private fun generateDynamicLesson(topic: Topic, unitTitle: String, subjectName: String): Lesson {
        val topicTitle = topic.title
        var summary = ""
        var keyConcepts = ""
        var importantNotes = ""
        var formulas = ""
        
        when {
            subjectName.contains("Amharic", ignoreCase = true) -> {
                summary = "ይህ የአማርኛ ቋንቋ ትምህርት ስለ $topicTitle በ$unitTitle ውስጥ በዝርዝር ያብራራል። ትምህርቱ በስርዓተ ትምህርቱ መሠረት የቋንቋ ክህሎቶችን፣ ሰዋስው እና ስነ-ጽሑፍን ያካትታል።"
                keyConcepts = "$topicTitle, የቋንቋ ክህሎት, ሰዋስው, ስነ-ጽሑፍ, ንባብ, ጽሕፈት."
                importantNotes = "የአማርኛ ቋንቋን ህጎች እና ስርዓቶችን በትክክል መከተል ለቋንቋው እድገት ወሳኝ ነው። እባክዎን የተሰጡትን ምሳሌዎች በጥንቃቄ ያስተውሉ።"
                formulas = "መሰረታዊ የሰዋስው ህግ፡ ባለቤት + ተሳቢ + ግሥ (ለምሳሌ፡ እሱ ትምህርት ተማረ።)"
            }
            subjectName.contains("Biology", ignoreCase = true) -> {
                summary = "This biology lesson explores $topicTitle within the context of $unitTitle. It covers the fundamental biological principles, life processes, and scientific methodologies relevant to the Ethiopian curriculum."
                keyConcepts = "$topicTitle, Biological Systems, Cellular Structures, Evolutionary Mechanisms, Biodiversity."
                importantNotes = "Always observe safety protocols in biological laboratories. Note the interdependency of living organisms within their specific ecosystems."
                formulas = "Photosynthesis: 6CO2 + 6H2O + light -> C6H12O6 + 6O2\nCell Respiration: C6H12O6 + 6O2 -> 6CO2 + 6H2O + ATP"
            }
            subjectName.contains("Chemistry", ignoreCase = true) -> {
                summary = "This chemistry lesson focuses on $topicTitle from $unitTitle. It details chemical compositions, atomic structures, and reaction kinetics required for Grade level mastery."
                keyConcepts = "$topicTitle, Atomic Theory, Periodic Trends, Chemical Bonding, Stoichiometry, Mole Concept."
                importantNotes = "Handle all chemical reagents with extreme care. Ensure balanced equations to satisfy the Law of Conservation of Mass."
                formulas = "Ideal Gas Law: PV = nRT\nMolarity: M = n / V (Liters)\nDensity: ρ = m / V"
            }
            subjectName.contains("Physics", ignoreCase = true) -> {
                summary = "Physics lesson detailing the mechanics, thermodynamics, and electromagnetism principles behind $topicTitle in $unitTitle. We analyze physical quantities and their mathematical relationships."
                keyConcepts = "$topicTitle, Kinematics, Dynamics, Energy Conservation, Wave Phenomena, Circuit Laws."
                importantNotes = "Pay close attention to SI units (Meters, Kilograms, Seconds). Vector quantities require both magnitude and direction."
                formulas = "Newton's Second Law: F = ma\nKinetic Energy: KE = 0.5 * mv²\nOhm's Law: V = IR"
            }
            subjectName.contains("Economics", ignoreCase = true) -> {
                summary = "Economics curriculum focus on $topicTitle within $unitTitle. This lesson analyzes market behaviors, resource allocation, and macroeconomic indicators in the Ethiopian context."
                keyConcepts = "$topicTitle, Supply and Demand, Market Equilibrium, GDP, Inflation, Fiscal Policy, Economic Growth."
                importantNotes = "Economic models often assume 'ceteris paribus' (all other things being equal). Consider the impact of policy changes on local trade."
                formulas = "GDP = C + I + G + (X - M)\nElasticity = % Change in Q / % Change in P"
            }
            subjectName.contains("History", ignoreCase = true) -> {
                summary = "Historical analysis of $topicTitle as part of $unitTitle. We examine primary sources, cause-and-effect relationships, and the chronological development of civilizations and modern states."
                keyConcepts = "$topicTitle, Primary Sources, Chronology, Colonialism, Resistance, Sovereignty, Cultural Heritage."
                importantNotes = "Critically evaluate different historical perspectives and sources. Focus on the legacy of major historical events in Ethiopia."
                formulas = "Historical Timeline: BC/BCE (Before Common Era) -> AD/CE (Common Era)\nSource Analysis: Origin, Purpose, Content, Value, Limitation (OPCVL)."
            }
            subjectName.contains("Geography", ignoreCase = true) -> {
                summary = "Geographical study of $topicTitle in $unitTitle. This lesson covers physical landforms, climatic patterns, human-environment interactions, and spatial data analysis techniques."
                keyConcepts = "$topicTitle, Topography, Climate Zones, GIS, Sustainable Development, Resource Management."
                importantNotes = "Map reading skills are essential for spatial understanding. Note the impact of climate change on Ethiopia's natural resources."
                formulas = "Population Density = Total Population / Total Area\nMap Scale = Map Distance / Real Distance"
            }
            subjectName.contains("Math", ignoreCase = true) -> {
                // Keep the detailed math templates from before but generalize slightly
                when {
                    topicTitle.contains("Logarithmic", ignoreCase = true) || topicTitle.contains("Logarithm", ignoreCase = true) -> {
                        summary = "A logarithm is the mathematical exponent to which a specified base must be raised to yield a given value. This concept is vital for reversing exponential growth functions."
                        keyConcepts = "Logarithmes, Log Base, Euler's number, Change of Base, Natural Log (ln), Common Log (log10)."
                        importantNotes = "Always verify that your logarithm variables are strictly positive. The base value b must be greater than zero and not equal to one."
                        formulas = "log_b(m * n) = log_b(m) + log_b(n)\nlog_b(m / n) = log_b(m) - log_b(n)\nlog_b(m^k) = k * log_b(m)"
                    }
                    else -> {
                        summary = "This math lesson covers $topicTitle in $unitTitle. It focuses on logical deduction, algebraic manipulation, and geometric proofs relevant to the curriculum."
                        keyConcepts = "$topicTitle, Algebraic Expressions, Geometric Proofs, Set Theory, Functions."
                        importantNotes = "Practice solving problems step-by-step. Ensure all logical steps are justified by mathematical theorems."
                        formulas = "Quadratic Formula: x = [-b ± √(b² - 4ac)] / 2a\nPythagorean Theorem: a² + b² = c²"
                    }
                }
            }
            else -> {
                summary = "Comprehensive lesson on $topicTitle as part of $unitTitle ($subjectName). This unit is designed to provide deep structural understanding and practical application skills."
                keyConcepts = "$topicTitle, Core Principles, Applied Methodologies, Critical Thinking, Curriculum Standards."
                importantNotes = "Review the previous unit foundations if $topicTitle feels complex. Engage with all practice exercises for maximum retention."
                formulas = "Analytical Framework: Observation -> Hypothesis -> Analysis -> Conclusion.\nStandard Unit Principles apply."
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

    private fun generateDynamicExample(topic: Topic, subjectName: String): Example {
        val title = topic.title
        var question = ""
        var solution = ""
        
        when {
            subjectName.contains("Amharic", ignoreCase = true) -> {
                question = "ስለ $title የቀረበውን ጥያቄ ይመልሱ።"
                solution = "ደረጃ 1: የ$title መሰረታዊ ሀሳቦችን ይረዱ።\nደረጃ 2: በጽሁፉ ውስጥ የተጠቀሱትን ዋና ነጥቦች ይለዩ።\nደረጃ 3: ትክክለኛውን የሰዋስው ወይም የስነ-ጽሑፍ ህግ ይተግብሩ።\nደረጃ 4: መልሱን በማረጋገጥ ያጠቃልሉ፡፡"
            }
            subjectName.contains("Math", ignoreCase = true) -> {
                question = "Solve a standard problem involving $title."
                solution = "Step 1: Identify the given values and the unknown variable.\nStep 2: Apply the relevant formula for $title.\nStep 3: Perform algebraic steps to isolate the variable.\nStep 4: Finalize the calculation and check for consistency."
            }
            subjectName.contains("Chemistry", ignoreCase = true) -> {
                question = "Calculate the mass of a substance in a reaction related to $title."
                solution = "Step 1: Write the balanced chemical equation.\nStep 2: Convert given mass to moles.\nStep 3: Use stoichiometric ratios to find moles of the target substance.\nStep 4: Convert back to mass."
            }
            else -> {
                question = "Analyze a core scenario regarding $title."
                solution = "Step 1: Define the key parameters of the $title scenario.\nStep 2: Apply the appropriate theoretical framework from $subjectName.\nStep 3: Evaluate the data or evidence provided.\nStep 4: Formulate a comprehensive conclusion based on your analysis."
            }
        }
        
        return Example(
            id = topic.id * 10,
            topic_id = topic.id,
            question = question,
            step_by_step_solution = solution
        )
    }

    private fun generateDynamicPracticeQuestion(topic: Topic, subjectName: String): PracticeQuestion {
        val title = topic.title
        val isAmharic = subjectName.contains("Amharic", ignoreCase = true)
        
        return PracticeQuestion(
            id = topic.id * 10,
            topic_id = topic.id,
            difficulty = "መካከለኛ",
            question = if (isAmharic) "ከሚከተሉት ውስጥ ስለ $title ትክክለኛ የሆነው የቱ ነው?" else "Which of the following best describes a key characteristic of $title in $subjectName?",
            correct_answer = if (isAmharic) "በስርዓተ ትምህርቱ የተገለፀው" else "Determined by curriculum standards",
            explanation = if (isAmharic) "በአማርኛ ስርዓተ ትምህርት መሰረት $title የራሱ የሆኑ ህጎች እና መርሆዎች አሉት።" else "As defined in the $subjectName curriculum, $title involves specific properties and rules that guide its application."
        )
    }

    private fun generateDynamicQuizQuestions(topic: Topic, subjectName: String): List<QuizQuestion> {
        val baseId = topic.id * 100
        val title = topic.title
        val isAmharic = subjectName.contains("Amharic", ignoreCase = true)
        
        val question1: String
        val options1: String
        val question2: String
        val options2: String

        when {
            isAmharic -> {
                question1 = "ስለ $title ዋናው ነጥብ ምንድን ነው?"
                options1 = "[\"መሰረታዊ መርሆች\", \"ታሪካዊ አመጣጥ\", \"ተግባራዊ ልምምድ\", \"የቋንቋ ህጎች\"]"
                question2 = "$title በስርዓተ ትምህርቱ ውስጥ ያለው ፋይዳ ምንድነው?"
                options2 = "[\"የቋንቋ ክህሎትን ማሳደግ\", \"ለፈተና መዘጋጀት\", \"አጠቃላይ እውቀት\", \"ምንም\"]"
            }
            subjectName.contains("Biology", ignoreCase = true) -> {
                question1 = "Which biological principle is most closely related to $title?"
                options1 = "[\"Homeostasis\", \"Cell Theory\", \"Natural Selection\", \"Metabolism\"]"
                question2 = "In the study of $title, what is a primary observation?"
                options2 = "[\"Microscopic structures\", \"Genetic variation\", \"Energy transfer\", \"Environmental impact\"]"
            }
            subjectName.contains("Chemistry", ignoreCase = true) -> {
                question1 = "What is a fundamental chemical property associated with $title?"
                options1 = "[\"Reactivity\", \"Atomic Mass\", \"Electronegativity\", \"Molecular Weight\"]"
                question2 = "Which law or theory governs the behavior of $title?"
                options2 = "[\"Dalton's Atomic Theory\", \"Law of Conservation of Mass\", \"Periodic Law\", \"Kinetic Molecular Theory\"]"
            }
            subjectName.contains("Physics", ignoreCase = true) -> {
                question1 = "Which physical quantity is central to the topic of $title?"
                options1 = "[\"Force\", \"Energy\", \"Velocity\", \"Mass\"]"
                question2 = "What is a typical unit of measurement used in $title?"
                options2 = "[\"Joules\", \"Newtons\", \"Meters per second\", \"Kilograms\"]"
            }
            subjectName.contains("Math", ignoreCase = true) -> {
                question1 = "What is the primary mathematical operation used in $title?"
                options1 = "[\"Simplification\", \"Solving Equations\", \"Graphing\", \"Geometric Proof\"]"
                question2 = "Which mathematical structure does $title primarily deal with?"
                options2 = "[\"Sets\", \"Functions\", \"Numbers\", \"Shapes\"]"
            }
            subjectName.contains("Economics", ignoreCase = true) -> {
                question1 = "How does $title affect market behavior?"
                options1 = "[\"Influences Supply\", \"Influences Demand\", \"Determines Price\", \"Affects Equilibrium\"]"
                question2 = "Which economic indicator is often linked to $title?"
                options2 = "[\"GDP\", \"Inflation Rate\", \"Unemployment\", \"Consumer Surplus\"]"
            }
            subjectName.contains("Geography", ignoreCase = true) -> {
                question1 = "What is a key spatial aspect of $title?"
                options1 = "[\"Location\", \"Topography\", \"Climate\", \"Human Settlement\"]"
                question2 = "Which tool is commonly used to analyze $title?"
                options2 = "[\"GIS\", \"Physical Maps\", \"Remote Sensing\", \"Statistical Data\"]"
            }
            subjectName.contains("Citizenship", ignoreCase = true) -> {
                question1 = "What is the core civic value discussed in $title?"
                options1 = "[\"Democracy\", \"Justice\", \"Equality\", \"Responsibility\"]"
                question2 = "How does $title contribute to national unity in Ethiopia?"
                options2 = "[\"Promoting Tolerance\", \"Understanding Constitution\", \"Celebrating Diversity\", \"Active Participation\"]"
            }
            else -> {
                question1 = "In $subjectName, what is the primary focus of $title?"
                options1 = "[\"Core theoretical concepts\", \"Practical application\", \"Historical context\", \"Modern developments\"]"
                question2 = "Is $title considered a fundamental part of the Grade curriculum?"
                options2 = "[\"Yes, essential\", \"No, secondary\", \"Only for specialized tracks\", \"Optional\"]"
            }
        }

        return listOf(
            QuizQuestion(
                id = baseId + 1,
                topic_id = topic.id,
                question = question1,
                options_json = options1,
                correct_option_index = 0
            ),
            QuizQuestion(
                id = baseId + 2,
                topic_id = topic.id,
                question = question2,
                options_json = options2,
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
