package com.dutchapp.learn.data.model

/** Types of step the learner sees inside a lesson. */
enum class ExerciseKind { TIP, INTRO, PICTURE, TRANSLATE, LISTEN, TYPE, MATCH, SPEAK, WORDORDER, CLOZE }

/** A single step inside a lesson session. */
sealed class Exercise {
    abstract val kind: ExerciseKind
}

/** Grammar note shown at the start of a unit (Duolingo-style "Tips"). Not graded. */
data class TipExercise(val title: String, val text: String) : Exercise() {
    override val kind = ExerciseKind.TIP
}

/** Teaching card shown before testing: emoji + word + translation + audio. */
data class IntroExercise(val item: VocabItem) : Exercise() {
    override val kind = ExerciseKind.INTRO
}

/** Rosetta-Stone style: hear/see the Dutch word, pick the matching picture. */
data class PictureExercise(
    val target: VocabItem,
    val options: List<VocabItem>
) : Exercise() {
    override val kind = ExerciseKind.PICTURE
}

/** Multiple choice translation. promptInDutch = show NL, choose IT (else reverse). */
data class TranslateExercise(
    val target: VocabItem,
    val options: List<VocabItem>,
    val promptInDutch: Boolean
) : Exercise() {
    override val kind = ExerciseKind.TRANSLATE
}

/** Listening: play Dutch audio, choose the matching written Dutch word. */
data class ListenExercise(
    val target: VocabItem,
    val options: List<VocabItem>
) : Exercise() {
    override val kind = ExerciseKind.LISTEN
}

/** Production: shown the Italian word, type the Dutch translation. */
data class TypeExercise(
    val target: VocabItem
) : Exercise() {
    override val kind = ExerciseKind.TYPE
}

/** Match Dutch words to their Italian translations. */
data class MatchExercise(
    val pairs: List<VocabItem>
) : Exercise() {
    override val kind = ExerciseKind.MATCH
}

/** Rosetta-Stone style pronunciation practice via speech recognition. Not graded. */
data class SpeakExercise(
    val target: VocabItem
) : Exercise() {
    override val kind = ExerciseKind.SPEAK
}

/**
 * Reorder the scrambled words to rebuild a correct Dutch sentence — trains
 * syntax (V2, verb-final in subordinate clauses). The bank holds exactly the
 * words of the reference sentence, shuffled, so there are no decoy words.
 *
 * `tokens` is the reference order; `scrambled` is what the bank shows.
 * `alternates` are extra accepted orders (e.g. time-adverb topicalisation:
 * "Gisteren was ik thuis" == "Ik was gisteren thuis").
 */
data class WordOrderExercise(
    val item: VocabItem,
    val tokens: List<String>,
    val scrambled: List<String>,
    val alternates: List<List<String>> = emptyList(),
    val translation: String = ""
) : Exercise() {
    override val kind = ExerciseKind.WORDORDER
}

/**
 * Fill the blank in an example sentence with the missing target word
 * (multiple choice). `before`/`after` surround the blank; `answer` is the
 * exact surface form removed; `options` = answer + distractors (shuffled).
 */
data class ClozeExercise(
    val item: VocabItem,
    val before: String,
    val after: String,
    val answer: String,
    val options: List<String>,
    val translation: String = ""
) : Exercise() {
    override val kind = ExerciseKind.CLOZE
}

/** Whether the step counts toward the lesson score. */
val Exercise.graded: Boolean
    get() = this !is TipExercise && this !is IntroExercise && this !is SpeakExercise
