package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.ZnpSeriesData;

/**
 * Репозиторий для работы с данными ЗНП
 */
public interface ZnpRepository {
    
    /**
     * Получает данные ЗНП для конкретной серии
     * 
     * @param seriesUuid UUID серии
     * @param callback Callback для получения результата
     */
    void getZnpDataForSeries(String seriesUuid, RepositoryCallback<ZnpSeriesData> callback);
    
    /**
     * Сохраняет резервирование ЗНП для серии
     * 
     * @param seriesUuid UUID серии
     * @param znpSeriesData Данные с резервированием
     * @param callback Callback для получения результата операции
     */
    void saveZnpReservation(String seriesUuid, ZnpSeriesData znpSeriesData, RepositoryCallback<Boolean> callback);
} 