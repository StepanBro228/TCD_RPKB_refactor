package com.step.tcd_rpkb.domain.model;



public class Product {
    private String productLineId; // Уникальный идентификатор строки товара
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
    private boolean exists = true;

    public Product(String productLineId, String parentProductLineId, String nomenclatureUuid, String nomenclatureName, String requestedUuid, 
                     String requestedName, String seriesName, String seriesUuid, double quantity, 
                     String unitName, String unitUuid, String senderStorageName, 
                     String senderStorageUuid, String receiverStorageName, String receiverStorageUuid, 
                     String responsibleReceiverName, String responsibleReceiverUuid, 
                     String reserveDocumentName, String reserveDocumentUuid, 
                     double freeBalanceInCell, double freeBalanceBySeries, double freeBalance, 
                     double totalBalance, double taken, boolean exists) {
        this.productLineId = productLineId;
        this.parentProductLineId = parentProductLineId;
        this.nomenclatureUuid = nomenclatureUuid;
        this.nomenclatureName = nomenclatureName;
        this.requestedUuid = requestedUuid;
        this.requestedName = requestedName;
        this.seriesName = seriesName;
        this.seriesUuid = seriesUuid;
        this.quantity = quantity;
        this.unitName = unitName;
        this.unitUuid = unitUuid;
        this.senderStorageName = senderStorageName;
        this.senderStorageUuid = senderStorageUuid;
        this.receiverStorageName = receiverStorageName;
        this.receiverStorageUuid = receiverStorageUuid;
        this.responsibleReceiverName = responsibleReceiverName;
        this.responsibleReceiverUuid = responsibleReceiverUuid;
        this.reserveDocumentName = reserveDocumentName;
        this.reserveDocumentUuid = reserveDocumentUuid;
        this.freeBalanceInCell = freeBalanceInCell;
        this.freeBalanceBySeries = freeBalanceBySeries;
        this.freeBalance = freeBalance;
        this.totalBalance = totalBalance;
        this.taken = taken;
        this.exists = exists;
    }


    public Product(String productLineId, String nomenclatureUuid, String nomenclatureName, String requestedUuid, 
                     String requestedName, String seriesName, String seriesUuid, double quantity, 
                     String unitName, String unitUuid, String senderStorageName, 
                     String senderStorageUuid, String receiverStorageName, String receiverStorageUuid, 
                     String responsibleReceiverName, String responsibleReceiverUuid, 
                     String reserveDocumentName, String reserveDocumentUuid, 
                     double freeBalanceInCell, double freeBalanceBySeries, double freeBalance, 
                     double totalBalance, double taken) {
        this(productLineId, null, nomenclatureUuid, nomenclatureName, requestedUuid, requestedName, 
             seriesName, seriesUuid, quantity, unitName, unitUuid, senderStorageName, 
             senderStorageUuid, receiverStorageName, receiverStorageUuid, responsibleReceiverName, 
             responsibleReceiverUuid, reserveDocumentName, reserveDocumentUuid, freeBalanceInCell, 
             freeBalanceBySeries, freeBalance, totalBalance, taken, true); // exists = true по умолчанию
    }


    public String getProductLineId() { return productLineId; }
    public String getParentProductLineId() { return parentProductLineId; }
    public String getNomenclatureUuid() { return nomenclatureUuid; }
    public String getNomenclatureName() { return nomenclatureName; }
    public String getRequestedUuid() { return requestedUuid; }
    public String getRequestedName() { return requestedName; }
    public String getSeriesName() { return seriesName; }
    public String getSeriesUuid() { return seriesUuid; }
    public double getQuantity() { return quantity; }
    public String getUnitName() { return unitName; }
    public String getUnitUuid() { return unitUuid; }

    public void setProductLineId(String productLineId) {
        this.productLineId = productLineId;
    }

    public void setParentProductLineId(String parentProductLineId) {
        this.parentProductLineId = parentProductLineId;
    }

    public String getSenderStorageName() { return senderStorageName; }
    public String getSenderStorageUuid() { return senderStorageUuid; }
    public String getReceiverStorageName() { return receiverStorageName; }
    public String getReceiverStorageUuid() { return receiverStorageUuid; }
    public String getResponsibleReceiverName() { return responsibleReceiverName; }
    public String getResponsibleReceiverUuid() { return responsibleReceiverUuid; }
    public String getReserveDocumentName() { return reserveDocumentName; }
    public String getReserveDocumentUuid() { return reserveDocumentUuid; }

    public void setSeriesUuid(String seriesUuid) {
        this.seriesUuid = seriesUuid;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public void setNomenclatureName(String nomenclatureName) {
        this.nomenclatureName = nomenclatureName;
    }

    public double getFreeBalanceInCell() { return freeBalanceInCell; }
    public double getFreeBalanceBySeries() { return freeBalanceBySeries; }
    public double getFreeBalance() { return freeBalance; }
    public double getTotalBalance() { return totalBalance; }
    public double getTaken() { return taken; }
    public boolean getExists() { return exists; }
    public void setTaken(double taken) { this.taken = taken; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setExists(boolean exists) { this.exists = exists; }

    @Override
    public String toString() {
        return "Product{" +
                "nomenclatureName='" + nomenclatureName + '\'' +
                ", quantity=" + quantity +
                ", taken=" + taken +
                ", exists=" + exists +
                '}';
    }
} 