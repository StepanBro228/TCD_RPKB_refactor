package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class InvoiceDto {
    @SerializedName("ТекстОшибки")
    private String errorText;
    
    @SerializedName("Результат")
    private boolean result;
    
    @SerializedName("ГУИДПеремещения")
    private String moveUuid;
    
    @SerializedName("Данные")
    private List<ProductDto> products;

    // Пустой конструктор для Gson
    public InvoiceDto() {}

    // Геттеры и Сеттеры для новых полей
    public String getErrorText() { 
        return errorText; 
    }
    
    public void setErrorText(String errorText) { 
        this.errorText = errorText; 
    }
    
    public boolean isResult() { 
        return result; 
    }
    
    public void setResult(boolean result) { 
        this.result = result; 
    }

    // Геттеры и Сеттеры для существующих полей
    public String getMoveUuid() {
        return moveUuid;
    }

    public void setMoveUuid(String moveUuid) {
        this.moveUuid = moveUuid;
    }

    public List<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDto> products) {
        this.products = products;
    }
} 