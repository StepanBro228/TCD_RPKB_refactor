package com.step.tcd_rpkb.data.network;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерсептор для добавления пользовательской авторизации к запросам проверки пароля
 */
public class UserAuthInterceptor implements Interceptor {
    
    private static final String TAG = "UserAuthInterceptor";
    
    private String username;
    private String password;
    
    public UserAuthInterceptor() {
        // Конструктор по умолчанию
    }
    
    /**
     * Устанавливает учетные данные пользователя для авторизации
     * @param username логин пользователя
     * @param password пароль пользователя
     */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        Log.d(TAG, "Установлены учетные данные для пользователя: " + username);
    }
    
    /**
     * Очищает учетные данные пользователя
     */
    public void clearCredentials() {
        Log.d(TAG, "Очищены учетные данные пользователя");
        this.username = null;
        this.password = null;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Проверяем, что учетные данные установлены
        if (username != null && password != null && 
            !username.isEmpty() && !password.isEmpty()) {
            
            // Создаем строку для Basic авторизации
            String authString = username + ":" + password;
            String encodedAuthString = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
            
            // Добавляем заголовок Authorization
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Basic " + encodedAuthString)
                    .build();
            
            Log.d(TAG, "Добавлена пользовательская авторизация для: " + username);
            return chain.proceed(authenticatedRequest);
        } else {
            Log.d(TAG, "Учетные данные пользователя не установлены");
            return chain.proceed(originalRequest);
        }
    }
} 