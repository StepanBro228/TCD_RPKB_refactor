package com.step.tcd_rpkb;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends Application {
    // Вы можете добавить здесь код инициализации, если он необходим при старте приложения
    @Override
    public void onCreate() {
        super.onCreate();
        // Например, инициализация библиотек, которые требуют этого на уровне Application
    }
} 