package com.dutchapp.learn.data.model

/** Types of step the learner sees inside a lesson. */
enum class ExerciseKind { INTRO, PICTURE, TRANSLATE, LISTEN, TYPE, MATCH }

/** A single step inside a lesson session. */
sealed class Exercise {
    abstract val kind: ExerciseKind
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
