package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AITutor
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    viewModel: MainViewModel, 
    topicId: Int, 
    onBack: () -> Unit,
    onNavigateToQuiz: (Int) -> Unit
) {
    val currentLesson by viewModel.currentLesson.collectAsState()
    val examples by viewModel.examples.collectAsState()
    val practiceQuestions by viewModel.practiceQuestions.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val topicProgress by viewModel.topicProgress.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAITutor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        viewModel.loadLesson(topicId)
        viewModel.loadExamples(topicId)
        viewModel.loadPracticeQuestions(topicId)
        viewModel.loadQuizQuestions(topicId)
        viewModel.loadProgress(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAITutor = true },
                icon = { Icon(Icons.Default.Assistant, contentDescription = "AI Tutor") },
                text = { Text("Ask AI Tutor") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                    Text("Learn", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                    Text("Examples", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }) {
                    Text("Practice", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTabIndex == 3, onClick = { selectedTabIndex = 3 }) {
                    Text("Quiz", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    currentLesson?.let { lesson ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(lesson.summary, style = MaterialTheme.typography.bodyLarge)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Key Concepts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(lesson.key_concepts, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Formulas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(lesson.formulas, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "AI Tutor is drafting complete lesson materials...",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Crafting comprehensive textbook Summary, Key Concepts, Formulas, examples, practice sheets, and quiz questions specifically for this topic...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    } else {
                                        Text("Curriculum Lesson Pending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Detailed text lessons and formulas for this topic are currently being prepared. You can instantly generate the full detailed lesson, step-by-step examples, practice exercises, and formal quizzes with our AI Tutor!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        
                                        generationError?.let { err ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                err,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.generateLessonForTopic(topicId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        ) {
                                            Text("Generate Lesson with AI")
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showAITutor = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        ) {
                                            Text("Ask AI Tutor a Question")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (examples.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "AI Tutor is generating full lesson and step-by-step examples...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    } else {
                                        Text("Examples Coming Soon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Step-by-step example equations and solutions for this topic are currently in development. Ask the AI Tutor for instant high-fidelity math examples, or generate full curriculum materials!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.generateLessonForTopic(topicId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Generate Examples with AI")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showAITutor = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ask AI Tutor")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(examples.size) { index ->
                                val example = examples[index]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Example ${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(example.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                        Text("Solution", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(example.step_by_step_solution)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (practiceQuestions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "AI Tutor is generating practice sheets specifically for this topic...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    } else {
                                        Text("Practice Problems Pending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Interactive practice questions for this topic are being updated. Tap below to have the AI Tutor generate a math practice sheet for you!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.generateLessonForTopic(topicId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Generate Practice with AI")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showAITutor = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Open AI Tutor Chat")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(practiceQuestions.size) { index ->
                                val question = practiceQuestions[index]
                                var showAnswer by remember { mutableStateOf(false) }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(), 
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text("Question ${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    question.difficulty, 
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(question.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { showAnswer = !showAnswer }, modifier = Modifier.fillMaxWidth()) {
                                            Text(if (showAnswer) "Hide Answer" else "Reveal Answer & Explanation")
                                        }
                                        
                                        if (showAnswer) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Correct Answer:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                    Text(question.correct_answer, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Explanation:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                    Text(question.explanation, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                 3 -> {
                     Box(
                         modifier = Modifier.fillMaxSize().padding(16.dp),
                         contentAlignment = androidx.compose.ui.Alignment.Center
                     ) {
                         Card(
                             modifier = Modifier.fillMaxWidth().testTag("quiz_launch_card"),
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                         ) {
                             Column(
                                 modifier = Modifier.padding(24.dp),
                                 horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                             ) {
                                 Text(
                                     "10-Question Progress Quiz",
                                     style = MaterialTheme.typography.titleLarge,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.onPrimaryContainer
                                 )
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Text(
                                     "Challenge yourself with 10 random mathematics questions from the offline practice curriculum. Achieve a high score to log progress inside the SQLite database!",
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                     textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                 )
                                 
                                 topicProgress?.let { prog ->
                                     if (prog.quiz_score > 0) {
                                         Spacer(modifier = Modifier.height(16.dp))
                                         Surface(
                                             color = MaterialTheme.colorScheme.primary,
                                             shape = RoundedCornerShape(12.dp)
                                         ) {
                                             Text(
                                                 "Highest Registered Score: ${prog.quiz_score}%",
                                                 style = MaterialTheme.typography.labelLarge,
                                                 color = MaterialTheme.colorScheme.onPrimary,
                                                 modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                             )
                                         }
                                     }
                                 }
                                 
                                 Spacer(modifier = Modifier.height(24.dp))
                                 Button(
                                     onClick = { onNavigateToQuiz(topicId) },
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .heightIn(min = 48.dp)
                                         .testTag("start_progress_quiz_button"),
                                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                 ) {
                                     Text("Begin Quiz Now", fontWeight = FontWeight.Bold)
                                 }
                             }
                         }
                     }
                 }
            }
        }
        
        if (showAITutor) {
            var question by remember { mutableStateOf("") }
            var response by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            val tutor = remember { AITutor() }

            AlertDialog(
                onDismissRequest = { showAITutor = false },
                title = { Text("AI Tutor") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("What part of the lesson is confusing you?")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else if (response.isNotEmpty()) {
                            Text(response, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (question.isNotBlank()) {
                            isLoading = true
                            scope.launch {
                                response = tutor.askQuestion("Lesson Topic ID: $topicId", question)
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Ask")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAITutor = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun QuizGauntlet(
    questions: List<com.example.data.model.QuizQuestion>,
    onSubmitScore: (Int) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    if (quizCompleted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quiz Completed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Score: $score / ${questions.size}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                currentIndex = 0
                score = 0
                quizCompleted = false
                selectedOption = null
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Retake Quiz")
            }
        }
        return
    }

    val currentQuestion = questions[currentIndex]
    val options: List<String> = try {
        kotlinx.serialization.json.Json.decodeFromString(currentQuestion.options_json)
    } catch (e: Exception) {
        emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Question ${currentIndex + 1} of ${questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(currentQuestion.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        options.forEachIndexed { index, opt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { selectedOption = index },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedOption == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = opt,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedOption == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                if (selectedOption != null) {
                    if (selectedOption == currentQuestion.correct_option_index) {
                        score += 1
                    }
                    if (currentIndex < questions.size - 1) {
                        currentIndex += 1
                        selectedOption = null
                    } else {
                        quizCompleted = true
                        val finalScore = ((score.toFloat() / questions.size.toFloat()) * 100).toInt()
                        onSubmitScore(finalScore)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedOption != null
        ) {
            Text(if (currentIndex < questions.size - 1) "Next Question" else "Submit Quiz")
        }
    }
}
