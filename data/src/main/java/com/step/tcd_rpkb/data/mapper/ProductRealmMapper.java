package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.local.realm.ProductRealm;
import com.step.tcd_rpkb.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductRealmMapper {
    
    @Inject
    public ProductRealmMapper() {}
    
    public ProductRealm toRealm (Product domain, String moveUuid){
        ProductRealm realm = new ProductRealm();
        realm.setProductLineId(domain.getProductLineId());
        realm.setMoveUuid(moveUuid);
        realm.setParentProductLineId(domain.getParentProductLineId());
        realm.setNomenclatureUuid(domain.getNomenclatureUuid());
        realm.setNomenclatureName(domain.getNomenclatureName());
        realm.setRequestedUuid(domain.getRequestedUuid());
        realm.setRequestedName(domain.getRequestedName());
        realm.setSeriesName(domain.getSeriesName());
        realm.setSeriesUuid(domain.getSeriesUuid());
        realm.setQuantity(domain.getQuantity());
        realm.setUnitName(domain.getUnitName());
        realm.setUnitUuid(domain.getUnitUuid());
        realm.setSenderStorageName(domain.getSenderStorageName());
        realm.setSenderStorageUuid(domain.getSenderStorageUuid());
        realm.setReceiverStorageName(domain.getReceiverStorageName());
        realm.setReceiverStorageUuid(domain.getReceiverStorageUuid());
        realm.setResponsibleReceiverName(domain.getResponsibleReceiverName());
        realm.setResponsibleReceiverUuid(domain.getResponsibleReceiverUuid());
        realm.setReserveDocumentName(domain.getReserveDocumentName());
        realm.setReserveDocumentUuid(domain.getReserveDocumentUuid());
        realm.setFreeBalanceInCell(domain.getFreeBalanceInCell());
        realm.setFreeBalanceBySeries(domain.getFreeBalanceBySeries());
        realm.setFreeBalance(domain.getFreeBalance());
        realm.setTotalBalance(domain.getTotalBalance());
        realm.setTaken(domain.getTaken());
        realm.setExists(domain.getExists());
        realm.setLastModifiedTimestamp(System.currentTimeMillis());
        return realm;
    }
    public Product toDomain(ProductRealm realm) {
        return new Product(
                realm.getProductLineId(),
                realm.getParentProductLineId(),
                realm.getNomenclatureUuid(),
                realm.getNomenclatureName(),
                realm.getRequestedUuid(),
                realm.getRequestedName(),
                realm.getSeriesName(),
                realm.getSeriesUuid(),
                realm.getQuantity(),
                realm.getUnitName(),
                realm.getUnitUuid(),
                realm.getSenderStorageName(),
                realm.getSenderStorageUuid(),
                realm.getReceiverStorageName(),
                realm.getReceiverStorageUuid(),
                realm.getResponsibleReceiverName(),
                realm.getResponsibleReceiverUuid(),
                realm.getReserveDocumentName(),
                realm.getReserveDocumentUuid(),
                realm.getFreeBalanceInCell(),
                realm.getFreeBalanceBySeries(),
                realm.getFreeBalance(),
                realm.getTotalBalance(),
                realm.getTaken(),
                realm.isExists()
        );
    }
    public List<Product> toDomainList (List<ProductRealm> realmList){
        List<Product> products = new ArrayList<>();
        for (ProductRealm product : realmList){
            products.add(toDomain(product));

        }
        return products;
    }
}
