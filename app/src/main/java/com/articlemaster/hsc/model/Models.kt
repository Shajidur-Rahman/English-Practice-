package com.articlemaster.hsc.model

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

val BLANK_KEYS = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")

val COMMON_PREPOSITIONS = listOf(
    "of", "in", "to", "for", "with", "on", "from", "by", "at", "about",
    "into", "through", "up", "towards", "around", "upon", "as", "after",
    "against", "down", "out", "over", "beyond", "among", "without", "like",
    "behind", "within", "under", "across", "between", "before", "during",
    "off", "since", "until", "till", "beside", "above", "below", "away",
    "inside", "outside", "near", "past", "round", "along", "amongst",
    "toward", "in front of"
)

enum class GrammarTopic(
    val label: String,
    val shortLabel: String,
    val chapterTitle: String
) {
    ARTICLE("Articles", "Art", "Chapter 01 • Use of Articles"),
    PREPOSITION("Prepositions", "Prep", "Chapter 02 • Use of Prepositions")
}

enum class ExamTopicFilter(val label: String) {
    ARTICLE("Articles"),
    PREPOSITION("Prepositions"),
    BOTH("Mixed (Both)")
}

data class ExerciseItem(
    val id: Int,
    val topic: GrammarTopic = GrammarTopic.ARTICLE,
    val page: Int,
    val boardInfo: String,
    val isBoardQuestion: Boolean,
    val imageAsset: String,
    val answers: Map<String, String>,
    val note: String? = null
) {
    val key: String get() = makeExerciseKey(topic, id)

    val solutionImageAsset: String
        get() {
            val padded = id.toString().padStart(2, '0')
            return when (topic) {
                GrammarTopic.ARTICLE -> "solutions/sol_art_$padded.webp"
                GrammarTopic.PREPOSITION -> "solutions/sol_prep_$padded.webp"
            }
        }

    companion object {
        fun makeExerciseKey(topic: GrammarTopic, id: Int): String = "${topic.name}_$id"
    }
}

enum class SelectionMode(val label: String) {
    ALL("All Passages"),
    BOARD_ONLY("Board Questions"),
    PRACTICE_ONLY("Practice Exercises"),
    RANGE("Custom Range"),
    RANDOM("Random N"),
    MISTAKES("Mistake Bank"),
    BOOKMARKED("Bookmarked"),
    UNATTEMPTED("Unattempted"),
    CUSTOM("Manual Pick")
}

enum class TimerMode(val label: String) {
    NONE("Untimed"),
    TOTAL_MINUTES("Total Exam Timer"),
    PER_PASSAGE_SECONDS("Per-Passage Timer")
}

enum class SessionType(val label: String) {
    PRACTICE("Practice Mode"),
    EXAM("Exam Mode")
}

data class ExamConfig(
    val title: String = "Practice Session",
    val topicFilter: ExamTopicFilter = ExamTopicFilter.ARTICLE,
    val sessionType: SessionType = SessionType.PRACTICE,
    val selectionMode: SelectionMode = SelectionMode.ALL,
    val rangeStart: Int = 1,
    val rangeEnd: Int = 10,
    val randomCount: Int = 5,
    val customSelectedKeys: Set<String> = emptySet(),
    val shuffleOrder: Boolean = false,
    val timerMode: TimerMode = TimerMode.NONE,
    val totalMinutes: Int = 10,
    val perPassageSeconds: Int = 90,
    val autoSubmitOnTimeout: Boolean = false,
    val instantFeedback: Boolean = true,
    val strictCapitalization: Boolean = false,
    val hscHalfMarkScale: Boolean = true,
    val negativePenaltyPerWrong: Double = 0.0,
    val showWordBankHint: Boolean = false
)

data class ExerciseStats(
    val attempts: Int = 0,
    val bestCorrect: Int = 0,
    val lastCorrect: Int = 0,
    val wrongBlankKeys: Set<String> = emptySet(),
    val bookmarked: Boolean = false,
    val mastered: Boolean = false,
    val lastAttemptTimestamp: Long = 0L
)

data class ExamHistoryEntry(
    val id: String,
    val title: String,
    val timestamp: Long,
    val topicFilter: ExamTopicFilter = ExamTopicFilter.ARTICLE,
    val sessionType: SessionType,
    val passageKeys: List<String>,
    val totalBlanks: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val rawScore: Double,
    val maxRawScore: Double,
    val durationSeconds: Int,
    val strictCapitalization: Boolean,
    val hscHalfMarkScale: Boolean
)

enum class MothersLayoutMode(val label: String, val subtitle: String) {
    SPLIT_5_5("5 + 5 Split", "5 answers left (a–e), 5 right (f–j)"),
    ONE_PER_LINE("One Line Each", "Single column, maximum width & size"),
    TWO_LINE_SHEET("2-Line Compact", "5 answers in top block, 5 below"),
    FULL_PAGE("Full Page (All)", "Scroll all passages on one clean sheet")
}

data class AppPreferences(
    val darkMode: Boolean? = null, // null = follow system
    val invertPassageInDark: Boolean = true,
    val autoAdvanceBlank: Boolean = true,
    val defaultStrictCase: Boolean = false,
    val defaultHscHalfMark: Boolean = true,
    val preferTextViewWhenAvailable: Boolean = true,
    val autoSaveMistakesToFlashcards: Boolean = true,
    val mothersLayout: MothersLayoutMode = MothersLayoutMode.SPLIT_5_5,
    val mothersTextScale: Float = 1.15f,
    val mothersMinimalHeader: Boolean = false
)

data class UserProgressData(
    val statsByKey: Map<String, ExerciseStats> = emptyMap(),
    val history: List<ExamHistoryEntry> = emptyList(),
    val customTexts: Map<String, String> = emptyMap(),
    val preferences: AppPreferences = AppPreferences()
)

data class BlankOutcome(
    val blankKey: String,
    val userAnswer: String?,
    val expectedAnswer: String,
    val isCorrect: Boolean,
    val isUnanswered: Boolean
)

data class PassageResult(
    val exercise: ExerciseItem,
    val blankOutcomes: List<BlankOutcome>,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val rawScore: Double,
    val maxRawScore: Double
)

data class ExamResult(
    val config: ExamConfig,
    val passageResults: List<PassageResult>,
    val totalCorrect: Int,
    val totalWrong: Int,
    val totalUnanswered: Int,
    val totalBlanks: Int,
    val totalScore: Double,
    val maxScore: Double,
    val elapsedSeconds: Int,
    val timedOut: Boolean,
    val flashcardsAddedOrUpdated: Int = 0
)

// ==================== FLASHCARD / FLASH MEMORY MODELS ====================

enum class FlashcardStatus(val label: String) {
    LEARNING("Learning"),
    MASTERED("Mastered")
}

enum class ReviewRating(val label: String, val quality: Int) {
    AGAIN("Again", 1),
    HARD("Hard", 3),
    GOOD("Good", 4),
    EASY("Easy", 5)
}

data class FlashcardEntity(
    val cardKey: String,
    val topic: GrammarTopic,
    val exerciseId: Int,
    val blankKey: String,
    val boardInfo: String,
    val imageAsset: String?,
    val customPrompt: String?,
    val correctAnswer: String,
    val lastWrongAnswer: String?,
    val mistakeCount: Int,
    val reviewCount: Int,
    val streak: Int,
    val easeFactor: Double,
    val intervalDays: Int,
    val nextReviewAt: Long,
    val status: FlashcardStatus,
    val isManual: Boolean,
    val userNote: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isCustomCard: Boolean get() = exerciseId <= 0 || !customPrompt.isNullOrBlank()
    fun isDue(nowMs: Long = System.currentTimeMillis()): Boolean = nextReviewAt <= nowMs

    companion object {
        fun makeCardKey(topic: GrammarTopic, exerciseId: Int, blankKey: String): String =
            "${topic.name}_${exerciseId}_${blankKey.lowercase(Locale.US)}"
    }
}

object SpacedRepetitionEngine {
    const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    const val RETRY_DELAY_MS: Long = 10L * 60L * 1000L // 10 min

    fun previewIntervalLabel(card: FlashcardEntity, rating: ReviewRating): String {
        val simulated = applyReview(card, rating, nowMs = 0L)
        return when {
            rating == ReviewRating.AGAIN -> "10m"
            simulated.intervalDays <= 1 -> "1d"
            else -> "${simulated.intervalDays}d"
        }
    }

    fun applyReview(
        card: FlashcardEntity,
        rating: ReviewRating,
        nowMs: Long = System.currentTimeMillis()
    ): FlashcardEntity {
        val ef = card.easeFactor.coerceIn(1.3, 3.0)
        return when (rating) {
            ReviewRating.AGAIN -> card.copy(
                reviewCount = card.reviewCount + 1,
                streak = 0,
                easeFactor = max(1.3, ef - 0.20),
                intervalDays = 0,
                nextReviewAt = nowMs + RETRY_DELAY_MS,
                status = FlashcardStatus.LEARNING,
                updatedAt = nowMs
            )

            ReviewRating.HARD -> {
                val nextDays = if (card.intervalDays <= 0) 1 else max(1, (card.intervalDays * 1.2).roundToInt())
                card.copy(
                    reviewCount = card.reviewCount + 1,
                    streak = max(1, card.streak),
                    easeFactor = max(1.3, ef - 0.15),
                    intervalDays = nextDays,
                    nextReviewAt = nowMs + nextDays * DAY_MS,
                    status = FlashcardStatus.LEARNING,
                    updatedAt = nowMs
                )
            }

            ReviewRating.GOOD -> {
                val nextStreak = card.streak + 1
                val nextDays = when (nextStreak) {
                    1 -> 1
                    2 -> 3
                    else -> max(4, (max(1, card.intervalDays) * ef).roundToInt())
                }
                val nextStatus = if (nextStreak >= 3) FlashcardStatus.MASTERED else FlashcardStatus.LEARNING
                card.copy(
                    reviewCount = card.reviewCount + 1,
                    streak = nextStreak,
                    easeFactor = ef,
                    intervalDays = nextDays,
                    nextReviewAt = nowMs + nextDays * DAY_MS,
                    status = nextStatus,
                    updatedAt = nowMs
                )
            }

            ReviewRating.EASY -> {
                val nextStreak = card.streak + 1
                val nextEf = (ef + 0.15).coerceAtMost(3.0)
                val nextDays = when (nextStreak) {
                    1 -> 4
                    else -> max(6, (max(2, card.intervalDays) * nextEf * 1.3).roundToInt())
                }
                val nextStatus = if (nextStreak >= 2) FlashcardStatus.MASTERED else FlashcardStatus.LEARNING
                card.copy(
                    reviewCount = card.reviewCount + 1,
                    streak = nextStreak,
                    easeFactor = nextEf,
                    intervalDays = nextDays,
                    nextReviewAt = nowMs + nextDays * DAY_MS,
                    status = nextStatus,
                    updatedAt = nowMs
                )
            }
        }
    }
}

// ==================== EVALUATOR ====================

object AnswerEvaluator {

    private val multiSpaceRegex = Regex("\\s+")

    fun normalizeInput(raw: String?, topic: GrammarTopic = GrammarTopic.ARTICLE): String? {
        val collapsed = raw?.trim()?.replace(multiSpaceRegex, " ").orEmpty()
        if (collapsed.isEmpty()) return null
        return when (topic) {
            GrammarTopic.ARTICLE -> when (collapsed.lowercase(Locale.US)) {
                "x", "×", "cross", "none", "-" -> "×"
                "a" -> if (collapsed == "A") "A" else "a"
                "an" -> when (collapsed) {
                    "An", "AN" -> "An"
                    else -> "an"
                }
                "the" -> if (collapsed.firstOrNull()?.isUpperCase() == true) "The" else "the"
                else -> collapsed
            }

            GrammarTopic.PREPOSITION -> when (collapsed.lowercase(Locale.US)) {
                "x", "×", "cross", "none", "-" -> "×"
                else -> collapsed
            }
        }
    }

    /**
     * Checks a single blank against the verified book solution key.
     * Supports slash alternatives like "a/the", "the/×", "of/for/about", "before/in/in front of".
     */
    fun isCorrect(
        userAnswer: String?,
        expectedKey: String,
        strictCapitalization: Boolean,
        topic: GrammarTopic = GrammarTopic.ARTICLE
    ): Boolean {
        val normalized = normalizeInput(userAnswer, topic) ?: return false
        val acceptedVariants = expectedKey.split("/").map {
            it.trim().replace(multiSpaceRegex, " ")
        }
        return if (strictCapitalization) {
            acceptedVariants.any { it == normalized }
        } else {
            val lowerUser = normalized.lowercase(Locale.US)
            acceptedVariants.any { it.lowercase(Locale.US) == lowerUser }
        }
    }

    fun evaluatePassage(
        exercise: ExerciseItem,
        userAnswers: Map<String, String>,
        config: ExamConfig
    ): PassageResult {
        val outcomes = BLANK_KEYS.map { key ->
            val expected = exercise.answers[key] ?: if (exercise.topic == GrammarTopic.ARTICLE) "×" else ""
            val userAns = normalizeInput(userAnswers[key], exercise.topic)
            val unanswered = userAns == null
            val correct = if (unanswered) false else isCorrect(userAns, expected, config.strictCapitalization, exercise.topic)
            BlankOutcome(
                blankKey = key,
                userAnswer = userAns,
                expectedAnswer = expected,
                isCorrect = correct,
                isUnanswered = unanswered
            )
        }
        val correctCount = outcomes.count { it.isCorrect }
        val wrongCount = outcomes.count { !it.isCorrect && !it.isUnanswered }
        val unansweredCount = outcomes.count { it.isUnanswered }

        val unitMark = if (config.hscHalfMarkScale) 0.5 else 1.0
        val penaltyUnit = if (config.hscHalfMarkScale) config.negativePenaltyPerWrong * 0.5 else config.negativePenaltyPerWrong
        val raw = max(0.0, correctCount * unitMark - wrongCount * penaltyUnit)
        val maxScore = BLANK_KEYS.size * unitMark

        return PassageResult(
            exercise = exercise,
            blankOutcomes = outcomes,
            correctCount = correctCount,
            wrongCount = wrongCount,
            unansweredCount = unansweredCount,
            rawScore = raw,
            maxRawScore = maxScore
        )
    }

    fun formatScore(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    fun formatDuration(seconds: Int): String {
        val safe = max(0, seconds)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
