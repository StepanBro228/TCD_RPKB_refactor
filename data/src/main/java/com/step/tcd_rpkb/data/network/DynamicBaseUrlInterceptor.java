package com.step.tcd_rpkb.data.network;

import android.util.Log;

import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DynamicBaseUrlInterceptor implements Interceptor {
    
    private static final String TAG = "DynamicBaseUrlInterceptor";
    
    private final UserSettingsRepository userSettingsRepository;
    
    public DynamicBaseUrlInterceptor(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        HttpUrl originalUrl = originalRequest.url();
        
        // Получаем актуальный базовый URL
        String currentBaseUrl = userSettingsRepository.getDatabaseURL();
        Log.d(TAG, "Используем базовый URL: " + currentBaseUrl);
        
        // Проверяем, содержит ли базовый URL уже hs/jsontsd
        boolean hasHsJson = currentBaseUrl.contains("/hs/jsontsd/") ||
                           (currentBaseUrl.endsWith("/hs/jsontsd"));
        
        // Парсим новый базовый URL
        HttpUrl newBaseUrl = HttpUrl.parse(currentBaseUrl);
        if (newBaseUrl == null) {
            Log.e(TAG, "Не удалось распарсить базовый URL: " + currentBaseUrl);
            return chain.proceed(originalRequest);
        }
        
        // Строим новый URL
        HttpUrl.Builder urlBuilder = newBaseUrl.newBuilder();
        
        // Проверяем тип запроса
        String originalUrlStr = originalUrl.toString();
        boolean isMovelist = originalUrlStr.contains("movelist");
        boolean isDocumentMoveSave = originalUrlStr.contains("documentmove_save");
        boolean isDocumentMove = originalUrlStr.contains("documentmove") && !isDocumentMoveSave; // Исключаем documentmove_save
        boolean isAuthorization = originalUrlStr.contains("rpkb_autorization");
        boolean isProductSeries = originalUrlStr.contains("ProductSeries");
        

        if (isAuthorization) {
            Log.d(TAG, "Определен запрос типа авторизация - возвращаем без изменений");
            Log.d(TAG, "Оригинальный URL авторизации: " + originalUrl);
            return chain.proceed(originalRequest);
        }
        

        if (!hasHsJson) {
            urlBuilder.addPathSegment("hs");
            urlBuilder.addPathSegment("jsontsd");
        }
        
        if (isMovelist) {
            // Для запроса списка перемещений
            urlBuilder.addPathSegment("movelist");
            Log.d(TAG, "Определен запрос типа movelist");
        } else if (isDocumentMoveSave) {
            // Для запроса сохранения данных перемещения в 1С
            urlBuilder.addPathSegment("documentmove_save");
            Log.d(TAG, "Определен запрос типа documentmove_save");
        } else if (isDocumentMove) {
            // Для запроса деталей документа
            urlBuilder.addPathSegment("documentmove");
            Log.d(TAG, "Определен запрос типа documentmove");
        } else if (isProductSeries) {
            // Для запроса серий товара
            urlBuilder.addPathSegment("ProductSeries");
            Log.d(TAG, "Определен запрос типа ProductSeries");
        } else {
            // Для других запросов - сохраняем структуру
            Log.d(TAG, "Неизвестный тип запроса, сохраняем оригинальную структуру пути");
            java.util.List<String> originalSegments = originalUrl.pathSegments();
            

            boolean skipFirst = true;
            for (String segment : originalSegments) {
                if (skipFirst) {
                    skipFirst = false;
                    continue;
                }
                if (!segment.isEmpty()) {
                    urlBuilder.addPathSegment(segment);
                }
            }
        }
        
        // Добавляем все query параметры
        for (int i = 0; i < originalUrl.querySize(); i++) {
            String name = originalUrl.queryParameterName(i);
            String value = originalUrl.queryParameterValue(i);
            urlBuilder.addQueryParameter(name, value);
            Log.d(TAG, "Добавлен параметр: " + name + "=" + value);
        }
        
        HttpUrl newUrl = urlBuilder.build();
        
        Log.d(TAG, "Оригинальный URL: " + originalUrl);
        Log.d(TAG, "Новый URL: " + newUrl);
        
        // Создаем новый запрос с обновленным URL
        Request newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build();
        
        return chain.proceed(newRequest);
    }
} 