package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.SeriesItem;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.SeriesRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * UseCase для получения серий товара
 */
public class GetSeriesForNomenclatureUseCase {

    private final SeriesRepository seriesRepository;

    @Inject
    public GetSeriesForNomenclatureUseCase(SeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    /**
     * Выполняет UseCase для получения серий товара
     * 
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     * @param productLineId УИД строки товара (для онлайн режима)
     * @param callback Callback для получения результата
     */
    public void execute(String nomenclatureUuid, String moveUuid, String productLineId, RepositoryCallback<List<SeriesItem>> callback) {
        seriesRepository.getSeriesForNomenclature(nomenclatureUuid, moveUuid, productLineId, callback);
    }
} 