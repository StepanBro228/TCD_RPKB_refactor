package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Invoice; // Предполагаем, что модель Invoice будет в domain.model
import com.step.tcd_rpkb.domain.model.Product;

import java.util.List;


public interface ProductsRepository {
    /**
     * Получает документ прихода по его UUID.
     *
     * @param moveUuid UUID документа.
     * @param callback Callback для получения результата.
     */
    void getPrixodDocument(String moveUuid, RepositoryCallback<Invoice> callback);
    void saveProduct(String moveUuid, Product product, RepositoryCallback<Boolean> callback);

    void saveProducts(String moveUuid, List<Product> products, RepositoryCallback<Boolean> callback);

    void loadProducts(String moveUuid, RepositoryCallback<List<Product>> callback);

    void deleteProduct(String moveUuid, String productLineId, RepositoryCallback<Boolean> callback);
}

