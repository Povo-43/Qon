package com.example.ailearning.di

import android.content.Context
import androidx.room.Room
import com.example.ailearning.data.local.db.AppDatabase

object DatabaseModule {
    
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ai_learning.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
