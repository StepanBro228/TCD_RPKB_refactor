package com.step.tcd_rpkb.data.local.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class SeriesItemRealm extends RealmObject {
    @PrimaryKey
    private String id;
    @Index
    private String moveUuid;
    @Index
    private String nomenclatureUuid;
    private String seriesUuid;
    private String seriesName;
    private String expiryDate;
    private double freeBalance;
    private double reservedByOthers;
    private double documentQuantity;
    private double allocatedQuantity;
    @Index
    private long lastModifiedTimestamp;

    public SeriesItemRealm() { }

    public String getMoveUuid() {
        return moveUuid;
    }

    public void setMoveUuid(String moveUuid) {
        this.moveUuid = moveUuid;
    }

    public String getNomenclatureUuid() {
        return nomenclatureUuid;
    }

    public void setNomenclatureUuid(String nomenclatureUuid) {
        this.nomenclatureUuid = nomenclatureUuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeriesUuid() {
        return seriesUuid;
    }

    public void setSeriesUuid(String seriesUuid) {
        this.seriesUuid = seriesUuid;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getFreeBalance() {
        return freeBalance;
    }

    public void setFreeBalance(double freeBalance) {
        this.freeBalance = freeBalance;
    }

    public double getReservedByOthers() {
        return reservedByOthers;
    }

    public void setReservedByOthers(double reservedByOthers) {
        this.reservedByOthers = reservedByOthers;
    }

    public double getDocumentQuantity() {
        return documentQuantity;
    }

    public void setDocumentQuantity(double documentQuantity) {
        this.documentQuantity = documentQuantity;
    }

    public double getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(double allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }
}
