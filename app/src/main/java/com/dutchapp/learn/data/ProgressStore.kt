package com.dutchapp.learn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val stars: Map<String, Int> = emptyMap(),
    val todayXp: Int = 0,
    val dailyGoal: Int = 30
) {
    fun starsFor(lessonId: String): Int = stars[lessonId] ?: 0
    fun isCompleted(lessonId: String): Boolean = lessonId in completedLessons
    val goalReached: Boolean get() = todayXp >= dailyGoal
    val goalFraction: Float get() = if (dailyGoal <= 0) 1f else (todayXp / dailyGoal.toFloat()).coerceIn(0f, 1f)
}

/** User preferences. darkMode: 0 = system, 1 = light, 2 = dark. */
data class Settings(
    val darkMode: Int = 0,
    val soundEnabled: Boolean = true,
    val dailyGoal: Int = 30
)

/**
 * Persists XP, daily streak, per-lesson stars and user settings in DataStore.
 * Dates are stored as an "epoch day" integer so we avoid java.time (minSdk 24).
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
        val today = currentEpochDay()
        val todayXp = if ((prefs[XP_DAY] ?: 0) == today) (prefs[TODAY_XP] ?: 0) else 0
        Progress(
            xp = prefs[XP] ?: 0,
            streak = prefs[STREAK] ?: 0,
            completedLessons = prefs[COMPLETED] ?: emptySet(),
            stars = stars,
            todayXp = todayXp,
            dailyGoal = prefs[DAILY_GOAL] ?: 30
        )
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            darkMode = prefs[DARK_MODE] ?: 0,
            soundEnabled = prefs[SOUND] ?: true,
            dailyGoal = prefs[DAILY_GOAL] ?: 30
        )
    }

    suspend fun completeLesson(lessonId: String, stars: Int, xpGain: Int) {
        addXpInternal(xpGain) { prefs ->
            val completed = (prefs[COMPLETED] ?: emptySet()).toMutableSet()
            completed += lessonId
            prefs[COMPLETED] = completed

            val starsKey = intPreferencesKey(STARS_PREFIX + lessonId)
            val prevStars = prefs[starsKey] ?: 0
            if (stars > prevStars) prefs[starsKey] = stars
        }
    }

    /** Adds XP for a practice session that is not tied to a specific lesson. */
    suspend fun addReviewXp(xpGain: Int) {
        addXpInternal(xpGain) {}
    }

    private suspend fun addXpInternal(xpGain: Int, extra: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs ->
            prefs[XP] = (prefs[XP] ?: 0) + xpGain

            val today = currentEpochDay()
            val sameDay = (prefs[XP_DAY] ?: 0) == today
            prefs[TODAY_XP] = (if (sameDay) (prefs[TODAY_XP] ?: 0) else 0) + xpGain
            prefs[XP_DAY] = today

            val last = prefs[LAST_DAY] ?: 0
            val streak = prefs[STREAK] ?: 0
            prefs[STREAK] = when {
                last == today -> if (streak == 0) 1 else streak
                today - last == 1 -> streak + 1
                else -> 1
            }
            prefs[LAST_DAY] = today
            extra(prefs)
        }
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[DAILY_GOAL] = goal }
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun setSound(enabled: Boolean) {
        context.dataStore.edit { it[SOUND] = enabled }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun currentEpochDay(): Int = (System.currentTimeMillis() / DAY_MILLIS).toInt()

    companion object {
        private const val DAY_MILLIS = 86_400_000L
        private const val STARS_PREFIX = "stars_"
        private val XP = intPreferencesKey("xp")
        private val STREAK = intPreferencesKey("streak")
        private val LAST_DAY = intPreferencesKey("last_day")
        private val COMPLETED = stringSetPreferencesKey("completed_lessons")
        private val TODAY_XP = intPreferencesKey("today_xp")
        private val XP_DAY = intPreferencesKey("xp_day")
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val DARK_MODE = intPreferencesKey("dark_mode")
        private val SOUND = booleanPreferencesKey("sound_enabled")
    }
}
