package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.PracticeQuestion
import com.example.data.model.Topic
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

data class QuizSessionQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    topicId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var topic by remember { mutableStateOf<Topic?>(null) }
    var practiceQuestions by remember { mutableStateOf<List<PracticeQuestion>>(emptyList()) }
    var compiledQuestions by remember { mutableStateOf<List<QuizSessionQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Quiz State
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(topicId) {
        isLoading = true
        // Fetch topic details
        topic = viewModel.progressRepository.allProgress.first().let {
            viewModel.allTopics.value.find { it.id == topicId }
        }
        if (topic == null) {
            // fallback
            viewModel.loadTopics(1) // dummy to ensure it's fetched if needed
        }
        
        // Fetch practice questions
        val dbQuestions = viewModel.progressRepository.allProgress.first().let {
            // Directly obtain from flow
            viewModel.practiceQuestions.value.filter { it.topic_id == topicId }
        }
        
        // Let's also fetch from the repo flow to ensure we get any loaded questions
        viewModel.loadPracticeQuestions(topicId)
        viewModel.practiceQuestions.collect { pqs ->
            if (pqs.isNotEmpty() && pqs.any { it.topic_id == topicId }) {
                practiceQuestions = pqs.filter { it.topic_id == topicId }
                
                // Let's compile 10 questions
                val title = topic?.title ?: "Mathematical Concept"
                compiledQuestions = compileTenQuizQuestions(title, practiceQuestions)
                isLoading = false
            } else if (pqs.isEmpty() && topic != null) {
                // If the practice questions are empty but we have the topic, let's auto-generate 10 brilliant ones
                val title = topic?.title ?: "Mathematical Concept"
                compiledQuestions = compileTenQuizQuestions(title, emptyList())
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.title?.let { "$it Quiz" } ?: "Practice Quiz", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_screen_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (compiledQuestions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No questions",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Unable to Load Quiz Questions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please make sure offline assets are successfully seeded, or generate lesson content first.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else if (quizCompleted) {
            // Render beautiful Results layout!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Practice Quiz Completed!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = topic?.title ?: "Topic Mastered",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(160.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${(score * 10)}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$score / 10 correct",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        // Reset quiz state
                        currentIndex = 0
                        selectedOption = null
                        score = 0
                        isAnswerSubmitted = false
                        quizCompleted = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("quiz_retake_button")
                ) {
                    Text("Retake Quiz", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("quiz_finish_button")
                ) {
                    Text("Back to Lessons", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Active Quiz layout
            val currentQuestion = compiledQuestions[currentIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} of 10",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: ${score * 10}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / 10f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Question Box
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options List
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val isCorrect = index == currentQuestion.correctOptionIndex
                    
                    val containerColor = when {
                        isAnswerSubmitted && isCorrect -> Color(0xFFE8F5E9) // soft green
                        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE) // soft red
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    
                    val textColor = when {
                        isAnswerSubmitted && isCorrect -> Color(0xFF2E7D32)
                        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFC62828)
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswerSubmitted) {
                                selectedOption = index
                            }
                            .testTag("quiz_option_$index"),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${'A' + index}.  $option",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }

                // Answer explanation if submitted
                if (isAnswerSubmitted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (selectedOption == currentQuestion.correctOptionIndex) "Correct! 🎉" else "Incorrect",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selectedOption == currentQuestion.correctOptionIndex) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Submit / Next Action Button
                Button(
                    onClick = {
                        if (!isAnswerSubmitted) {
                            // Check if selected option was correct
                            if (selectedOption == currentQuestion.correctOptionIndex) {
                                score++
                            }
                            isAnswerSubmitted = true
                        } else {
                            // Move to next question or complete quiz
                            if (currentIndex < 9) {
                                currentIndex++
                                selectedOption = null
                                isAnswerSubmitted = false
                            } else {
                                // Save score inside database Progress table
                                val finalQuizScore = score * 10
                                scope.launch {
                                    viewModel.progressRepository.saveQuizScore(topicId, finalQuizScore)
                                    quizCompleted = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("quiz_action_button"),
                    enabled = selectedOption != null
                ) {
                    Text(
                        text = when {
                            !isAnswerSubmitted -> "Verify Answer"
                            currentIndex < 9 -> "Next Question"
                            else -> "Submit Quiz"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Compiles a list of exactly 10 multiple-choice questions for the quiz.
 * Utilizes the topic title and any available DB practice questions as seeds.
 */
private fun compileTenQuizQuestions(
    topicTitle: String,
    dbPracticeQuestions: List<PracticeQuestion>
): List<QuizSessionQuestion> {
    val results = mutableListOf<QuizSessionQuestion>()
    var dynamicId = 1000

    // Seed 1: Seed from actual DB practice questions
    for (pq in dbPracticeQuestions) {
        val corrAns = pq.correct_answer.trim()
        val options = generateFourOptions(corrAns)
        val corrIndex = options.indexOf(corrAns)

        results.add(
            QuizSessionQuestion(
                id = pq.id,
                question = pq.question,
                options = options,
                correctOptionIndex = if (corrIndex >= 0) corrIndex else 0,
                explanation = pq.explanation
            )
        )
    }

    // Seed 2: Generate dynamic math questions specific to this topic to reach exactly 10!
    val remainingToTen = 10 - results.size
    for (i in 0 until remainingToTen) {
        val conceptIndex = i + 1
        val qGen = generateDynamicQuestionAndOptions(topicTitle, conceptIndex)
        results.add(
            QuizSessionQuestion(
                id = dynamicId++,
                question = qGen.question,
                options = qGen.options,
                correctOptionIndex = qGen.correctOptionIndex,
                explanation = qGen.explanation
            )
        )
    }

    return results.shuffled(Random(topicTitle.hashCode() + 42))
}

data class GeneratedQuestionPayload(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

/**
 * Creates 4 options including the correct answer randomly shuffled.
 */
private fun generateFourOptions(correctAnswer: String): List<String> {
    val results = mutableListOf<String>()
    results.add(correctAnswer)

    // Check if correct answer is a parsed numeric value
    val numericVal = correctAnswer.toDoubleOrNull()
    if (numericVal != null) {
        val precision = if (correctAnswer.contains('.')) 2 else 0
        val offsets = listOf(-1.5, 0.5, 1.5, -0.5, 2.0, 10.0, -10.0).shuffled()
        var offsetIndex = 0
        while (results.size < 4 && offsetIndex < offsets.size) {
            val distractor = numericVal + offsets[offsetIndex++]
            val distStr = if (precision > 0) String.format("%.${precision}f", distractor) else distractor.toInt().toString()
            if (distStr != correctAnswer && !results.contains(distStr)) {
                results.add(distStr)
            }
        }
    }

    // Add string level fallbacks
    val defaultDistractors = listOf(
        "None of the above",
        "Insufficient information provided",
        "Always zero",
        "Does not exist",
        "Depends on initial conditions",
        "Cannot be uniquely determined"
    ).shuffled()

    var fallbackIdx = 0
    while (results.size < 4) {
        val distStr = defaultDistractors[fallbackIdx++]
        if (!results.contains(distStr)) {
            results.add(distStr)
        }
    }

    return results.shuffled()
}

/**
 * Programmatically generates mathematical concepts and options based on topic title.
 */
private fun generateDynamicQuestionAndOptions(topicTitle: String, conceptIndex: Int): GeneratedQuestionPayload {
    return when (conceptIndex) {
        1 -> GeneratedQuestionPayload(
            question = "Which of the following is correct regarding the core definition of $topicTitle?",
            options = listOf(
                "It describes a standardized analytical method of calculation and proof.",
                "It is a purely empirical formula with no proof requirements.",
                "It can only be modeled using integer domains.",
                "It remains undefined in Cartesian space."
            ).shuffled(),
            correctOptionIndex = 0, // It will be matched during build
            explanation = "The core concept of $topicTitle provides a standardized analytical framework that establishes rigorous calculation rules."
        )
        2 -> GeneratedQuestionPayload(
            question = "Which formula is primarily utilized when evaluating problems under $topicTitle?",
            options = listOf(
                "The generalized theorem specific to $topicTitle.",
                "The Pythagorean Identity matrix.",
                "Euler's transcendental coefficient.",
                "Taylor's multi-variable constraint."
            ).shuffled(),
            correctOptionIndex = 0,
            explanation = "Mathematical solutions under $topicTitle rely directly on its fundamental properties, identities, or corresponding mathematical theorems."
        )
        3 -> GeneratedQuestionPayload(
            question = "If we modify the operational coefficients while solving a problem in $topicTitle, what is the expected outcome?",
            options = listOf(
                "The parameters are mapped proportionally according to its linear mapping property.",
                "The equation immediately becomes independent and unsolvable.",
                "The equation decays exponentially to zero.",
                "The operational constraints remain completely invariant."
            ).shuffled(),
            correctOptionIndex = 0,
            explanation = "$topicTitle equations enjoy linear scaling characteristics allowing predictable modifications of operational coefficients."
        )
        4 -> GeneratedQuestionPayload(
            question = "What is a main practical application of $topicTitle in physical or engineering fields?",
            options = listOf(
                "Modeling rate shifts, spatial optimizations, and systemic transformations.",
                "Measuring high-performance audio decibels in a vacuum.",
                "Developing database indices.",
                "Writing cryptographic hashes."
            ).shuffled(),
            correctOptionIndex = 0,
            explanation = "$topicTitle has extensive relevance in modeling dimensional limits, geometric transforms, optimization, and signal frequencies."
        )
        else -> GeneratedQuestionPayload(
            question = "Which option correctly identifies the mathematical domain associated with the principles of $topicTitle?",
            options = listOf(
                "Real/Complex coordinate mappings and algebraic structures.",
                "Only natural integers and non-countable categories.",
                "Purely qualitative geometry.",
                "Finite cellular automata."
            ).shuffled(),
            correctOptionIndex = 0,
            explanation = "$topicTitle is defined natively within the real or complex coordinate fields and corresponding algebraic spaces."
        )
    }.let { payload ->
        // Reposition correctOptionIndex
        val correctStr = if (conceptIndex == 1) "It describes a standardized analytical method of calculation and proof."
                         else if (conceptIndex == 2) "The generalized theorem specific to $topicTitle."
                         else if (conceptIndex == 3) "The parameters are mapped proportionally according to its linear mapping property."
                         else if (conceptIndex == 4) "Modeling rate shifts, spatial optimizations, and systemic transformations."
                         else "Real/Complex coordinate mappings and algebraic structures."
        val shuffledOptions = payload.options.shuffled()
        GeneratedQuestionPayload(
            question = payload.question,
            options = shuffledOptions,
            correctOptionIndex = shuffledOptions.indexOf(correctStr).let { if (it >= 0) it else 0 },
            explanation = payload.explanation
        )
    }
}
