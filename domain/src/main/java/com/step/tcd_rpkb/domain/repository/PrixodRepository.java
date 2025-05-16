package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Invoice; // Предполагаем, что модель Invoice будет в domain.model
import com.step.tcd_rpkb.domain.model.Product; // Предполагаем, что модель Product будет в domain.model

import java.util.List;

public interface PrixodRepository {
    /**
     * Получает документ прихода по его UUID.
     *
     * @param moveUuid UUID документа.
     * @param callback Callback для получения результата.
     */
    void getPrixodDocument(String moveUuid, RepositoryCallback<Invoice> callback);

    // Можно добавить другие методы, например, для сохранения данных прихода
    // void savePrixodDocument(Invoice invoice, RepositoryCallback<Void> callback);
} 