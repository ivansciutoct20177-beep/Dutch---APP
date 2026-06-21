package com.dutchapp.learn.data

import com.dutchapp.learn.data.model.Exercise
import com.dutchapp.learn.data.model.IntroExercise
import com.dutchapp.learn.data.model.Lesson
import com.dutchapp.learn.data.model.ListenExercise
import com.dutchapp.learn.data.model.MatchExercise
import com.dutchapp.learn.data.model.PictureExercise
import com.dutchapp.learn.data.model.SpeakExercise
import com.dutchapp.learn.data.model.TranslateExercise
import com.dutchapp.learn.data.model.TypeExercise
import com.dutchapp.learn.data.model.VocabItem
import kotlin.random.Random

/**
 * Builds a Duolingo/Rosetta-style lesson session out of plain vocabulary.
 *
 * Flow: a short teaching phase (one intro card per new word) followed by a
 * mixed quiz that rotates through picture, listening, translation and typing
 * exercises, plus a matching round. Wrong-answer options ("distractors") are
 * drawn from the wider level so they stay plausible.
 */
object ExerciseGenerator {

    fun build(
        lesson: Lesson,
        levelPool: List<VocabItem>,
        random: Random = Random(System.nanoTime()),
        includeSpeaking: Boolean = true
    ): List<Exercise> {
        val items = lesson.items
        if (items.isEmpty()) return emptyList()

        val pool = (levelPool + items).distinctBy { it.nl }
        val pictureItems = pool.filter { it.hasPicture }

        val session = mutableListOf<Exercise>()

        // 1) Teaching phase
        items.forEach { session += IntroExercise(it) }

        // 2) Quiz phase — rotate exercise kinds for variety
        val quiz = mutableListOf<Exercise>()
        items.forEachIndexed { index, item ->
            val canPicture = item.hasPicture &&
                pictureItems.count { it.emoji != item.emoji } >= 3
            quiz += when (index % 4) {
                0 -> if (canPicture) pictureExercise(item, pictureItems, random)
                     else translateExercise(item, pool, random, promptInDutch = true)
                1 -> listenExercise(item, pool, random)
                2 -> translateExercise(item, pool, random, promptInDutch = false)
                else -> TypeExercise(item)
            }
        }

        // 3) Matching round (if the lesson has enough words)
        if (items.size >= 3) {
            quiz += MatchExercise(items.shuffled(random).take(5))
        }

        // 4) A couple of reinforcement items
        if (items.size >= 2) {
            quiz += translateExercise(items.first(), pool, random, promptInDutch = true)
            quiz += listenExercise(items.last(), pool, random)
        }

        // 5) Optional pronunciation practice (Rosetta-Stone style, not graded)
        if (includeSpeaking && items.size >= 3) {
            quiz += SpeakExercise(items.shuffled(random).first())
        }

        session += quiz.shuffled(random)
        return session
    }

    private fun pictureExercise(
        target: VocabItem,
        picturePool: List<VocabItem>,
        random: Random
    ): Exercise {
        val distractors = picturePool
            .filter { it.nl != target.nl && it.emoji != target.emoji }
            .distinctBy { it.emoji }
            .shuffled(random)
            .take(3)
        val options = (distractors + target).shuffled(random)
        return PictureExercise(target, options)
    }

    private fun translateExercise(
        target: VocabItem,
        pool: List<VocabItem>,
        random: Random,
        promptInDutch: Boolean
    ): Exercise {
        val distractors = pool
            .filter { it.nl != target.nl && it.it != target.it }
            .distinctBy { if (promptInDutch) it.it else it.nl }
            .shuffled(random)
            .take(3)
        val options = (distractors + target).shuffled(random)
        return TranslateExercise(target, options, promptInDutch)
    }

    private fun listenExercise(
        target: VocabItem,
        pool: List<VocabItem>,
        random: Random
    ): Exercise {
        val distractors = pool
            .filter { it.nl != target.nl }
            .distinctBy { it.nl }
            .shuffled(random)
            .take(3)
        val options = (distractors + target).shuffled(random)
        return ListenExercise(target, options)
    }
}
