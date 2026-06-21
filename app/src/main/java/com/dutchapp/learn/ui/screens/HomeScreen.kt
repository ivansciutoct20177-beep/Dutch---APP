package com.dutchapp.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.data.Progress
import com.dutchapp.learn.data.model.Course
import com.dutchapp.learn.data.model.CourseUnit
import com.dutchapp.learn.data.model.Lesson
import com.dutchapp.learn.data.model.Level
import com.dutchapp.learn.ui.components.DailyGoalCard
import com.dutchapp.learn.ui.components.StarsRow
import com.dutchapp.learn.ui.components.StatsHeader
import com.dutchapp.learn.viewmodel.CourseProgressLogic
import com.dutchapp.learn.viewmodel.LessonState

@Composable
fun HomeScreen(
    course: Course,
    progress: Progress,
    onLessonClick: (Lesson) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                StatsHeader(xp = progress.xp, streak = progress.streak)
                DailyGoalCard(todayXp = progress.todayXp, dailyGoal = progress.dailyGoal)
                Text(
                    text = "Leer Nederlands",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
                Text(
                    text = "Impara l'olandese passo dopo passo · A1 → C1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            course.levels.forEach { level ->
                item(key = "level_${level.id}") {
                    LevelHeader(level)
                }
                level.units.forEach { unit ->
                    item(key = "unit_${unit.id}") {
                        UnitHeader(unit)
                    }
                    item(key = "lessons_${unit.id}") {
                        UnitLessons(
                            course = course,
                            progress = progress,
                            unit = unit,
                            onLessonClick = onLessonClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelHeader(level: Level) {
    val color = runCatching { Color(android.graphics.Color.parseColor(level.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = level.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            if (level.subtitle.isNotBlank()) {
                Text(
                    text = level.subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun UnitHeader(unit: CourseUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(unit.icon, fontSize = 24.sp)
        Text(
            text = unit.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UnitLessons(
    course: Course,
    progress: Progress,
    unit: CourseUnit,
    onLessonClick: (Lesson) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        unit.lessons.forEachIndexed { index, lesson ->
            val state = CourseProgressLogic.lessonState(course, progress, lesson.id)
            val bias = when (index % 4) {
                0 -> 0f
                1 -> 0.55f
                2 -> 0f
                else -> -0.55f
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Box(Modifier.align(BiasAlignment(bias, 0f))) {
                    LessonNode(
                        lesson = lesson,
                        state = state,
                        stars = progress.starsFor(lesson.id),
                        onClick = { if (state != LessonState.LOCKED) onLessonClick(lesson) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonNode(
    lesson: Lesson,
    state: LessonState,
    stars: Int,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, icon) = when (state) {
        LessonState.COMPLETED -> scheme.tertiary to Icons.Filled.Check
        LessonState.UNLOCKED -> scheme.primary to Icons.Filled.PlayArrow
        LessonState.LOCKED -> scheme.outline to Icons.Filled.Lock
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = 4.dp,
                    color = bg.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .clickable(enabled = state != LessonState.LOCKED) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = lesson.title,
            fontSize = 12.sp,
            color = scheme.onBackground.copy(alpha = if (state == LessonState.LOCKED) 0.4f else 0.85f),
            fontWeight = FontWeight.SemiBold
        )
        if (state == LessonState.COMPLETED) {
            StarsRow(stars = stars, starSize = 14)
        }
    }
}
