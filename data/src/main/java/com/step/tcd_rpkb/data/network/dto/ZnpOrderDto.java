package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO для заказа на производство из JSON
 */
public class ZnpOrderDto {
    
    @SerializedName("znpNumber")
    private String znpNumber;
    
    @SerializedName("fromDate")
    private String fromDate;
    
    @SerializedName("quantityToProcure")
    private double quantityToProcure;
    
    @SerializedName("reserve")
    private double reserve;
    
    @SerializedName("reserveByOthers")
    private double reserveByOthers;
    
    public ZnpOrderDto() {
    }
    

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
} 