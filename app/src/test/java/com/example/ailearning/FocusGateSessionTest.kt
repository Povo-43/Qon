package com.example.ailearning

import com.example.ailearning.domain.model.Question
import com.example.ailearning.presentation.focus.FocusGateSession
import com.example.ailearning.presentation.focus.FocusGateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGateSessionTest {

    private fun question(id: String, correct: Int = 2) = Question(
        id = id,
        learningSetId = "set1",
        questionText = "問題$id",
        optionA = "A",
        optionB = "B",
        optionC = "C",
        optionD = "D",
        correctAnswer = correct,
        explanation = "解説$id",
        difficulty = 2,
        category = "一般",
        source = Question.SOURCE_AI
    )

    @Test
    fun initialPhaseIsLoading() {
        assertEquals(FocusGateUiState.Phase.LOADING, FocusGateSession().state.value.phase)
    }

    @Test
    fun questionLoadMovesToAnswering() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1"))
        assertEquals(FocusGateUiState.Phase.ANSWERING, session.state.value.phase)
        assertEquals(0, session.state.value.attempts)
    }

    @Test
    fun correctAnswerUnlocksInSingleQuestionMode() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1", correct = 2))
        val correct = session.onAnswer(2)
        assertTrue(correct)
        assertEquals(FocusGateUiState.Phase.UNLOCKED, session.state.value.phase)
        assertEquals(1, session.state.value.attempts)
    }

    @Test
    fun wrongAnswerShowsResultWithSelectedAndCorrect() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1", correct = 2))
        val correct = session.onAnswer(0)
        assertEquals(false, correct)
        assertEquals(FocusGateUiState.Phase.RESULT_WRONG, session.state.value.phase)
        assertEquals(0, session.state.value.selectedAnswer)
        assertEquals(2, session.state.value.question?.correctAnswer)
        assertEquals(1, session.state.value.wrongCount)
    }

    @Test
    fun nextQuestionClearsSelectionAndReturnsToLoading() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1", correct = 2))
        session.onAnswer(0)
        session.onNextQuestion()
        assertEquals(FocusGateUiState.Phase.LOADING, session.state.value.phase)
        assertEquals(-1, session.state.value.selectedAnswer)
        assertEquals(false, session.state.value.isCorrect)

        session.onQuestionLoaded(question("q2"))
        assertEquals(FocusGateUiState.Phase.ANSWERING, session.state.value.phase)
        assertEquals(-1, session.state.value.selectedAnswer)
    }

    @Test
    fun multiCorrectModeRequiresRequiredCorrectAnswers() {
        val session = FocusGateSession()
        session.reset(requiredCorrect = 2, showCorrectAnswer = true)
        session.onQuestionLoaded(question("q1", correct = 2))
        session.onAnswer(2)
        assertEquals(FocusGateUiState.Phase.RESULT_CORRECT, session.state.value.phase)
        assertEquals(1, session.state.value.correctCount)

        session.onNextQuestion()
        session.onQuestionLoaded(question("q2", correct = 1))
        session.onAnswer(1)
        assertEquals(FocusGateUiState.Phase.UNLOCKED, session.state.value.phase)
        assertEquals(2, session.state.value.correctCount)
    }

    @Test
    fun onAnswerIgnoresDoubleTap() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1", correct = 2))
        assertTrue(session.onAnswer(2))
        // 結果表示中の2回目は無視される
        assertFalse(session.onAnswer(2))
        assertEquals(FocusGateUiState.Phase.UNLOCKED, session.state.value.phase)
        assertEquals(1, session.state.value.attempts)
    }

    @Test
    fun nullQuestionTriggersFailOpen() {
        val session = FocusGateSession()
        session.onQuestionLoaded(null)
        assertEquals(FocusGateUiState.Phase.FAIL_OPEN, session.state.value.phase)
        assertTrue(session.state.value.failOpenMessage!!.isNotBlank())
    }

    @Test
    fun excludeIdsContainsEveryShownQuestion() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1"))
        session.onAnswer(0)
        session.onNextQuestion()
        session.onQuestionLoaded(question("q2"))
        session.onAnswer(1)
        session.onNextQuestion()
        session.onQuestionLoaded(question("q3"))
        session.onAnswer(3)
        assertEquals(3, session.state.value.attempts)
        assertEquals(setOf("q1", "q2", "q3"), session.excludeIds())
    }

    @Test
    fun resetClearsEverything() {
        val session = FocusGateSession()
        session.onQuestionLoaded(question("q1"))
        session.onAnswer(0)
        session.reset(requiredCorrect = 1, showCorrectAnswer = false)
        assertEquals(FocusGateUiState.Phase.LOADING, session.state.value.phase)
        assertEquals(0, session.state.value.attempts)
        assertEquals(emptySet<String>(), session.excludeIds())
        assertEquals(false, session.state.value.showCorrectAnswer)
    }
}
