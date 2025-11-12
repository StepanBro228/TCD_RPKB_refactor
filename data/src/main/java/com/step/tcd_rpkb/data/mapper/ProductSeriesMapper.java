package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.ProductSeriesDto;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Маппер для преобразования ProductSeriesDto в SeriesItem
 */
public class ProductSeriesMapper {
    
    /**
     * Преобразует DTO в доменную модель
     */
    public static SeriesItem mapToDomain(ProductSeriesDto dto) {
        if (dto == null) {
            return null;
        }
        
        return new SeriesItem(
            dto.getSeriesGuid(),                // UUID серии (СерияГУИД) 
            dto.getSeriesNomenclatureUuid(),    // Номер серии (СерияНоменклатуры)
            dto.getExpiryDate(),                // Срок годности
            dto.getFreeBalanceBySeries(),       // Свободный остаток по серии
            dto.getReservedByOthers(),          // Резерв других ЗНП
            dto.getDocumentQuantity()           // Количество документа
        );
    }
    
    /**
     * Преобразует список DTO в список доменных моделей
     */
    public static List<SeriesItem> mapToDomainList(List<ProductSeriesDto> dtoList) {
        if (dtoList == null) {
            return new ArrayList<>();
        }
        
        List<SeriesItem> seriesItems = new ArrayList<>();
        for (ProductSeriesDto dto : dtoList) {
            SeriesItem item = mapToDomain(dto);
            if (item != null) {
                seriesItems.add(item);
            }
        }
        
        return seriesItems;
    }
} 