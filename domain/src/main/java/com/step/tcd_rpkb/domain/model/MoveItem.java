package com.step.tcd_rpkb.domain.model;

// import com.google.gson.annotations.SerializedName;

public class MoveItem {
    // @SerializedName("ПеремещениеГУИД")
    private final String movementId;
    // @SerializedName("ПеремещениеПредставление")
    private final String movementDisplayText;
    // @SerializedName("ЦПС")
    private final boolean isCps;
    // @SerializedName("Дата")
    private final String date;
    // @SerializedName("Номер")
    private final String number;
    // @SerializedName("Комментарий")
    private final String comment;
    // @SerializedName("НоменклатураНаименование")
    private final String productName;
    // @SerializedName("ОтветственныйЗаПолучениеНаименование")
    private final String responsiblePersonName;
    // @SerializedName("Цвет")
    private final String color;
    // @SerializedName("Приоритет")
    private final String priority;
    // @SerializedName("КомплектовщикНаименование")
    private final String assemblerName;
    // @SerializedName("СтатусПодписания")
    private  String signingStatus;
    // @SerializedName("СкладОтправительНаименование")
    private final String sourceWarehouseName;
    // @SerializedName("СкладПолучательНаименование")
    private final String destinationWarehouseName;
    // @SerializedName("КолвоШтук")
    private final double itemsCount;
    // @SerializedName("КолвоПозиций")
    private final int positionsCount;

    public MoveItem(String movementId, String movementDisplayText, boolean isCps, String date, 
                    String number, String comment, String productName, String responsiblePersonName, 
                    String color, String priority, String assemblerName, String signingStatus, 
                    String sourceWarehouseName, String destinationWarehouseName, 
                    double itemsCount, int positionsCount) {
        this.movementId = movementId;
        this.movementDisplayText = movementDisplayText;
        this.isCps = isCps;
        this.date = date;
        this.number = number;
        this.comment = comment;
        this.productName = productName;
        this.responsiblePersonName = responsiblePersonName;
        this.color = color;
        this.priority = priority;
        this.assemblerName = assemblerName;
        this.signingStatus = signingStatus;
        this.sourceWarehouseName = sourceWarehouseName;
        this.destinationWarehouseName = destinationWarehouseName;
        this.itemsCount = itemsCount;
        this.positionsCount = positionsCount;
    }

    public void setSigningStatus(String signingStatus) {
        this.signingStatus = signingStatus;
    }

    // Геттеры
    public String getMovementId() { return movementId; }
    public String getMovementDisplayText() { return movementDisplayText; }
    public boolean isCps() { return isCps; } // Для boolean геттер isCps(), а не getIsCps()
    public String getDate() { return date; }
    public String getNumber() { return number; }
    public String getComment() { return comment; }
    public String getProductName() { return productName; }
    public String getResponsiblePersonName() { return responsiblePersonName; }
    public String getColor() { return color; }
    public String getPriority() { return priority; }
    public String getAssemblerName() { return assemblerName; }
    public String getSigningStatus() { return signingStatus; }
    public String getSourceWarehouseName() { return sourceWarehouseName; }
    public String getDestinationWarehouseName() { return destinationWarehouseName; }
    public double getItemsCount() { return itemsCount; }
    public int getPositionsCount() { return positionsCount; }

    // Метод hasCpsValue() был просто дубликатом isCps(), так что его можно убрать,
    // если isCps() является стандартным геттером для boolean.
} 