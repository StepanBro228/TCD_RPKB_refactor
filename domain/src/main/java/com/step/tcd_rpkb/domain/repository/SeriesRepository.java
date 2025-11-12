package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.SeriesItem;
import java.util.List;

/**
 * Репозиторий для работы с данными о сериях товаров
 */
public interface SeriesRepository {
    
    /**
     * Получает список серий для конкретного товара в перемещении
     * 
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     * @param productLineId УИД строки товара (для онлайн режима)
     * @param callback Callback для получения результата
     */
    void getSeriesForNomenclature(String nomenclatureUuid, String moveUuid, String productLineId, RepositoryCallback<List<SeriesItem>> callback);
    
    /**
     * Сохраняет распределение серий для товара в перемещении
     * 
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     * @param seriesItems Список распределенных серий
     * @param callback Callback для получения результата операции
     */
    void saveSeriesAllocation(String nomenclatureUuid, String moveUuid, List<SeriesItem> seriesItems, RepositoryCallback<Boolean> callback);
} 