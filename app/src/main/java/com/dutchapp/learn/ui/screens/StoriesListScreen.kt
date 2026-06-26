package com.dutchapp.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.data.model.Story

@Composable
fun StoriesListScreen(
    stories: List<Story>,
    onOpen: (Story) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val order = listOf("A1", "A2", "B1", "B2", "C1")
    val grouped = stories.groupBy { it.level }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Text(
                    "Racconti",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                )
                Text(
                    "Piccole storie da A1 a C1: ascoltale (anche lentamente) e tocca le parole per il significato.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            if (stories.isEmpty()) {
                item {
                    Text(
                        "Nessun racconto disponibile.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            order.forEach { level ->
                val list = grouped[level].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "h_$level") { LevelChip(level) }
                    items(list, key = { it.id }) { story ->
                        StoryCard(story, onClick = { onOpen(story) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(level: String) {
    Box(
        Modifier
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(level, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun StoryCard(story: Story, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(story.icon, fontSize = 34.sp)
        Column(Modifier.weight(1f)) {
            Text(story.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (story.titleIt.isNotBlank()) {
                Text(story.titleIt, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Text(
                "${story.sentences.size} frasi · ${story.glossary.size} termini chiave",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
    }
}
