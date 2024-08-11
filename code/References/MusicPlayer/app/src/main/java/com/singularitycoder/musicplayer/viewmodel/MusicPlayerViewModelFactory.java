package com.singularitycoder.musicplayer.viewmodel;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;

public class MusicPlayerViewModelFactory extends AbstractSavedStateViewModelFactory {
    private Application appContext;

    public MusicPlayerViewModelFactory(Application appContext) {
        this.appContext = appContext;
    }

//    @SuppressWarnings("unchecked")
//    @NonNull
//    @Override
//    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
//        return (T) new MusicPlayerViewModel(appContext);
//    }

    @androidx.annotation.NonNull
    @Override
    protected <T extends androidx.lifecycle.ViewModel> T create(@androidx.annotation.NonNull String s, @androidx.annotation.NonNull Class<T> aClass, @androidx.annotation.NonNull SavedStateHandle savedStateHandle) {
        return (T) new MusicPlayerViewModel(appContext);
    }
}
