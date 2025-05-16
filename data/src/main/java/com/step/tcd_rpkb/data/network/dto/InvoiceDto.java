package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// DTO для Invoice. Структура соответствует JSON.
public class InvoiceDto {
    @SerializedName("ГУИДПеремещения")
    private String moveUuid;
    
    @SerializedName("Данные")
    private List<ProductDto> products; // Используем ProductDto

    // Пустой конструктор для Gson
    public InvoiceDto() {}

    // Геттеры и Сеттеры (или только геттеры, если объект не меняется после создания Gson'ом)
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