package com.step.tcd_rpkb.utils;

import java.util.UUID;

/**
 * Утилитный класс для генерации уникальных идентификаторов
 */
public class UuidGenerator {
    
    /**
     * Генерирует новый уникальный GUID для строки товара
     * @return строка с уникальным идентификатором для УИДСтрокиТовары
     */
    public static String generateProductLineId() {
        return UUID.randomUUID().toString();
    }
    

} 