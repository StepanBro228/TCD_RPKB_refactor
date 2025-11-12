package com.step.tcd_rpkb.domain.model;

/**
 * Класс, представляющий информацию о серии товара.
 */
public class SeriesItem {
    private String seriesUuid;
    private String seriesName;
    private String expiryDate;
    private double freeBalance;
    private double reservedByOthers;
    private double documentQuantity;
    private double allocatedQuantity;

    public SeriesItem(String seriesUuid, String seriesName, String expiryDate, 
                     double freeBalance, double reservedByOthers, double documentQuantity) {
        this.seriesUuid = seriesUuid;
        this.seriesName = seriesName;
        this.expiryDate = expiryDate;
        this.freeBalance = freeBalance;
        this.reservedByOthers = reservedByOthers;
        this.documentQuantity = documentQuantity;
        this.allocatedQuantity = 0; // Изначально не распределено
    }

    public String getSeriesUuid() {
        return seriesUuid;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public String getExpiryDate() {
        return expiryDate;
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

    /**
     * Возвращает максимальное количество, которое можно распределить на эту серию
     */
    public double getMaxAllowedAllocation() {
        return Math.min(freeBalance, documentQuantity);
    }
    
    /**
     * Создает копию текущего объекта SeriesItem
     * @return новый объект SeriesItem с теми же значениями полей
     */
    public SeriesItem copy() {
        SeriesItem copy = new SeriesItem(
            this.seriesUuid,
            this.seriesName, 
            this.expiryDate,
            this.freeBalance,
            this.reservedByOthers,
            this.documentQuantity
        );
        copy.setAllocatedQuantity(this.allocatedQuantity);
        return copy;
    }
} 