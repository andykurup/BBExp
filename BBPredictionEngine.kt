package com.bbexperience.launcher

import kotlin.math.hypot

class BBPredictionEngine {

    private val dictionary = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her",
        "she", "or", "an", "will", "my", "one", "all", "would", "there",
        "their", "what", "so", "up", "out", "if", "about", "who", "get",
        "which", "go", "me", "when", "make", "can", "like", "time", "no",
        "just", "him", "know", "take", "people", "into", "year", "your",
        "good", "some", "could", "them", "see", "other", "than", "then",
        "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first",
        "well", "way", "even", "new", "want", "because", "any", "these",
        "give", "day", "most", "us", "end", "send", "find", "mind", "kind",
        "email", "meeting", "call", "urgent", "blackberry", "keyboard",
        "thanks", "regards", "please", "schedule", "project", "update",
        "review", "today", "tomorrow", "confirmed", "attached", "client"
    )

    private val emojiMap = mapOf(
        "fire" to "🔥", "love" to "❤️", "ok" to "👌", "good" to "👍",
        "smile" to "😊", "check" to "✅", "call" to "📞", "mail" to "✉️",
        "work" to "💼", "coffee" to "☕", "car" to "🚗", "star" to "⭐"
    )

    // Normalized QWERTY coordinates (x in 0..10, y in 0..2) for geometric Flow/Glide matching
    private val keyCoords = mapOf(
        'q' to Pair(0.5f, 0f), 'w' to Pair(1.5f, 0f), 'e' to Pair(2.5f, 0f), 'r' to Pair(3.5f, 0f),
        't' to Pair(4.5f, 0f), 'y' to Pair(5.5f, 0f), 'u' to Pair(6.5f, 0f), 'i' to Pair(7.5f, 0f),
        'o' to Pair(8.5f, 0f), 'p' to Pair(9.5f, 0f),
        'a' to Pair(0.8f, 1f), 's' to Pair(1.8f, 1f), 'd' to Pair(2.8f, 1f), 'f' to Pair(3.8f, 1f),
        'g' to Pair(4.8f, 1f), 'h' to Pair(5.8f, 1f), 'j' to Pair(6.8f, 1f), 'k' to Pair(7.8f, 1f),
        'l' to Pair(8.8f, 1f),
        'z' to Pair(1.5f, 2f), 'x' to Pair(2.5f, 2f), 'c' to Pair(3.5f, 2f), 'v' to Pair(4.5f, 2f),
        'b' to Pair(5.5f, 2f), 'n' to Pair(6.5f, 2f), 'm' to Pair(7.5f, 2f)
    )

    fun getPredictions(prefix: String): List<String> {
        if (prefix.isBlank()) return emptyList()
        val clean = prefix.lowercase()

        val results = mutableListOf<String>()
        emojiMap[clean]?.let { results.add(it) }

        val matches = dictionary.filter { it.startsWith(clean) && it != clean }
            .sortedBy { it.length }
            .take(3 - results.size)

        results.addAll(matches)
        return results
    }

    /**
     * Resolves a swiped key trail (e.g. ['e', 'f', 'd'] or ['e', 'r', 'd', 'f', 'd'])
     * to the closest matching dictionary words using first/last key anchoring + spatial path distance.
     */
    fun matchFlowGesture(swipedKeys: List<Char>): List<String> {
        if (swipedKeys.size < 2) return emptyList()
        val cleanTrail = swipedKeys.map { it.lowercaseChar() }.filter { keyCoords.containsKey(it) }
        if (cleanTrail.size < 2) return emptyList()

        val startChar = cleanTrail.first()
        val endChar = cleanTrail.last()

        val candidates = dictionary.filter { word ->
            word.length >= 2 &&
                isNearKey(word.first(), startChar, 1.3f) &&
                isNearKey(word.last(), endChar, 1.3f)
        }

        return candidates.sortedBy { word ->
            scoreWordAgainstTrail(word, cleanTrail)
        }.take(3)
    }

    private fun isNearKey(a: Char, b: Char, maxDist: Float): Boolean {
        val ca = keyCoords[a] ?: return false
        val cb = keyCoords[b] ?: return false
        return hypot(ca.first - cb.first, ca.second - cb.second) <= maxDist
    }

    private fun scoreWordAgainstTrail(word: String, trail: List<Char>): Float {
        var totalError = 0f
        for (i in word.indices) {
            val expectedCoord = keyCoords[word[i]] ?: continue
            val trailIdx = ((i.toFloat() / (word.length - 1).coerceAtLeast(1)) * (trail.size - 1)).toInt()
            val actualCoord = keyCoords[trail[trailIdx]] ?: continue
            totalError += hypot(expectedCoord.first - actualCoord.first, expectedCoord.second - actualCoord.second)
        }
        return totalError / word.length
    }
}
