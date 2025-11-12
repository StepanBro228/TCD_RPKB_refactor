package com.step.tcd_rpkb.data.datasources;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.step.tcd_rpkb.data.network.dto.ZnpSeriesDataDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

/**
 * Локальный источник данных ЗНП из assets файлов
 */
public class ZnpLocalDataSource {
    
    private static final String TAG = "ZnpLocalDataSource";
    
    private final Context context;
    private final Gson gson;
    
    @Inject
    public ZnpLocalDataSource(Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }
    
    /**
     * Получает данные ЗНП для серии из соответствующего JSON файла
     * 
     * @param seriesUuid UUID серии
     * @return ZnpSeriesDataDto или null если данные не найдены
     */
    public ZnpSeriesDataDto getZnpDataForSeries(String seriesUuid) {
        try {
            String fileName = getFileNameForSeries(seriesUuid);
            if (fileName == null) {
                Log.w(TAG, "Не найден файл для серии: " + seriesUuid);
                return null;
            }
            
            String jsonData = loadJsonFromAssets(fileName);
            if (jsonData == null || jsonData.isEmpty()) {
                Log.w(TAG, "Не удалось загрузить данные из файла: " + fileName);
                return null;
            }
            
            ZnpSeriesDataDto znpData = gson.fromJson(jsonData, ZnpSeriesDataDto.class);
            Log.d(TAG, "Успешно загружены данные ЗНП для серии: " + seriesUuid);
            return znpData;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке данных ЗНП для серии " + seriesUuid, e);
            return null;
        }
    }
    
    /**
     * Определяет имя файла на основе UUID серии
     * В реальном приложении это может быть более сложная логика
     */
    private String getFileNameForSeries(String seriesUuid) {
        if (seriesUuid == null) {
            return null;
        }
        
        // Соответствие UUID серий из обновленных JSON файлов
        switch (seriesUuid) {
            case "a86a2db2-86dd-11e6-80f6-00155d00160d": // Серия 00828140624
                return "znp_data_series1.json";
            case "a86a2db2-86dd-11e6-80f6-00155d00160l": // Серия 00828140694  
                return "znp_data_series2.json";
            case "b77a9cd4-90ef-11e6-80fe-00155d00160d": // Серия 00828140712
                return "znp_data_series3.json";
            default:
                // Для неизвестных серий возвращаем null, чтобы показать сообщение об отсутствии данных
                Log.w(TAG, "Неизвестная серия с UUID: " + seriesUuid);
                return null;
        }
    }
    
    /**
     * Загружает JSON данные из assets
     */
    private String loadJsonFromAssets(String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при чтении файла " + fileName, e);
            return null;
        }
    }
    
    /**
     * Сохраняет данные ЗНП (заглушка для демонстрации)
     * В реальном приложении здесь будет вызов API или сохранение в БД
     */
    public boolean saveZnpReservation(String seriesUuid, ZnpSeriesDataDto znpData) {
        Log.d(TAG, "Сохранение резервирования ЗНП для серии: " + seriesUuid);
        Log.d(TAG, "Данные: " + gson.toJson(znpData));
        
        // Имитация успешного сохранения
        return true;
    }
} 