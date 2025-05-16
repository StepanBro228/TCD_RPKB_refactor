package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Credentials;

public interface UserSettingsRepository {

    void saveCredentials(Credentials credentials);

    Credentials getCredentials();

    void setOnlineMode(boolean isOnline);

    boolean isOnlineMode();
    
    // Можно добавить метод для получения всех настроек разом, если потребуется
    // UserSettings getAllUserSettings(); 
    // void saveAllUserSettings(UserSettings settings);
} 