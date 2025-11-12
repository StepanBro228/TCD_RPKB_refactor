package com.step.tcd_rpkb.data.network;

import android.util.Base64;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерсептор для добавления фиксированной авторизации к запросам авторизации
 */
public class AuthorizationInterceptor implements Interceptor {
    
    private static final String TAG = "AuthorizationInterceptor";
    
    // Фиксированные учетные данные для авторизационных запросов
    private static final String FIXED_USERNAME = "WebServiceIntegration";
    private static final String FIXED_PASSWORD = "hd678KBs#jd}DD";
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Создаем строку для Basic авторизации
        String authString = FIXED_USERNAME + ":" + FIXED_PASSWORD;
        String encodedAuthString = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
        
        // Добавляем заголовок Authorization
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Basic " + encodedAuthString)
                .build();
        
        Log.d(TAG, "Добавлена фиксированная авторизация для запроса: " + originalRequest.url());
        return chain.proceed(authenticatedRequest);
    }
} 