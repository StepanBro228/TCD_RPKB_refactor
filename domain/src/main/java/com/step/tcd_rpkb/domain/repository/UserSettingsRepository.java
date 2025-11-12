package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Credentials;

public interface UserSettingsRepository {

    void saveCredentials(Credentials credentials);
    void saveCredentialsWithDeviceNum(Credentials credentials);


    Credentials getCredentials();


    void setOnlineMode(boolean isOnline);

    boolean isOnlineMode();
    
    void setDatabaseURL (String DatabaseURL);
    
    String getDatabaseURL();


} 