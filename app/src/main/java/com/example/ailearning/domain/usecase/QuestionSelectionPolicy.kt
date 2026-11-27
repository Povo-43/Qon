package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.QuestionWithStats
import kotlin.math.exp
import kotlin.random.Random

/**
 * 出題選択ポリシー（純粋関数のみ・単体テスト対象）。
 *
 * どの画面（連続学習・ランダム・ゲート・ウィジェット）で解いても同じ順序規則を適用する。
 * 復習モード（弱点問題中心）は対象外。
 *
 * 選ばれる順序:
 *
 * 1. **未出の問題（まだ一度も答えていない）をまず出す**
 *    - 未回答の問題が残っている限り、既答の問題は選ばれない（重みではなく厳格な優先）
 * 2. **既答の問題は「正答率が低い」→「解答数が少ない」の順に優先し、ランダムに回す**
 *    - (正答率 ASC, 解答回数 ASC) で並べ、上位ほど選ばれやすい重み付きランダム（非復元抽出）
 *    - 毎回正解している問題はほとんど出ないが、完全には排除しない
 */
object QuestionSelectionPolicy {

    /** 既答リストの何割かを基準にした重み減衰のスケール。小さいほど上位固定になる */
    private const val RANK_DECAY_SCALE = 4.0

    /** 最下位（毎回正解・たくさん解答済み）でも残る最低重み。0にすると完全排除になるので少し残す */
    private const val MIN_WEIGHT = 0.02

    /**
     * 候補から [count] 問を選ぶ。
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
        val pool = candidates.filter { it.question.id !in excludeIds }

        val picked = ArrayList<Question>(count.coerceAtMost(pool.size))

        // ---- 優先1: 未出の問題をまず出す（ランダムに回す）----
        val unanswered = pool.filter { it.isUnanswered }.map { it.question }
        if (unanswered.isNotEmpty()) {
            picked += unanswered.shuffled(random).take(count - picked.size)
        }

        // ---- 優先2: 既答は「正答率が低い」「解答数が少ない」の順に重み付きで回す ----
        if (picked.size < count) {
            val answered = pool
                .filter { !it.isUnanswered }
                .sortedWith(compareBy({ it.accuracy ?: 1.0 }, { it.attempts }))
            if (answered.isNotEmpty()) {
                picked += weightedSample(answered, count - picked.size, random).map { it.question }
            }
        }

        return picked
    }

    /**
     * (正答率昇順, 解答回数昇順) にソート済みのリストから、ランクに応じた重み付きで
     * [count] 件を非復元抽出する。上位（正答率が低い・解答数が少ない）ほど選ばれやすいが、
     * 下位も確率は低く残るため、出題が回っていく。
     */
    private fun <T> weightedSample(
        sorted: List<T>,
        count: Int,
        random: Random
    ): List<T> {
        val pool = sorted.toMutableList()
        val picked = ArrayList<T>(count.coerceAtMost(pool.size))
        val decay = RANK_DECAY_SCALE
        while (picked.size < count && pool.isNotEmpty()) {
            val tau = (pool.size / decay).coerceAtLeast(1.0)
            var total = 0.0
            val weights = DoubleArray(pool.size)
            for (i in pool.indices) {
                val w = (exp(-i / tau)).coerceAtLeast(MIN_WEIGHT)
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
            picked.add(pool.removeAt(index))
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
