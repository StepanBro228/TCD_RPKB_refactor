package com.step.tcd_rpkb.domain.model;

/**
 * Модель элемента продукта для замены серии
 */
public class SeriesChangeItem {
    
    private String productLineId; // УИДСтрокиТовары - для идентификации продукта
    private String parentProductLineId; // УИДСтрокиТоварыРодитель - для отслеживания родительского продукта
    private String seriesName; // СерияНаименование  
    private String znpName; // ДокументРезерваПредставление
    private double quantityToProcure; // Количество (К обеспечению)
    private boolean isSelected; // Выбран ли чекбокс
    
    // Дополнительные поля для работы с продуктом
    private String nomenclatureUuid;
    private String nomenclatureName;
    private String seriesUuid;
    private String reserveDocumentUuid;
    private String unitName;
    private String unitUuid;
    private String senderStorageName;
    private String senderStorageUuid;
    private String receiverStorageName;
    private String receiverStorageUuid;
    private String responsibleReceiverName;
    private String responsibleReceiverUuid;
    private String requestedUuid;
    private String requestedName;
    private double freeBalanceInCell;
    private double freeBalanceBySeries;
    private double freeBalance;
    private double totalBalance;
    private double taken;
    private boolean exists;
    
    public SeriesChangeItem() {
    }
    
    public SeriesChangeItem(String productLineId, String seriesName, String znpName, double quantityToProcure) {
        this.productLineId = productLineId;
        this.seriesName = seriesName;
        this.znpName = znpName;
        this.quantityToProcure = quantityToProcure;
        this.isSelected = false;
    }
    
    /**
     * Создает SeriesChangeItem из Product
     */
    public static SeriesChangeItem fromProduct(Product product) {
        SeriesChangeItem item = new SeriesChangeItem();
        item.productLineId = product.getProductLineId();
        item.parentProductLineId = product.getParentProductLineId(); // Копируем родительский ID
        item.seriesName = product.getSeriesName();
        item.znpName = product.getReserveDocumentName();
        item.quantityToProcure = product.getQuantity();
        item.isSelected = false;
        
        // Копируем все остальные поля
        item.nomenclatureUuid = product.getNomenclatureUuid();
        item.nomenclatureName = product.getNomenclatureName();
        item.seriesUuid = product.getSeriesUuid();
        item.reserveDocumentUuid = product.getReserveDocumentUuid();
        item.unitName = product.getUnitName();
        item.unitUuid = product.getUnitUuid();
        item.senderStorageName = product.getSenderStorageName();
        item.senderStorageUuid = product.getSenderStorageUuid();
        item.receiverStorageName = product.getReceiverStorageName();
        item.receiverStorageUuid = product.getReceiverStorageUuid();
        item.responsibleReceiverName = product.getResponsibleReceiverName();
        item.responsibleReceiverUuid = product.getResponsibleReceiverUuid();
        item.requestedUuid = product.getRequestedUuid();
        item.requestedName = product.getRequestedName();
        item.freeBalanceInCell = product.getFreeBalanceInCell();
        item.freeBalanceBySeries = product.getFreeBalanceBySeries();
        item.freeBalance = product.getFreeBalance();
        item.totalBalance = product.getTotalBalance();
        item.taken = product.getTaken();
        item.exists = product.getExists(); // Копируем поле exists
        
        return item;
    }
    
    /**
     * Создает Product из SeriesChangeItem
     */
    public Product toProduct() {
        return new Product(
            productLineId, parentProductLineId, nomenclatureUuid, nomenclatureName, requestedUuid, requestedName,
            seriesName, seriesUuid, quantityToProcure, unitName, unitUuid,
            senderStorageName, senderStorageUuid, receiverStorageName, receiverStorageUuid,
            responsibleReceiverName, responsibleReceiverUuid, znpName, reserveDocumentUuid,
            freeBalanceInCell, freeBalanceBySeries, freeBalance, totalBalance, taken, exists
        );
    }
    
    // Getters
    public String getProductLineId() { return productLineId; }
    public String getParentProductLineId() { return parentProductLineId; }
    public String getSeriesName() { return seriesName; }
    public String getZnpName() { return znpName; }
    public double getQuantityToProcure() { return quantityToProcure; }
    public boolean isSelected() { return isSelected; }
    public String getNomenclatureUuid() { return nomenclatureUuid; }
    public String getNomenclatureName() { return nomenclatureName; }
    public String getSeriesUuid() { return seriesUuid; }
    public String getReserveDocumentUuid() { return reserveDocumentUuid; }
    public String getUnitName() { return unitName; }
    public String getUnitUuid() { return unitUuid; }
    public String getSenderStorageName() { return senderStorageName; }
    public String getSenderStorageUuid() { return senderStorageUuid; }
    public String getReceiverStorageName() { return receiverStorageName; }
    public String getReceiverStorageUuid() { return receiverStorageUuid; }
    public String getResponsibleReceiverName() { return responsibleReceiverName; }
    public String getResponsibleReceiverUuid() { return responsibleReceiverUuid; }
    public String getRequestedUuid() { return requestedUuid; }
    public String getRequestedName() { return requestedName; }
    public double getFreeBalanceInCell() { return freeBalanceInCell; }
    public double getFreeBalanceBySeries() { return freeBalanceBySeries; }
    public double getFreeBalance() { return freeBalance; }
    public double getTotalBalance() { return totalBalance; }
    public double getTaken() { return taken; }
    public boolean getExists() { return exists; }
    
    // Setters
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    public void setParentProductLineId(String parentProductLineId) { this.parentProductLineId = parentProductLineId; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public void setZnpName(String znpName) { this.znpName = znpName; }
    public void setQuantityToProcure(double quantityToProcure) { this.quantityToProcure = quantityToProcure; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
    public void setNomenclatureUuid(String nomenclatureUuid) { this.nomenclatureUuid = nomenclatureUuid; }
    public void setNomenclatureName(String nomenclatureName) { this.nomenclatureName = nomenclatureName; }
    public void setSeriesUuid(String seriesUuid) { this.seriesUuid = seriesUuid; }
    public void setReserveDocumentUuid(String reserveDocumentUuid) { this.reserveDocumentUuid = reserveDocumentUuid; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public void setUnitUuid(String unitUuid) { this.unitUuid = unitUuid; }
    public void setSenderStorageName(String senderStorageName) { this.senderStorageName = senderStorageName; }
    public void setSenderStorageUuid(String senderStorageUuid) { this.senderStorageUuid = senderStorageUuid; }
    public void setReceiverStorageName(String receiverStorageName) { this.receiverStorageName = receiverStorageName; }
    public void setReceiverStorageUuid(String receiverStorageUuid) { this.receiverStorageUuid = receiverStorageUuid; }
    public void setResponsibleReceiverName(String responsibleReceiverName) { this.responsibleReceiverName = responsibleReceiverName; }
    public void setResponsibleReceiverUuid(String responsibleReceiverUuid) { this.responsibleReceiverUuid = responsibleReceiverUuid; }
    public void setRequestedUuid(String requestedUuid) { this.requestedUuid = requestedUuid; }
    public void setRequestedName(String requestedName) { this.requestedName = requestedName; }
    public void setFreeBalanceInCell(double freeBalanceInCell) { this.freeBalanceInCell = freeBalanceInCell; }
    public void setFreeBalanceBySeries(double freeBalanceBySeries) { this.freeBalanceBySeries = freeBalanceBySeries; }
    public void setFreeBalance(double freeBalance) { this.freeBalance = freeBalance; }
    public void setTotalBalance(double totalBalance) { this.totalBalance = totalBalance; }
    public void setTaken(double taken) { this.taken = taken; }
    public void setExists(boolean exists) { this.exists = exists; }
    @Override
    public String toString() {
        return "SeriesChangeItem{" +
                "productLineId='" + productLineId + '\'' +
                ", seriesName='" + seriesName + '\'' +
                ", znpName='" + znpName + '\'' +
                ", quantityToProcure=" + quantityToProcure +
                ", isSelected=" + isSelected +
                '}';
    }
} 