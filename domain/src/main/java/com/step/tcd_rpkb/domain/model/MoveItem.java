package com.step.tcd_rpkb.domain.model;



public class MoveItem {
    private final String movementId;
    private final String movementDisplayText;
    private final boolean isCps;
    private final String date;
    private final String number;
    private final boolean isCompleted;
    private final String comment;
    private final String productName;
    private final String responsiblePersonName;
    private final String color;
    private final String priority;
    private final String assemblerName;
    private  String signingStatus;
    private final String sourceWarehouseName;
    private final String destinationWarehouseName;
    private final double itemsCount;
    private final int positionsCount;

    public MoveItem(String movementId, String movementDisplayText, boolean isCps, String date, 
                    String number, boolean isCompleted, String comment, String productName, String responsiblePersonName, 
                    String color, String priority, String assemblerName, String signingStatus, 
                    String sourceWarehouseName, String destinationWarehouseName, 
                    double itemsCount, int positionsCount) {
        this.movementId = movementId;
        this.movementDisplayText = movementDisplayText;
        this.isCps = isCps;
        this.date = date;
        this.number = number;
        this.isCompleted = isCompleted;
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

    public String getMovementId() { return movementId; }
    public String getMovementDisplayText() { return movementDisplayText; }
    public boolean isCps() { return isCps; }
    public String getDate() { return date; }
    public String getNumber() { return number; }
    public boolean isCompleted() { return isCompleted; }
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


} 