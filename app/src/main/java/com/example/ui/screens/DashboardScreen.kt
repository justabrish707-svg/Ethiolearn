package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Progress
import com.example.data.model.Topic
import com.example.data.model.UnitTable
import com.example.data.model.Subject
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val allUnits by viewModel.allUnits.collectAsStateWithLifecycle()
    val allTopics by viewModel.allTopics.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAllProgress()
    }

    // Calculations
    val completedCount = allProgress.count { it.completed_lessons }
    val totalTopicsCount = allTopics.size
    val overallCompletionPercent = if (totalTopicsCount > 0) (completedCount.toFloat() / totalTopicsCount * 100).toInt() else 0

    val quizScores = allProgress.filter { it.quiz_score > 0 }.map { it.quiz_score }
    val avgQuizScore = if (quizScores.isNotEmpty()) quizScores.average().toInt() else 0

    val timeRemainingList = allProgress.filter { it.quiz_score > 0 }.map { it.last_quiz_remaining_time_seconds }
    val avgTimeRemaining = if (timeRemainingList.isNotEmpty()) timeRemainingList.average().toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Progress Dashboard", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dashboard_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("dashboard_scrollable_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OverallStatsCard(
                    completedCount = completedCount,
                    totalTopicsCount = totalTopicsCount,
                    completionPercent = overallCompletionPercent,
                    avgQuizScore = avgQuizScore,
                    avgTimeRemaining = avgTimeRemaining
                )
            }

            item {
                Text(
                    text = "Grade Level Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(9, 10, 11, 12).forEach { gradeId ->
                            val gradeSubjects: List<Subject> = allSubjects.filter { it.grade_id == gradeId }
                            val gradeTopics: List<Topic> = allTopics.filter { topic: Topic -> 
                                gradeSubjects.any { sub: Subject -> 
                                    allUnits.find { u: UnitTable -> u.id == topic.unit_id }?.subject_id == sub.id 
                                }
                            }
                            
                            val gradeTotal = gradeTopics.size
                            val gradeCompleted = gradeTopics.count { topic: Topic ->
                                allProgress.any { p: Progress -> p.topic_id == topic.id && p.completed_lessons }
                            }
                            val gradePercent = if (gradeTotal > 0) (gradeCompleted.toFloat() / gradeTotal * 100).toInt() else 0
                            
                            GradeProgressRow(
                                gradeName = "Grade $gradeId",
                                percentage = gradePercent,
                                completed = gradeCompleted,
                                total = gradeTotal
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Unit Progress Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (allUnits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(allUnits) { unit ->
                    val unitTopics = allTopics.filter { it.unit_id == unit.id }
                    val unitTotal = unitTopics.size
                    val unitCompleted = unitTopics.count { topic ->
                        allProgress.any { p -> p.topic_id == topic.id && p.completed_lessons }
                    }
                    val unitPercent = if (unitTotal > 0) (unitCompleted.toFloat() / unitTotal * 100).toInt() else 0

                    val unitQuizzes = unitTopics.mapNotNull { topic ->
                        allProgress.find { it.topic_id == topic.id && it.quiz_score > 0 }?.quiz_score
                    }
                    val unitQuizAvg = if (unitQuizzes.isNotEmpty()) unitQuizzes.average().toInt() else 0

                    UnitProgressCard(
                        unit = unit,
                        completedCount = unitCompleted,
                        totalCount = unitTotal,
                        percentage = unitPercent,
                        avgQuizScore = unitQuizAvg
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OverallStatsCard(
    completedCount: Int,
    totalTopicsCount: Int,
    completionPercent: Int,
    avgQuizScore: Int,
    avgTimeRemaining: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_stats_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your Academic Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatMetric(
                        icon = Icons.Default.AssignmentTurnedIn,
                        value = "$completedCount / $totalTopicsCount",
                        label = "Lessons Done",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    StatMetric(
                        icon = Icons.Default.Percent,
                        value = "$completionPercent%",
                        label = "Completion",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    StatMetric(
                        icon = Icons.Default.Star,
                        value = if (avgQuizScore > 0) "$avgQuizScore%" else "N/A",
                        label = "Quiz Avg",
                        tint = Color(0xFFFFB300)
                    )
                    
                    val mins = avgTimeRemaining / 60
                    val secs = avgTimeRemaining % 60
                    StatMetric(
                        icon = Icons.Default.AccessTime,
                        value = if (avgTimeRemaining > 0) String.format("%01d:%02d", mins, secs) else "N/A",
                        label = "Avg Time",
                        tint = Color(0xFF673AB7)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Overall Progress",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$completionPercent%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completionPercent.toFloat() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetric(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = tint.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun UnitProgressCard(
    unit: UnitTable,
    completedCount: Int,
    totalCount: Int,
    percentage: Int,
    avgQuizScore: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unit_progress_card_${unit.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = borderStrokeForProgress(percentage),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit ${unit.unit_number}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = unit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { percentage.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (percentage == 100) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount / $totalCount topics mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (avgQuizScore > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Quiz Score",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Score: $avgQuizScore%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeProgressRow(
    gradeName: String,
    percentage: Int,
    completed: Int,
    total: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gradeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$percentage% ($completed/$total)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage.toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun borderStrokeForProgress(percentage: Int): androidx.compose.foundation.BorderStroke? {
    return if (percentage == 100) {
        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
    } else null
}
