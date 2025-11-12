package com.step.tcd_rpkb.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO для ответа сервера при получении информации о пользователе по GUID
 */
public class UserInfoResponseDto {
    
    @SerializedName("ТекстОшибки")
    private String errorText;
    
    @SerializedName("Результат")
    private boolean result = true;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("fullname")
    private String fullName;
    
    @SerializedName("guid")
    private String guid;
    

    @SerializedName("error")
    private String error;

    
    public UserInfoResponseDto(String name, String fullName, String guid) {
        this.name = name;
        this.fullName = fullName;
        this.guid = guid;
        this.result = true;
        this.errorText = "";
    }
    
    // Геттеры для новых полей
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getGuid() {
        return guid;
    }
    
    public void setGuid(String guid) {
        this.guid = guid;
    }
    

    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Проверяет, есть ли ошибка в ответе сервера
     * @return true если есть ошибка
     */
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
} 