package com.step.tcd_rpkb.domain.model;


import java.util.List;

public class Invoice {
    private final String errorText;
    private final boolean result;
    private String uuid;
    private List<Product> products;

    // Конструктор с новыми полями
    public Invoice(String errorText, boolean result, String uuid, List<Product> products) {
        this.errorText = errorText;
        this.result = result;
        this.uuid = uuid;
        this.products = products;
    }

    public Invoice(String uuid, List<Product> products) {
        this.errorText = "";
        this.result = true;
        this.uuid = uuid;
        this.products = products;
    }

    public Invoice() {
        this.errorText = "";
        this.result = true;
    }


    public String getErrorText() { 
        return errorText; 
    }
    
    public boolean isResult() { 
        return result; 
    }

    public String getUuid() {
        return uuid;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "errorText='" + errorText + '\'' +
                ", result=" + result +
                ", uuid='" + uuid + '\'' +
                ", products=" + (products != null ? products.size() : 0) + " items" +
                '}';
    }
} 