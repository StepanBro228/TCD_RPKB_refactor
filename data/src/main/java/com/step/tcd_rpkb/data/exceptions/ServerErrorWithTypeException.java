package com.step.tcd_rpkb.data.exceptions;

/**
 * Исключение для серверных ошибок с указанием типа операции для правильного заголовка диалога
 */
public class ServerErrorWithTypeException extends ServerErrorException {
    
    public enum ErrorType {
        MOVE_LIST("Ошибка загрузки списка перемещений"),
        DOCUMENT_MOVE("Ошибка загрузки состава перемещения"),
        CHANGE_MOVE_STATUS("Ошибка смены статуса перемещения"),
        PRODUCT_SERIES("Ошибка загрузки серий"),
        USER_INFO("Ошибка получения информации о пользователе");
        
        private final String title;
        
        ErrorType(String title) {
            this.title = title;
        }
        
        public String getTitle() {
            return title;
        }
    }
    
    private final ErrorType errorType;
    
    public ServerErrorWithTypeException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    public ServerErrorWithTypeException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public String getDialogTitle() {
        return errorType.getTitle();
    }
} 