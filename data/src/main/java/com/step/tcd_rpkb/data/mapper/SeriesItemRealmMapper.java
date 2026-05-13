package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.local.realm.SeriesItemRealm;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesItemRealmMapper {
    
    @Inject
    public SeriesItemRealmMapper() {}
    
    public SeriesItemRealm toRealm (SeriesItem domain, String moveUuid, String nomenclatureUuid, String id){
        SeriesItemRealm realm = new SeriesItemRealm();
        realm.setId(id);
        realm.setMoveUuid(moveUuid);
        realm.setNomenclatureUuid(nomenclatureUuid);
        realm.setSeriesUuid(domain.getSeriesUuid());
        realm.setSeriesName(domain.getSeriesName());
        realm.setExpiryDate(domain.getExpiryDate());
        realm.setFreeBalance(domain.getFreeBalance());
        realm.setReservedByOthers(domain.getReservedByOthers());
        realm.setDocumentQuantity(domain.getDocumentQuantity());
        realm.setAllocatedQuantity(domain.getAllocatedQuantity());
        realm.setLastModifiedTimestamp(System.currentTimeMillis());
        return realm;
    }
    public SeriesItem toDomain(SeriesItemRealm realm){
        SeriesItem item =  new SeriesItem(
                realm.getSeriesUuid(),
                realm.getSeriesName(),
                realm.getExpiryDate(),
                realm.getFreeBalance(),
                realm.getReservedByOthers(),
                realm.getDocumentQuantity()
        );
        item.setAllocatedQuantity(realm.getAllocatedQuantity());
        return item;
    }
    public List <SeriesItem> toDomainList (List<SeriesItemRealm> realmList){
        List <SeriesItem> newList = new ArrayList<>();
        for(SeriesItemRealm item : realmList){
            newList.add(toDomain(item));
        }
        return newList;
    }
}
