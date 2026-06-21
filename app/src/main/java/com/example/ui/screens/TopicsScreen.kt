package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(viewModel: MainViewModel, unitId: Int, onNavigateToLesson: (Int) -> Unit, onBack: () -> Unit) {
    val topics by viewModel.topics.collectAsState()

    LaunchedEffect(unitId) {
        viewModel.loadTopics(unitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn {
                items(topics) { topic ->
                    ListItem(
                        headlineContent = { Text(topic.title, fontWeight = FontWeight.Medium) },
                        trailingContent = { Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go") },
                        modifier = Modifier.clickable { onNavigateToLesson(topic.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
