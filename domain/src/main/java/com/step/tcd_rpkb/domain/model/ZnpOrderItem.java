package com.step.tcd_rpkb.domain.model;

/**
 * Модель заказа на производство (ЗНП)
 */
public class ZnpOrderItem {
    
    private String znpNumber;
    private String fromDate;
    private double quantityToProcure;
    private double reserve;
    private double reserveByOthers;
    private double reservedQuantity; // Количество, которое пользователь хочет зарезервировать
    
    public ZnpOrderItem() {
    }
    
    public ZnpOrderItem(String znpNumber, String fromDate, double quantityToProcure, 
                        double reserve, double reserveByOthers) {
        this.znpNumber = znpNumber;
        this.fromDate = fromDate;
        this.quantityToProcure = quantityToProcure;
        this.reserve = reserve;
        this.reserveByOthers = reserveByOthers;
        this.reservedQuantity = 0.0;
    }
    
    // Getters
    public String getZnpNumber() {
        return znpNumber;
    }
    
    public String getFromDate() {
        return fromDate;
    }
    
    public double getQuantityToProcure() {
        return quantityToProcure;
    }
    
    public double getReserve() {
        return reserve;
    }
    
    public double getReserveByOthers() {
        return reserveByOthers;
    }
    
    public double getReservedQuantity() {
        return reservedQuantity;
    }
    
    // Setters
    public void setZnpNumber(String znpNumber) {
        this.znpNumber = znpNumber;
    }
    
    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }
    
    public void setQuantityToProcure(double quantityToProcure) {
        this.quantityToProcure = quantityToProcure;
    }
    
    public void setReserve(double reserve) {
        this.reserve = reserve;
    }
    
    public void setReserveByOthers(double reserveByOthers) {
        this.reserveByOthers = reserveByOthers;
    }
    
    public void setReservedQuantity(double reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    /**
     * Создает копию объекта
     */
    public ZnpOrderItem copy() {
        ZnpOrderItem copy = new ZnpOrderItem(znpNumber, fromDate, quantityToProcure, reserve, reserveByOthers);
        copy.setReservedQuantity(reservedQuantity);
        return copy;
    }
    
    @Override
    public String toString() {
        return "ZnpOrderItem{" +
                "znpNumber='" + znpNumber + '\'' +
                ", fromDate='" + fromDate + '\'' +
                ", quantityToProcure=" + quantityToProcure +
                ", reserve=" + reserve +
                ", reserveByOthers=" + reserveByOthers +
                ", reservedQuantity=" + reservedQuantity +
                '}';
    }
} 