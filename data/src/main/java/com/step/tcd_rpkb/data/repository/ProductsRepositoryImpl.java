package com.step.tcd_rpkb.data.repository;


import com.step.tcd_rpkb.data.datasources.LocalRealmDataSource;
import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.ProductsRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.MoveRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductsRepositoryImpl implements ProductsRepository {


    private final MoveRepository moveRepository;
    private final LocalRealmDataSource localDataSource;


    @Inject

    public ProductsRepositoryImpl(MoveRepository moveRepository, LocalRealmDataSource localDataSource, InvoiceMapper invoiceMapper) {
        this.moveRepository = moveRepository;
        this.localDataSource = localDataSource;
    }



    @Override
    public void saveProduct(String moveUuid, Product product, RepositoryCallback<Boolean> callback) {
        localDataSource.saveProduct(moveUuid, product);
        callback.onSuccess(true);
    }

    @Override
    public void saveProducts(String moveUuid, List<Product> products, RepositoryCallback<Boolean> callback) {
        localDataSource.saveProducts(moveUuid, products);
        callback.onSuccess(true);
    }

    @Override
    public void loadProducts(String moveUuid, RepositoryCallback<List<Product>> callback) {
        List<Product> products = localDataSource.loadProducts(moveUuid);
        callback.onSuccess(products);
    }

    @Override
    public void deleteProduct(String moveUuid, String productLineId, RepositoryCallback<Boolean> callback) {
        localDataSource.deleteProduct(moveUuid, productLineId);
        callback.onSuccess(true);
    }

    @Override
    public void getPrixodDocument(String moveUuid, RepositoryCallback<Invoice> callback) {

        moveRepository.getDocumentMove(moveUuid, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                if (invoice != null) {
                    callback.onSuccess(invoice);
                } else {
                    callback.onError(new Exception("MoveRepository вернул null Invoice для UUID: " + moveUuid));
                }
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }
} 

