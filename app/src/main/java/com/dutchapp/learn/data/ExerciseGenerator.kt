package com.dutchapp.learn.data

import com.dutchapp.learn.data.model.ClozeExercise
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
import com.dutchapp.learn.data.model.WordOrderExercise
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

        // 4b) Sentence-syntax phase — reorder + cloze, from the example sentences.
        //     Generated 100% from the exampleNl we already have on every word.
        val withExamples = items.filter { it.exampleNl.isNotBlank() }
        withExamples
            .mapNotNull { wordOrderExercise(it, random) }
            .shuffled(random)
            .take(2)
            .forEach { quiz += it }
        withExamples
            .mapNotNull { clozeExercise(it, pool, random) }
            .shuffled(random)
            .take(2)
            .forEach { quiz += it }

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

    /* ----------------------- Sentence-based exercises ----------------------- */

    /** Reorder the words of the example sentence. Needs 4..9 tokens. */
    private fun wordOrderExercise(item: VocabItem, random: Random): WordOrderExercise? {
        val tokens = tokenize(item.exampleNl)
        if (tokens.size !in 4..9) return null
        var scrambled = tokens.shuffled(random)
        var guard = 0
        while (scrambled == tokens && guard++ < 6) scrambled = tokens.shuffled(random)
        if (scrambled == tokens) return null   // e.g. all-identical tokens (never happens)
        return WordOrderExercise(
            item = item,
            tokens = tokens,
            scrambled = scrambled,
            alternates = timeFrontingAlternates(tokens),
            translation = item.exampleIt
        )
    }

    /** Blank out the target word in its example sentence; multiple choice. */
    private fun clozeExercise(item: VocabItem, pool: List<VocabItem>, random: Random): ClozeExercise? {
        val tokens = tokenize(item.exampleNl)
        if (tokens.size < 4) return null
        val idx = blankIndex(item, tokens)
        if (idx < 0) return null
        val answer = tokens[idx]
        val answerLow = answer.lowercase()
        val samePos = pool.filter { it.pos == item.pos && it.nl.lowercase() != item.nl.lowercase() }
        val distractors = (samePos.ifEmpty { pool })
            .map { surfaceForm(it) }
            .filter { it.isNotBlank() && !it.contains(' ') && it.lowercase() != answerLow }
            .distinctBy { it.lowercase() }
            .shuffled(random)
            .take(3)
        if (distractors.isEmpty()) return null
        val options = (distractors + answer).distinctBy { it.lowercase() }.shuffled(random)
        return ClozeExercise(
            item = item,
            before = tokens.subList(0, idx).joinToString(" "),
            after = tokens.subList(idx + 1, tokens.size).joinToString(" "),
            answer = answer,
            options = options,
            translation = item.exampleIt
        )
    }

    // Punctuation stripped from word chips (apostrophes kept: 't, 's, z'n).
    private val STRIP = "“”«»\".,!?;:()".toCharArray()

    private fun tokenize(s: String): List<String> =
        s.split(Regex("\\s+"))
            .map { it.trim(*STRIP).trim() }
            .filter { it.isNotEmpty() }

    private val SEP_PREFIXES = listOf(
        "aan", "af", "bij", "binnen", "buiten", "door", "in", "mee", "na", "neer",
        "om", "onder", "op", "over", "rond", "samen", "schoon", "tegen", "terug",
        "toe", "uit", "vast", "voor", "weg"
    )

    // Unambiguous time/frequency adverbs only. Excludes "zo" (=so), "dan"
    // (=than), "net" (=neat/just) which are ambiguous and would mis-front.
    private val TIME_ADVERBS = setOf(
        "gisteren", "eergisteren", "vandaag", "morgen", "overmorgen", "nu", "straks",
        "daarna", "soms", "altijd", "vaak", "nooit", "meestal",
        "vroeger", "later", "tegenwoordig", "binnenkort", "onlangs"
    )

    private val SUBJECT_PRONOUNS = setOf(
        "ik", "jij", "je", "u", "hij", "ze", "zij", "we", "wij", "jullie", "het", "men"
    )

    // Object/reflexive pronouns must hug the verb in the middle field, so a
    // blind swap that moves a time adverb in front of them is wrong.
    private val OBJECT_PRONOUNS = setOf(
        "me", "mij", "je", "jou", "zich", "ons", "hem", "haar", "het", "ze",
        "hen", "hun", "u", "er", "'m", "'t"
    )

    // Subordinators: a time adverb sitting beyond one of these is inside a
    // subordinate clause and cannot be topicalised to the main field.
    private val SUBORDINATORS = setOf(
        "dat", "omdat", "hoewel", "terwijl", "als", "wanneer", "zodat", "voordat",
        "nadat", "doordat", "waar", "wat", "wie", "of", "die", "zoals", "sinds",
        "totdat", "tenzij", "aangezien"
    )

    /**
     * Deterministic V2 transformation: a sentence with a fronted time adverb and
     * a pronoun subject has an equally valid SVO order, and vice-versa.
     *   "Gisteren was ik thuis"  <->  "Ik was gisteren thuis"
     * Conservative guards avoid producing invalid orders (reflexive clitics,
     * adverbs trapped inside a subordinate clause).
     */
    private fun timeFrontingAlternates(tokens: List<String>): List<List<String>> {
        if (tokens.size < 3) return emptyList()
        val low = tokens.map { it.lowercase() }
        val alts = mutableListOf<List<String>>()
        // [TIME][V][PRON][rest] -> [PRON][V][TIME][rest]
        // blocked if an object/reflexive pronoun must hug the verb (token 3).
        if (low[0] in TIME_ADVERBS && low[2] in SUBJECT_PRONOUNS &&
            (tokens.size <= 3 || low[3] !in OBJECT_PRONOUNS)
        ) {
            val s = tokens.toMutableList()
            val t = s[0]; s[0] = s[2]; s[2] = t
            alts += s
        }
        // [PRON][V] ... [TIME in main clause] -> fronted.
        // blocked if a subordinator sits between the verb and the adverb.
        if (low[0] in SUBJECT_PRONOUNS) {
            val j = (2 until low.size).firstOrNull { low[it] in TIME_ADVERBS }
            if (j != null && (2 until j).none { low[it] in SUBORDINATORS }) {
                val rest = tokens.filterIndexed { i, _ -> i != 0 && i != 1 && i != j }
                alts += listOf(tokens[j], tokens[1], tokens[0]) + rest
            }
        }
        return alts
    }

    /** Index in `tokens` of the word matching the lesson target (incl. inflection). */
    private fun blankIndex(item: VocabItem, tokens: List<String>): Int {
        val forms = targetForms(item)
        tokens.indexOfFirst { it.lowercase() in forms }.let { if (it >= 0) return it }
        // inflected forms: token shares a stem with one of the base forms
        return tokens.indexOfFirst { tok ->
            val t = tok.lowercase()
            forms.any { f -> f.length >= 3 && (t.startsWith(f) || (t.length >= 4 && f.startsWith(t))) }
        }
    }

    private fun targetForms(item: VocabItem): Set<String> {
        val nl = item.nl.lowercase()
        return when (item.pos) {
            "v" -> verbFragments(nl)
            "n" -> {
                val core = surfaceForm(item).lowercase()
                val last = core.split(" ").last()
                setOf(core, last, shorten(core), shorten(last)).filter { it.isNotEmpty() }.toSet()
            }
            else -> if (nl.length >= 5) setOf(nl, nl.substring(0, 5)) else setOf(nl)
        }
    }

    /** Bare lexical form for distractors: noun without article, else the word. */
    private fun surfaceForm(item: VocabItem): String {
        val toks = item.nl.split(" ")
        return if (item.pos == "n" && toks.size > 1 && toks[0] in setOf("de", "het"))
            toks.drop(1).joinToString(" ") else item.nl
    }

    private fun stemOf(v: String): String {
        var s = if (v.endsWith("en") && v.length > 3) v.dropLast(2) else v
        if (s.length >= 2 && s[s.length - 1] == s[s.length - 2] && s.last() !in "aeiou")
            s = s.dropLast(1)                       // pakk -> pak, zwemm -> zwem
        return s
    }

    /** Open-syllable vowel lengthening: hop -> hoop, verget -> vergeet. */
    private fun lengthen(s: String): Set<String> {
        val out = mutableSetOf(s)
        val m = Regex("([aeiou])([bcdfghjklmnpqrstvwxz])$").find(s)
        if (m != null && s.length >= 3) {
            val i = m.range.first
            if (i - 1 >= 0 && s[i - 1] !in "aeiou") out += s.substring(0, i) + s[i] + s.substring(i)
        }
        return out
    }

    /** Plural vowel shortening: aandeel -> aandel, paneel -> panel. */
    private fun shorten(w: String): String {
        val m = Regex("(aa|ee|oo|uu)([bcdfghjklmnpqrstvwxz])$").find(w)
        if (m != null) {
            val i = m.range.first
            return w.substring(0, i) + w[i] + w.substring(i + 2)
        }
        return w
    }

    private fun verbFragments(inf: String): Set<String> {
        var base = inf
        for (p in SEP_PREFIXES) if (inf.startsWith(p) && inf.length - p.length >= 3) {
            base = inf.substring(p.length); break
        }
        val frags = mutableSetOf(inf, base)
        for (s in listOf(stemOf(inf), stemOf(base))) frags += lengthen(s)
        return frags.filter { it.length >= 3 }.map { it.lowercase() }.toSet()
    }
}
