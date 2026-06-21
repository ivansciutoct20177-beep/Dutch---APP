package com.dutchapp.learn.viewmodel

import com.dutchapp.learn.data.Progress
import com.dutchapp.learn.data.model.Course

enum class LessonState { LOCKED, UNLOCKED, COMPLETED }

/**
 * Linear "step by step" progression: the first lesson is open; every other
 * lesson unlocks when the previous one in the global order is completed.
 */
object CourseProgressLogic {

    fun lessonState(course: Course, progress: Progress, lessonId: String): LessonState {
        val lessons = course.allLessons
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index < 0) return LessonState.LOCKED
        if (progress.isCompleted(lessonId)) return LessonState.COMPLETED
        if (index == 0) return LessonState.UNLOCKED
        val previous = lessons[index - 1]
        return if (progress.isCompleted(previous.id)) LessonState.UNLOCKED else LessonState.LOCKED
    }

    fun isUnitUnlocked(course: Course, progress: Progress, unitId: String): Boolean {
        val unit = course.allUnits.firstOrNull { it.id == unitId } ?: return false
        val firstLesson = unit.lessons.firstOrNull() ?: return false
        return lessonState(course, progress, firstLesson.id) != LessonState.LOCKED
    }

    /** A crude learner rank derived from total XP, for the profile header. */
    fun rankFor(xp: Int): String = when {
        xp < 100 -> "Beginner"
        xp < 300 -> "Verkenner"      // explorer
        xp < 700 -> "Reiziger"       // traveller
        xp < 1500 -> "Spreker"       // speaker
        xp < 3000 -> "Gevorderde"    // advanced
        else -> "Meester"            // master
    }
}
