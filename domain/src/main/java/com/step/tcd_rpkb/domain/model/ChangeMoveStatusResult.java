package com.step.tcd_rpkb.domain.model;

import java.util.List;

public class ChangeMoveStatusResult {
    private final String errorText;
    private final boolean result;
    
    private final String status;
    private final String user;
    private final List<String> data;
    private final String moveGuid;

    public ChangeMoveStatusResult(String errorText, boolean result, String status, String user, List<String> data, String moveGuid) {
        this.errorText = errorText;
        this.result = result;
        this.status = status;
        this.user = user;
        this.data = data;
        this.moveGuid = moveGuid;
    }
    
    // Конструктор для обратной совместимости
    public ChangeMoveStatusResult(boolean result, String status, String user, List<String> data) {
        this.errorText = "";
        this.result = result;
        this.status = status;
        this.user = user;
        this.data = data;
        this.moveGuid = "";
    }

    public String getErrorText() { return errorText; }
    public boolean isResult() { return result; }
    
    public String getStatus() { return status; }
    public String getUser() { return user; }
    public List<String> getData() { return data; }
    public String getMoveGuid() { return moveGuid; }
} 