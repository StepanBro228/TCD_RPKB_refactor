package com.step.tcd_rpkb.data.network;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Кастомный интерцептор для полного логирования тела запроса без ограничений размера
 * Заменяет HttpLoggingInterceptor для случаев, когда нужно видеть полное тело запроса
 */
public class FullBodyLoggingInterceptor implements Interceptor {
    
    private static final String TAG = "FullBodyLogging";
    private static final int MAX_LOG_LENGTH = 4000; // Максимальная длина одного лога Android
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        
        // Логируем заголовки запроса
        Log.w(TAG, "=== HTTP ЗАПРОС ===");
        Log.w(TAG, "URL: " + request.url());
        Log.w(TAG, "Метод: " + request.method());
        
        // Логируем заголовки
        if (request.headers().size() > 0) {
            Log.w(TAG, "Заголовки:");
            for (int i = 0; i < request.headers().size(); i++) {
                String name = request.headers().name(i);
                String value = request.headers().value(i);
                Log.w(TAG, "  " + name + ": " + value);
            }
        }
        
        // Логируем тело запроса, если оно есть
        RequestBody requestBody = request.body();
        if (requestBody != null) {
            try {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                
                MediaType contentType = requestBody.contentType();
                Charset charset = StandardCharsets.UTF_8;
                if (contentType != null && contentType.charset() != null) {
                    charset = contentType.charset();
                }
                
                String bodyString = buffer.readString(charset);
                
                Log.w(TAG, "Content-Type: " + contentType);
                Log.w(TAG, "Content-Length: " + requestBody.contentLength() + " байт");
                Log.w(TAG, "Тело запроса (" + bodyString.length() + " символов):");
                
                // Разбиваем тело на части для логирования
                logLargeString(bodyString);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при логировании тела запроса: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Тело запроса отсутствует");
        }
        
        Log.w(TAG, "=== ОТПРАВКА ЗАПРОСА ===");
        
        // Выполняем запрос и логируем ответ
        long startTime = System.currentTimeMillis();
        Response response = chain.proceed(request);
        long endTime = System.currentTimeMillis();
        
        Log.w(TAG, "=== HTTP ОТВЕТ ===");
        Log.w(TAG, "Код ответа: " + response.code() + " " + response.message());
        Log.w(TAG, "Время выполнения: " + (endTime - startTime) + " мс");
        
        // Логируем заголовки ответа
        if (response.headers().size() > 0) {
            Log.w(TAG, "Заголовки ответа:");
            for (int i = 0; i < response.headers().size(); i++) {
                String name = response.headers().name(i);
                String value = response.headers().value(i);
                Log.w(TAG, "  " + name + ": " + value);
            }
        }
        
        Log.w(TAG, "=== КОНЕЦ HTTP ОТВЕТА ===");
        
        return response;
    }
    
    /**
     * Логирует длинную строку, разбивая её на части без потери данных
     */
    private void logLargeString(String text) {
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "(пустое тело)");
            return;
        }
        
        int length = text.length();
        if (length <= MAX_LOG_LENGTH) {
            Log.w(TAG, text);
            return;
        }
        
        Log.w(TAG, "--- Начало тела запроса ---");
        
        int position = 0;
        int partNumber = 1;
        
        while (position < length) {
            int end = Math.min(position + MAX_LOG_LENGTH, length);
            

            if (end < length) {

                int safeBreakPoint = findSafeBreakPoint(text, Math.max(position, end - 300), end);
                if (safeBreakPoint > position) {
                    end = safeBreakPoint + 1;
                }
            }
            
            String part = text.substring(position, end);
            Log.w(TAG, "Часть " + partNumber + ": " + part);
            
            position = end;
            partNumber++;
        }
        
        Log.w(TAG, "--- Конец тела запроса ---");
    }
    
    /**
     * Ищет безопасное место для разрыва строки (после запятой, скобки и т.д.)
     * @param text исходный текст
     * @param startSearch начало поиска
     * @param maxEnd максимальная позиция
     * @return позиция для безопасного разрыва или -1 если не найдено
     */
    private int findSafeBreakPoint(String text, int startSearch, int maxEnd) {
        // Ищем в обратном порядке от maxEnd к startSearch
        for (int i = maxEnd - 1; i >= startSearch && i >= 0; i--) {
            char c = text.charAt(i);
            // Ищем символы, после которых безопасно разрывать JSON
            // Приоритет: запятая > закрывающая скобка > кавычка
            if (c == ',') {
                return i; // Самый безопасный разрыв - после запятой
            }
            if (c == '}' || c == ']') {
                return i;
            }
        }
        
        // Если не нашли запятую или скобки, ищем кавычки
        for (int i = maxEnd - 1; i >= startSearch && i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '"') {
                return i; // Разрыв после завершения строкового значения
            }
        }
        
        return -1; // Безопасное место не найдено
    }
} 