package com.dutchapp.learn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.ui.components.ChunkyButton
import com.dutchapp.learn.ui.theme.ErrorRed
import com.dutchapp.learn.ui.theme.GoldYellow
import com.dutchapp.learn.ui.theme.LeafGreen

@Composable
fun ResultScreen(
    stars: Int,
    correct: Int,
    total: Int,
    xp: Int,
    passed: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (passed) LeafGreen else ErrorRed,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (passed) "Lezione completata!" else "Cuori esauriti",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (passed) "Goed gedaan! 🇳🇱" else "Riprova: imparerai ancora meglio",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(24.dp))

            if (passed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = if (i < stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (i < stars) GoldYellow else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(
                    label = "Risposte",
                    value = "$correct / $total",
                    color = LeafGreen
                )
                StatBox(
                    label = "XP guadagnati",
                    value = "+$xp",
                    color = GoldYellow,
                    icon = true
                )
            }

            Spacer(Modifier.height(36.dp))
            ChunkyButton(
                text = if (passed) "Continua" else "Riprova",
                onClick = if (passed) onContinue else onRetry,
                containerColor = if (passed) LeafGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            if (!passed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Torna alla mappa",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onContinue() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, icon: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
