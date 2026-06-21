package com.dutchapp.learn.data.model

/** Types of step the learner sees inside a lesson. */
enum class ExerciseKind { TIP, INTRO, PICTURE, TRANSLATE, LISTEN, TYPE, MATCH, SPEAK }

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

/** Whether the step counts toward the lesson score. */
val Exercise.graded: Boolean
    get() = this !is TipExercise && this !is IntroExercise && this !is SpeakExercise
