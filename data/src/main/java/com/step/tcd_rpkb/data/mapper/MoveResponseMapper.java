package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.MoveItemDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.data.mapper.MoveItemMapper;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class MoveResponseMapper {

    private final MoveItemMapper moveItemMapper;

    @Inject
    public MoveResponseMapper(MoveItemMapper moveItemMapper) {
        this.moveItemMapper = moveItemMapper;
    }

    public MoveResponse mapToDomain(MoveResponseDto dto) {
        if (dto == null) return null;
        return new MoveResponse(
                dto.getErrorText(),
                dto.isResult(),
                dto.getState(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getStatusList(),
                moveItemMapper.mapToDomainList(dto.getItems())
        );
    }

    public List<MoveResponse> mapToDomainList(List<MoveResponseDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        List<MoveResponse> domainList = new ArrayList<>();
        for (MoveResponseDto dto : dtoList) {
            domainList.add(mapToDomain(dto));
        }
        return domainList;
    }

    public MoveResponseDto mapToDto(MoveResponse domain) {
        if (domain == null) return null;
        MoveResponseDto dto = new MoveResponseDto();
        dto.setErrorText(domain.getErrorText());
        dto.setResult(domain.isResult());
        dto.setState(domain.getState());
        dto.setStartDate(domain.getStartDate());
        dto.setEndDate(domain.getEndDate());
        dto.setStatusList(domain.getStatusList());
        dto.setItems(moveItemMapper.mapToDtoList(domain.getItems()));
        return dto;
    }

    public List<MoveResponseDto> mapToDtoList(List<MoveResponse> domainList) {
        if (domainList == null) return new ArrayList<>();
        List<MoveResponseDto> dtoList = new ArrayList<>();
        for (MoveResponse domain : domainList) {
            dtoList.add(mapToDto(domain));
        }
        return dtoList;
    }
} 