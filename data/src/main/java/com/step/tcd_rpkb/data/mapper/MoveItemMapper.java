package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.MoveItemDto;
import com.step.tcd_rpkb.domain.model.MoveItem;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class MoveItemMapper {

    @Inject
    public MoveItemMapper() {}

    public MoveItem mapToDomain(MoveItemDto dto) {
        if (dto == null) return null;
        return new MoveItem(
                dto.getMovementId(),
                dto.getMovementDisplayText(),
                dto.isCps(),
                dto.getDate(),
                dto.getNumber(),
                dto.getComment(),
                dto.getProductName(),
                dto.getResponsiblePersonName(),
                dto.getColor(),
                dto.getPriority(),
                dto.getAssemblerName(),
                dto.getSigningStatus(),
                dto.getSourceWarehouseName(),
                dto.getDestinationWarehouseName(),
                dto.getItemsCount(),
                dto.getPositionsCount()
        );
    }

    public List<MoveItem> mapToDomainList(List<MoveItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        List<MoveItem> domainList = new ArrayList<>();
        for (MoveItemDto dto : dtoList) {
            domainList.add(mapToDomain(dto));
        }
        return domainList;
    }

    public MoveItemDto mapToDto(MoveItem domain) {
        if (domain == null) return null;
        MoveItemDto dto = new MoveItemDto();
        dto.setMovementId(domain.getMovementId());
        dto.setMovementDisplayText(domain.getMovementDisplayText());
        dto.setCps(domain.isCps());
        dto.setDate(domain.getDate());
        dto.setNumber(domain.getNumber());
        dto.setComment(domain.getComment());
        dto.setProductName(domain.getProductName());
        dto.setResponsiblePersonName(domain.getResponsiblePersonName());
        dto.setColor(domain.getColor());
        dto.setPriority(domain.getPriority());
        dto.setAssemblerName(domain.getAssemblerName());
        dto.setSigningStatus(domain.getSigningStatus());
        dto.setSourceWarehouseName(domain.getSourceWarehouseName());
        dto.setDestinationWarehouseName(domain.getDestinationWarehouseName());
        dto.setItemsCount(domain.getItemsCount());
        dto.setPositionsCount(domain.getPositionsCount());
        return dto;
    }

    public List<MoveItemDto> mapToDtoList(List<MoveItem> domainList) {
        if (domainList == null) return new ArrayList<>();
        List<MoveItemDto> dtoList = new ArrayList<>();
        for (MoveItem domain : domainList) {
            dtoList.add(mapToDto(domain));
        }
        return dtoList;
    }
} 