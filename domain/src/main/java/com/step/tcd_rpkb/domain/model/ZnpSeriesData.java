package com.step.tcd_rpkb.domain.model;

import java.util.List;

/**
 * Модель данных ЗНП для конкретной серии
 */
public class ZnpSeriesData {
    
    private String seriesUuid;
    private String warehouse;
    private String unitOfMeasurement;
    private double freeBalance;
    private List<ZnpOrderItem> znpOrders;
    
    public ZnpSeriesData() {
    }
    
    public ZnpSeriesData(String seriesUuid, String warehouse, String unitOfMeasurement, 
                         double freeBalance, List<ZnpOrderItem> znpOrders) {
        this.seriesUuid = seriesUuid;
        this.warehouse = warehouse;
        this.unitOfMeasurement = unitOfMeasurement;
        this.freeBalance = freeBalance;
        this.znpOrders = znpOrders;
    }
    
    // Getters
    public String getSeriesUuid() {
        return seriesUuid;
    }
    
    public String getWarehouse() {
        return warehouse;
    }
    
    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }
    
    public double getFreeBalance() {
        return freeBalance;
    }
    
    public List<ZnpOrderItem> getZnpOrders() {
        return znpOrders;
    }
    
    // Setters
    public void setSeriesUuid(String seriesUuid) {
        this.seriesUuid = seriesUuid;
    }
    
    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
    
    public void setUnitOfMeasurement(String unitOfMeasurement) {
        this.unitOfMeasurement = unitOfMeasurement;
    }
    
    public void setFreeBalance(double freeBalance) {
        this.freeBalance = freeBalance;
    }
    
    public void setZnpOrders(List<ZnpOrderItem> znpOrders) {
        this.znpOrders = znpOrders;
    }
    

    
    @Override
    public String toString() {
        return "ZnpSeriesData{" +
                "seriesUuid='" + seriesUuid + '\'' +
                ", warehouse='" + warehouse + '\'' +
                ", unitOfMeasurement='" + unitOfMeasurement + '\'' +
                ", freeBalance=" + freeBalance +
                ", znpOrders=" + znpOrders +
                '}';
    }
} 