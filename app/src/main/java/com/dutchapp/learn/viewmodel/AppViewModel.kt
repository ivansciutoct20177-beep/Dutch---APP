package com.dutchapp.learn.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dutchapp.learn.data.CourseRepository
import com.dutchapp.learn.data.ExerciseGenerator
import com.dutchapp.learn.data.Progress
import com.dutchapp.learn.data.ProgressStore
import com.dutchapp.learn.data.Settings
import com.dutchapp.learn.data.model.Course
import com.dutchapp.learn.data.model.Exercise
import com.dutchapp.learn.data.model.Lesson
import com.dutchapp.learn.data.model.TipExercise
import com.dutchapp.learn.data.model.VocabItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Shared application state: the loaded course, the persisted progress and the
 * user settings. Created once and reused across all screens.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application)
    private val store = ProgressStore(application)

    val course: Course = repository.loadCourse()

    val progress: StateFlow<Progress> = store.progress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Progress()
    )

    val settings: StateFlow<Settings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings()
    )

    fun lesson(lessonId: String): Lesson? = repository.findLesson(lessonId)

    fun buildSession(lessonId: String): List<Exercise> {
        val lesson = repository.findLesson(lessonId) ?: return emptyList()
        val base = ExerciseGenerator.build(lesson, repository.levelPoolFor(lessonId))
        // Prepend the unit's grammar tip when this is the unit's first lesson.
        val unit = repository.unitOf(lessonId)
        return if (unit != null && unit.tip.isNotBlank() && unit.lessons.firstOrNull()?.id == lessonId) {
            listOf(TipExercise(unit.title, unit.tip)) + base
        } else {
            base
        }
    }

    /** Words eligible for review = all words from completed lessons. */
    fun reviewableWords(): List<VocabItem> {
        val current = progress.value
        return course.allLessons
            .filter { current.isCompleted(it.id) }
            .flatMap { it.items }
            .distinctBy { it.nl }
    }

    /** Builds a mixed practice session from previously learned words. */
    fun buildReviewSession(): List<Exercise> {
        val pool = reviewableWords()
        if (pool.size < 4) return emptyList()
        val random = Random(System.nanoTime())
        val picked = pool.shuffled(random).take(10)
        val reviewLesson = Lesson(id = "review", title = "Ripasso", items = picked)
        // Skip teaching cards: keep only the graded quiz part.
        return ExerciseGenerator.build(reviewLesson, pool, random, includeSpeaking = false)
            .filter { it !is com.dutchapp.learn.data.model.IntroExercise }
    }

    fun allWords(): List<VocabItem> =
        course.allLessons.flatMap { it.items }.distinctBy { it.nl }

    fun completeLesson(lessonId: String, stars: Int, xpGain: Int) {
        viewModelScope.launch { store.completeLesson(lessonId, stars, xpGain) }
    }

    fun completeReview(xpGain: Int) {
        viewModelScope.launch { store.addReviewXp(xpGain) }
    }

    fun setDailyGoal(goal: Int) = viewModelScope.launch { store.setDailyGoal(goal) }
    fun setDarkMode(mode: Int) = viewModelScope.launch { store.setDarkMode(mode) }
    fun setSound(enabled: Boolean) = viewModelScope.launch { store.setSound(enabled) }
    fun resetProgress() = viewModelScope.launch { store.resetAll() }
}
