package com.step.tcd_rpkb.data.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.step.tcd_rpkb.domain.util.ConnectivityChecker;
import javax.inject.Inject;
import dagger.hilt.android.qualifiers.ApplicationContext;

public class ConnectivityCheckerImpl implements ConnectivityChecker {
    private final Context appContext;

    @Inject
    public ConnectivityCheckerImpl(@ApplicationContext Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public boolean isNetworkAvailable() {
        if (appContext == null) return false;
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
} 