package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO для данных ЗНП серии из JSON
 */
public class ZnpSeriesDataDto {
    
    @SerializedName("seriesUuid")
    private String seriesUuid;
    
    @SerializedName("warehouse")
    private String warehouse;
    
    @SerializedName("unitOfMeasurement")
    private String unitOfMeasurement;
    
    @SerializedName("freeBalance")
    private double freeBalance;
    
    @SerializedName("znpOrders")
    private List<ZnpOrderDto> znpOrders;
    
    public ZnpSeriesDataDto() {
    }
    

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
    
    public List<ZnpOrderDto> getZnpOrders() {
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
    
    public void setZnpOrders(List<ZnpOrderDto> znpOrders) {
        this.znpOrders = znpOrders;
    }
} 