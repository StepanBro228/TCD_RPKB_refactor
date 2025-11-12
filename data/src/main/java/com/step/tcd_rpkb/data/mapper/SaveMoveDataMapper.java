package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.SaveMoveDataItemDto;
import com.step.tcd_rpkb.data.network.dto.SaveMoveDataRequestDto;
import com.step.tcd_rpkb.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Mapper для преобразования данных при сохранении перемещения в 1С
 */
@Singleton
public class SaveMoveDataMapper {
    
    @Inject
    public SaveMoveDataMapper() {}
    
    /**
     * Преобразует Product в SaveMoveDataItemDto
     */
    public SaveMoveDataItemDto mapToSaveItem(Product product) {
        if (product == null) {
            return null;
        }
        
        return new SaveMoveDataItemDto(
            product.getProductLineId(),
            product.getParentProductLineId(),
            product.getNomenclatureUuid(),
            product.getSeriesUuid(),
            product.getReserveDocumentUuid(),
            product.getExists(),
            product.getQuantity()
        );
    }
    
    /**
     * Преобразует список Product в список SaveMoveDataItemDto
     */
    public List<SaveMoveDataItemDto> mapToSaveItemList(List<Product> products) {
        if (products == null) {
            return new ArrayList<>();
        }
        
        List<SaveMoveDataItemDto> saveItems = new ArrayList<>();
        for (Product product : products) {
            SaveMoveDataItemDto saveItem = mapToSaveItem(product);
            if (saveItem != null) {
                saveItems.add(saveItem);
            }
        }
        return saveItems;
    }
    
    /**
     * Создает полный запрос для сохранения данных перемещения
     */
    public SaveMoveDataRequestDto createSaveRequest(String moveGuid, String userGuid, List<Product> products) {
        List<SaveMoveDataItemDto> saveItems = mapToSaveItemList(products);
        return new SaveMoveDataRequestDto(moveGuid, userGuid, saveItems);
    }
} 