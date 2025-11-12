package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MoveResponseDto {
    @SerializedName("ТекстОшибки")
    private String errorText;
    @SerializedName("Результат")
    private boolean result;
    @SerializedName("state")
    private String state;
    @SerializedName("ДатаНачала")
    private String startDate;
    @SerializedName("ДатаОкончания")
    private String endDate;
    @SerializedName("МассивСостоянийСтрокой")
    private List<String> statusList;
    @SerializedName("Данные")
    private List<MoveItemDto> items;


    public MoveResponseDto() {}

    // Геттеры и Сеттеры
    public String getErrorText() { return errorText; }
    public void setErrorText(String errorText) { this.errorText = errorText; }
    public boolean isResult() { return result; }
    public void setResult(boolean result) { this.result = result; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<String> getStatusList() { return statusList; }
    public void setStatusList(List<String> statusList) { this.statusList = statusList; }
    public List<MoveItemDto> getItems() { return items; }
    public void setItems(List<MoveItemDto> items) { this.items = items; }
} 