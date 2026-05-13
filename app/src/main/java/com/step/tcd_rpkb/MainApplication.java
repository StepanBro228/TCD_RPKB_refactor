package com.step.tcd_rpkb;

import android.app.Application;
import android.util.Log;

import dagger.hilt.android.HiltAndroidApp;
import io.realm.Realm;

@HiltAndroidApp
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        Log.d("MainApplication", "Realm инициализирован");

    }
} 