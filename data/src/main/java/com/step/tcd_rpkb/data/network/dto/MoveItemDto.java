package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

public class MoveItemDto {
    @SerializedName("ПеремещениеГУИД")
    private String movementId;
    @SerializedName("ПеремещениеПредставление")
    private String movementDisplayText;
    @SerializedName("ЦПС")
    private boolean isCps;
    @SerializedName("Дата")
    private String date;
    @SerializedName("Номер")
    private String number;
    @SerializedName("Комментарий")
    private String comment;
    @SerializedName("НоменклатураНаименование")
    private String productName;
    @SerializedName("ОтветственныйЗаПолучениеНаименование")
    private String responsiblePersonName;
    @SerializedName("Цвет")
    private String color;
    @SerializedName("Приоритет")
    private String priority;
    @SerializedName("КомплектовщикНаименование")
    private String assemblerName;
    @SerializedName("СтатусПодписания")
    private String signingStatus;
    @SerializedName("СкладОтправительНаименование")
    private String sourceWarehouseName;
    @SerializedName("СкладПолучательНаименование")
    private String destinationWarehouseName;
    @SerializedName("КолвоШтук")
    private double itemsCount;
    @SerializedName("КолвоПозиций")
    private int positionsCount;

    // Пустой конструктор для Gson
    public MoveItemDto() {}

    // Геттеры и Сеттеры (можно оставить только геттеры)
    public String getMovementId() { return movementId; }
    public void setMovementId(String movementId) { this.movementId = movementId; }
    public String getMovementDisplayText() { return movementDisplayText; }
    public void setMovementDisplayText(String movementDisplayText) { this.movementDisplayText = movementDisplayText; }
    public boolean isCps() { return isCps; }
    public void setCps(boolean cps) { isCps = cps; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getResponsiblePersonName() { return responsiblePersonName; }
    public void setResponsiblePersonName(String responsiblePersonName) { this.responsiblePersonName = responsiblePersonName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getAssemblerName() { return assemblerName; }
    public void setAssemblerName(String assemblerName) { this.assemblerName = assemblerName; }
    public String getSigningStatus() { return signingStatus; }
    public void setSigningStatus(String signingStatus) { this.signingStatus = signingStatus; }
    public String getSourceWarehouseName() { return sourceWarehouseName; }
    public void setSourceWarehouseName(String sourceWarehouseName) { this.sourceWarehouseName = sourceWarehouseName; }
    public String getDestinationWarehouseName() { return destinationWarehouseName; }
    public void setDestinationWarehouseName(String destinationWarehouseName) { this.destinationWarehouseName = destinationWarehouseName; }
    public double getItemsCount() { return itemsCount; }
    public void setItemsCount(double itemsCount) { this.itemsCount = itemsCount; }
    public int getPositionsCount() { return positionsCount; }
    public void setPositionsCount(int positionsCount) { this.positionsCount = positionsCount; }
} 