package com.example.ailearning.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ailearning.data.local.db.AppDatabase

object DatabaseModule {

    /** v1 → v2: ウィジェットのカスタマイズ項目を追加。既存データは保持する。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN themeId TEXT NOT NULL DEFAULT 'indigo'")
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN backgroundColor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN accentColor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN currentIndex INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** v2 → v3: 集中モード（ロック対象アプリ）テーブルを追加。既存データは保持する。 */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `focus_blocked_apps` (" +
                    "`packageName` TEXT NOT NULL, " +
                    "`label` TEXT NOT NULL, " +
                    "`enabled` INTEGER NOT NULL, " +
                    "`addedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`packageName`))"
            )
        }
    }

    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ai_learning.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
}
