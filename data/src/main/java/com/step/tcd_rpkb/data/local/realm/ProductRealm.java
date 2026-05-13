package com.step.tcd_rpkb.data.local.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class ProductRealm  extends RealmObject {
    @PrimaryKey
    private String productLineId; // Уникальный идентификатор строки товара
    @Index
    private String moveUuid;
    private String parentProductLineId; // UUID строки товара родителя (для копий)
    private String nomenclatureUuid;
    private String nomenclatureName;
    private String requestedUuid;
    private String requestedName;
    private String seriesName;
    private String seriesUuid;
    private double quantity;
    private String unitName;
    private String unitUuid;
    private String senderStorageName;
    private String senderStorageUuid;
    private String receiverStorageName;
    private String receiverStorageUuid;
    private String responsibleReceiverName;
    private String responsibleReceiverUuid;
    private String reserveDocumentName;
    private String reserveDocumentUuid;
    private double freeBalanceInCell;
    private double freeBalanceBySeries;
    private double freeBalance;
    private double totalBalance;
    private double taken;
    private boolean exists;
    @Index
    private long lastModifiedTimestamp;

    public ProductRealm() {}

    public String getProductLineId() {
        return productLineId;
    }

    public void setProductLineId(String productLineId) {
        this.productLineId = productLineId;
    }

    public String getMoveUuid() {
        return moveUuid;
    }

    public void setMoveUuid(String moveUuid) {
        this.moveUuid = moveUuid;
    }

    public String getParentProductLineId() {
        return parentProductLineId;
    }

    public void setParentProductLineId(String parentProductLineId) {
        this.parentProductLineId = parentProductLineId;
    }

    public String getNomenclatureUuid() {
        return nomenclatureUuid;
    }

    public void setNomenclatureUuid(String nomenclatureUuid) {
        this.nomenclatureUuid = nomenclatureUuid;
    }

    public String getNomenclatureName() {
        return nomenclatureName;
    }

    public void setNomenclatureName(String nomenclatureName) {
        this.nomenclatureName = nomenclatureName;
    }

    public String getRequestedUuid() {
        return requestedUuid;
    }

    public void setRequestedUuid(String requestedUuid) {
        this.requestedUuid = requestedUuid;
    }

    public String getRequestedName() {
        return requestedName;
    }

    public void setRequestedName(String requestedName) {
        this.requestedName = requestedName;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getSeriesUuid() {
        return seriesUuid;
    }

    public void setSeriesUuid(String seriesUuid) {
        this.seriesUuid = seriesUuid;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnitUuid() {
        return unitUuid;
    }

    public void setUnitUuid(String unitUuid) {
        this.unitUuid = unitUuid;
    }

    public String getSenderStorageName() {
        return senderStorageName;
    }

    public void setSenderStorageName(String senderStorageName) {
        this.senderStorageName = senderStorageName;
    }

    public String getSenderStorageUuid() {
        return senderStorageUuid;
    }

    public void setSenderStorageUuid(String senderStorageUuid) {
        this.senderStorageUuid = senderStorageUuid;
    }

    public String getReceiverStorageName() {
        return receiverStorageName;
    }

    public void setReceiverStorageName(String receiverStorageName) {
        this.receiverStorageName = receiverStorageName;
    }

    public String getReceiverStorageUuid() {
        return receiverStorageUuid;
    }

    public void setReceiverStorageUuid(String receiverStorageUuid) {
        this.receiverStorageUuid = receiverStorageUuid;
    }

    public String getResponsibleReceiverName() {
        return responsibleReceiverName;
    }

    public void setResponsibleReceiverName(String responsibleReceiverName) {
        this.responsibleReceiverName = responsibleReceiverName;
    }

    public String getResponsibleReceiverUuid() {
        return responsibleReceiverUuid;
    }

    public void setResponsibleReceiverUuid(String responsibleReceiverUuid) {
        this.responsibleReceiverUuid = responsibleReceiverUuid;
    }

    public String getReserveDocumentName() {
        return reserveDocumentName;
    }

    public void setReserveDocumentName(String reserveDocumentName) {
        this.reserveDocumentName = reserveDocumentName;
    }

    public String getReserveDocumentUuid() {
        return reserveDocumentUuid;
    }

    public void setReserveDocumentUuid(String reserveDocumentUuid) {
        this.reserveDocumentUuid = reserveDocumentUuid;
    }

    public double getFreeBalanceInCell() {
        return freeBalanceInCell;
    }

    public void setFreeBalanceInCell(double freeBalanceInCell) {
        this.freeBalanceInCell = freeBalanceInCell;
    }

    public double getFreeBalanceBySeries() {
        return freeBalanceBySeries;
    }

    public void setFreeBalanceBySeries(double freeBalanceBySeries) {
        this.freeBalanceBySeries = freeBalanceBySeries;
    }

    public double getFreeBalance() {
        return freeBalance;
    }

    public void setFreeBalance(double freeBalance) {
        this.freeBalance = freeBalance;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public double getTaken() {
        return taken;
    }

    public void setTaken(double taken) {
        this.taken = taken;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }
}
