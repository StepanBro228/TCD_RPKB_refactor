package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.ZnpOrderDto;
import com.step.tcd_rpkb.data.network.dto.ZnpSeriesDataDto;
import com.step.tcd_rpkb.domain.model.ZnpOrderItem;
import com.step.tcd_rpkb.domain.model.ZnpSeriesData;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper для преобразования DTO в domain модели
 */
public class ZnpDataMapper {
    
    /**
     * Преобразует DTO заказа в domain модель
     */
    public static ZnpOrderItem mapToDomain(ZnpOrderDto dto) {
        if (dto == null) {
            return null;
        }
        
        return new ZnpOrderItem(
                dto.getZnpNumber(),
                dto.getFromDate(),
                dto.getQuantityToProcure(),
                dto.getReserve(),
                dto.getReserveByOthers()
        );
    }
    
    /**
     * Преобразует список DTO заказов в domain модели
     */
    public static List<ZnpOrderItem> mapToDomainList(List<ZnpOrderDto> dtoList) {
        if (dtoList == null) {
            return new ArrayList<>();
        }
        
        List<ZnpOrderItem> domainList = new ArrayList<>();
        for (ZnpOrderDto dto : dtoList) {
            ZnpOrderItem domainItem = mapToDomain(dto);
            if (domainItem != null) {
                domainList.add(domainItem);
            }
        }
        return domainList;
    }
    
    /**
     * Преобразует DTO данных серии в domain модель
     */
    public static ZnpSeriesData mapToDomain(ZnpSeriesDataDto dto) {
        if (dto == null) {
            return null;
        }
        
        List<ZnpOrderItem> znpOrders = mapToDomainList(dto.getZnpOrders());
        
        return new ZnpSeriesData(
                dto.getSeriesUuid(),
                dto.getWarehouse(),
                dto.getUnitOfMeasurement(),
                dto.getFreeBalance(),
                znpOrders
        );
    }
    
    /**
     * Преобразует domain модель заказа в DTO
     */
    public static ZnpOrderDto mapToDto(ZnpOrderItem domain) {
        if (domain == null) {
            return null;
        }
        
        ZnpOrderDto dto = new ZnpOrderDto();
        dto.setZnpNumber(domain.getZnpNumber());
        dto.setFromDate(domain.getFromDate());
        dto.setQuantityToProcure(domain.getQuantityToProcure());
        dto.setReserve(domain.getReserve());
        dto.setReserveByOthers(domain.getReserveByOthers());
        return dto;
    }
    
    /**
     * Преобразует список domain моделей в DTO
     */
    public static List<ZnpOrderDto> mapToDtoList(List<ZnpOrderItem> domainList) {
        if (domainList == null) {
            return new ArrayList<>();
        }
        
        List<ZnpOrderDto> dtoList = new ArrayList<>();
        for (ZnpOrderItem domain : domainList) {
            ZnpOrderDto dto = mapToDto(domain);
            if (dto != null) {
                dtoList.add(dto);
            }
        }
        return dtoList;
    }
    
    /**
     * Преобразует domain модель данных серии в DTO
     */
    public static ZnpSeriesDataDto mapToDto(ZnpSeriesData domain) {
        if (domain == null) {
            return null;
        }
        
        List<ZnpOrderDto> znpOrders = mapToDtoList(domain.getZnpOrders());
        
        ZnpSeriesDataDto dto = new ZnpSeriesDataDto();
        dto.setSeriesUuid(domain.getSeriesUuid());
        dto.setWarehouse(domain.getWarehouse());
        dto.setUnitOfMeasurement(domain.getUnitOfMeasurement());
        dto.setFreeBalance(domain.getFreeBalance());
        dto.setZnpOrders(znpOrders);
        return dto;
    }
} 