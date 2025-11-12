package com.step.tcd_rpkb.data.mapper;


import com.step.tcd_rpkb.data.network.dto.ProductDto; // Добавляем импорт ProductDto
import com.step.tcd_rpkb.domain.model.Product;    // Доменная модель продукта

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ProductMapper {

    @Inject
    public ProductMapper() {}

    public Product mapToDomain(ProductDto dtoProduct) { // Изменяем тип параметра на ProductDto
        if (dtoProduct == null) {
            return null;
        }

        return new Product(
                dtoProduct.getProductLineId(),
                dtoProduct.getParentProductLineId(),
                dtoProduct.getNomenclatureUuid(),
                dtoProduct.getNomenclatureName(),
                dtoProduct.getRequestedUuid(),
                dtoProduct.getRequestedName(),
                dtoProduct.getSeriesName(),
                dtoProduct.getSeriesUuid(),
                dtoProduct.getQuantity(),
                dtoProduct.getUnitName(),
                dtoProduct.getUnitUuid(),
                dtoProduct.getSenderStorageName(),
                dtoProduct.getSenderStorageUuid(),
                dtoProduct.getReceiverStorageName(),
                dtoProduct.getReceiverStorageUuid(),
                dtoProduct.getResponsibleReceiverName(),
                dtoProduct.getResponsibleReceiverUuid(),
                dtoProduct.getReserveDocumentName(),
                dtoProduct.getReserveDocumentUuid(),
                dtoProduct.getFreeBalanceInCell(),
                dtoProduct.getFreeBalanceBySeries(),
                dtoProduct.getFreeBalance(),
                dtoProduct.getTotalBalance(),
                dtoProduct.getTaken(),
                dtoProduct.getExists()
        );
    }

    public ProductDto mapToDto(Product domainProduct) {
        if (domainProduct == null) {
            return null;
        }
        
        // логирование
        android.util.Log.d("ProductMapper", "mapToDto: Маппинг продукта " + domainProduct.getProductLineId() + 
                          ", nomenclature=" + domainProduct.getNomenclatureName() + 
                          ", quantity=" + domainProduct.getQuantity() + 
                          ", taken=" + domainProduct.getTaken());
        

        ProductDto dtoProduct = new ProductDto();
        dtoProduct.setProductLineId(domainProduct.getProductLineId());
        dtoProduct.setParentProductLineId(domainProduct.getParentProductLineId());
        dtoProduct.setNomenclatureUuid(domainProduct.getNomenclatureUuid());
        dtoProduct.setNomenclatureName(domainProduct.getNomenclatureName());
        dtoProduct.setRequestedUuid(domainProduct.getRequestedUuid());
        dtoProduct.setRequestedName(domainProduct.getRequestedName());
        dtoProduct.setSeriesName(domainProduct.getSeriesName());
        dtoProduct.setSeriesUuid(domainProduct.getSeriesUuid());
        dtoProduct.setQuantity(domainProduct.getQuantity());
        dtoProduct.setUnitName(domainProduct.getUnitName());
        dtoProduct.setUnitUuid(domainProduct.getUnitUuid());
        dtoProduct.setSenderStorageName(domainProduct.getSenderStorageName());
        dtoProduct.setSenderStorageUuid(domainProduct.getSenderStorageUuid());
        dtoProduct.setReceiverStorageName(domainProduct.getReceiverStorageName());
        dtoProduct.setReceiverStorageUuid(domainProduct.getReceiverStorageUuid());
        dtoProduct.setResponsibleReceiverName(domainProduct.getResponsibleReceiverName());
        dtoProduct.setResponsibleReceiverUuid(domainProduct.getResponsibleReceiverUuid());
        dtoProduct.setReserveDocumentName(domainProduct.getReserveDocumentName());
        dtoProduct.setReserveDocumentUuid(domainProduct.getReserveDocumentUuid());
        dtoProduct.setFreeBalanceInCell(domainProduct.getFreeBalanceInCell());
        dtoProduct.setFreeBalanceBySeries(domainProduct.getFreeBalanceBySeries());
        dtoProduct.setFreeBalance(domainProduct.getFreeBalance());
        dtoProduct.setTotalBalance(domainProduct.getTotalBalance());
        dtoProduct.setTaken(domainProduct.getTaken());
        dtoProduct.setExists(domainProduct.getExists());
        
        // Логирование
        android.util.Log.d("ProductMapper", "mapToDto РЕЗУЛЬТАТ: DTO " + dtoProduct.getProductLineId() + 
                          ", nomenclature=" + dtoProduct.getNomenclatureName() + 
                          ", quantity=" + dtoProduct.getQuantity() + 
                          ", taken=" + dtoProduct.getTaken());
        
        return dtoProduct;
    }

    public List<Product> mapToDomainList(List<ProductDto> dtoListProducts) {
        if (dtoListProducts == null) {
            return new ArrayList<>();
        }
        List<Product> domainProducts = new ArrayList<>();
        for (ProductDto dtoProduct : dtoListProducts) {
            domainProducts.add(mapToDomain(dtoProduct));
        }
        return domainProducts;
    }

    public List<ProductDto> mapToDtoList(List<Product> domainProducts) {
        if (domainProducts == null) {
            return new ArrayList<>();
        }
        List<ProductDto> dtoListProducts = new ArrayList<>();
        for (Product domainProduct : domainProducts) {
            dtoListProducts.add(mapToDto(domainProduct));
        }
        return dtoListProducts;
    }
} 