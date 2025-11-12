package com.step.tcd_rpkb.UI.movelist.viewmodel;

/**
 * Класс для передачи данных диалога ошибки с заголовком
 */
public class ErrorDialogData {
    private final String title;
    private final String message;
    
    public ErrorDialogData(String title, String message) {
        this.title = title;
        this.message = message;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getMessage() {
        return message;
    }
} 