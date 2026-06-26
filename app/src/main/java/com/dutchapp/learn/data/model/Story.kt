package com.dutchapp.learn.data.model

import kotlinx.serialization.Serializable

/** A graded reading: a short story with sentence translations and a glossary. */
@Serializable
data class Story(
    val id: String,
    val level: String,              // "A1" .. "C1"
    val title: String,              // Dutch title
    val titleIt: String = "",       // Italian title
    val icon: String = "📖",
    val intro: String = "",         // short Italian description
    val sentences: List<StorySentence> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList()
) {
    val fullText: String get() = sentences.joinToString(" ") { it.nl }
}

@Serializable
data class StorySentence(
    val nl: String,
    val it: String = ""
)

/** A key term highlighted in the story that can be tapped for its meaning. */
@Serializable
data class GlossaryEntry(
    val nl: String,
    val it: String,
    val en: String = ""
)

@Serializable
data class StoryBook(
    val stories: List<Story> = emptyList()
)
