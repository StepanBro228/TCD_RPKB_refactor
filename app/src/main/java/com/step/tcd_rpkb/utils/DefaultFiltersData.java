package com.step.tcd_rpkb.utils;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Модель данных для хранения фильтров пользователя по умолчанию
 */
public class DefaultFiltersData {
    
    @SerializedName("userGuid")
    private String userGuid;
    
    @SerializedName("userName")
    private String userName;
    
    // Единые фильтры для всех вкладок
    @SerializedName("sender")
    private String sender = "";
    
    @SerializedName("movementNumber")
    private String movementNumber = "";
    @SerializedName("nomenculature")
    private String nomenculature = "";
    @SerializedName("series")
    private String series = "";

    @SerializedName("recipient")
    private String recipient = "";
    
    @SerializedName("assembler")
    private String assembler = "";
    
    @SerializedName("priority")
    private String priority = "";
    
    @SerializedName("receiver")
    private String receiver = "";
    
    @SerializedName("cpsChecked")
    private boolean cpsChecked = true;
    
    @SerializedName("availabilityChecked")
    private boolean availabilityChecked = true;
    

    public DefaultFiltersData(String userGuid, String userName) {
        this.userGuid = userGuid;
        this.userName = userName;
    }
    

    public String getUserGuid() { return userGuid; }
    public void setUserGuid(String userGuid) { this.userGuid = userGuid; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    // Геттеры и сеттеры для единых фильтров
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getMovementNumber() { return movementNumber; }
    public void setMovementNumber(String movementNumber) { this.movementNumber = movementNumber; }

    public String getNomenculature() { return nomenculature;}

    public void setNomenculature(String nomenculature) {this.nomenculature = nomenculature; }

    public String getSeries() {return series; }

    public void setSeries(String series) {this.series = series; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getAssembler() { return assembler; }
    public void setAssembler(String assembler) { this.assembler = assembler; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public boolean isCpsChecked() { return cpsChecked; }
    public void setCpsChecked(boolean cpsChecked) { this.cpsChecked = cpsChecked; }
    
    public boolean isAvailabilityChecked() { return availabilityChecked; }
    public void setAvailabilityChecked(boolean availabilityChecked) { this.availabilityChecked = availabilityChecked; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultFiltersData that = (DefaultFiltersData) o;
        return Objects.equals(userGuid, that.userGuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userGuid);
    }
    
    @Override
    public String toString() {
        return "DefaultFiltersData{" +
                "userGuid='" + userGuid + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
} 