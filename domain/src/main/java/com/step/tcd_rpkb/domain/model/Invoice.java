package com.step.tcd_rpkb.domain.model;

// import com.google.gson.annotations.SerializedName; // В идеале, аннотации Gson должны быть в DTO слоя data

import java.util.List;

public class Invoice {
    // @SerializedName("ГУИДПеремещения") // Атрибут для Gson, лучше в DTO
    private String uuid;
    
    // @SerializedName("Данные") // Атрибут для Gson, лучше в DTO
    // private String documentNumber; // Удаляем поле
    // private String documentDate;   // Удаляем поле
    private List<Product> products;

    // Конструктор, геттеры и сеттеры (или сделать поля final и использовать только конструктор)
    public Invoice(String uuid, /*String documentNumber, String documentDate,*/ List<Product> products) {
        this.uuid = uuid;
        // this.documentNumber = documentNumber; // Удаляем присваивание
        // this.documentDate = documentDate;     // Удаляем присваивание
        this.products = products;
    }

    public Invoice() { // Пустой конструктор может быть полезен для некоторых фреймворков/мапперов
    }

    public String getUuid() {
        return uuid;
    }

    // public String getDocumentNumber() { // Удаляем геттер
    //     return documentNumber;
    // }

    // public String getDocumentDate() { // Удаляем геттер
    //     return documentDate;
    // }

    public List<Product> getProducts() {
        return products;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    // public void setDocumentNumber(String documentNumber) { // Удаляем сеттер
    //     this.documentNumber = documentNumber;
    // }

    // public void setDocumentDate(String documentDate) { // Удаляем сеттер
    //     this.documentDate = documentDate;
    // }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "uuid='" + uuid + '\'' +
                // ", documentNumber='" + documentNumber + '\'' + // Удаляем из toString
                // ", documentDate='" + documentDate + '\'' +   // Удаляем из toString
                ", products=" + (products != null ? products.size() : 0) + " items" +
                '}';
    }
} 