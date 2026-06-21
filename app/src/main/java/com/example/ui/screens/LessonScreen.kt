package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AITutor
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(viewModel: MainViewModel, topicId: Int, onBack: () -> Unit) {
    val currentLesson by viewModel.currentLesson.collectAsState()
    val examples by viewModel.examples.collectAsState()
    val practiceQuestions by viewModel.practiceQuestions.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAITutor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        viewModel.loadLesson(topicId)
        viewModel.loadExamples(topicId)
        viewModel.loadPracticeQuestions(topicId)
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("No lesson data available for this topic.")
                        }
                    }
                }
                1 -> {
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
                2 -> {
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
