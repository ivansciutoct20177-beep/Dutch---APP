package com.dutchapp.learn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.ui.theme.GoldYellow
import com.dutchapp.learn.ui.theme.Orange

/** Darken a color by a factor (0..1) for the 3D button base. */
fun Color.darken(factor: Float = 0.78f): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

/** Chunky 3D button in the Duolingo style. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White
) {
    val base = if (enabled) containerColor else MaterialTheme.colorScheme.outline
    val shade = base.darken()
    Box(modifier.height(56.dp)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shade)
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(base)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) contentColor else Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Selectable answer card used in multiple-choice exercises. */
@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    correct: Boolean? = null,        // null = not revealed; true/false = revealed
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor: Color
    val bg: Color
    when {
        correct == true -> { borderColor = scheme.tertiary; bg = scheme.tertiary.copy(alpha = 0.12f) }
        correct == false -> { borderColor = scheme.error; bg = scheme.error.copy(alpha = 0.10f) }
        selected -> { borderColor = scheme.primary; bg = scheme.primaryContainer.copy(alpha = 0.5f) }
        else -> { borderColor = scheme.outline; bg = scheme.surface }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Thin progress bar shown at the top of a lesson. */
@Composable
fun LessonProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

/** Row of three stars indicating lesson mastery. */
@Composable
fun StarsRow(stars: Int, starSize: Int = 22) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Icon(
                imageVector = if (i < stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i < stars) GoldYellow else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}

/** Compact pill showing an icon + value (XP, streak). */
@Composable
fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Top stats bar: streak + XP. */
@Composable
fun StatsHeader(xp: Int, streak: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatPill(Icons.Filled.LocalFireDepartment, streak.toString(), Orange)
        StatPill(Icons.Filled.Bolt, xp.toString(), GoldYellow)
    }
}

/** Daily goal ring with current/target XP (Duolingo-style daily goal). */
@Composable
fun DailyGoalCard(todayXp: Int, dailyGoal: Int, modifier: Modifier = Modifier) {
    val frac = if (dailyGoal <= 0) 1f else (todayXp / dailyGoal.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { frac },
                modifier = Modifier.size(54.dp),
                color = GoldYellow,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                strokeWidth = 6.dp
            )
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Obiettivo di oggi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$todayXp / $dailyGoal XP",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (frac >= 1f) {
            Text("✅", fontSize = 22.sp)
        }
    }
}
