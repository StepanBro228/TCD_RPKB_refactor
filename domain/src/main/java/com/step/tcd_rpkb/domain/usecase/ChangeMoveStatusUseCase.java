package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import java.util.List;

import javax.inject.Inject;

public class ChangeMoveStatusUseCase {
    private final MoveRepository moveRepository;
    private final SaveMoveDataBeforeStatusChangeUseCase saveMoveDataUseCase;

    @Inject
    public ChangeMoveStatusUseCase(MoveRepository moveRepository, 
                                   SaveMoveDataBeforeStatusChangeUseCase saveMoveDataUseCase) {
        this.moveRepository = moveRepository;
        this.saveMoveDataUseCase = saveMoveDataUseCase;
    }

    /**
     * Выполняет смену статуса перемещения без предварительного сохранения данных
     * (для случаев когда данные уже сохранены или не требуют сохранения)
     */
    public void execute(String guid, String targetState, String userGuid, RepositoryCallback<ChangeMoveStatusResult> callback) {
        moveRepository.changeMoveStatus(guid, targetState, userGuid, callback);
    }
    
    /**
     * Выполняет сохранение данных в 1С, а затем смену статуса перемещения
     * @param guid GUID перемещения
     * @param targetState Целевой статус
     * @param userGuid GUID пользователя
     * @param products Список продуктов для сохранения в 1С (может быть null/пустым)
     * @param callback Callback для получения результата
     */
    public void executeWithDataSave(String guid, String targetState, String userGuid, 
                                   List<Product> products, RepositoryCallback<ChangeMoveStatusResult> callback) {
        
        // Сначала сохраняем данные в 1С
        saveMoveDataUseCase.execute(guid, userGuid, products, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean saveSuccess) {
                if (saveSuccess) {
                    // Если сохранение прошло успешно, меняем статус
                    moveRepository.changeMoveStatus(guid, targetState, userGuid, callback);
                } else {
                    callback.onError(new Exception("Не удалось сохранить данные в 1С"));
                }
            }
            
            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }
} 