package com.example.ailearning

import android.app.Application
import android.content.Context
import com.example.ailearning.di.AppContainer

class AiLearningApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(applicationContext)
    }
}
