package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.local.realm.MoveMetadataRealm;
import com.step.tcd_rpkb.domain.model.MoveItem;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoveMetadataRealmMapper {
    
    @Inject
    public MoveMetadataRealmMapper() {}
    
    public MoveMetadataRealm toRealm (MoveItem moveItem){
        MoveMetadataRealm realm = new MoveMetadataRealm();
        realm.setMoveUuid(moveItem.getMovementId());
        realm.setMoveNumber(moveItem.getNumber());
        realm.setMoveDate(moveItem.getDate());
        realm.setSigningStatus(moveItem.getSigningStatus());
        realm.setSyncedWith1C(false);
        realm.setCreatedTimestamp(System.currentTimeMillis());
        realm.setLastModifiedTimestamp(System.currentTimeMillis());
        return realm;
    }


}
