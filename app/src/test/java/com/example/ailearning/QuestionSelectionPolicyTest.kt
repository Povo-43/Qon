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

    // ---------------------------------------------------------- 未出優先（厳格）

    @Test
    fun unansweredQuestionsAreAlwaysPickedBeforeAnsweredOnes() {
        // 未回答が2問しかなくても、既答が何問あろうと必ず未回答が先に出る
        val candidates = (1..100).map { item("answered$it", 10, 5) } +
            listOf(item("new1", 0, 0), item("new2", 0, 0))
        val result = QuestionSelectionPolicy.select(candidates, 2, random = Random(42))
        val ids = result.map { it.id }.toSet()
        assertEquals(setOf("new1", "new2"), ids)
    }

    @Test
    fun answeredOnesOnlyAppearAfterUnansweredPoolIsExhausted() {
        val candidates = listOf(item("new1", 0, 0), item("new2", 0, 0)) +
            (1..20).map { item("answered$it", 4, 1) }
        val result = QuestionSelectionPolicy.select(candidates, 10, random = Random(7))
        val ids = result.map { it.id }
        // 最初の2問は必ず未回答、残り8問は既答から
        assertEquals(setOf("new1", "new2"), ids.take(2).toSet())
        assertTrue(ids.drop(2).all { it.startsWith("answered") })
        assertEquals(10, ids.size)
    }

    @Test
    fun unansweredPicksAreRotatedRandomly() {
        val candidates = (1..20).map { item("new$it", 0, 0) }
        val a = QuestionSelectionPolicy.select(candidates, 5, random = Random(1))
        val b = QuestionSelectionPolicy.select(candidates, 5, random = Random(2))
        // シードが違えば並びも変わる（固定順ではない＝ランダムに回る）
        assertTrue(a.map { it.id } != b.map { it.id })
    }

    // ---------------------------------------------------------- 既答の重み付け

    @Test
    fun strugglingQuestionsArePickedMoreOftenThanMasteredOnes() {
        // 誤答率が高い問題は、毎回正解の問題より優先されて出る
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
    fun fewerAttemptedQuestionsArePreferredAmongEqualAccuracy() {
        // 正答率が同じなら、解答数が少ない問題がより多く出る
        val candidates = (1..10).map { item("fresh$it", attempts = 1, correct = 0) } +
            (11..20).map { item("stale$it", attempts = 10, correct = 0) }
        val random = Random(42)
        var freshPicks = 0
        val trials = 400
        repeat(trials) {
            val picked = QuestionSelectionPolicy.select(candidates, 1, random = random).first()
            if (picked.id.startsWith("fresh")) freshPicks++
        }
        assertTrue(
            "fewer-attempts picked ${freshPicks}/${trials}",
            freshPicks > trials * 0.55
        )
    }

    @Test
    fun masteredQuestionsStillAppearOccasionally() {
        // 毎回正解の問題も完全には排除しない（たまには出る）
        val candidates = (1..20).map { item("mastered$it", 10, 10) }
        val random = Random(42)
        val picked = (1..100).mapNotNull {
            QuestionSelectionPolicy.selectOne(candidates, random = random)
        }
        assertTrue(picked.isNotEmpty())
    }

    // ---------------------------------------------------------- 基本動作

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
        val candidates = (1..30).map { item("q$it", it % 5, (it * 3) % 5) }
        val result = QuestionSelectionPolicy.select(candidates, 30, random = Random(7))
        assertEquals(result.size, result.map { it.id }.distinct().size)
    }

    @Test
    fun selectIsDeterministicWithSameSeed() {
        val candidates = (1..30).map { item("q$it", it % 5, (it * 3) % 5) }
        val a = QuestionSelectionPolicy.select(candidates, 10, random = Random(123))
        val b = QuestionSelectionPolicy.select(candidates, 10, random = Random(123))
        assertEquals(a.map { it.id }, b.map { it.id })
    }
}
