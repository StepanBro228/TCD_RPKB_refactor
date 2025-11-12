package com.step.tcd_rpkb.domain.model;

import java.util.List;

public class MoveResponse {
    private final String errorText;
    private final boolean result;
    private final String state;
    private final String startDate;
    private final String endDate;
    private final List<String> statusList;
    private final List<MoveItem> items;

    public MoveResponse(String errorText, boolean result, String state, String startDate, String endDate, List<String> statusList, List<MoveItem> items) {
        this.errorText = errorText;
        this.result = result;
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.statusList = statusList;
        this.items = items;
    }

    public String getErrorText() { return errorText; }
    public boolean isResult() { return result; }
    public String getState() { return state; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public List<String> getStatusList() { return statusList; }
    public List<MoveItem> getItems() { return items; }
} 