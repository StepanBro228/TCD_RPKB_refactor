package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * UseCase для сохранения данных перемещения в 1С перед сменой статуса
 */
@Singleton
public class SaveMoveDataBeforeStatusChangeUseCase {
    
    private final MoveRepository moveRepository;
    
    @Inject
    public SaveMoveDataBeforeStatusChangeUseCase(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }
    
    /**
     * Сохраняет данные перемещения в 1С
     * @param moveGuid GUID перемещения
     * @param userGuid GUID пользователя
     * @param products Список продуктов для сохранения
     * @param callback Callback для получения результата
     */
    public void execute(String moveGuid, String userGuid, List<Product> products, RepositoryCallback<Boolean> callback) {
        moveRepository.saveMoveData(moveGuid, userGuid, products, callback);
    }
} 