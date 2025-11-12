package com.step.tcd_rpkb.data.network;

import android.util.Log;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерцептор для добавления номера устройства в заголовок HTTP запросов
 */
public class DeviceNumInterceptor implements Interceptor {
    
    private static final String TAG = "DeviceNumInterceptor";
    private static final String DEVICE_NUM_HEADER = "Device-Num";
    
    private final UserSettingsRepository userSettingsRepository;
    
    public DeviceNumInterceptor(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Получаем сохраненные учетные данные
        Credentials credentials = userSettingsRepository.getCredentials();
        
        if (credentials != null && 
            credentials.getDeviceNum() != null && 
            !credentials.getDeviceNum().isEmpty()) {
            
            // Добавляем заголовок Device-Num
            Request requestWithDeviceNum = originalRequest.newBuilder()
                    .header(DEVICE_NUM_HEADER, credentials.getDeviceNum())
                    .build();
            
            Log.d(TAG, "Добавлен заголовок Device-Num: " + credentials.getDeviceNum());
            return chain.proceed(requestWithDeviceNum);
        } else {
            Log.d(TAG, "Номер устройства не найден, запрос без заголовка Device-Num");
            return chain.proceed(originalRequest);
        }
    }
}

