package com.example.ailearning

import com.example.ailearning.data.remote.api.QuestionJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionJsonParserTest {

    @Test
    fun `標準形式をパースできる`() {
        val json = """
            {"questions":[
              {"questionText":"Q1","options":["a","b","c","d"],"correctAnswer":2,
               "explanation":"解説","difficulty":3,"category":"一般"},
              {"questionText":"Q2","options":["x","y","z","w"],"correctAnswer":0,
               "explanation":"","difficulty":1,"category":""}
            ]}
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(2, questions.size)
        assertEquals("Q1", questions[0].questionText)
        assertEquals(2, questions[0].correctAnswer)
        assertEquals(4, questions[0].options.size)
        assertEquals("一般", questions[0].category)
    }

    @Test
    fun `コードフェンス付きでもパースできる`() {
        val json = """
            以下が問題です。
            ```json
            {"questions":[{"questionText":"Q","options":["a","b","c","d"],"correctAnswer":1}]}
            ```
            以上です。
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(1, questions.size)
        assertEquals(1, questions[0].correctAnswer)
    }

    @Test
    fun `トップレベル配列をパースできる`() {
        val json = """
            [{"question":"Q?","choices":["a","b","c","d"],"answer":"C"}]
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(1, questions.size)
        assertEquals(2, questions[0].correctAnswer)
        assertEquals("Q?", questions[0].questionText)
    }

    @Test
    fun `文字列の正解番号1始まりを扱える`() {
        val json = """
            {"questions":[{"questionText":"Q","options":["a","b","c","d"],"correctAnswer":"4"}]}
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(3, questions[0].correctAnswer)
    }

    @Test
    fun `quizキーとA-Dオブジェクト形式を扱える`() {
        val json = """
            {"quiz":[{"question":"Q","A":"ア","B":"イ","C":"ウ","D":"エ","answer":"B","difficulty":"easy"}]}
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(1, questions.size)
        assertEquals(listOf("ア", "イ", "ウ", "エ"), questions[0].options)
        assertEquals(1, questions[0].correctAnswer)
        assertEquals(1, questions[0].difficulty)
    }

    @Test
    fun `選択肢が3つでも4つにパディングされる`() {
        val json = """
            {"questions":[{"questionText":"Q","options":["a","b","c"],"correctAnswer":0}]}
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(4, questions[0].options.size)
        assertEquals("―", questions[0].options[3])
    }

    @Test
    fun `テキストのみの不正な応答は空を返す`() {
        assertTrue(QuestionJsonParser.parse("申し訳ありませんが生成できません").isEmpty())
        assertTrue(QuestionJsonParser.parse("").isEmpty())
        assertTrue(QuestionJsonParser.parse(null).isEmpty())
    }

    @Test
    fun `難易度と解説の欠落に耐える`() {
        val json = """
            {"questions":[{"questionText":"Q","options":["a","b","c","d"],"answer":2}]}
        """.trimIndent()
        val questions = QuestionJsonParser.parse(json)
        assertEquals(2, questions[0].correctAnswer)
        assertEquals(3, questions[0].difficulty)
        assertEquals("", questions[0].explanation)
    }
}
