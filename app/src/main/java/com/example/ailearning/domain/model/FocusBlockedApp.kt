package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 集中モード（v1.6.0）でロック対象に指定されたアプリ。
 * 画面ロック解除のたびにゲートの問題に正解するまで起動を阻む。
 */
@Entity(tableName = "focus_blocked_apps")
data class FocusBlockedApp(
    /** 対象アプリのパッケージ名（主キー） */
    @PrimaryKey val packageName: String,
    /** 表示用アプリ名 */
    val label: String,
    /** 一時的に無効化できる */
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
