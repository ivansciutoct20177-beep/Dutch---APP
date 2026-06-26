package com.dutchapp.learn.util

/**
 * Lightweight text similarity used to score pronunciation: compares what the
 * speech recogniser heard against the target word with a Levenshtein ratio.
 */
object TextSimilarity {

    /** Returns a similarity ratio in 0f..1f (1 = identical). */
    fun ratio(a: String, b: String): Float {
        val s1 = normalize(a)
        val s2 = normalize(b)
        if (s1.isEmpty() && s2.isEmpty()) return 1f
        if (s1.isEmpty() || s2.isEmpty()) return 0f
        val distance = levenshtein(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return (1f - distance.toFloat() / maxLen).coerceIn(0f, 1f)
    }

    private fun normalize(s: String): String =
        s.lowercase().trim().replace(Regex("[^\\p{L} ]"), "").replace(Regex("\\s+"), " ")

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}
