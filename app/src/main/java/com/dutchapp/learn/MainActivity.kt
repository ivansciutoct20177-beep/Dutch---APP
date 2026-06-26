package com.dutchapp.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dutchapp.learn.audio.LocalTts
import com.dutchapp.learn.audio.rememberTtsManager
import com.dutchapp.learn.ui.screens.DictionaryScreen
import com.dutchapp.learn.ui.screens.HomeScreen
import com.dutchapp.learn.ui.screens.LessonScreen
import com.dutchapp.learn.ui.screens.ProfileScreen
import com.dutchapp.learn.ui.screens.ResultScreen
import com.dutchapp.learn.ui.screens.ReviewScreen
import com.dutchapp.learn.ui.screens.SettingsScreen
import com.dutchapp.learn.ui.screens.StoriesListScreen
import com.dutchapp.learn.ui.screens.StoryReaderScreen
import com.dutchapp.learn.ui.theme.DutchLearnTheme
import com.dutchapp.learn.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val settings by appViewModel.settings.collectAsState()
            val darkTheme = when (settings.darkMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            DutchLearnTheme(darkTheme = darkTheme) {
                val tts = rememberTtsManager()
                LaunchedEffect(settings.soundEnabled) { tts.muted = !settings.soundEnabled }
                CompositionLocalProvider(LocalTts provides tts) {
                    AppNavHost(appViewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val progress by appViewModel.progress.collectAsState()
    val settings by appViewModel.settings.collectAsState()
    val tts = LocalTts.current

    fun selectTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                course = appViewModel.course,
                progress = progress,
                onLessonClick = { lesson -> navController.navigate("lesson/${lesson.id}") },
                bottomBar = { BottomBar("home", ::selectTab) }
            )
        }

        composable("stories") {
            StoriesListScreen(
                stories = appViewModel.stories(),
                onOpen = { story -> navController.navigate("story/${story.id}") },
                bottomBar = { BottomBar("stories", ::selectTab) }
            )
        }

        composable(
            route = "story/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.StringType })
        ) { entry ->
            val storyId = entry.arguments?.getString("storyId").orEmpty()
            val story = appViewModel.story(storyId)
            if (story != null) {
                StoryReaderScreen(story = story, onBack = { navController.popBackStack() })
            } else {
                navController.popBackStack()
            }
        }

        composable("review") {
            ReviewScreen(
                reviewableCount = appViewModel.reviewableWords().size,
                onStartReview = { navController.navigate("reviewSession") },
                bottomBar = { BottomBar("review", ::selectTab) }
            )
        }

        composable("words") {
            val learned = remember(progress) { appViewModel.reviewableWords().map { it.nl }.toSet() }
            DictionaryScreen(
                words = appViewModel.allWords(),
                learned = learned,
                onSpeak = { tts?.speak(it) },
                bottomBar = { BottomBar("words", ::selectTab) }
            )
        }

        composable("profile") {
            ProfileScreen(
                course = appViewModel.course,
                progress = progress,
                onOpenSettings = { navController.navigate("settings") },
                bottomBar = { BottomBar("profile", ::selectTab) }
            )
        }

        composable("settings") {
            SettingsScreen(
                settings = settings,
                onDarkMode = { appViewModel.setDarkMode(it) },
                onSound = { appViewModel.setSound(it) },
                onDailyGoal = { appViewModel.setDailyGoal(it) },
                onReset = { appViewModel.resetProgress() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "lesson/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { entry ->
            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
            val session = remember(lessonId) { appViewModel.buildSession(lessonId) }
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

        composable("reviewSession") {
            val session = remember { appViewModel.buildReviewSession() }
            LessonScreen(
                session = session,
                onExit = { navController.popBackStack() },
                onFinish = { result ->
                    if (result.passed) appViewModel.completeReview(result.xp)
                    val route = "result/review/${result.correct}/${result.total}/" +
                        "${result.stars}/${result.xp}/${if (result.passed) 1 else 0}"
                    navController.navigate(route) {
                        popUpTo("reviewSession") { inclusive = true }
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
                    val dest = if (lessonId == "review") "reviewSession" else "lesson/$lessonId"
                    navController.navigate(dest) { popUpTo("home") }
                }
            )
        }
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("Impara", Icons.Filled.Home, current == "home") { onSelect("home") }
        BottomItem("Storie", Icons.Filled.AutoStories, current == "stories") { onSelect("stories") }
        BottomItem("Ripasso", Icons.Filled.Autorenew, current == "review") { onSelect("review") }
        BottomItem("Parole", Icons.Filled.Translate, current == "words") { onSelect("words") }
        BottomItem("Profilo", Icons.Filled.Person, current == "profile") { onSelect("profile") }
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
