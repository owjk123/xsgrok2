package com.xsgrok2.app

import android.app.Application

class App : Application() {
    val database by lazy { com.xsgrok2.app.data.database.AppDatabase.getInstance(this) }
    val preferences by lazy { com.xsgrok2.app.data.preferences.AppPreferences.getInstance(this) }
}
