package com.dutchapp.learn.data.model

import kotlinx.serialization.Serializable

/**
 * Whole course = ordered list of CEFR levels (A1 .. C1).
 * Loaded from the JSON files in assets/curriculum/.
 */
@Serializable
data class Course(
    val levels: List<Level> = emptyList()
) {
    val allUnits: List<CourseUnit> get() = levels.flatMap { it.units }
    val allLessons: List<Lesson> get() = allUnits.flatMap { it.lessons }
}

@Serializable
data class Level(
    val id: String,             // "A1"
    val title: String,          // "Principiante (A1)"
    val subtitle: String = "",
    val color: String = "#F2641A",
    val units: List<CourseUnit> = emptyList()
)

/**
 * A thematic unit (e.g. "Saluti", "Famiglia"). Named CourseUnit because
 * `Unit` is reserved in Kotlin.
 */
@Serializable
data class CourseUnit(
    val id: String,
    val title: String,
    val icon: String = "📘",     // emoji shown on the unit header
    val tip: String = "",        // optional grammar note (Duolingo-style "Tips")
    val lessons: List<Lesson> = emptyList()
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val items: List<VocabItem> = emptyList()
)

/**
 * A single vocabulary entry. The Dutch word is what we teach; Italian and
 * English are the learner's reference languages. `emoji` powers the
 * Rosetta-Stone style picture exercises; example sentences add context.
 */
@Serializable
data class VocabItem(
    val nl: String,
    val it: String,
    val en: String = "",
    val emoji: String = "",
    val exampleNl: String = "",
    val exampleIt: String = "",
    val pos: String = ""        // part of speech: n, v, adj, adv, phr ...
) {
    val hasPicture: Boolean get() = emoji.isNotBlank()
}
