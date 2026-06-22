package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.db.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database.appDao(), applicationContext)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val factory = MainViewModelFactory(repository)
                val viewModel: MainViewModel = viewModel(factory = factory)
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel, 
                                onNavigateToGrade = { gradeId ->
                                    navController.navigate("subjects/$gradeId")
                                },
                                onNavigateToLesson = { topicId ->
                                    navController.navigate("lesson/$topicId")
                                }
                            )
                        }
                        composable(
                            "subjects/{gradeId}",
                            arguments = listOf(navArgument("gradeId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val gradeId = backStackEntry.arguments?.getInt("gradeId") ?: 0
                            SubjectsScreen(viewModel, gradeId, onNavigateToUnits = { subjectId ->
                                navController.navigate("units/$subjectId")
                            }, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "units/{subjectId}",
                            arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0
                            UnitsScreen(viewModel, subjectId, onNavigateToTopics = { unitId ->
                                navController.navigate("topics/$unitId")
                            }, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "topics/{unitId}",
                            arguments = listOf(navArgument("unitId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val unitId = backStackEntry.arguments?.getInt("unitId") ?: 0
                            TopicsScreen(viewModel, unitId, onNavigateToLesson = { topicId ->
                                navController.navigate("lesson/$topicId")
                            }, onBack = { navController.popBackStack() })
                        }
                        composable(
                            "lesson/{topicId}",
                            arguments = listOf(navArgument("topicId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val topicId = backStackEntry.arguments?.getInt("topicId") ?: 0
                            LessonScreen(viewModel, topicId, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

