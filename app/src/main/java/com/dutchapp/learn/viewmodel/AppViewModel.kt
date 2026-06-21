package com.dutchapp.learn.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dutchapp.learn.data.CourseRepository
import com.dutchapp.learn.data.ExerciseGenerator
import com.dutchapp.learn.data.Progress
import com.dutchapp.learn.data.ProgressStore
import com.dutchapp.learn.data.model.Course
import com.dutchapp.learn.data.model.Exercise
import com.dutchapp.learn.data.model.Lesson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared application state: the loaded course and the persisted progress.
 * Created once and reused across all screens.
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

    fun lesson(lessonId: String): Lesson? = repository.findLesson(lessonId)

    fun buildSession(lessonId: String): List<Exercise> {
        val lesson = repository.findLesson(lessonId) ?: return emptyList()
        return ExerciseGenerator.build(lesson, repository.levelPoolFor(lessonId))
    }

    fun completeLesson(lessonId: String, stars: Int, xpGain: Int) {
        viewModelScope.launch {
            store.completeLesson(lessonId, stars, xpGain)
        }
    }

    fun resetProgress() {
        viewModelScope.launch { store.resetAll() }
    }
}
