package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
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
import com.example.data.model.QuizQuestion
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    topicId: Int,
    onBack: () -> Unit
) {
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val scope = rememberCoroutineScope()
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableIntStateOf(600) }

    LaunchedEffect(topicId) {
        viewModel.loadQuizQuestions(topicId)
    }

    LaunchedEffect(quizCompleted) {
        if (!quizCompleted) {
            while (timeLeftSeconds > 0) {
                delay(1000L)
                timeLeftSeconds--
            }
            if (timeLeftSeconds == 0) quizCompleted = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topic Quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%02d:%02d", timeLeftSeconds / 60, timeLeftSeconds % 60),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (timeLeftSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (quizQuestions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (quizCompleted) {
            QuizResultScreen(
                score = score,
                total = quizQuestions.size,
                onBack = onBack,
                modifier = Modifier.padding(padding)
            )
        } else {
            val currentQuestion = quizQuestions[currentIndex]
            
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / quizQuestions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(currentQuestion.difficulty.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Question ${currentIndex + 1} of ${quizQuestions.size}", style = MaterialTheme.typography.labelMedium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(currentQuestion.question_text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val isCorrect = index == currentQuestion.correct_option_index
                    
                    val color = when {
                        isSubmitted && isCorrect -> Color(0xFF4CAF50)
                        isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    
                    OutlinedCard(
                        onClick = { if (!isSubmitted) selectedOption = index },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(if (isSelected || isSubmitted) 2.dp else 1.dp, color),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!isSubmitted) selectedOption = index },
                                enabled = !isSubmitted
                            )
                            Text(option, modifier = Modifier.padding(start = 8.dp))
                            if (isSubmitted && isCorrect) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                            } else if (isSubmitted && isSelected && !isCorrect) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedVisibility(visible = isSubmitted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Explanation", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(currentQuestion.explanation)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (!isSubmitted) {
                            if (selectedOption == currentQuestion.correct_option_index) score++
                            isSubmitted = true
                        } else {
                            if (currentIndex < quizQuestions.size - 1) {
                                currentIndex++
                                selectedOption = null
                                isSubmitted = false
                            } else {
                                scope.launch {
                                    viewModel.submitQuizScore(topicId, score)
                                    quizCompleted = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedOption != null
                ) {
                    Text(if (!isSubmitted) "Submit Answer" else if (currentIndex < quizQuestions.size - 1) "Next Question" else "Finish Quiz")
                }
            }
        }
    }
}

@Composable
fun QuizResultScreen(score: Int, total: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val percentage = (score.toFloat() / total.toFloat() * 100).toInt()
        
        Text("Quiz Results", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = { score.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percentage%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("$score / $total correct", style = MaterialTheme.typography.bodyLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = when {
                percentage >= 80 -> "Excellent! You've mastered this topic."
                percentage >= 60 -> "Good job! A little more practice and you'll be an expert."
                else -> "Keep studying! Review the lesson and try again."
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Back to Lessons")
        }
    }
}
