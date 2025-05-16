package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

// Константы SharedPreferences перенесены сюда из DataProvider
public class UserSettingsRepositoryImpl implements UserSettingsRepository {

    private static final String PREFS_NAME = "DataProviderPrefs"; // Имя файла настроек
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_ONLINE_MODE = "online_mode";

    private static final String DEFAULT_EMPTY_STRING = "";

    private final SharedPreferences sharedPreferences;

    // Context необходим для доступа к SharedPreferences.
    // В будущем будет предоставлен через Dependency Injection (Hilt).
    public UserSettingsRepositoryImpl(Context context) {
        // Используем ApplicationContext, чтобы избежать утечек памяти Activity/Fragment context
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME, // Используем локальную константу
                Context.MODE_PRIVATE
        );
    }

    @Override
    public void saveCredentials(Credentials credentials) {
        if (credentials == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USERNAME, credentials.getUsername());
        editor.putString(PREF_PASSWORD, credentials.getPassword());
        editor.apply();
    }

    @Override
    public Credentials getCredentials() {
        String username = sharedPreferences.getString(PREF_USERNAME, DEFAULT_EMPTY_STRING);
        String password = sharedPreferences.getString(PREF_PASSWORD, DEFAULT_EMPTY_STRING);
        return new Credentials(username, password);
    }

    @Override
    public void setOnlineMode(boolean isOnline) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_ONLINE_MODE, isOnline);
        editor.apply();
    }

    @Override
    public boolean isOnlineMode() {
        return sharedPreferences.getBoolean(PREF_ONLINE_MODE, false); // false - значение по умолчанию
    }
} 