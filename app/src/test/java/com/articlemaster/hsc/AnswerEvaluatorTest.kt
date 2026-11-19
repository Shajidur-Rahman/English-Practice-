package com.articlemaster.hsc

import com.articlemaster.hsc.data.GitHubUpdateChecker
import com.articlemaster.hsc.data.UpdateCheckResult
import com.articlemaster.hsc.model.AnswerEvaluator
import com.articlemaster.hsc.model.BLANK_KEYS
import com.articlemaster.hsc.model.ExamConfig
import com.articlemaster.hsc.model.ExerciseItem
import com.articlemaster.hsc.model.FlashcardEntity
import com.articlemaster.hsc.model.FlashcardStatus
import com.articlemaster.hsc.model.GrammarTopic
import com.articlemaster.hsc.model.ReviewRating
import com.articlemaster.hsc.model.SpacedRepetitionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnswerEvaluatorTest {

    @Test
    fun slashVariantsAcceptEitherAlternative() {
        assertTrue(AnswerEvaluator.isCorrect("a", "a/the", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("the", "a/the", strictCapitalization = true))
        assertFalse(AnswerEvaluator.isCorrect("an", "a/the", strictCapitalization = true))

        assertTrue(AnswerEvaluator.isCorrect("x", "the/×", strictCapitalization = false))
        assertTrue(AnswerEvaluator.isCorrect("×", "the/×", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("the", "the/×", strictCapitalization = true))
    }

    @Test
    fun strictAndFlexibleCapitalizationBehaveCorrectly() {
        assertFalse(AnswerEvaluator.isCorrect("the", "The", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("The", "The", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("the", "The", strictCapitalization = false))

        assertTrue(AnswerEvaluator.isCorrect("A", "A/the", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("the", "A/the", strictCapitalization = true))
        assertFalse(AnswerEvaluator.isCorrect("a", "A/the", strictCapitalization = true))
        assertTrue(AnswerEvaluator.isCorrect("a", "A/the", strictCapitalization = false))
    }

    @Test
    fun prepositionMultiVariantsAndMultiWordPhrasesEvaluateProperly() {
        assertTrue(AnswerEvaluator.isCorrect("of", "of/for/about", false, GrammarTopic.PREPOSITION))
        assertTrue(AnswerEvaluator.isCorrect("for", "of/for/about", true, GrammarTopic.PREPOSITION))
        assertTrue(AnswerEvaluator.isCorrect("about", "of/for/about", true, GrammarTopic.PREPOSITION))
        assertFalse(AnswerEvaluator.isCorrect("with", "of/for/about", false, GrammarTopic.PREPOSITION))

        // Multi-word phrase with extra whitespace
        assertTrue(
            AnswerEvaluator.isCorrect("  in   front   of ", "before/in/in front of", true, GrammarTopic.PREPOSITION)
        )
        assertTrue(
            AnswerEvaluator.isCorrect("IN FRONT OF", "before/in/in front of", false, GrammarTopic.PREPOSITION)
        )
        assertFalse(
            AnswerEvaluator.isCorrect("In front of", "before/in/in front of", true, GrammarTopic.PREPOSITION)
        )

        // Capitalized sentence starter variants
        assertTrue(AnswerEvaluator.isCorrect("With", "With/From", true, GrammarTopic.PREPOSITION))
        assertFalse(AnswerEvaluator.isCorrect("with", "With/From", true, GrammarTopic.PREPOSITION))
        assertTrue(AnswerEvaluator.isCorrect("from", "With/From", false, GrammarTopic.PREPOSITION))
        assertTrue(AnswerEvaluator.isCorrect("After", "On/After", true, GrammarTopic.PREPOSITION))
    }

    @Test
    fun evaluationSupportsHscHalfMarkAndNegativePenaltyForBothTopics() {
        val artItem = ExerciseItem(
            id = 1,
            topic = GrammarTopic.ARTICLE,
            page = 49,
            boardInfo = "Dhaka Board-2019",
            isBoardQuestion = true,
            imageAsset = "passages/p_01.webp",
            answers = mapOf(
                "a" to "The", "b" to "an", "c" to "a", "d" to "the", "e" to "the",
                "f" to "a", "g" to "The", "h" to "×", "i" to "×", "j" to "a"
            )
        )
        val userAnswers = mapOf(
            "a" to "The", "b" to "an", "c" to "a", "d" to "the", "e" to "the",
            "f" to "a", "g" to "The", "h" to "×", "i" to "the", "j" to "a" // 9 correct, 1 wrong
        )
        val resHalf = AnswerEvaluator.evaluatePassage(
            artItem,
            userAnswers,
            ExamConfig(hscHalfMarkScale = true, strictCapitalization = true, negativePenaltyPerWrong = 0.0)
        )
        assertEquals(9, resHalf.correctCount)
        assertEquals(1, resHalf.wrongCount)
        assertEquals(4.5, resHalf.rawScore, 0.001)
        assertEquals(5.0, resHalf.maxRawScore, 0.001)

        val resNeg = AnswerEvaluator.evaluatePassage(
            artItem,
            userAnswers,
            ExamConfig(hscHalfMarkScale = false, strictCapitalization = true, negativePenaltyPerWrong = 0.25)
        )
        assertEquals(8.75, resNeg.rawScore, 0.001)

        val prepItem = ExerciseItem(
            id = 26,
            topic = GrammarTopic.PREPOSITION,
            page = 86,
            boardInfo = "Mymensingh Board-2022",
            isBoardQuestion = true,
            imageAsset = "passages/prep_26.webp",
            answers = mapOf(
                "a" to "of", "b" to "in", "c" to "of", "d" to "from", "e" to "to",
                "f" to "with", "g" to "under", "h" to "before/in/in front of", "i" to "in/within", "j" to "after"
            )
        )
        val prepUser = mapOf(
            "a" to "of", "b" to "in", "c" to "of", "d" to "from", "e" to "to",
            "f" to "with", "g" to "under", "h" to "in front of", "i" to "within", "j" to "after"
        )
        val prepRes = AnswerEvaluator.evaluatePassage(
            prepItem,
            prepUser,
            ExamConfig(hscHalfMarkScale = true, strictCapitalization = true)
        )
        assertEquals(10, prepRes.correctCount)
        assertEquals(5.0, prepRes.rawScore, 0.001)
    }

    @Test
    fun spacedRepetitionSchedulesMasteryAndResetsOnAgain() {
        val base = FlashcardEntity(
            cardKey = "PREPOSITION_1_b",
            topic = GrammarTopic.PREPOSITION,
            exerciseId = 1,
            blankKey = "b",
            boardInfo = "Dhaka Board-2025",
            imageAsset = "passages/prep_01.webp",
            customPrompt = null,
            correctAnswer = "of/for/about",
            lastWrongAnswer = "to",
            mistakeCount = 1,
            reviewCount = 0,
            streak = 0,
            easeFactor = 2.5,
            intervalDays = 0,
            nextReviewAt = 1000L,
            status = FlashcardStatus.LEARNING,
            isManual = false,
            userNote = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val r1 = SpacedRepetitionEngine.applyReview(base, ReviewRating.GOOD, nowMs = 10_000L)
        assertEquals(1, r1.streak)
        assertEquals(1, r1.intervalDays)
        assertEquals(FlashcardStatus.LEARNING, r1.status)

        val r2 = SpacedRepetitionEngine.applyReview(r1, ReviewRating.GOOD, nowMs = 20_000L)
        assertEquals(2, r2.streak)
        assertEquals(3, r2.intervalDays)
        assertEquals(FlashcardStatus.LEARNING, r2.status)

        val r3 = SpacedRepetitionEngine.applyReview(r2, ReviewRating.GOOD, nowMs = 30_000L)
        assertEquals(3, r3.streak)
        assertTrue(r3.intervalDays >= 4)
        assertEquals(FlashcardStatus.MASTERED, r3.status)

        val lapse = SpacedRepetitionEngine.applyReview(r3, ReviewRating.AGAIN, nowMs = 40_000L)
        assertEquals(0, lapse.streak)
        assertEquals(0, lapse.intervalDays)
        assertEquals(FlashcardStatus.LEARNING, lapse.status)
        assertEquals(40_000L + SpacedRepetitionEngine.RETRY_DELAY_MS, lapse.nextReviewAt)
    }

    @Test
    fun gitHubUpdaterSemanticVersionComparisonWorks() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("v2.2.0", "2.1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v2.1.1", "2.1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("2.10.0", "2.2.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v3.0", "2.9.9"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v2.1.0", "2.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("2.0.9", "2.1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.9.9", "2.1.0"))

        val apkUrl = "https://github.com/Shajidur-Rahman/English-Practice-/releases/download/v2.2.0/EnglishPractice.apk"
        val htmlUrl = "https://github.com/Shajidur-Rahman/English-Practice-/releases/tag/v2.2.0"

        val avail = GitHubUpdateChecker.evaluateRelease(
            tag = "v2.2.0",
            title = "English Practice v2.2.0",
            releaseNotes = "Added new questions",
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            publishedAt = "2026-11-20",
            currentVersion = "2.1.0"
        )
        assertTrue(avail is UpdateCheckResult.UpdateAvailable)
        val info = (avail as UpdateCheckResult.UpdateAvailable).info
        assertEquals("v2.2.0", info.latestTag)
        assertEquals(apkUrl, info.downloadUrl)

        val fallback = GitHubUpdateChecker.evaluateRelease(
            tag = "v2.2.0",
            title = "",
            releaseNotes = "",
            htmlUrl = htmlUrl,
            apkDownloadUrl = null,
            publishedAt = "2026-11-20",
            currentVersion = "2.1.0"
        )
        assertTrue(fallback is UpdateCheckResult.UpdateAvailable)
        assertEquals(htmlUrl, (fallback as UpdateCheckResult.UpdateAvailable).info.downloadUrl)

        val same = GitHubUpdateChecker.evaluateRelease(
            tag = "v2.2.0",
            title = "English Practice v2.2.0",
            releaseNotes = "",
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            publishedAt = "2026-11-20",
            currentVersion = "2.2.0"
        )
        assertTrue(same is UpdateCheckResult.UpToDate)
    }

    @Test
    fun allArticleAndPrepositionAssetsAndKeysExist() {
        val assetsDir = File("src/main/assets")

        val artJson = File(assetsDir, "answer_keys.json")
        assertTrue("answer_keys.json must exist at ${artJson.absolutePath}", artJson.exists())
        val artRaw = artJson.readText()
        for (id in 1..76) {
            val padded = id.toString().padStart(2, '0')
            val img = File(assetsDir, "passages/p_$padded.webp")
            assertTrue("Missing article asset ${img.path}", img.exists() && img.length() > 1000)
            val solImg = File(assetsDir, "solutions/sol_art_$padded.webp")
            assertTrue("Missing article solution photo ${solImg.path}", solImg.exists() && solImg.length() > 1000)
            assertTrue("Missing article id $id in JSON", artRaw.contains("\"id\": $id"))
        }

        val prepJson = File(assetsDir, "preposition_answer_keys.json")
        assertTrue("preposition_answer_keys.json must exist at ${prepJson.absolutePath}", prepJson.exists())
        val prepRaw = prepJson.readText()
        for (id in 1..80) {
            val padded = id.toString().padStart(2, '0')
            val img = File(assetsDir, "passages/prep_$padded.webp")
            assertTrue("Missing preposition asset ${img.path}", img.exists() && img.length() > 1000)
            val solImg = File(assetsDir, "solutions/sol_prep_$padded.webp")
            assertTrue("Missing preposition solution photo ${solImg.path}", solImg.exists() && solImg.length() > 1000)
            assertTrue("Missing preposition id $id in JSON", prepRaw.contains("\"id\": $id"))
        }

        BLANK_KEYS.forEach { k ->
            assertTrue("Missing blank key $k in articles", artRaw.contains("\"$k\":"))
            assertTrue("Missing blank key $k in prepositions", prepRaw.contains("\"$k\":"))
        }
    }
}
