package com.example.ailearning.data.remote.api

import com.example.ailearning.domain.model.GeneratedQuestion
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * AI 応答から問題リストをできるだけ寛容に取り出す共通パーサ。
 *
 * 対応する形式:
 * - {"questions": [...]} / {"quiz": [...]} / {"items": [...]} など一般的なキー
 * - トップレベル配列 [...]
 * - 単一問題オブジェクト {"questionText": ...}
 * - 問題文の別名: questionText / question / text / q / prompt / body
 * - 選択肢の別名: options / choices / answers / optionA..D / A..D キー
 *   （配列だけでなく {"A": "...", "B": "..."} 形式や {"text": "..."} オブジェクト配列も可）
 * - 正解の別名: correctAnswer / answer / correct / correctOption / answerIndex など
 *   （数値インデックス、0-3 / 1-4、"A"〜"D" の文字列に対応）
 */
object QuestionJsonParser {

    private val arrayKeys = listOf(
        "questions", "quiz", "quizzes", "items", "problems", "results", "data", "list"
    )

    private val questionTextKeys = listOf(
        "questionText", "question_text", "questionTextJp", "question", "text", "q", "prompt", "body", "title"
    )

    private val optionsKeys = listOf("options", "choices", "answers", "candidates", "selects", "alternatives")

    private val correctAnswerKeys = listOf(
        "correctAnswer", "correct_answer", "answer", "correct", "correctOption",
        "correct_option", "correctIndex", "correct_index", "answerIndex", "answer_index",
        "answerNumber", "correctNumber", "solution"
    )

    private val explanationKeys = listOf(
        "explanation", "explain", "explanationText", "explanation_text", "commentary",
        "reason", "rationale", "description", "note", "解説"
    )

    private val difficultyKeys = listOf("difficulty", "difficultyLevel", "difficulty_level", "level", "stars")

    private val categoryKeys = listOf("category", "categoryName", "category_name", "tag", "topic", "section", "field", "unit")

    private val optionObjectKeys = listOf("text", "label", "value", "option", "content", "choice", "answer")

    /** AI 応答本文から問題リストへ変換する。失敗時は空リスト。 */
    fun parse(content: String?): List<GeneratedQuestion> {
        if (content.isNullOrBlank()) return emptyList()
        val root = extractJson(content) ?: return emptyList()
        val array = findQuestionArray(root) ?: return emptyList()
        return array.mapNotNull { parseQuestion(it) }
    }

    /** 本文から JSON 部分だけを取り出す（コードフェンス・前置き・後書きを許容）。 */
    private fun extractJson(content: String): JsonElement? {
        var text = content.trim().removePrefix("\uFEFF")

        if (text.contains("```")) {
            val fenceStart = text.indexOf("```")
            var inner = text.substring(fenceStart + 3)
            inner = inner.trimStart().removePrefix("json").removePrefix("JSON").trimStart()
            val fenceEnd = inner.indexOf("```")
            if (fenceEnd >= 0) inner = inner.substring(0, fenceEnd)
            text = inner.trim()
        }

        try {
            return JsonParser.parseString(text)
        } catch (e: Exception) {
            // 前後に説明文が混ざっているケースへフォールバック
        }

        val objStart = text.indexOf('{')
        val arrStart = text.indexOf('[')
        val start = listOf(objStart, arrStart).filter { it >= 0 }.minOrNull() ?: return null
        val open = text[start]
        val close = if (open == '{') '}' else ']'
        val end = text.lastIndexOf(close)
        if (end <= start) return null

        return try {
            JsonParser.parseString(text.substring(start, end + 1))
        } catch (e: Exception) {
            null
        }
    }

    /** 問題配列を探す。ネスト（{"quiz": {"questions": [...]}}）にも対応。 */
    private fun findQuestionArray(root: JsonElement): JsonArray? {
        when {
            root.isJsonArray -> {
                val arr = root.asJsonArray
                // 配列の中にさらに配列が入っている場合（[ [ {...}, ... ] ]）を1段ほどく
                if (arr.size() > 0 && arr[0].isJsonArray) {
                    return arr.flatMap { el -> if (el.isJsonArray) el.asJsonArray.toList() else listOf(el) }
                        .let { JsonArray().apply { it.forEach(::add) } }
                }
                return arr
            }
            root.isJsonObject -> {
                val obj = root.asJsonObject
                for (key in arrayKeys) {
                    val el = obj.get(key) ?: continue
                    if (el.isJsonArray) return el.asJsonArray
                    if (el.isJsonObject) findQuestionArray(el)?.let { return it }
                }
                // 配列キーが無い場合は、オブジェクトの配列を含む最初の値を探す
                val nested = obj.entrySet()
                    .mapNotNull { it.value }
                    .filter { it.isJsonArray }
                    .map { it.asJsonArray }
                    .firstOrNull { it.size() > 0 && it[0].isJsonObject }
                if (nested != null) return nested

                // 単一問題オブジェクトなら 1 件として扱う
                if (looksLikeQuestion(obj)) {
                    return JsonArray().apply { add(obj) }
                }
                return null
            }
            else -> return null
        }
    }

    private fun looksLikeQuestion(obj: JsonObject): Boolean =
        firstString(obj, questionTextKeys) != null || firstElement(obj, optionsKeys) != null

    private fun parseQuestion(element: JsonElement?): GeneratedQuestion? {
        if (element == null || !element.isJsonObject) return null
        val obj = element.asJsonObject

        // {"question": {"text": ..., "options": [...]}} のようなネストをほどく
        val nestedQuestion = obj.get("question")
        val source: JsonObject = if (nestedQuestion != null && nestedQuestion.isJsonObject) {
            mergeJsonObjects(obj, nestedQuestion.asJsonObject)
        } else {
            obj
        }

        val questionText = firstString(source, questionTextKeys)?.trim().orEmpty()
        if (questionText.isBlank()) return null

        val options = parseOptions(source)
        if (options.size < 2) return null

        var correct = parseCorrectAnswer(source, options.size)
        if (correct == null) {
            // 正解指定が "選択肢の文章そのもの" の場合も試す
            val correctText = firstString(source, listOf("correctAnswerText", "correctAnswerTextJp", "answerText"))
            correct = correctText?.let { text -> options.indexOfFirst { it.trim() == text.trim() } }?.takeIf { it >= 0 }
        }
        if (correct == null) correct = 0
        if (correct >= options.size) correct = options.size - 1
        if (correct < 0) correct = 0

        // 選択肢が4未満なら「―」で埋める（UI は 4 択前提）
        val padded = buildList {
            addAll(options.take(4))
            while (size < 4) add("―")
        }

        val explanation = firstString(source, explanationKeys).orEmpty()
        val difficulty = parseDifficulty(source)
        val category = firstString(source, categoryKeys).orEmpty().trim()

        return GeneratedQuestion(
            questionText = questionText,
            options = padded,
            correctAnswer = correct,
            explanation = explanation,
            difficulty = difficulty,
            category = category
        )
    }

    private fun parseOptions(obj: JsonObject): List<String> {
        val result = mutableListOf<String>()

        for (key in optionsKeys) {
            val el = obj.get(key) ?: continue
            val parsed = when {
                el.isJsonArray -> el.asJsonArray.mapNotNull { optionToString(it) }
                el.isJsonObject -> el.asJsonObject.entrySet()
                    .sortedBy { it.key }
                    .mapNotNull { optionToString(it.value) }
                else -> emptyList()
            }
            if (parsed.size >= 2) {
                result.addAll(parsed)
                break
            }
        }

        if (result.isEmpty()) {
            // optionA..D / a..d の個別フィールド
            val letterFields = listOf("optionA", "optionB", "optionC", "optionD", "a", "b", "c", "d")
            val letters = listOf("A", "B", "C", "D")
            letters.forEachIndexed { index, letter ->
                val direct = firstString(obj, listOf(letter, letter.lowercase(), "option$letter", "choice$letter"))
                if (!direct.isNullOrBlank()) {
                    result.add(direct)
                } else {
                    val field = letterFields.getOrNull(index + 4) // "a","b","c","d"
                    val value = field?.let { firstString(obj, listOf(it)) }
                    if (!value.isNullOrBlank()) result.add(value)
                }
            }
        }

        return result.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun optionToString(el: JsonElement?): String? {
        if (el == null || el.isJsonNull) return null
        return when {
            el.isJsonPrimitive -> el.asJsonPrimitive.asString
            el.isJsonObject -> firstString(el.asJsonObject, optionObjectKeys)
            else -> null
        }
    }

    /** 正解を 0 始まりインデックスで返す。判定できなければ null。 */
    private fun parseCorrectAnswer(obj: JsonObject, optionCount: Int): Int? {
        for (key in correctAnswerKeys) {
            val el = obj.get(key) ?: continue
            if (el.isJsonNull) continue

            if (el.isJsonPrimitive) {
                val primitive = el.asJsonPrimitive
                if (primitive.isNumber) {
                    val value = primitive.asInt
                    return when {
                        value in 0 until optionCount -> value
                        value == optionCount -> optionCount - 1 // 1-4 指定の "4"
                        else -> value.coerceIn(0, optionCount - 1)
                    }
                }
                if (primitive.isString) {
                    parseLetterOrNumber(primitive.asString, optionCount)?.let { return it }
                }
            }

            if (el.isJsonObject) {
                val nested = el.asJsonObject
                for (innerKey in correctAnswerKeys) {
                    val inner = nested.get(innerKey) ?: continue
                    if (inner.isJsonPrimitive && inner.asJsonPrimitive.isString) {
                        parseLetterOrNumber(inner.asString, optionCount)?.let { return it }
                    }
                    if (inner.isJsonPrimitive && inner.asJsonPrimitive.isNumber) {
                        return inner.asInt.coerceIn(0, optionCount - 1)
                    }
                }
            }
        }
        return null
    }

    private fun parseLetterOrNumber(raw: String, optionCount: Int): Int? {
        // 全角英数字・記号を半角へ寄せてから判定する
        val text = raw.trim().replace(Regex("[Ａ-Ｚａ-ｚ０-９]")) { match ->
            val code = match.value[0].code
            ((code - 0xFEE0).toChar()).toString()
        }
        if (text.isEmpty()) return null

        // "A", "B", "C", "D"（"B." / "(C)" / "答え: D" なども許容）
        val letter = text.firstOrNull { it in 'A'..'D' || it in 'a'..'d' }
        if (letter != null) {
            val index = letter.uppercaseChar() - 'A'
            if (index in 0 until optionCount) return index
        }

        val digits = text.filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            val value = digits.toIntOrNull() ?: return null
            return when {
                value in 0 until optionCount -> value
                value == optionCount -> optionCount - 1
                else -> value.coerceIn(0, optionCount - 1)
            }
        }
        return null
    }

    private fun parseDifficulty(obj: JsonObject): Int {
        for (key in difficultyKeys) {
            val el = obj.get(key) ?: continue
            if (el.isJsonNull) continue

            if (el.isJsonPrimitive) {
                val primitive = el.asJsonPrimitive
                if (primitive.isNumber) return primitive.asInt.coerceIn(1, 5)
                if (primitive.isString) {
                    val value = primitive.asString.trim().lowercase()
                    val number = value.filter { it.isDigit() }.toIntOrNull()
                    if (number != null) return number.coerceIn(1, 5)
                    return when (value) {
                        "easy", "初級", "かんたん", "易しい" -> 1
                        "normal", "novice", "中級" -> 2
                        "medium", "intermediate" -> 3
                        "hard", "上級" -> 4
                        "expert", "非常に難しい", "最上級" -> 5
                        else -> 3
                    }
                }
            }
        }
        return 3
    }

    private fun firstElement(obj: JsonObject, keys: List<String>): JsonElement? {
        for (key in keys) {
            val el = obj.get(key)
            if (el != null && !el.isJsonNull) return el
        }
        return null
    }

    private fun firstString(obj: JsonObject, keys: List<String>): String? {
        for (key in keys) {
            val el = obj.get(key) ?: continue
            when {
                el.isJsonPrimitive && el.asJsonPrimitive.isString -> {
                    val value = el.asString
                    if (value.isNotBlank()) return value
                }
                el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> return el.asJsonPrimitive.asString
                el.isJsonObject -> {
                    // {"ja": "..."} のような多言語オブジェクトから値を拾う
                    val nested = el.asJsonObject
                    firstString(nested, listOf("ja", "jp", "text", "value", "label"))?.let { return it }
                }
            }
        }
        return null
    }

    /** 外側のオブジェクトをベースに、内側（question 本体など）の値で不足キーを補う。 */
    private fun mergeJsonObjects(base: JsonObject, override: JsonObject): JsonObject {
        val merged = JsonObject()
        for ((key, value) in base.entrySet()) {
            if (!value.isJsonNull) merged.add(key, value)
        }
        for ((key, value) in override.entrySet()) {
            if (value.isJsonNull) continue
            val existing = merged.get(key)
            if (existing == null || existing.isJsonNull) {
                merged.add(key, value)
            } else if (existing.isJsonObject && value.isJsonObject) {
                merged.add(key, mergeJsonObjects(existing.asJsonObject, value.asJsonObject))
            } else {
                // question 本体の値を優先
                merged.add(key, value)
            }
        }
        return merged
    }
}
