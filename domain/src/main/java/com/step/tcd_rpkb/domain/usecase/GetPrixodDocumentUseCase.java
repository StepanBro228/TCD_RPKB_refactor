package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.repository.PrixodRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import javax.inject.Inject;

public class GetPrixodDocumentUseCase {

    private final PrixodRepository prixodRepository;

    @Inject
    public GetPrixodDocumentUseCase(PrixodRepository prixodRepository) {
        this.prixodRepository = prixodRepository;
    }

    public void execute(String moveUuid, RepositoryCallback<Invoice> callback) {
        if (moveUuid == null || moveUuid.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("ID документа прихода не может быть пустым."));
            }
            return;
        }
        prixodRepository.getPrixodDocument(moveUuid, callback);
    }
} 