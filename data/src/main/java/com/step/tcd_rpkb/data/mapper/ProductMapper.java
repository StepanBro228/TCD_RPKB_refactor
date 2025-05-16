package com.step.tcd_rpkb.data.mapper;

// import com.step.tcd_rpkb.Parsing_info.Product; // Удаляем старый импорт
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
        // Важно: убедитесь, что все поля корректно мапятся.
        // Если в DomainProduct меньше полей, чем в DtoProduct, это нормально.
        // Если в DomainProduct есть поля, которых нет в DtoProduct, они будут null/дефолтными.
        return new Product(
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
                dtoProduct.getTaken()
        );
    }

    public ProductDto mapToDto(Product domainProduct) { // Изменяем тип возвращаемого значения и имя метода
        if (domainProduct == null) {
            return null;
        }
        // При обратном маппинге, возможно, не все поля из DomainProduct нужны в DtoProduct.
        ProductDto dtoProduct = new ProductDto(); // Создаем ProductDto
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
        return dtoProduct;
    }

    public List<Product> mapToDomainList(List<ProductDto> dtoListProducts) { // Изменяем тип параметра
        if (dtoListProducts == null) {
            return new ArrayList<>(); // Возвращаем пустой список, а не null
        }
        List<Product> domainProducts = new ArrayList<>();
        for (ProductDto dtoProduct : dtoListProducts) { // Изменяем тип элемента
            domainProducts.add(mapToDomain(dtoProduct));
        }
        return domainProducts;
    }

    public List<ProductDto> mapToDtoList(List<Product> domainProducts) { // Изменяем тип возвращаемого значения и имя метода
        if (domainProducts == null) {
            return new ArrayList<>();
        }
        List<ProductDto> dtoListProducts = new ArrayList<>(); // Изменяем тип списка
        for (Product domainProduct : domainProducts) {
            dtoListProducts.add(mapToDto(domainProduct)); // Используем mapToDto
        }
        return dtoListProducts;
    }
} 