package com.step.tcd_rpkb.data.network;

import com.step.tcd_rpkb.data.network.dto.UserInfoResponseDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * API сервис для работы с авторизацией (использует базовые учетные данные)
 */
public interface AuthApiService {
    
    /**
     * Получает информацию о пользователе по GUID (для QR-авторизации)
     * @param userGuid GUID пользователя в формате guid=xxx
     * @return Call с информацией о пользователе
     */
    @GET("hs/rpkb_autorization")
    Call<UserInfoResponseDto> getUserInfoByGuid(@Query("guid") String userGuid);
    
    /**
     * Проверяет существование пользователя по логину (для ручной авторизации)
     * @param login Логин пользователя для проверки
     * @return Call с информацией о пользователе, если логин существует
     */
    @GET("hs/rpkb_autorization")
    Call<UserInfoResponseDto> checkUserLogin(@Query("login") String login);
    

} 