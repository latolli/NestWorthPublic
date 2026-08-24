package com.example.nestworth

import android.app.Application
import com.example.nestworth.Repository.db.AppDatabase
import com.example.nestworth.Repository.settings.SettingsRepository

class NestWorthApp : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val settingsRepository by lazy {
        SettingsRepository(this)
    }
}