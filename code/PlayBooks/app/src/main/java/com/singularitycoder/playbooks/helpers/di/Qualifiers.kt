package com.singularitycoder.playbooks.helpers.di

import javax.inject.Qualifier

@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention
@Qualifier
annotation class MainDispatcher
