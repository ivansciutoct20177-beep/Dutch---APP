package com.dutchapp.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.audio.LocalTts
import com.dutchapp.learn.data.model.GlossaryEntry
import com.dutchapp.learn.data.model.Story

private fun norm(s: String): String = s.lowercase().filter { it.isLetter() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryReaderScreen(story: Story, onBack: () -> Unit) {
    val tts = LocalTts.current
    val glossary = remember(story) { story.glossary.associateBy { norm(it.nl) } }
    var selectedWord by remember(story) { mutableStateOf<String?>(null) }
    var selectedEntry by remember(story) { mutableStateOf<GlossaryEntry?>(null) }
    val revealed = remember(story) { mutableStateListOf<Int>() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Header
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp).clickable { tts?.stop(); onBack() }
            )
            Column(Modifier.weight(1f)) {
                Text(story.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                if (story.titleIt.isNotBlank()) {
                    Text(story.titleIt, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(story.level, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Playback controls
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlPill("Ascolta", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                tts?.speakSequence(story.sentences.map { it.nl }, slow = false)
            }
            ControlPill("Lento", null, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), emoji = "🐢") {
                tts?.speakSequence(story.sentences.map { it.nl }, slow = true)
            }
            ControlPill("Stop", Icons.Filled.Stop, MaterialTheme.colorScheme.error, Modifier.weight(1f)) {
                tts?.stop()
            }
        }

        if (story.intro.isNotBlank()) {
            Text(
                story.intro,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Story body
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            story.sentences.forEachIndexed { index, sentence ->
                SentenceBlock(
                    index = index,
                    nl = sentence.nl,
                    it = sentence.it,
                    glossary = glossary,
                    revealed = revealed.contains(index),
                    onSpeakSentence = { tts?.speak(sentence.nl) },
                    onToggleIt = {
                        if (revealed.contains(index)) revealed.remove(index) else revealed.add(index)
                    },
                    onWordTap = { clean, entry ->
                        selectedWord = clean
                        selectedEntry = entry
                        tts?.speak(clean)
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // Tapped-word meaning panel
        if (selectedWord != null) {
            WordPanel(
                word = selectedWord!!,
                entry = selectedEntry,
                onSpeak = { tts?.speak(selectedWord!!) },
                onClose = { selectedWord = null; selectedEntry = null }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceBlock(
    index: Int,
    nl: String,
    it: String,
    glossary: Map<String, GlossaryEntry>,
    revealed: Boolean,
    onSpeakSentence: () -> Unit,
    onToggleIt: () -> Unit,
    onWordTap: (String, GlossaryEntry?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniIcon(Icons.Filled.VolumeUp, MaterialTheme.colorScheme.secondary, onSpeakSentence)
            MiniIcon(Icons.Filled.Translate, MaterialTheme.colorScheme.primary, onToggleIt)
            Text("${index + 1}", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            nl.split(" ").forEach { token ->
                if (token.isBlank()) return@forEach
                val clean = token.filter { it.isLetter() }
                val entry = glossary[norm(token)]
                val highlighted = entry != null
                Text(
                    text = token,
                    fontSize = 19.sp,
                    color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (highlighted) TextDecoration.Underline else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onWordTap(clean.ifBlank { token }, entry) }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                )
            }
        }
        if (revealed) {
            Spacer(Modifier.height(2.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun MiniIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ControlPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color,
    modifier: Modifier = Modifier,
    emoji: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        } else if (emoji.isNotBlank()) {
            Text(emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.size(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun WordPanel(
    word: String,
    entry: GlossaryEntry?,
    onSpeak: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary).clickable { onSpeak() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Ascolta", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(word, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                entry?.let { it.it + (if (it.en.isNotBlank()) "  ·  ${it.en}" else "") }
                    ?: "Tocca una parola evidenziata per il significato",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Chiudi",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp).clickable { onClose() }
        )
    }
}
