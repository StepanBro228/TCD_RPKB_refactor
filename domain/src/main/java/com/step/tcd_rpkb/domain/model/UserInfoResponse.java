package com.step.tcd_rpkb.domain.model;

/**
 * Модель ответа сервера при получении информации о пользователе по GUID
 */
public class UserInfoResponse {
    private final String name;     // логин пользователя
    private final String fullName; // полное имя пользователя
    private final String userGuid; // GUID пользователя из QR кода
    
    public UserInfoResponse(String name, String fullName) {
        this(name, fullName, null);
    }
    
    public UserInfoResponse(String name, String fullName, String userGuid) {
        this.name = name;
        this.fullName = fullName;
        this.userGuid = userGuid;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getUserGuid() {
        return userGuid;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserInfoResponse that = (UserInfoResponse) o;
        
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
        return userGuid != null ? userGuid.equals(that.userGuid) : that.userGuid == null;
    }
    
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        result = 31 * result + (userGuid != null ? userGuid.hashCode() : 0);
        return result;
    }
} 