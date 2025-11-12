package com.step.tcd_rpkb.data.network;

import android.util.Log;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.net.SocketTimeoutException;
import java.net.ConnectException;

public class ConnectionRetryInterceptor implements Interceptor {
    
    private static final String TAG = "RetryInterceptor";
    private static final int MAX_RETRIES = 3;
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        

        int retryDelay = 1000;
        int retryCount = 0;
        
        Response response = null;
        IOException lastException = null;
        
        while (retryCount < MAX_RETRIES) {
            try {
                if (retryCount > 0) {
                    Log.w(TAG, "Повторная попытка #" + retryCount + " для URL: " + request.url());
                }
                
                response = chain.proceed(request);
                
                // Проверяем на ошибки сервера
                if (response.isSuccessful() || response.code() < 500) {
                    return response;
                }
                

                if (response.body() != null) {
                    response.close();
                }
                
                Log.w(TAG, "Ошибка сервера с кодом: " + response.code() + ", пробуем снова");
                
            } catch (SocketTimeoutException | ConnectException e) {
                lastException = e;
                Log.e(TAG, "Ошибка соединения: " + e.getMessage() + ", попытка #" + (retryCount + 1));
            }
            
            // Увеличиваем счетчик попыток
            retryCount++;
            

            if (retryCount >= MAX_RETRIES) {
                Log.e(TAG, "Достигнуто максимальное количество попыток для URL: " + request.url());
                break;
            }
            

            try {
                Log.i(TAG, "Ожидание " + retryDelay + "ms перед повторной попыткой...");
                Thread.sleep(retryDelay);
                retryDelay *= 2; // Увеличиваем задержку в 2 раза для следующей попытки
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Прерывание во время ожидания повторной попытки", ie);
            }
        }
        

        if (response != null) {
            return response;
        } else if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Не удалось выполнить запрос после " + MAX_RETRIES + " попыток");
        }
    }
} 