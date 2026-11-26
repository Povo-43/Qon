package com.example.ailearning

import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.QuestionWithStats
import com.example.ailearning.domain.usecase.QuestionSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionSelectionPolicyTest {

    private fun question(id: String) = Question(
        id = id,
        learningSetId = "set1",
        questionText = "Q$id",
        optionA = "a", optionB = "b", optionC = "c", optionD = "d",
        correctAnswer = 0,
        explanation = "e",
        difficulty = 3,
        category = "cat",
        source = Question.SOURCE_AI
    )

    private fun item(id: String, attempts: Int, correct: Int) =
        QuestionWithStats(question(id), attempts, correct)

    // ---------------------------------------------------------- weightOf

    @Test
    fun unansweredQuestionHasHighestWeight() {
        assertEquals(QuestionSelectionPolicy.UNANSWERED_WEIGHT, QuestionSelectionPolicy.weightOf(0, 0), 1e-9)
        assertEquals(QuestionSelectionPolicy.UNANSWERED_WEIGHT, QuestionSelectionPolicy.weightOf(-1, 0), 1e-9)
    }

    @Test
    fun alwaysCorrectQuestionHasLowestWeight() {
        assertEquals(
            QuestionSelectionPolicy.MASTERED_WEIGHT,
            QuestionSelectionPolicy.weightOf(10, 10),
            1e-9
        )
    }

    @Test
    fun allWrongQuestionWeightsMoreThanAlwaysCorrect() {
        val allWrong = QuestionSelectionPolicy.weightOf(5, 0)
        val half = QuestionSelectionPolicy.weightOf(4, 2)
        val allCorrect = QuestionSelectionPolicy.weightOf(5, 5)
        assertTrue(allWrong > half)
        assertTrue(half > allCorrect)
        assertEquals(QuestionSelectionPolicy.STRUGGLING_WEIGHT, allWrong, 1e-9)
    }

    // ---------------------------------------------------------- select

    @Test
    fun selectReturnsEmptyWhenNoCandidates() {
        val result = QuestionSelectionPolicy.select(emptyList(), 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun selectReturnsAtMostCount() {
        val candidates = (1..30).map { item("q$it", 0, 0) }
        val result = QuestionSelectionPolicy.select(candidates, 10)
        assertEquals(10, result.size)
    }

    @Test
    fun selectReturnsAllWhenPoolSmallerThanCount() {
        val candidates = (1..3).map { item("q$it", 0, 0) }
        val result = QuestionSelectionPolicy.select(candidates, 10)
        assertEquals(3, result.size)
    }

    @Test
    fun selectNeverReturnsExcludedIds() {
        val candidates = (1..30).map { item("q$it", 0, 0) }
        val excluded = setOf("q1", "q2", "q3")
        val result = QuestionSelectionPolicy.select(candidates, 30, excluded)
        assertTrue(result.none { it.id in excluded })
        assertEquals(27, result.size)
    }

    @Test
    fun selectReturnsNothingWhenAllExcluded() {
        val candidates = (1..5).map { item("q$it", 0, 0) }
        val excluded = (1..5).map { "q$it" }.toSet()
        val result = QuestionSelectionPolicy.select(candidates, 5, excluded)
        assertTrue(result.isEmpty())
    }

    @Test
    fun selectDoesNotDuplicate() {
        val candidates = (1..30).map { item("q$it", 0, 0) }
        val result = QuestionSelectionPolicy.select(candidates, 30, random = Random(7))
        assertEquals(result.size, result.map { it.id }.distinct().size)
    }

    // ---------------------------------------------------------- 傾向（重み付け）

    @Test
    fun unansweredQuestionsArePickedFarMoreOftenThanMasteredOnes() {
        // 未回答10問 vs 毎回正解10問 → 未回答が圧倒的に多く選ばれるべき
        val candidates = (1..10).map { item("new$it", 0, 0) } +
            (11..20).map { item("mastered$it", 10, 10) }

        val random = Random(42)
        var unansweredPicks = 0
        val trials = 500
        repeat(trials) {
            val picked = QuestionSelectionPolicy.select(candidates, 1, random = random).first()
            if (picked.id.startsWith("new")) unansweredPicks++
        }
        assertTrue(
            "unanswered picked ${unansweredPicks}/${trials}",
            unansweredPicks > trials * 0.7
        )
    }

    @Test
    fun strugglingQuestionsArePickedMoreOftenThanMasteredOnes() {
        // 誤答率が高い問題は、毎回正解の問題より優先されて出てよい
        val candidates = (1..10).map { item("wrong$it", 10, 0) } +
            (11..20).map { item("mastered$it", 10, 10) }

        val random = Random(42)
        var wrongPicks = 0
        val trials = 500
        repeat(trials) {
            val picked = QuestionSelectionPolicy.select(candidates, 1, random = random).first()
            if (picked.id.startsWith("wrong")) wrongPicks++
        }
        assertTrue(
            "struggling picked ${wrongPicks}/${trials}",
            wrongPicks > trials * 0.7
        )
    }

    @Test
    fun masteredQuestionsStillAppearOccasionally() {
        // 毎回正解の問題も完全には排除しない（たまには出る）
        val candidates = (1..20).map { item("mastered$it", 10, 10) }
        val random = Random(42)
        val picked = (1..50).mapNotNull {
            QuestionSelectionPolicy.selectOne(candidates, random = random)
        }
        assertTrue(picked.isNotEmpty())
    }

    @Test
    fun selectIsDeterministicWithSameSeed() {
        val candidates = (1..30).map { item("q$it", it % 5, (it * 3) % 5) }
        val a = QuestionSelectionPolicy.select(candidates, 10, random = Random(123))
        val b = QuestionSelectionPolicy.select(candidates, 10, random = Random(123))
        assertEquals(a.map { it.id }, b.map { it.id })
    }
}
