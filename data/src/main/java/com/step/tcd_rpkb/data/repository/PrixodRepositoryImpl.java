package com.step.tcd_rpkb.data.repository;


import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
// import com.step.tcd_rpkb.network.DataProvider; // УДАЛЯЕМ ЗАВИСИМОСТЬ
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.repository.PrixodRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.MoveRepository; // <-- ДОБАВЛЯЕМ ЗАВИСИМОСТЬ

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrixodRepositoryImpl implements PrixodRepository {


    private final MoveRepository moveRepository;


    @Inject

    public PrixodRepositoryImpl(MoveRepository moveRepository, InvoiceMapper invoiceMapper) {
        this.moveRepository = moveRepository;
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