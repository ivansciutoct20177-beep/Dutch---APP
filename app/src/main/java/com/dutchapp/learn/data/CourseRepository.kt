package com.dutchapp.learn.data

import android.content.Context
import com.dutchapp.learn.data.model.Course
import com.dutchapp.learn.data.model.CourseUnit
import com.dutchapp.learn.data.model.Lesson
import com.dutchapp.learn.data.model.Level
import com.dutchapp.learn.data.model.VocabItem
import kotlinx.serialization.json.Json

/**
 * Loads the curriculum from JSON files in assets/curriculum. Each file is one
 * CEFR level. Files are cached after first load.
 */
class CourseRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cached: Course? = null

    fun loadCourse(): Course {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val levels = LEVEL_FILES.mapNotNull { fileId ->
                runCatching {
                    val text = context.assets
                        .open("curriculum/$fileId.json")
                        .bufferedReader()
                        .use { it.readText() }
                    json.decodeFromString<Level>(text)
                }.getOrNull()
            }
            val course = Course(levels)
            cached = course
            return course
        }
    }

    fun findLesson(lessonId: String): Lesson? =
        loadCourse().allLessons.firstOrNull { it.id == lessonId }

    /** Words from the whole level a lesson belongs to — used as distractors. */
    fun levelPoolFor(lessonId: String): List<VocabItem> {
        val course = loadCourse()
        val level = course.levels.firstOrNull { level ->
            level.units.any { unit -> unit.lessons.any { it.id == lessonId } }
        } ?: return emptyList()
        return level.units.flatMap { it.lessons }.flatMap { it.items }
    }

    fun unitOf(lessonId: String): CourseUnit? =
        loadCourse().allUnits.firstOrNull { unit -> unit.lessons.any { it.id == lessonId } }

    companion object {
        private val LEVEL_FILES = listOf("a1", "a2", "b1", "b2", "c1")
    }
}
