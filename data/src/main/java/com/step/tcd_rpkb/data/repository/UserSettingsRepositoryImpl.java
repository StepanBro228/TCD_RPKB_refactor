package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;


public class UserSettingsRepositoryImpl implements UserSettingsRepository {

    private static final String PREFS_NAME = "DataProviderPrefs";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_DEVICENUM = "deviceNum";
    private static final String PREF_ONLINE_MODE = "online_mode";
    private static final String PREF_DATABASE_URL = "database_url";

    private static final String DEFAULT_EMPTY_STRING = "";
    
    // URL для баз данных
    private static final String MAIN_DATABASE_URL = "http://rdc1c-upp/upp82/ru_RU/hs/jsontsd/";
    private static final String TEST_DATABASE_URL = "https://rdc1c.rpkb.ru/upp82_dev3/ru_RU/";

    private final SharedPreferences sharedPreferences;


    public UserSettingsRepositoryImpl(Context context) {

        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
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
    public void saveCredentialsWithDeviceNum(Credentials credentials) {
        if (credentials == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USERNAME, credentials.getUsername());
        editor.putString(PREF_PASSWORD, credentials.getPassword());
        editor.putString(PREF_DEVICENUM, credentials.getDeviceNum());
        editor.apply();
    }

    @Override
    public Credentials getCredentials() {
        String username = sharedPreferences.getString(PREF_USERNAME, DEFAULT_EMPTY_STRING);
        String password = sharedPreferences.getString(PREF_PASSWORD, DEFAULT_EMPTY_STRING);
        String deviceNum = sharedPreferences.getString(PREF_DEVICENUM, DEFAULT_EMPTY_STRING);
        return new Credentials(username, password, deviceNum);
    }



    @Override
    public void setOnlineMode(boolean isOnline) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_ONLINE_MODE, isOnline);
        editor.apply();
    }

    @Override
    public boolean isOnlineMode() {
        return sharedPreferences.getBoolean(PREF_ONLINE_MODE, false);
    }

    @Override
    public void setDatabaseURL (String DatabaseURL) {
        android.util.Log.d("UserSettingsRepo", "Сохранение URL базы данных: " + DatabaseURL);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_DATABASE_URL, DatabaseURL);
        editor.apply();
        android.util.Log.d("UserSettingsRepo", "Настройка сохранена. Текущий URL: " + getDatabaseURL());
    }

    @Override
    public String getDatabaseURL() {
        String URL = sharedPreferences.getString(PREF_DATABASE_URL, ""); // true - основная база по умолчанию
        android.util.Log.d("UserSettingsRepo", "Получение настройки базы данных: " + URL);
        return URL;
    }

} 