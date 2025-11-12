package com.step.tcd_rpkb.data.exceptions;

/**
 * Исключение для серверных ошибок, когда поле "Результат" = false
 * Такие ошибки должны показываться в диалоговом окне, а не в Toast
 */
public class ServerErrorException extends Exception {
    
    public ServerErrorException(String message) {
        super(message);
    }
    
    public ServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
} 