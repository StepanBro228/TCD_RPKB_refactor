package com.step.tcd_rpkb.domain.model;

// import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MoveResponse {
    // @SerializedName("ДатаНачала")
    private final String startDate;
    // @SerializedName("ДатаОкончания")
    private final String endDate;
    // @SerializedName("МассивСостоянийСтрокой")
    private final List<String> statusList;
    // @SerializedName("Данные")
    private final List<MoveItem> items;

    public MoveResponse(String startDate, String endDate, List<String> statusList, List<MoveItem> items) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.statusList = statusList;
        this.items = items;
    }

    // Геттеры
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public List<String> getStatusList() { return statusList; }
    public List<MoveItem> getItems() { return items; }
} 