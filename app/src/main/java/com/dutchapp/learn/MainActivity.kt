package com.dutchapp.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dutchapp.learn.audio.LocalTts
import com.dutchapp.learn.audio.rememberTtsManager
import com.dutchapp.learn.ui.screens.HomeScreen
import com.dutchapp.learn.ui.screens.LessonScreen
import com.dutchapp.learn.ui.screens.ProfileScreen
import com.dutchapp.learn.ui.screens.ResultScreen
import com.dutchapp.learn.ui.theme.DutchLearnTheme
import com.dutchapp.learn.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DutchLearnTheme {
                val tts = rememberTtsManager()
                CompositionLocalProvider(LocalTts provides tts) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()
    val progress by appViewModel.progress.collectAsState()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                course = appViewModel.course,
                progress = progress,
                onLessonClick = { lesson -> navController.navigate("lesson/${lesson.id}") },
                bottomBar = {
                    BottomBar(
                        current = "home",
                        onHome = { },
                        onProfile = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                course = appViewModel.course,
                progress = progress,
                onReset = { appViewModel.resetProgress() },
                bottomBar = {
                    BottomBar(
                        current = "profile",
                        onHome = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onProfile = { }
                    )
                }
            )
        }

        composable(
            route = "lesson/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { entry ->
            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
            val session = androidx.compose.runtime.remember(lessonId) {
                appViewModel.buildSession(lessonId)
            }
            LessonScreen(
                session = session,
                onExit = { navController.popBackStack() },
                onFinish = { result ->
                    if (result.passed) {
                        appViewModel.completeLesson(lessonId, result.stars, result.xp)
                    }
                    val route = "result/$lessonId/${result.correct}/${result.total}/" +
                        "${result.stars}/${result.xp}/${if (result.passed) 1 else 0}"
                    navController.navigate(route) {
                        popUpTo("lesson/$lessonId") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "result/{lessonId}/{correct}/{total}/{stars}/{xp}/{passed}",
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("correct") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType },
                navArgument("stars") { type = NavType.IntType },
                navArgument("xp") { type = NavType.IntType },
                navArgument("passed") { type = NavType.IntType }
            )
        ) { entry ->
            val args = entry.arguments!!
            val lessonId = args.getString("lessonId").orEmpty()
            ResultScreen(
                stars = args.getInt("stars"),
                correct = args.getInt("correct"),
                total = args.getInt("total"),
                xp = args.getInt("xp"),
                passed = args.getInt("passed") == 1,
                onContinue = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRetry = {
                    navController.navigate("lesson/$lessonId") {
                        popUpTo("home")
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomBar(
    current: String,
    onHome: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("Impara", Icons.Filled.Home, current == "home", onHome)
        BottomItem("Profilo", Icons.Filled.Person, current == "profile", onProfile)
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 24.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
