package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO для запроса сохранения данных перемещения в 1С
 */
public class SaveMoveDataRequestDto {
    
    @SerializedName("ГУИДПеремещения")
    private String moveGuid;
    
    @SerializedName("ГУИДПользователя") 
    private String userGuid;
    
    @SerializedName("Данные")
    private List<SaveMoveDataItemDto> data;
    

    
    public SaveMoveDataRequestDto(String moveGuid, String userGuid, List<SaveMoveDataItemDto> data) {
        this.moveGuid = moveGuid;
        this.userGuid = userGuid;
        this.data = data;
    }
    
    public String getMoveGuid() { return moveGuid; }
    public void setMoveGuid(String moveGuid) { this.moveGuid = moveGuid; }
    
    public String getUserGuid() { return userGuid; }
    public void setUserGuid(String userGuid) { this.userGuid = userGuid; }
    
    public List<SaveMoveDataItemDto> getData() { return data; }
    public void setData(List<SaveMoveDataItemDto> data) { this.data = data; }
} 