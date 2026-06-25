package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuizQuestion
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay

import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: MainViewModel,
    examId: Int,
    onBack: () -> Unit
) {
    val questions by viewModel.currentExamQuestions.collectAsStateWithLifecycle()
    val session by viewModel.currentExamSession.collectAsStateWithLifecycle()
    
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var timeLeftSeconds by remember { mutableIntStateOf(60 * 60) } // 60 mins default
    var isExamFinished by remember { mutableStateOf(false) }
    var examEndTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(examId) {
        viewModel.loadExamQuestions(examId)
    }

    LaunchedEffect(session) {
        session?.let { s ->
            if (!s.is_finished) {
                examEndTimestamp = s.exam_end_timestamp
                val remaining = ((examEndTimestamp - System.currentTimeMillis()) / 1000).toInt()
                timeLeftSeconds = maxOf(0, remaining)
                currentQuestionIndex = s.current_question_index
                // simple delimited string parsing e.g. "0:1,1:3"
                val map = mutableMapOf<Int, Int>()
                if (s.answers_json.isNotEmpty()) {
                    s.answers_json.split(",").forEach { pair ->
                        val parts = pair.split(":")
                        if (parts.size == 2) {
                            map[parts[0].toInt()] = parts[1].toInt()
                        }
                    }
                }
                userAnswers = map
                selectedOption = map[currentQuestionIndex]
            } else {
                isExamFinished = true
            }
        } ?: run {
            // New session
            examEndTimestamp = System.currentTimeMillis() + (60 * 60 * 1000)
            timeLeftSeconds = 60 * 60
        }
    }

    // Save session automatically every 5 seconds
    LaunchedEffect(timeLeftSeconds, isExamFinished) {
        if (timeLeftSeconds > 0 && !isExamFinished) {
            delay(1000)
            val remaining = ((examEndTimestamp - System.currentTimeMillis()) / 1000).toInt()
            timeLeftSeconds = maxOf(0, remaining)
            
            if (timeLeftSeconds % 5 == 0 && questions.isNotEmpty()) {
                val answersString = userAnswers.map { "${it.key}:${it.value}" }.joinToString(",")
                viewModel.saveExamSession(
                    com.example.data.model.ExamSession(
                        exam_id = examId,
                        answers_json = answersString,
                        exam_end_timestamp = examEndTimestamp,
                        current_question_index = currentQuestionIndex,
                        is_finished = false
                    )
                )
            }
        } else if (timeLeftSeconds == 0) {
            isExamFinished = true
        }
        
        if (isExamFinished && questions.isNotEmpty()) {
            val answersString = userAnswers.map { "${it.key}:${it.value}" }.joinToString(",")
            var correctCount = 0
            questions.forEachIndexed { index, question ->
                if (userAnswers[index] == question.correct_option) {
                    correctCount++
                }
            }
            viewModel.saveExamSession(
                com.example.data.model.ExamSession(
                    exam_id = examId,
                    answers_json = answersString,
                    exam_end_timestamp = examEndTimestamp,
                    current_question_index = currentQuestionIndex,
                    is_finished = true,
                    score = correctCount
                )
            )
        }
    }

    if (isExamFinished) {
        ExamResultScreen(questions, userAnswers, onBack)
    } else if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val currentQuestion = questions[currentQuestionIndex]
        val optionsList = remember(currentQuestion) {
            val list = mutableListOf<String>()
            try {
                val arr = JSONArray(currentQuestion.options_json)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                list.addAll(currentQuestion.options_json.split("||"))
            }
            list
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Exam in Progress", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Question ${currentQuestionIndex + 1}/${questions.size}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    actions = {
                        Text(
                            formatTime(timeLeftSeconds),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeftSeconds < 300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                            enabled = currentQuestionIndex > 0
                        ) {
                            Text("Previous")
                        }
                        
                        if (currentQuestionIndex == questions.size - 1) {
                            Button(
                                onClick = { isExamFinished = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Finish Exam")
                            }
                        } else {
                            Button(onClick = {
                                if (selectedOption != null) {
                                    userAnswers[currentQuestionIndex] = selectedOption!!
                                }
                                currentQuestionIndex++
                                selectedOption = userAnswers[currentQuestionIndex]
                            }) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )

                Text(
                    text = currentQuestion.question_text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(24.dp))

                optionsList.forEachIndexed { index, option ->
                    val isSelected = selectedOption == index || userAnswers[currentQuestionIndex] == index
                    
                    Surface(
                        onClick = { selectedOption = index; userAnswers[currentQuestionIndex] = index },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(option)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamResultScreen(
    questions: List<QuizQuestion>,
    userAnswers: Map<Int, Int>,
    onBack: () -> Unit
) {
    var correctCount = 0
    val weakTopics = mutableSetOf<Int>()
    val weakObjectives = mutableSetOf<Int>()

    questions.forEachIndexed { index, question ->
        if (userAnswers[index] == question.correct_option) {
            correctCount++
        } else {
            weakTopics.add(question.topic_id)
            if (question.learning_objective_id != null) {
                weakObjectives.add(question.learning_objective_id)
            }
        }
    }
    val scorePercentage = if (questions.isNotEmpty()) (correctCount.toFloat() / questions.size * 100).toInt() else 0

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Exam Finished!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Your Final Score", style = MaterialTheme.typography.titleMedium)
            Text("$scorePercentage%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            
            Spacer(Modifier.height(16.dp))
            Text("$correctCount out of ${questions.size} correct", style = MaterialTheme.typography.bodyLarge)
            
            Spacer(Modifier.height(32.dp))
            
            if (weakTopics.isNotEmpty()) {
                Text("Areas for Improvement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• ${weakTopics.size} Weak Topics Detected", style = MaterialTheme.typography.bodyMedium)
                Text("• ${weakObjectives.size} Learning Objectives to Review", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
            }
            
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Back to Exams")
            }
            
            if (weakTopics.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Review Weak Areas")
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
