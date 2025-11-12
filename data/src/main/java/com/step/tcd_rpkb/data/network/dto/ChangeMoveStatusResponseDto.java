package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChangeMoveStatusResponseDto {
    @SerializedName("ТекстОшибки")
    private String errorText;
    
    @SerializedName("Результат")
    private boolean result;

    @SerializedName("Статус")
    private String status;
    
    @SerializedName("Пользователь")
    private String user;
    
    @SerializedName("Данные")
    private List<String> data;
    
    @SerializedName("ГУИДПеремещения")
    private String moveGuid;


    public String getErrorText() { 
        return errorText; 
    }
    
    public void setErrorText(String errorText) { 
        this.errorText = errorText; 
    }

    public boolean isResult() { 
        return result; 
    }
    
    public void setResult(boolean result) { 
        this.result = result; 
    }

    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }
    
    public String getUser() { 
        return user; 
    }
    
    public void setUser(String user) { 
        this.user = user; 
    }
    
    public List<String> getData() { 
        return data; 
    }
    
    public void setData(List<String> data) { 
        this.data = data; 
    }
    
    public String getMoveGuid() { 
        return moveGuid; 
    }
    
    public void setMoveGuid(String moveGuid) { 
        this.moveGuid = moveGuid; 
    }
} 