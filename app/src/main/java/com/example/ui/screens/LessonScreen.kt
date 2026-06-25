package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayCircle
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
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    viewModel: MainViewModel, 
    topicId: Int, 
    onBack: () -> Unit,
    onNavigateToQuiz: (Int) -> Unit,
    onNavigateToPractice: (Int) -> Unit
) {
    val currentLesson by viewModel.currentLesson.collectAsState()
    val objectives by viewModel.objectives.collectAsState()
    val examples by viewModel.examples.collectAsState()
    val topicProgress by viewModel.topicProgress.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(topicId) {
        viewModel.loadLesson(topicId)
        viewModel.loadObjectives(topicId)
        viewModel.loadExamples(topicId)
        viewModel.loadProgress(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson Content") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                    Text("Quiz", modifier = Modifier.padding(16.dp))
                }
            }

            when (selectedTabIndex) {
                0 -> LearnTab(currentLesson?.content ?: "", currentLesson?.video_url, objectives.map { it.objective_text })
                1 -> ExamplesTab(examples)
                2 -> QuizTab(topicProgress?.quiz_score ?: 0, onNavigateToQuiz = { onNavigateToQuiz(topicId) }, onNavigateToPractice = { onNavigateToPractice(topicId) })
            }
        }
    }
}

@Composable
fun LearnTab(content: String, videoUrl: String?, objectives: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (objectives.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Learning Objectives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    objectives.forEach { obj ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold)
                            Text(obj)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (videoUrl != null) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { /* Handle video */ },
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Watch Lesson Video", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Text("Lesson Content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(content ?: "", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ExamplesTab(examples: List<com.example.data.model.Example>) {
    if (examples.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No examples available for this topic.")
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(examples.size) { index ->
                val example = examples[index]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(Modifier.width(8.dp))
                            Text(example.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(example.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("Solution:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(example.solution ?: "")
                    }
                }
            }
        }
    }
}

@Composable
fun QuizTab(highScore: Int, onNavigateToQuiz: () -> Unit, onNavigateToPractice: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Ready to Test?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Take a 10-question quiz or practice at your own pace.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (highScore > 0) {
                    Spacer(Modifier.height(16.dp))
                    Text("Your Best Score: $highScore%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToQuiz,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Start Quiz")
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onNavigateToPractice,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Practice Mode")
                }
            }
        }
    }
}
