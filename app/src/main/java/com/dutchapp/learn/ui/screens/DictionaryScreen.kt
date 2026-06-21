package com.dutchapp.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.data.model.VocabItem
import com.dutchapp.learn.ui.theme.LeafGreen

@Composable
fun DictionaryScreen(
    words: List<VocabItem>,
    learned: Set<String>,
    onSpeak: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, words) {
        if (query.isBlank()) words
        else words.filter {
            it.nl.contains(query, ignoreCase = true) || it.it.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Text(
                "Le mie parole",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp)
            )
            Text(
                "${words.size} parole · ${learned.size} imparate",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Cerca in italiano o olandese…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filtered, key = { it.nl }) { word ->
                    WordRow(word, isLearned = learned.contains(word.nl), onSpeak = { onSpeak(word.nl) })
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: VocabItem, isLearned: Boolean, onSpeak: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onSpeak() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(word.emoji.ifBlank { "🔤" }, fontSize = 26.sp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(word.nl, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (isLearned) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Imparata", tint = LeafGreen, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                word.it + (if (word.en.isNotBlank()) "  ·  ${word.en}" else ""),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onSpeak() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Ascolta", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        }
    }
}
