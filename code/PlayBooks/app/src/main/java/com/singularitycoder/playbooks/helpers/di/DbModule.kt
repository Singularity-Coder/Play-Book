package com.singularitycoder.playbooks.helpers.di

import android.content.Context
import androidx.room.Room
import com.singularitycoder.playbooks.BookDao
import com.singularitycoder.playbooks.helpers.Db
import com.singularitycoder.playbooks.helpers.db.PlayBookDatabase
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
    fun injectConnectMeDatabase(@ApplicationContext context: Context): PlayBookDatabase {
        return Room.databaseBuilder(context, PlayBookDatabase::class.java, Db.CONNECT_ME).build()
    }

    @Singleton
    @Provides
    fun injectFeedDao(db: PlayBookDatabase): BookDao = db.bookDao()
}
