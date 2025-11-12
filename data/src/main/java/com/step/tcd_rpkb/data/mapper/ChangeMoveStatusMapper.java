package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.ChangeMoveStatusResponseDto;
import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import java.util.List;

public class ChangeMoveStatusMapper {
    public ChangeMoveStatusResult mapToDomain(ChangeMoveStatusResponseDto dto) {
        if (dto == null) return null;
        return new ChangeMoveStatusResult(
            dto.getErrorText(),
            dto.isResult(),
            dto.getStatus(),
            dto.getUser(),
            dto.getData(),
            dto.getMoveGuid()
        );
    }
} 