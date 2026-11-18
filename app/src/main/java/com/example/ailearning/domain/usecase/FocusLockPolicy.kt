package com.example.ailearning.domain.usecase

/**
 * 集中モードの判定ロジック（純粋関数のみ・単体テスト対象）。
 *
 * 前提の仕様:
 * - 画面ロック解除のたびに「未解除」状態へ戻る
 * - 未解除の間、対象アプリが前面に来たらゲート（1問正解）を出す
 * - 正解したら次のロックまで有効
 * - 問題を用意できない場合はフェイルオープン（通す）
 */
object FocusLockPolicy {

    /** キーガードが閉じている（= ロック画面表示中）と観測された直後の状態 */
    const val SESSION_NONE = 0

    /**
     * ゲートを表示すべきか。
     *
     * @param focusEnabled 集中モードが有効
     * @param blockedPackages ロック対象のパッケージ名集合
     * @param packageName 前面に来たパッケージ名
     * @param keyguardLocked ロック画面が表示中か
     * @param unlockSessionActive 次のロックまでの解除セッション中か
     * @param isOwnPackage 自分自身（Qon）か
     */
    fun shouldShowGate(
        focusEnabled: Boolean,
        blockedPackages: Set<String>,
        packageName: String,
        keyguardLocked: Boolean,
        unlockSessionActive: Boolean,
        isOwnPackage: Boolean
    ): Boolean =
        focusEnabled &&
            !keyguardLocked &&
            !unlockSessionActive &&
            !isOwnPackage &&
            packageName in blockedPackages

    /** ロック画面が表示されたので、ゲートと解除セッションを閉じるべきか */
    fun shouldLock(keyguardLocked: Boolean): Boolean = keyguardLocked

    /** 「ロック解除 → 未解除状態へ戻す」遷移か（前回ロック中 → 今回解除） */
    fun isUnlockTransition(wasKeyguardLocked: Boolean, isKeyguardLocked: Boolean): Boolean =
        wasKeyguardLocked && !isKeyguardLocked

    /**
     * ロック対象にすると端末の操作が困難になる保護対象パッケージ。
     * 設定・電話・システムUI・自アプリは常に選択不可。
     */
    fun isProtectedPackage(packageName: String, ownPackageName: String): Boolean {
        if (packageName == ownPackageName) return true
        return packageName in PROTECTED_PACKAGES ||
            PROTECTED_PREFIXES.any { packageName.startsWith(it) }
    }

    /** 誤答のあと、同じ問題の正解・解説を見せる → 次の問題へ。表示すべき次問題の選択。 */
    fun <T> pickNext(candidates: List<T>, recentKeys: Set<String>, keyOf: (T) -> String): T? =
        candidates.firstOrNull { keyOf(it) !in recentKeys }
            ?: candidates.firstOrNull()

    private val PROTECTED_PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.incallui",
        "com.android.emergency",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.samsung.android.smartservice" // Samsung 設定系
    )

    /** プレフィックスで保護する（OEM差異を吸収） */
    private val PROTECTED_PREFIXES = listOf(
        "com.android.systemui",
        "com.android.server"
    )
}
