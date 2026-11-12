package com.bbexperience.launcher

class BBPredictionEngine {

    private val commonWords = listOf(
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
        "give", "day", "most", "us", "email", "meeting", "call", "urgent",
        "blackberry", "keyboard", "thanks", "regards", "please", "schedule"
    )

    private val emojiMap = mapOf(
        "fire" to "🔥", "love" to "❤️", "ok" to "👌", "good" to "👍",
        "smile" to "😊", "check" to "✅", "call" to "📞", "mail" to "✉️",
        "work" to "💼", "coffee" to "☕", "car" to "🚗", "star" to "⭐"
    )

    fun getPredictions(prefix: String): List<String> {
        if (prefix.isBlank()) return emptyList()
        val clean = prefix.lowercase()

        val results = mutableListOf<String>()
        emojiMap[clean]?.let { results.add(it) }

        val matches = commonWords.filter { it.startsWith(clean) && it != clean }
            .sortedBy { it.length }
            .take(3 - results.size)

        results.addAll(matches)
        return results
    }
}
