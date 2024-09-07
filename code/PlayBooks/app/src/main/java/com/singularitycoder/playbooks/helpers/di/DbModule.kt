package com.singularitycoder.playbooks.helpers.di

import android.content.Context
import androidx.room.Room
import com.singularitycoder.playbooks.BookDao
import com.singularitycoder.playbooks.BookDataDao
import com.singularitycoder.playbooks.helpers.Db
import com.singularitycoder.playbooks.helpers.db.PlayBooksDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Singleton
    @Provides
    fun injectPlayBooksDatabase(@ApplicationContext context: Context): PlayBooksDatabase {
        return Room.databaseBuilder(context, PlayBooksDatabase::class.java, Db.PLAY_BOOKS).build()
    }

    @Singleton
    @Provides
    fun injectBookDao(db: PlayBooksDatabase): BookDao = db.bookDao()

    @Singleton
    @Provides
    fun injectBookDataDao(db: PlayBooksDatabase): BookDataDao = db.bookDataDao()
}
