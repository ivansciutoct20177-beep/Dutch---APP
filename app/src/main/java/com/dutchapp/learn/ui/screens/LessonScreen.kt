package com.dutchapp.learn.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutchapp.learn.audio.LocalTts
import com.dutchapp.learn.data.model.Exercise
import com.dutchapp.learn.data.model.IntroExercise
import com.dutchapp.learn.data.model.ListenExercise
import com.dutchapp.learn.data.model.MatchExercise
import com.dutchapp.learn.data.model.PictureExercise
import com.dutchapp.learn.data.model.SpeakExercise
import com.dutchapp.learn.data.model.TipExercise
import com.dutchapp.learn.data.model.TranslateExercise
import com.dutchapp.learn.data.model.TypeExercise
import com.dutchapp.learn.data.model.VocabItem
import com.dutchapp.learn.data.model.graded
import com.dutchapp.learn.ui.components.ChunkyButton
import com.dutchapp.learn.ui.components.LessonProgressBar
import com.dutchapp.learn.ui.components.OptionCard
import com.dutchapp.learn.ui.theme.ErrorRed
import com.dutchapp.learn.ui.theme.LeafGreen
import com.dutchapp.learn.ui.theme.Orange

data class LessonResult(
    val correct: Int,
    val total: Int,
    val stars: Int,
    val xp: Int,
    val passed: Boolean
)

private fun normalize(s: String): String =
    s.trim().lowercase().replace(Regex("[.!?,;]"), "").replace(Regex("\\s+"), " ")

@Composable
fun LessonScreen(
    session: List<Exercise>,
    onExit: () -> Unit,
    onFinish: (LessonResult) -> Unit
) {
    if (session.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Lezione non disponibile")
        }
        return
    }

    val tts = LocalTts.current
    val gradedTotal = remember(session) { session.count { it.graded } }

    var index by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }
    var wrong by remember { mutableStateOf(0) }
    var hearts by remember { mutableStateOf(5) }

    // Per-step state
    var selected by remember { mutableStateOf(-1) }
    var typed by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf(false) }
    var matchDone by remember { mutableStateOf(false) }

    val current = session[index]

    fun resetStep() {
        selected = -1
        typed = ""
        revealed = false
        lastCorrect = false
        matchDone = false
    }

    // Autoplay Dutch audio when a step appears
    LaunchedEffect(index) {
        when (val ex = current) {
            is IntroExercise -> tts?.speak(ex.item.nl)
            is ListenExercise -> tts?.speak(ex.target.nl)
            is PictureExercise -> tts?.speak(ex.target.nl)
            is SpeakExercise -> tts?.speak(ex.target.nl)
            else -> {}
        }
    }

    fun evaluate(): Boolean = when (val ex = current) {
        is PictureExercise -> selected >= 0 && ex.options[selected].nl == ex.target.nl
        is TranslateExercise -> selected >= 0 && ex.options[selected].nl == ex.target.nl
        is ListenExercise -> selected >= 0 && ex.options[selected].nl == ex.target.nl
        is TypeExercise -> normalize(typed) == normalize(ex.target.nl)
        is MatchExercise -> matchDone
        is IntroExercise -> true
        is TipExercise -> true
        is SpeakExercise -> true
    }

    fun answerProvided(): Boolean = when (current) {
        is TypeExercise -> typed.isNotBlank()
        is MatchExercise -> matchDone
        is IntroExercise, is TipExercise, is SpeakExercise -> true
        else -> selected >= 0
    }

    fun advanceOrFinish() {
        if (index < session.lastIndex) {
            index += 1
            resetStep()
        } else {
            val stars = when {
                wrong == 0 -> 3
                wrong <= 2 -> 2
                else -> 1
            }
            val xp = 10 + correct * 2 + if (wrong == 0) 5 else 0
            onFinish(LessonResult(correct, gradedTotal, stars, xp, passed = true))
        }
    }

    fun onPrimary() {
        when {
            !current.graded -> advanceOrFinish()
            !revealed -> {
                val ok = evaluate()
                revealed = true
                lastCorrect = ok
                if (ok) {
                    correct += 1
                    // pronounce the Dutch answer as positive reinforcement
                    when (val ex = current) {
                        is TranslateExercise -> tts?.speak(ex.target.nl)
                        is TypeExercise -> tts?.speak(ex.target.nl)
                        else -> {}
                    }
                } else {
                    wrong += 1
                    hearts -= 1
                    if (hearts <= 0) {
                        onFinish(
                            LessonResult(correct, gradedTotal, stars = 0, xp = 0, passed = false)
                        )
                        return
                    }
                }
            }
            else -> advanceOrFinish()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header: close, progress, hearts
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Esci",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp).clickable { onExit() }
            )
            LessonProgressBar(
                progress = index / session.size.toFloat(),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
            Text(hearts.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        // Body
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            when (val ex = current) {
                is TipExercise -> TipContent(ex)
                is SpeakExercise -> SpeakContent(ex) { tts?.speak(ex.target.nl) }
                is IntroExercise -> IntroContent(ex.item) { tts?.speak(ex.item.nl) }
                is PictureExercise -> PictureContent(
                    ex = ex, selected = selected, revealed = revealed,
                    onSelect = { if (!revealed) selected = it },
                    onSpeak = { tts?.speak(ex.target.nl) }
                )
                is TranslateExercise -> TranslateContent(
                    ex = ex, selected = selected, revealed = revealed,
                    onSelect = { if (!revealed) selected = it },
                    onSpeak = { tts?.speak(ex.target.nl) }
                )
                is ListenExercise -> ListenContent(
                    ex = ex, selected = selected, revealed = revealed,
                    onSelect = { if (!revealed) selected = it },
                    onSpeak = { tts?.speak(ex.target.nl) },
                    onSpeakSlow = { tts?.speakSlow(ex.target.nl) }
                )
                is TypeExercise -> TypeContent(
                    ex = ex, typed = typed, revealed = revealed, correct = lastCorrect,
                    onChange = { if (!revealed) typed = it }
                )
                is MatchExercise -> MatchContent(
                    ex = ex,
                    onComplete = { matchDone = true; correct += 0 },
                    onSpeak = { tts?.speak(it) }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Feedback + primary button
        FeedbackAndButton(
            revealed = revealed,
            nonGraded = !current.graded,
            isCorrect = lastCorrect,
            correctAnswer = correctAnswerText(current),
            enabled = answerProvided(),
            onClick = { onPrimary() }
        )
    }
}

private fun correctAnswerText(ex: Exercise): String = when (ex) {
    is TranslateExercise -> if (ex.promptInDutch) ex.target.it else ex.target.nl
    is PictureExercise -> "${ex.target.emoji}  ${ex.target.nl}"
    is ListenExercise -> ex.target.nl
    is TypeExercise -> ex.target.nl
    else -> ""
}

/* ----------------------------- Exercise bodies ----------------------------- */

@Composable
private fun SpeakerButton(size: Int = 56, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.VolumeUp, contentDescription = "Ascolta", tint = Color.White, modifier = Modifier.size((size * 0.5).dp))
    }
}

@Composable
private fun IntroContent(item: VocabItem, onSpeak: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nuova parola", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (item.hasPicture) {
            Text(item.emoji, fontSize = 96.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                item.nl,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            SpeakerButton(size = 44, onClick = onSpeak)
        }
        Text(
            item.it + (if (item.en.isNotBlank()) "  ·  ${item.en}" else ""),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        if (item.exampleNl.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onSpeak() }
                    .padding(14.dp)
            ) {
                Column {
                    Text("\"${item.exampleNl}\"", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    if (item.exampleIt.isNotBlank()) {
                        Text(item.exampleIt, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TipContent(ex: TipExercise) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("💡 Suggerimento", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            ex.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text(
                ex.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SpeakContent(ex: SpeakExercise, onSpeakModel: () -> Unit) {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var recognized by remember(ex) { mutableStateOf<String?>(null) }
    var listening by remember(ex) { mutableStateOf(false) }
    var matched by remember(ex) { mutableStateOf<Boolean?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val recognizer = remember(ex) {
        if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(ex) {
        onDispose { recognizer?.destroy() }
    }

    fun startListening() {
        val r = recognizer ?: return
        recognized = null
        matched = null
        listening = true
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val said = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                recognized = said
                val n1 = normalize(said)
                val n2 = normalize(ex.target.nl)
                matched = said.isNotBlank() && (n1.contains(n2) || n2.contains(n1))
                listening = false
            }
            override fun onError(error: Int) {
                listening = false
                if (matched == null) matched = false
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        r.startListening(intent)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) startListening()
    }

    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🗣️ Pronuncia", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (ex.target.hasPicture) {
            Text(ex.target.emoji, fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ex.target.nl, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            SpeakerButton(size = 44, onClick = onSpeakModel)
        }
        Text("Tocca il microfono e ripeti", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (listening) ErrorRed else MaterialTheme.colorScheme.secondary)
                .clickable {
                    when {
                        !available -> {}
                        !hasPermission -> permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        else -> startListening()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Parla", tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        when {
            !available -> Text(
                "Riconoscimento vocale non disponibile qui. Puoi continuare.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            listening -> Text("Sto ascoltando…", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            matched == true -> Text("Ottima pronuncia! ✅", color = LeafGreen, fontWeight = FontWeight.Bold)
            matched == false -> Text("Ho sentito: \"${recognized.orEmpty()}\". Riprova 🎙️", color = Orange, fontWeight = FontWeight.SemiBold)
            else -> {}
        }
    }
}

@Composable
private fun Prompt(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
    )
}

private fun optionCorrectness(revealed: Boolean, isTarget: Boolean, isSelected: Boolean): Boolean? =
    when {
        !revealed -> null
        isTarget -> true
        isSelected -> false
        else -> null
    }

@Composable
private fun PictureContent(
    ex: PictureExercise,
    selected: Int,
    revealed: Boolean,
    onSelect: (Int) -> Unit,
    onSpeak: () -> Unit
) {
    Prompt("Quale immagine?")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SpeakerButton(onClick = onSpeak)
        Text(ex.target.nl, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
    Spacer(Modifier.height(16.dp))
    ex.options.chunked(2).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            rowItems.forEach { opt ->
                val idx = ex.options.indexOf(opt)
                OptionCard(
                    modifier = Modifier.weight(1f).height(120.dp).padding(vertical = 6.dp),
                    selected = selected == idx,
                    correct = optionCorrectness(revealed, opt.nl == ex.target.nl, selected == idx),
                    onClick = { onSelect(idx) }
                ) {
                    Text(opt.emoji, fontSize = 56.sp)
                }
            }
        }
    }
}

@Composable
private fun TranslateContent(
    ex: TranslateExercise,
    selected: Int,
    revealed: Boolean,
    onSelect: (Int) -> Unit,
    onSpeak: () -> Unit
) {
    Prompt(if (ex.promptInDutch) "Traduci in italiano" else "Traduci in olandese")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (ex.promptInDutch) SpeakerButton(onClick = onSpeak)
        Text(
            if (ex.promptInDutch) ex.target.nl else ex.target.it,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
    Spacer(Modifier.height(20.dp))
    ex.options.forEachIndexed { idx, opt ->
        OptionCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            selected = selected == idx,
            correct = optionCorrectness(revealed, opt.nl == ex.target.nl, selected == idx),
            onClick = { onSelect(idx) }
        ) {
            Text(
                if (ex.promptInDutch) opt.it else opt.nl,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ListenContent(
    ex: ListenExercise,
    selected: Int,
    revealed: Boolean,
    onSelect: (Int) -> Unit,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit
) {
    Prompt("Cosa senti?")
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        SpeakerButton(size = 72, onClick = onSpeak)
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onSpeakSlow() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("🐢 Lento", color = MaterialTheme.colorScheme.onBackground)
        }
    }
    Spacer(Modifier.height(20.dp))
    ex.options.forEachIndexed { idx, opt ->
        OptionCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            selected = selected == idx,
            correct = optionCorrectness(revealed, opt.nl == ex.target.nl, selected == idx),
            onClick = { onSelect(idx) }
        ) {
            Text(
                opt.nl,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TypeContent(
    ex: TypeExercise,
    typed: String,
    revealed: Boolean,
    correct: Boolean,
    onChange: (String) -> Unit
) {
    Prompt("Scrivi in olandese")
    Text(
        ex.target.it,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    if (ex.target.en.isNotBlank()) {
        Text(ex.target.en, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = typed,
        onValueChange = onChange,
        enabled = !revealed,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Digita qui…") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
    if (revealed && !correct) {
        Spacer(Modifier.height(10.dp))
        Text(
            "Risposta corretta: ${ex.target.nl}",
            color = ErrorRed,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatchContent(
    ex: MatchExercise,
    onComplete: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val pairs = ex.pairs
    val leftItems = remember(ex) { pairs.map { it.nl } }
    val rightItems = remember(ex) { pairs.map { it.it }.shuffled() }
    val matched = remember(ex) { mutableStateListOf<String>() } // nl values matched
    var selectedLeft by remember(ex) { mutableStateOf<String?>(null) }
    var selectedRight by remember(ex) { mutableStateOf<String?>(null) }
    var wrongFlash by remember(ex) { mutableStateOf<Pair<String, String>?>(null) }

    fun tryMatch() {
        val l = selectedLeft ?: return
        val r = selectedRight ?: return
        val pair = pairs.firstOrNull { it.nl == l }
        if (pair != null && pair.it == r) {
            matched.add(l)
            selectedLeft = null
            selectedRight = null
            wrongFlash = null
            if (matched.size == pairs.size) onComplete()
        } else {
            wrongFlash = l to r
            selectedLeft = null
            selectedRight = null
        }
    }

    Prompt("Abbina le coppie")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            leftItems.forEach { nl ->
                val done = matched.contains(nl)
                val isSel = selectedLeft == nl
                OptionCard(
                    selected = isSel,
                    correct = if (done) true else null,
                    onClick = {
                        if (!done) {
                            selectedLeft = nl
                            onSpeak(nl)
                            tryMatch()
                        }
                    }
                ) {
                    Text(nl, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rightItems.forEach { it_ ->
                val done = matched.any { m -> pairs.first { it.nl == m }.it == it_ }
                val isSel = selectedRight == it_
                OptionCard(
                    selected = isSel,
                    correct = if (done) true else null,
                    onClick = {
                        if (!done) {
                            selectedRight = it_
                            tryMatch()
                        }
                    }
                ) {
                    Text(it_, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
    if (matched.size == pairs.size) {
        Spacer(Modifier.height(12.dp))
        Text("Ottimo! Tutte abbinate ✅", color = LeafGreen, fontWeight = FontWeight.Bold)
    } else if (wrongFlash != null) {
        Spacer(Modifier.height(12.dp))
        Text("Riprova quella coppia", color = Orange, fontWeight = FontWeight.SemiBold)
    }
}

/* --------------------------- Feedback + button ---------------------------- */

@Composable
private fun FeedbackAndButton(
    revealed: Boolean,
    nonGraded: Boolean,
    isCorrect: Boolean,
    correctAnswer: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        !revealed || nonGraded -> MaterialTheme.colorScheme.surface
        isCorrect -> LeafGreen.copy(alpha = 0.15f)
        else -> ErrorRed.copy(alpha = 0.12f)
    }
    val buttonColor = when {
        nonGraded -> Orange
        !revealed -> Orange
        isCorrect -> LeafGreen
        else -> ErrorRed
    }
    val buttonText = when {
        nonGraded -> "Continua"
        !revealed -> "Controlla"
        else -> "Continua"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(16.dp)
    ) {
        if (revealed && !nonGraded) {
            Text(
                text = if (isCorrect) "Esatto! 🎉" else "Risposta corretta:",
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) LeafGreen else ErrorRed
            )
            if (!isCorrect && correctAnswer.isNotBlank()) {
                Text(correctAnswer, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
        }
        ChunkyButton(
            text = buttonText,
            onClick = onClick,
            enabled = enabled,
            containerColor = buttonColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
