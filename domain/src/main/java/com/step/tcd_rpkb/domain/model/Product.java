package com.step.tcd_rpkb.domain.model;

// import com.google.gson.annotations.SerializedName; // В идеале, аннотации Gson должны быть в DTO слоя data

public class Product {
    // @SerializedName("НоменклатураГУИД")
    private String nomenclatureUuid;
    // @SerializedName("НоменклатураНаименование")
    private String nomenclatureName;
    // @SerializedName("ЗатребованоГУИД")
    private String requestedUuid;
    // @SerializedName("ЗатребованоНаименование")
    private String requestedName;
    // @SerializedName("СерияНаименование")
    private String seriesName;
    // @SerializedName("СерияГУИД")
    private String seriesUuid;
    // @SerializedName("Количество")
    private double quantity;
    // @SerializedName("ЕдиницаИзмеренияНаименование")
    private String unitName;
    // @SerializedName("ЕдиницаИзмеренияГУИД")
    private String unitUuid;
    // @SerializedName("МестоХраненияОтправителяНаименование")
    private String senderStorageName;
    // @SerializedName("МестоХраненияОтправителяГУИД")
    private String senderStorageUuid;
    // @SerializedName("МестоХраненияПолучателяНаименование")
    private String receiverStorageName;
    // @SerializedName("МестоХраненияПолучателяГУИД")
    private String receiverStorageUuid;
    // @SerializedName("ОтветственныйЗаПолучениеНаименование")
    private String responsibleReceiverName;
    // @SerializedName("ОтветственныйЗаПолучениеГУИД")
    private String responsibleReceiverUuid;
    // @SerializedName("ДокументРезерваПредставление")
    private String reserveDocumentName;
    // @SerializedName("ДокументРезерваГУИД")
    private String reserveDocumentUuid;
    // @SerializedName("СвободныйОстатокВЯчейке")
    private double freeBalanceInCell;
    // @SerializedName("СвободныйОстатокПоСерии")
    private double freeBalanceBySeries;
    // @SerializedName("СвободныйОстаток")
    private double freeBalance;
    // @SerializedName("ОбщийОстаток")
    private double totalBalance;
    
    // @SerializedName("Взял")
    private int taken; // Это поле изменяемое, для ввода пользователя

    public Product(String nomenclatureUuid, String nomenclatureName, String requestedUuid, 
                     String requestedName, String seriesName, String seriesUuid, double quantity, 
                     String unitName, String unitUuid, String senderStorageName, 
                     String senderStorageUuid, String receiverStorageName, String receiverStorageUuid, 
                     String responsibleReceiverName, String responsibleReceiverUuid, 
                     String reserveDocumentName, String reserveDocumentUuid, 
                     double freeBalanceInCell, double freeBalanceBySeries, double freeBalance, 
                     double totalBalance, int taken) {
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
    }

    // Геттеры для всех полей
    public String getNomenclatureUuid() { return nomenclatureUuid; }
    public String getNomenclatureName() { return nomenclatureName; }
    public String getRequestedUuid() { return requestedUuid; }
    public String getRequestedName() { return requestedName; }
    public String getSeriesName() { return seriesName; }
    public String getSeriesUuid() { return seriesUuid; }
    public double getQuantity() { return quantity; }
    public String getUnitName() { return unitName; }
    public String getUnitUuid() { return unitUuid; }
    public String getSenderStorageName() { return senderStorageName; }
    public String getSenderStorageUuid() { return senderStorageUuid; }
    public String getReceiverStorageName() { return receiverStorageName; }
    public String getReceiverStorageUuid() { return receiverStorageUuid; }
    public String getResponsibleReceiverName() { return responsibleReceiverName; }
    public String getResponsibleReceiverUuid() { return responsibleReceiverUuid; }
    public String getReserveDocumentName() { return reserveDocumentName; }
    public String getReserveDocumentUuid() { return reserveDocumentUuid; }
    public double getFreeBalanceInCell() { return freeBalanceInCell; }
    public double getFreeBalanceBySeries() { return freeBalanceBySeries; }
    public double getFreeBalance() { return freeBalance; }
    public double getTotalBalance() { return totalBalance; }
    public int getTaken() { return taken; }

    // Сеттеры (только для тех полей, которые могут изменяться в domain слое)
    public void setTaken(int taken) { this.taken = taken; }
    public void setQuantity(double quantity) { this.quantity = quantity; } // Если количество может меняться

    @Override
    public String toString() {
        return "Product{" +
                "nomenclatureName='" + nomenclatureName + '\'' +
                ", quantity=" + quantity +
                ", taken=" + taken +
                '}';
    }
} 