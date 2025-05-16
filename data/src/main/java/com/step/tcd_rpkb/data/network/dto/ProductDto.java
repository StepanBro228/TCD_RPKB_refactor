package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class ProductDto {
    @SerializedName("НоменклатураГУИД")
    private String nomenclatureUuid;
    @SerializedName("НоменклатураНаименование")
    private String nomenclatureName;
    @SerializedName("ЗатребованоГУИД")
    private String requestedUuid;
    @SerializedName("ЗатребованоНаименование")
    private String requestedName;
    @SerializedName("СерияНаименование")
    private String seriesName;
    @SerializedName("СерияГУИД")
    private String seriesUuid;
    @SerializedName("Количество")
    private double quantity;
    @SerializedName("ЕдиницаИзмеренияНаименование")
    private String unitName;
    @SerializedName("ЕдиницаИзмеренияГУИД")
    private String unitUuid;
    @SerializedName("МестоХраненияОтправителяНаименование")
    private String senderStorageName;
    @SerializedName("МестоХраненияОтправителяГУИД")
    private String senderStorageUuid;
    @SerializedName("МестоХраненияПолучателяНаименование")
    private String receiverStorageName;
    @SerializedName("МестоХраненияПолучателяГУИД")
    private String receiverStorageUuid;
    @SerializedName("ОтветственныйЗаПолучениеНаименование")
    private String responsibleReceiverName;
    @SerializedName("ОтветственныйЗаПолучениеГУИД")
    private String responsibleReceiverUuid;
    @SerializedName("ДокументРезерваПредставление")
    private String reserveDocumentName;
    @SerializedName("ДокументРезерваГУИД")
    private String reserveDocumentUuid;
    @SerializedName("СвободныйОстатокВЯчейке")
    private double freeBalanceInCell;
    @SerializedName("СвободныйОстатокПоСерии")
    private double freeBalanceBySeries;
    @SerializedName("СвободныйОстаток")
    private double freeBalance;
    @SerializedName("ОбщийОстаток")
    private double totalBalance;
    @SerializedName("Взял")
    private int taken;

    // Пустой конструктор для Gson
    public ProductDto() {}

    // Геттеры и сеттеры (оставим для простоты, хотя для DTO часто достаточно геттеров)
    public String getNomenclatureUuid() { return nomenclatureUuid; }
    public void setNomenclatureUuid(String nomenclatureUuid) { this.nomenclatureUuid = nomenclatureUuid; }
    public String getNomenclatureName() { return nomenclatureName; }
    public void setNomenclatureName(String nomenclatureName) { this.nomenclatureName = nomenclatureName; }
    public String getRequestedUuid() { return requestedUuid; }
    public void setRequestedUuid(String requestedUuid) { this.requestedUuid = requestedUuid; }
    public String getRequestedName() { return requestedName; }
    public void setRequestedName(String requestedName) { this.requestedName = requestedName; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public String getSeriesUuid() { return seriesUuid; }
    public void setSeriesUuid(String seriesUuid) { this.seriesUuid = seriesUuid; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getUnitUuid() { return unitUuid; }
    public void setUnitUuid(String unitUuid) { this.unitUuid = unitUuid; }
    public String getSenderStorageName() { return senderStorageName; }
    public void setSenderStorageName(String senderStorageName) { this.senderStorageName = senderStorageName; }
    public String getSenderStorageUuid() { return senderStorageUuid; }
    public void setSenderStorageUuid(String senderStorageUuid) { this.senderStorageUuid = senderStorageUuid; }
    public String getReceiverStorageName() { return receiverStorageName; }
    public void setReceiverStorageName(String receiverStorageName) { this.receiverStorageName = receiverStorageName; }
    public String getReceiverStorageUuid() { return receiverStorageUuid; }
    public void setReceiverStorageUuid(String receiverStorageUuid) { this.receiverStorageUuid = receiverStorageUuid; }
    public String getResponsibleReceiverName() { return responsibleReceiverName; }
    public void setResponsibleReceiverName(String responsibleReceiverName) { this.responsibleReceiverName = responsibleReceiverName; }
    public String getResponsibleReceiverUuid() { return responsibleReceiverUuid; }
    public void setResponsibleReceiverUuid(String responsibleReceiverUuid) { this.responsibleReceiverUuid = responsibleReceiverUuid; }
    public String getReserveDocumentName() { return reserveDocumentName; }
    public void setReserveDocumentName(String reserveDocumentName) { this.reserveDocumentName = reserveDocumentName; }
    public String getReserveDocumentUuid() { return reserveDocumentUuid; }
    public void setReserveDocumentUuid(String reserveDocumentUuid) { this.reserveDocumentUuid = reserveDocumentUuid; }
    public double getFreeBalanceInCell() { return freeBalanceInCell; }
    public void setFreeBalanceInCell(double freeBalanceInCell) { this.freeBalanceInCell = freeBalanceInCell; }
    public double getFreeBalanceBySeries() { return freeBalanceBySeries; }
    public void setFreeBalanceBySeries(double freeBalanceBySeries) { this.freeBalanceBySeries = freeBalanceBySeries; }
    public double getFreeBalance() { return freeBalance; }
    public void setFreeBalance(double freeBalance) { this.freeBalance = freeBalance; }
    public double getTotalBalance() { return totalBalance; }
    public void setTotalBalance(double totalBalance) { this.totalBalance = totalBalance; }
    public int getTaken() { return taken; }
    public void setTaken(int taken) { this.taken = taken; }
} 