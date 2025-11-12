package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO для элемента данных при сохранении перемещения в 1С
 */
public class SaveMoveDataItemDto {
    
    @SerializedName("УИДСтрокиТовары")
    private String productLineId;
    
    @SerializedName("УИДСтрокиТоварыРодитель")
    private String parentProductLineId;
    
    @SerializedName("НоменклатураГУИД")
    private String nomenclatureGuid;
    
    @SerializedName("СерияГУИД")
    private String seriesGuid;
    
    @SerializedName("ДокументРезерваГУИД")
    private String reserveDocumentGuid;
    
    @SerializedName("Exist")
    private String exist;
    
    @SerializedName("Количество")
    private double quantity;
    

    
    public SaveMoveDataItemDto(String productLineId, String parentProductLineId, String nomenclatureGuid,
                               String seriesGuid, String reserveDocumentGuid, boolean exist, double quantity) {
        this.productLineId = productLineId;
        this.parentProductLineId = parentProductLineId;
        this.nomenclatureGuid = nomenclatureGuid;
        this.seriesGuid = seriesGuid;
        this.reserveDocumentGuid = reserveDocumentGuid;
        this.exist = String.valueOf(exist);
        this.quantity = quantity;
    }
    
    public String getProductLineId() { return productLineId; }
    public void setProductLineId(String productLineId) { this.productLineId = productLineId; }
    
    public String getParentProductLineId() { return parentProductLineId; }
    public void setParentProductLineId(String parentProductLineId) { this.parentProductLineId = parentProductLineId; }
    
    public String getNomenclatureGuid() { return nomenclatureGuid; }
    public void setNomenclatureGuid(String nomenclatureGuid) { this.nomenclatureGuid = nomenclatureGuid; }
    
    public String getSeriesGuid() { return seriesGuid; }
    public void setSeriesGuid(String seriesGuid) { this.seriesGuid = seriesGuid; }
    
    public String getReserveDocumentGuid() { return reserveDocumentGuid; }
    public void setReserveDocumentGuid(String reserveDocumentGuid) { this.reserveDocumentGuid = reserveDocumentGuid; }
    
    public String getExist() { return exist; }
    public void setExist(String exist) { this.exist = exist; }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
} 