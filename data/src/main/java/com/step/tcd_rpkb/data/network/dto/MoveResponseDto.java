package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MoveResponseDto {
    @SerializedName("ДатаНачала")
    private String startDate;
    @SerializedName("ДатаОкончания")
    private String endDate;
    @SerializedName("МассивСостоянийСтрокой")
    private List<String> statusList;
    @SerializedName("Данные")
    private List<MoveItemDto> items; // Используем MoveItemDto

    // Пустой конструктор для Gson
    public MoveResponseDto() {}

    // Геттеры и Сеттеры
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<String> getStatusList() { return statusList; }
    public void setStatusList(List<String> statusList) { this.statusList = statusList; }
    public List<MoveItemDto> getItems() { return items; }
    public void setItems(List<MoveItemDto> items) { this.items = items; }
} 