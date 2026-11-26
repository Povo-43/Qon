package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.QuestionWithStats
import kotlin.random.Random

/**
 * 出題選択ポリシー（純粋関数のみ・単体テスト対象）。
 *
 *要件:
 * - なるべく「答えたことのない問題」を出す
 * - 誤答率が高い（正答率の低い）問題は出てもよい
 * - 毎回正解している問題は繰り返し出さない
 *
 * そのため各問題に「出題しやすさ」の重みを与え、重み付きランダムで選ぶ。
 *
 * | 状態                     | 重み            | 意味 |
 * |--------------------------|-----------------|------|
 * | 未回答                   | 3.0             | 最優先。既答のどんな問題より出やすい |
 * | 正答率 0%（全部間違い）   | 1.2             | 復習価値が高いので積極的に出す |
 * | 正答率 50%               | 0.7             | たまに出る |
 * | 正答率 100%（全部正解）   | 0.2             | ほとんど出ない（未回答の 1/15） |
 */
object QuestionSelectionPolicy {

    /** 未回答問題の重み */
    const val UNANSWERED_WEIGHT = 3.0

    /** 既答問題の重みの下限（正答率100%の問題）。 */
    const val MASTERED_WEIGHT = 0.2

    /** 既答問題の重みの上限（正答率0%の問題）。 */
    const val STRUGGLING_WEIGHT = 1.2

    /**
     * 回答統計から出題重みを計算する。
     *
     * @param attempts 回答回数（スキップ含む）
     * @param correct 正解回数
     */
    fun weightOf(attempts: Int, correct: Int): Double {
        if (attempts <= 0) return UNANSWERED_WEIGHT
        val accuracy = correct.toDouble().coerceAtLeast(0.0) / attempts
        val mistakeRate = (1.0 - accuracy).coerceIn(0.0, 1.0)
        return MASTERED_WEIGHT + (STRUGGLING_WEIGHT - MASTERED_WEIGHT) * mistakeRate
    }

    fun weightOf(item: QuestionWithStats): Double = weightOf(item.attempts, item.correct)

    /**
     * 候補から [count] 問を重み付きランダム（非復元抽出）で選ぶ。
     *
     * @param candidates 問題と回答統計のリスト
     * @param count 選ぶ問題数
     * @param excludeIds 除外する問題ID（例: 今のセッションで既に回答した問題）
     * @param random 乱数源（テストでシードを固定できる）
     */
    fun select(
        candidates: List<QuestionWithStats>,
        count: Int,
        excludeIds: Set<String> = emptySet(),
        random: Random = Random.Default
    ): List<Question> {
        if (count <= 0) return emptyList()
        val pool = candidates
            .filter { it.question.id !in excludeIds }
            .toMutableList()
        if (pool.isEmpty()) return emptyList()

        val picked = ArrayList<Question>(count.coerceAtMost(pool.size))
        val weights = DoubleArray(pool.size)
        while (picked.size < count && pool.isNotEmpty()) {
            var total = 0.0
            for (i in pool.indices) {
                val w = weightOf(pool[i])
                weights[i] = w
                total += w
            }
            var roll = random.nextDouble() * total
            var index = pool.size - 1
            for (i in pool.indices) {
                roll -= weights[i]
                if (roll < 0.0) {
                    index = i
                    break
                }
            }
            picked.add(pool.removeAt(index).question)
        }
        return picked
    }

    /** 候補から1問だけ選ぶ。無ければ null。 */
    fun selectOne(
        candidates: List<QuestionWithStats>,
        excludeIds: Set<String> = emptySet(),
        random: Random = Random.Default
    ): Question? = select(candidates, 1, excludeIds, random).firstOrNull()
}
