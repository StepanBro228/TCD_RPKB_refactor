package com.step.tcd_rpkb.data.datasources;

import android.util.Log;
import com.google.gson.GsonBuilder;

import com.step.tcd_rpkb.data.mapper.ProductSeriesMapper;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.network.dto.ProductSeriesDto;
import com.step.tcd_rpkb.data.network.dto.ProductSeriesResponseDto;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Remote data source для получения серий товаров через API
 */
public class RemoteSeriesDataSource {
    
    private static final String TAG = "RemoteSeriesDataSource";
    
    private final MoveApiService moveApiService;
    
    @Inject
    public RemoteSeriesDataSource(MoveApiService moveApiService) {
        this.moveApiService = moveApiService;
    }
    
    /**
     * Получает серии товара через API ProductSeries
     *
     * @param moveGuid GUID перемещения
     * @param lineGuid GUID строки товара
     * @param callback колбэк для получения результата
     */
    public void getProductSeries(String moveGuid, String lineGuid, DataSourceCallback<List<SeriesItem>> callback) {
        Log.d(TAG, "Запрос серий товара: moveGuid=" + moveGuid + ", lineGuid=" + lineGuid);
        
        try {
            Call<ProductSeriesResponseDto> call = moveApiService.getProductSeries(moveGuid, lineGuid);
            
            // Логируем URL запроса для отладки
            String requestUrl = call.request().url().toString();
            Log.d(TAG, "ProductSeries URL: " + requestUrl);
            
            call.enqueue(new Callback<ProductSeriesResponseDto>() {
                @Override
                public void onResponse(Call<ProductSeriesResponseDto> call, Response<ProductSeriesResponseDto> response) {
                    Log.d(TAG, "ProductSeries onResponse: Code=" + response.code() + ", isSuccessful=" + response.isSuccessful());
                    
                    ProductSeriesResponseDto dto = response.body();
                    String rawErrorBody = null;
                    
                    // Если response.body() пустой, пытаемся получить JSON из errorBody
                    if (dto == null && response.errorBody() != null) {
                        try {
                            rawErrorBody = response.errorBody().string();
                            Log.d(TAG, "ProductSeries: попытка парсинга JSON из errorBody: " + rawErrorBody);
                            com.google.gson.Gson gson = new GsonBuilder()
                                    .disableHtmlEscaping()
                                    .create();
                            dto = gson.fromJson(rawErrorBody, ProductSeriesResponseDto.class);
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при парсинге errorBody как JSON в ProductSeries", e);
                        }
                    }
                    
                    // Обрабатываем DTO если получен
                    if (dto != null) {
                        processProductSeriesResponseDto(dto, callback);
                    } else {
                        // Если DTO не получен, возвращаем общую ошибку
                        String errorMessage = "Ошибка получения серий товара: " + response.code();
                        if (rawErrorBody != null && !rawErrorBody.isEmpty()) {
                            errorMessage += " - " + rawErrorBody;
                        } else if (response.errorBody() != null) {
                            try {
                                String fallbackErrorBody = response.errorBody().string();
                                if (!fallbackErrorBody.isEmpty()) {
                                    errorMessage += " - " + fallbackErrorBody;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка чтения errorBody для ProductSeries (fallback)", e);
                            }
                        }
                        Log.e(TAG, errorMessage);
                        callback.onError(new Exception(errorMessage));
                    }
                }
                
                @Override
                public void onFailure(Call<ProductSeriesResponseDto> call, Throwable t) {
                    String errorMessage = "Сетевая ошибка при получении серий товара: " + t.getMessage();
                    Log.e(TAG, errorMessage, t);
                    callback.onError(new Exception(errorMessage, t));
                }
            });
            
        } catch (Exception e) {
            String errorMessage = "Ошибка при создании запроса серий товара: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            callback.onError(new Exception(errorMessage, e));
        }
    }
    
    /**
     * Обрабатывает DTO ответа серий товара, проверяет поле "Результат" и выполняет соответствующие действия
     */
    private void processProductSeriesResponseDto(ProductSeriesResponseDto dto, DataSourceCallback<List<SeriesItem>> callback) {
        // Проверяем поле "Результат"
        if (!dto.isResult()) {
            // Если результат false, возвращаем серверную ошибку с текстом из "ТекстОшибки"
            String errorText = dto.getErrorText();
            if (errorText == null || errorText.trim().isEmpty()) {
                errorText = "Произошла ошибка при получении серий товара";
            }
            Log.e(TAG, "ProductSeries: Результат false, ТекстОшибки: " + errorText);
            callback.onError(new com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException(errorText, com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException.ErrorType.PRODUCT_SERIES));
            return;
        }
        
        // Если результат true, обрабатываем как обычно
        try {
            List<ProductSeriesDto> dtoList = dto.getData();
            if (dtoList != null) {
                Log.d(TAG, "Получено серий: " + dtoList.size());
                
                // Маппим DTO в доменные модели
                List<SeriesItem> seriesItems = ProductSeriesMapper.mapToDomainList(dtoList);
                
                Log.d(TAG, "Успешно получены серии товара, количество: " + seriesItems.size());
                callback.onSuccess(seriesItems);
            } else {
                Log.d(TAG, "Получен пустой список серий");
                callback.onSuccess(new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            Log.e(TAG, "ProductSeries: Ошибка при маппинге данных: " + e.getMessage());
            callback.onError(new Exception("Ошибка при обработке данных серий товара: " + e.getMessage(), e));
        }
    }
} 