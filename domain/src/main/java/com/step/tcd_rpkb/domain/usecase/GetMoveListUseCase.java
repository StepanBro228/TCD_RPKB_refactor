package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import javax.inject.Inject;

public class GetMoveListUseCase {

    private final MoveRepository moveRepository;

    @Inject
    public GetMoveListUseCase(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    public void execute(String state, String startDate, String endDate, RepositoryCallback<MoveResponse> callback) {
        // Дополнительная бизнес-логика, если нужна
        // ...
        moveRepository.getMoveList(state, startDate, endDate, callback);
    }
} 