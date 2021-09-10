package com.singularitycoder.sqlitecodesnippets.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.singularitycoder.sqlitecodesnippets.model.Todo

@Database(entities = [Todo::class], version = 1)
abstract class TodoRoomDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var instance: TodoRoomDatabase? = null

        @Synchronized
        fun getInstance(context: Context): TodoRoomDatabase? {
            return if (null == instance) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoRoomDatabase::class.java,
                    "Todo_Database"
                ).fallbackToDestructiveMigration().build()
                instance
            } else instance
        }
    }
}