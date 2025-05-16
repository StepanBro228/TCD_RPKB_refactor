package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.ProductDto;
import com.step.tcd_rpkb.domain.model.Invoice;    // Доменная модель накладной
import com.step.tcd_rpkb.domain.model.Product;    // Доменная модель продукта

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class InvoiceMapper {

    private final ProductMapper productMapper;

    @Inject
    public InvoiceMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Invoice mapToDomain(InvoiceDto dtoInvoice) {
        if (dtoInvoice == null) {
            return null;
        }
        List<Product> domainProducts = productMapper.mapToDomainList(dtoInvoice.getProducts());
        
        return new Invoice(
                dtoInvoice.getMoveUuid(),
                domainProducts
        );
    }

    public InvoiceDto mapToDto(Invoice domainInvoice) {
        if (domainInvoice == null) {
            return null;
        }
        List<ProductDto> dtoProducts = productMapper.mapToDtoList(domainInvoice.getProducts());
        
        InvoiceDto dtoInvoice = new InvoiceDto();
        dtoInvoice.setMoveUuid(domainInvoice.getUuid());
        dtoInvoice.setProducts(dtoProducts);
        return dtoInvoice;
    }

    public List<Invoice> mapToDomainList(List<InvoiceDto> dtoListInvoices) {
        if (dtoListInvoices == null) {
            return new ArrayList<>();
        }
        List<Invoice> domainInvoices = new ArrayList<>();
        for (InvoiceDto dtoInvoice : dtoListInvoices) {
            domainInvoices.add(mapToDomain(dtoInvoice));
        }
        return domainInvoices;
    }

    public List<InvoiceDto> mapToDtoList(List<Invoice> domainInvoices) {
        if (domainInvoices == null) {
            return new ArrayList<>();
        }
        List<InvoiceDto> dtoListInvoices = new ArrayList<>();
        for (Invoice domainInvoice : domainInvoices) {
            dtoListInvoices.add(mapToDto(domainInvoice));
        }
        return dtoListInvoices;
    }
} 