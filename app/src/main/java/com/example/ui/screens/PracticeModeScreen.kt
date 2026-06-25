package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuizQuestion
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeModeScreen(
    viewModel: MainViewModel,
    topicId: Int,
    onBack: () -> Unit
) {
    val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    LaunchedEffect(topicId) {
        viewModel.loadQuizQuestions(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (quizQuestions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentQuestion = quizQuestions[currentIndex]
            val optionsList = remember(currentQuestion) {
                val list = mutableListOf<String>()
                try {
                    val arr = JSONArray(currentQuestion.options_json)
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                } catch (e: Exception) {
                    // Fallback if not pure JSON
                    list.addAll(currentQuestion.options_json.split("||"))
                }
                list
            }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / quizQuestions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(currentQuestion.question_text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                optionsList.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index
                    val isCorrect = index == currentQuestion.correct_option
                    
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
                            if (selectedOption == currentQuestion.correct_option) {
                                score++
                            }
                            isSubmitted = true
                        } else {
                            if (currentIndex < quizQuestions.size - 1) {
                                currentIndex++
                                selectedOption = null
                                isSubmitted = false
                            } else {
                                val scaledScore = if (quizQuestions.isNotEmpty()) {
                                    (score.toFloat() / quizQuestions.size * 10).toInt()
                                } else 0
                                viewModel.submitQuizScore(topicId, scaledScore)
                                onBack() // Done
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedOption != null
                ) {
                    Text(if (!isSubmitted) "Submit Answer" else if (currentIndex < quizQuestions.size - 1) "Next Question" else "Finish Practice")
                }
            }
        }
    }
}
