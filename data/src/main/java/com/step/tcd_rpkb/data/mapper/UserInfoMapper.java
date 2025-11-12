package com.step.tcd_rpkb.data.mapper;

import com.step.tcd_rpkb.data.network.dto.UserInfoResponseDto;
import com.step.tcd_rpkb.domain.model.UserInfoResponse;

/**
 * Маппер для преобразования UserInfoResponseDto в доменную модель UserInfoResponse
 */
public class UserInfoMapper {
    
    /**
     * Преобразует DTO в доменную модель (для QR-авторизации)
     * GUID берется из ответа сервера
     * @param dto DTO с информацией о пользователе
     * @return Доменная модель UserInfoResponse
     */
    public static UserInfoResponse mapToDomain(UserInfoResponseDto dto) {
        if (dto == null) {
            android.util.Log.w("UserInfoMapper", "DTO пуст, возвращаем null");
            return null;
        }
        
        android.util.Log.d("UserInfoMapper", "Маппинг: dto.name=" + dto.getName() + 
                          ", dto.fullName=" + dto.getFullName() + 
                          ", dto.guid=" + dto.getGuid());
        
        UserInfoResponse result = new UserInfoResponse(
            dto.getName(),
            dto.getFullName(),
            dto.getGuid() // GUID берется из ответа сервера
        );
        
        android.util.Log.d("UserInfoMapper", "Результат маппинга: name=" + result.getName() + 
                          ", fullName=" + result.getFullName() + 
                          ", userGuid=" + result.getUserGuid());
        
        return result;
    }
    
    /**
     * @deprecated Используйте mapToDomain(UserInfoResponseDto dto) - GUID теперь берется из DTO
     */
    @Deprecated
    public static UserInfoResponse mapToDomain(UserInfoResponseDto dto, String userGuid) {
        android.util.Log.w("UserInfoMapper", "Использован устаревший метод mapToDomain с userGuid параметром");
        return mapToDomain(dto);
    }
    
    /**
     * Преобразует доменную модель в DTO
     * @param domain Доменная модель
     * @return DTO UserInfoResponseDto
     */
    public static UserInfoResponseDto mapToDto(UserInfoResponse domain) {
        if (domain == null) {
            return null;
        }
        
        return new UserInfoResponseDto(
            domain.getName(),
            domain.getFullName(),
            domain.getUserGuid()
        );
    }
} 