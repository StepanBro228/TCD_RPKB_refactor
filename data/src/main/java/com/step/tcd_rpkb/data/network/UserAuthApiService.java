package com.step.tcd_rpkb.data.network;

import com.step.tcd_rpkb.data.network.dto.UserInfoResponseDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * API сервис для проверки пароля пользователя с пользовательскими учетными данными
 */
public interface UserAuthApiService {
    
    /**
     * Проверяет пароль пользователя через эндпоинт авторизации
     * @param login Логин пользователя для проверки пароля
     * @return Call с информацией о пользователе, если пароль верный
     */
    @GET("rpkb_autorization")
    Call<UserInfoResponseDto> checkUserPassword(@Query("login") String login);
} 