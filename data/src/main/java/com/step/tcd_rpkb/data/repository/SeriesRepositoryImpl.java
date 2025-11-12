package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.step.tcd_rpkb.data.datasources.DataSourceCallback;
import com.step.tcd_rpkb.data.datasources.RemoteSeriesDataSource;
import com.step.tcd_rpkb.data.mapper.ProductSeriesMapper;
import com.step.tcd_rpkb.data.network.dto.ProductSeriesDto;
import com.step.tcd_rpkb.domain.model.SeriesItem;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.SeriesRepository;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import com.step.tcd_rpkb.domain.util.ConnectivityChecker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Реализация SeriesRepository для работы с онлайн/оффлайн данными о сериях
 */
@Singleton
public class SeriesRepositoryImpl implements SeriesRepository {

    private static final String TAG = "SeriesRepositoryImpl";
    
    private final Context context;
    private final Gson gson;
    private final UserSettingsRepository userSettingsRepository;
    private final ConnectivityChecker connectivityChecker;
    private final RemoteSeriesDataSource remoteSeriesDataSource;
    
    @Inject
    public SeriesRepositoryImpl(@ApplicationContext Context context, 
                               Gson gson,
                               UserSettingsRepository userSettingsRepository,
                               ConnectivityChecker connectivityChecker,
                               RemoteSeriesDataSource remoteSeriesDataSource) {
        this.context = context;
        this.gson = gson;
        this.userSettingsRepository = userSettingsRepository;
        this.connectivityChecker = connectivityChecker;
        this.remoteSeriesDataSource = remoteSeriesDataSource;
    }

    @Override
    public void getSeriesForNomenclature(String nomenclatureUuid, String moveUuid, String productLineId, RepositoryCallback<List<SeriesItem>> callback) {
        boolean onlineMode = userSettingsRepository.isOnlineMode();
        boolean networkAvailable = connectivityChecker.isNetworkAvailable();
        
        Log.d(TAG, "Запрос серий: nomenclatureUuid=" + nomenclatureUuid + 
                   ", moveUuid=" + moveUuid + 
                   ", productLineId=" + productLineId + 
                   ", onlineMode=" + onlineMode + 
                   ", networkAvailable=" + networkAvailable);

        if (onlineMode && networkAvailable) {
            // Онлайн режим: получаем данные через API
            Log.d(TAG, "Получение серий в онлайн режиме через API ProductSeries");
            
            // Используем переданный productLineId для API запроса
            String lineGuid = productLineId != null ? productLineId : null;
            
            remoteSeriesDataSource.getProductSeries(moveUuid, lineGuid, new DataSourceCallback<List<SeriesItem>>() {
                @Override
                public void onSuccess(List<SeriesItem> data) {
                    Log.d(TAG, "Успешно получены серии из API: " + data.size() + " элементов");
                    callback.onSuccess(data);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Ошибка при получении серий из API: " + exception.getMessage());
                    callback.onError(exception);
                }
            });
            
        } else {
            // Оффлайн режим: используем локальные данные
            Log.d(TAG, "Получение серий в оффлайн режиме из assets");
            loadOfflineSeriesData(nomenclatureUuid, moveUuid, productLineId, callback);
        }
    }

    /**
     * Загружает данные серий из локального JSON файла
     */
    private void loadOfflineSeriesData(String nomenclatureUuid, String moveUuid, String productLineId, RepositoryCallback<List<SeriesItem>> callback) {
        Log.d(TAG, "Загрузка оффлайн данных для nomenclatureUuid=" + nomenclatureUuid + ", moveUuid=" + moveUuid);
        
        // Имитируем асинхронную загрузку данных
        new Thread(() -> {
            try {
                Thread.sleep(500);
                

                String jsonString = loadJSONFromAsset("series_data.json");
                if (jsonString == null) {
                    invokeCallbackOnMainThread(callback, null, new Exception("Не удалось загрузить данные о сериях из assets"));
                    return;
                }
                
                try {
                    Type seriesType = new TypeToken<List<ProductSeriesDto>>(){}.getType();
                    List<ProductSeriesDto> seriesDtoList = gson.fromJson(jsonString, seriesType);
                    
                    if (seriesDtoList == null) {
                        seriesDtoList = new ArrayList<>();
                    }

                    List<SeriesItem> seriesList = ProductSeriesMapper.mapToDomainList(seriesDtoList);
                    
                    Log.d(TAG, "Загружено серий из assets: " + seriesList.size());

                    invokeCallbackOnMainThread(callback, seriesList, null);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при парсинге JSON: " + e.getMessage(), e);
                    invokeCallbackOnMainThread(callback, null, new Exception("Ошибка при обработке данных о сериях: " + e.getMessage()));
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Операция прервана: " + e.getMessage());
                invokeCallbackOnMainThread(callback, null, new Exception("Операция прервана"));
            }
        }).start();
    }

    @Override
    public void saveSeriesAllocation(String nomenclatureUuid, String moveUuid, List<SeriesItem> seriesItems, RepositoryCallback<Boolean> callback) {
        boolean onlineMode = userSettingsRepository.isOnlineMode();
        boolean networkAvailable = connectivityChecker.isNetworkAvailable();
        
        Log.d(TAG, "Сохранение распределения серий: nomenclatureUuid=" + nomenclatureUuid + 
                   ", moveUuid=" + moveUuid + 
                   ", onlineMode=" + onlineMode + 
                   ", networkAvailable=" + networkAvailable);

        if (onlineMode && networkAvailable) {
            Log.d(TAG, "Сохранение серий в онлайн режиме (пока не реализовано - используется заглушка)");
            saveOfflineSeriesAllocation(nomenclatureUuid, moveUuid, seriesItems, callback);
            
        } else {
            // Оффлайн режим: используем заглушку
            Log.d(TAG, "Сохранение серий в оффлайн режиме");
            saveOfflineSeriesAllocation(nomenclatureUuid, moveUuid, seriesItems, callback);
        }
    }
    
    /**
     * Заглушка для сохранения распределения серий в оффлайн режиме
     */
    private void saveOfflineSeriesAllocation(String nomenclatureUuid, String moveUuid, List<SeriesItem> seriesItems, RepositoryCallback<Boolean> callback) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                
                Log.d(TAG, "=== СОХРАНЕНИЕ РАСПРЕДЕЛЕНИЯ СЕРИЙ ===");
                Log.d(TAG, "nomenclatureUuid: " + nomenclatureUuid);
                Log.d(TAG, "moveUuid: " + moveUuid);
                Log.d(TAG, "Количество серий: " + seriesItems.size());
                
                // Создаем JSON представление данных для отправки
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                        .disableHtmlEscaping()
                        .create();
                String seriesJson = gson.toJson(seriesItems);
                
                Log.d(TAG, "Размер JSON: " + seriesJson.length() + " символов");
                
                // Логируем детальную информацию о каждой серии
                for (int i = 0; i < seriesItems.size(); i++) {
                    SeriesItem item = seriesItems.get(i);
                    Log.d(TAG, "Серия #" + (i + 1) + ": " +
                              "UUID=" + item.getSeriesUuid() +
                              ", название=" + item.getSeriesName() +
                              ", свободный остаток=" + item.getFreeBalance() +
                              ", резерв другими=" + item.getReservedByOthers() +
                              ", количество в документе=" + item.getDocumentQuantity() +
                              ", распределено=" + item.getAllocatedQuantity() +
                              ", максимально доступно=" + item.getMaxAllowedAllocation());
                }
                
                // Логируем полный JSON
                if (seriesJson.length() < 3000) {
                    Log.d(TAG, "Полный JSON для сохранения серий: " + seriesJson);
                } else {
                    Log.d(TAG, "JSON серий (первые 1000 символов): " + seriesJson.substring(0, 1000) + "...");
                    Log.d(TAG, "JSON серий (последние 500 символов): ..." + seriesJson.substring(seriesJson.length() - 500));
                }
                
                Log.d(TAG, "Имитация успешного сохранения распределения серий");
                Log.d(TAG, "=== КОНЕЦ СОХРАНЕНИЯ РАСПРЕДЕЛЕНИЯ СЕРИЙ ===");
                
                // Возвращаем успех
                invokeCallbackOnMainThread(callback, true, null);
            } catch (InterruptedException e) {
                Log.e(TAG, "Операция сохранения серий прервана", e);
                invokeCallbackOnMainThread(callback, false, new Exception("Операция прервана"));
            }
        }).start();
    }
    
    /**
     * Вспомогательный метод для чтения JSON из assets
     */
    private String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e(TAG, "Ошибка при чтении JSON из assets: " + ex.getMessage());
            return null;
        }
        return json;
    }
    
    /**
     * Вспомогательный метод для выполнения callback в главном потоке
     */
    private <T> void invokeCallbackOnMainThread(RepositoryCallback<T> callback, T result, Exception error) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onSuccess(result);
            }
        });
    }
} 