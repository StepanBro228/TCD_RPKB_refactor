package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.AuthenticationResult;
import com.step.tcd_rpkb.domain.model.UserInfoResponse;

/**
 * Репозиторий для работы с авторизацией пользователей
 */
public interface AuthRepository {
    
    /**
     * Получает информацию о пользователе по GUID
     * @param userGuid GUID пользователя
     * @param callback Callback для получения результата
     */
    void getUserInfoByGuid(String userGuid, RepositoryCallback<UserInfoResponse> callback);
    
    /**
     * Проверяет авторизацию пользователя с логином и паролем
     * @param login Логин пользователя
     * @param password Пароль пользователя
     * @param callback Callback для получения результата
     */
    void authenticateUser(String login, String password, RepositoryCallback<AuthenticationResult> callback);
} 