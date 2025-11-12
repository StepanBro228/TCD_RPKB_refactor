package com.step.tcd_rpkb.domain.model;

/**
 * Результат операции авторизации
 */
public class AuthenticationResult {
    private final boolean success;
    private final String errorMessage;
    private final User user;
    
    // Конструктор для успешной авторизации
    public AuthenticationResult(User user) {
        this.success = true;
        this.errorMessage = null;
        this.user = user;
    }
    
    // Конструктор для неудачной авторизации
    public AuthenticationResult(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.user = null;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public User getUser() {
        return user;
    }
} 