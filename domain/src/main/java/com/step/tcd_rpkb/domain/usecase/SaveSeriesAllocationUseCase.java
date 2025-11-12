package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.SeriesItem;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.SeriesRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * UseCase для сохранения распределения серий товара
 */
public class SaveSeriesAllocationUseCase {

    private final SeriesRepository seriesRepository;

    @Inject
    public SaveSeriesAllocationUseCase(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    /**
     * Выполняет UseCase для сохранения распределения серий товара
     * 
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     * @param seriesItems Список серий с распределенными количествами
     * @param callback Callback для получения результата операции
     */
    public void execute(String nomenclatureUuid, String moveUuid, List<SeriesItem> seriesItems, RepositoryCallback<Boolean> callback) {
        seriesRepository.saveSeriesAllocation(nomenclatureUuid, moveUuid, seriesItems, callback);
    }
} 