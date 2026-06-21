package com.dutchapp.learn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

/** Snapshot of the learner's progress used across the UI. */
data class Progress(
    val xp: Int = 0,
    val streak: Int = 0,
    val completedLessons: Set<String> = emptySet(),
    val stars: Map<String, Int> = emptyMap()
) {
    fun starsFor(lessonId: String): Int = stars[lessonId] ?: 0
    fun isCompleted(lessonId: String): Boolean = lessonId in completedLessons
}

/**
 * Persists XP, daily streak and per-lesson stars in DataStore. Dates are
 * stored as an "epoch day" integer so we avoid java.time (minSdk 24).
 */
class ProgressStore(private val context: Context) {

    val progress: Flow<Progress> = context.dataStore.data.map { prefs ->
        val stars = prefs.asMap()
            .asSequence()
            .filter { it.key.name.startsWith(STARS_PREFIX) }
            .mapNotNull { entry ->
                (entry.value as? Int)?.let { entry.key.name.removePrefix(STARS_PREFIX) to it }
            }
            .toMap()
        Progress(
            xp = prefs[XP] ?: 0,
            streak = prefs[STREAK] ?: 0,
            completedLessons = prefs[COMPLETED] ?: emptySet(),
            stars = stars
        )
    }

    suspend fun completeLesson(lessonId: String, stars: Int, xpGain: Int) {
        context.dataStore.edit { prefs ->
            prefs[XP] = (prefs[XP] ?: 0) + xpGain

            val completed = (prefs[COMPLETED] ?: emptySet()).toMutableSet()
            completed += lessonId
            prefs[COMPLETED] = completed

            val starsKey = intPreferencesKey(STARS_PREFIX + lessonId)
            val prevStars = prefs[starsKey] ?: 0
            if (stars > prevStars) prefs[starsKey] = stars

            val today = (System.currentTimeMillis() / DAY_MILLIS).toInt()
            val last = prefs[LAST_DAY] ?: 0
            val streak = prefs[STREAK] ?: 0
            prefs[STREAK] = when {
                last == today -> if (streak == 0) 1 else streak
                today - last == 1 -> streak + 1
                else -> 1
            }
            prefs[LAST_DAY] = today
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L
        private const val STARS_PREFIX = "stars_"
        private val XP = intPreferencesKey("xp")
        private val STREAK = intPreferencesKey("streak")
        private val LAST_DAY = intPreferencesKey("last_day")
        private val COMPLETED = stringSetPreferencesKey("completed_lessons")
    }
}
