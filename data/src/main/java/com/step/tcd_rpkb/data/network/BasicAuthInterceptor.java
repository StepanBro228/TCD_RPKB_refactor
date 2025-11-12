package com.step.tcd_rpkb.data.network;

import android.util.Base64;
import android.util.Log;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерсептор для добавления Basic авторизации к HTTP запросам
 */
public class BasicAuthInterceptor implements Interceptor {
    
    private static final String TAG = "BasicAuthInterceptor";
    
    private final UserSettingsRepository userSettingsRepository;
    
    public BasicAuthInterceptor(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Получаем сохраненные учетные данные
        Credentials credentials = userSettingsRepository.getCredentials();
        
        if (credentials != null && 
            credentials.getUsername() != null && !credentials.getUsername().isEmpty() &&
            credentials.getPassword() != null && !credentials.getPassword().isEmpty()) {
            
            // Создаем строку для Basic авторизации
            String authString = credentials.getUsername() + ":" + credentials.getPassword();
            String encodedAuthString = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
            
            // Добавляем заголовок Authorization
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Basic " + encodedAuthString)
                    .build();
            
            Log.d(TAG, "Добавлена Basic авторизация для пользователя: " + credentials.getUsername());
            return chain.proceed(authenticatedRequest);
        } else {
            Log.d(TAG, "Учетные данные не найдены, запрос без авторизации");
            return chain.proceed(originalRequest);
        }
    }
} 