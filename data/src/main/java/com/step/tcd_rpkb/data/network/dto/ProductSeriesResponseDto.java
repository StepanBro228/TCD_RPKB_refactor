package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO для ответа API серий товаров
 */
public class ProductSeriesResponseDto {
    
    @SerializedName("ТекстОшибки")
    private String errorText;
    
    @SerializedName("Результат")
    private boolean result;
    
    @SerializedName("Данные")
    private List<ProductSeriesDto> data;
    
    public ProductSeriesResponseDto() {
        // Пустой конструктор для Gson
    }
    
    // Геттеры и сеттеры
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
    
    public List<ProductSeriesDto> getData() { 
        return data; 
    }
    
    public void setData(List<ProductSeriesDto> data) { 
        this.data = data; 
    }
} 