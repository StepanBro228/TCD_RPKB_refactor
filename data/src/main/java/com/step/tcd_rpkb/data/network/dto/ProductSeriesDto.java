package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * DTO для элемента серии товара из API ProductSeries
 */
public class ProductSeriesDto {
    
    @SerializedName("СерияНоменклатуры")
    private String seriesNomenclatureUuid;
    
    @SerializedName("СерияГУИД")
    private String seriesGuid;
    
    @SerializedName("СрокГодности")
    private String expiryDate;
    
    @SerializedName("СвободныйОстаток")
    @JsonAdapter(SafeDoubleDeserializer.class)
    private double freeBalanceBySeries;
    
    @SerializedName("РезервДругихЗНП")
    @JsonAdapter(SafeDoubleDeserializer.class)
    private double reservedByOthers;
    
    @SerializedName("КоличествоДокумент")
    @JsonAdapter(SafeDoubleDeserializer.class)
    private double documentQuantity;
    
    public ProductSeriesDto() {
    }
    

    public String getSeriesNomenclatureUuid() {
        return seriesNomenclatureUuid;
    }
    
    public String getSeriesGuid() {
        return seriesGuid;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public double getFreeBalanceBySeries() {
        return freeBalanceBySeries;
    }
    
    public double getReservedByOthers() {
        return reservedByOthers;
    }
    
    public double getDocumentQuantity() {
        return documentQuantity;
    }
    

    public void setSeriesNomenclatureUuid(String seriesNomenclatureUuid) {
        this.seriesNomenclatureUuid = seriesNomenclatureUuid;
    }
    
    public void setSeriesGuid(String seriesGuid) {
        this.seriesGuid = seriesGuid;
    }
    
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public void setFreeBalanceBySeries(double freeBalanceBySeries) {
        this.freeBalanceBySeries = freeBalanceBySeries;
    }
    
    public void setReservedByOthers(double reservedByOthers) {
        this.reservedByOthers = reservedByOthers;
    }
    
    public void setDocumentQuantity(double documentQuantity) {
        this.documentQuantity = documentQuantity;
    }
    
    /**
     * Кастомный десериализатор для безопасного парсинга double значений
     * Обрабатывает пустые строки и возвращает 0.0
     */
    public static class SafeDoubleDeserializer implements JsonDeserializer<Double> {
        @Override
        public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return 0.0;
                }
                
                String value = json.getAsString();
                if (value == null || value.trim().isEmpty()) {
                    return 0.0;
                }
                
                // Заменяем запятую на точку для поддержки русской локали
                String normalizedValue = value.trim().replace(',', '.');
                return Double.parseDouble(normalizedValue);
            } catch (Exception e) {
                // Если не удалось распарсить, возвращаем 0.0
                return 0.0;
            }
        }
    }
} 