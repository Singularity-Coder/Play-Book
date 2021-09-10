package com.singularitycoder.sqlitecodesnippets

import android.app.Application
import com.singularitycoder.sqlitecodesnippets.db.TodoRoomDatabase

class MyApp : Application() {

    var database: TodoRoomDatabase? = null

    override fun onCreate() {
        super.onCreate()
        database = TodoRoomDatabase.getInstance(applicationContext)
    }
}