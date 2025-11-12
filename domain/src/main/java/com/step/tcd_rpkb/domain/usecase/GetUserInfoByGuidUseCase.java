package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.UserInfoResponse;
import com.step.tcd_rpkb.domain.repository.AuthRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import javax.inject.Inject;

/**
 * UseCase для получения информации о пользователе по GUID
 */
public class GetUserInfoByGuidUseCase {
    
    private final AuthRepository authRepository;
    
    @Inject
    public GetUserInfoByGuidUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }
    
    /**
     * Выполняет получение информации о пользователе по GUID
     * @param userGuid GUID пользователя
     * @param callback Callback для получения результата
     */
    public void execute(String userGuid, RepositoryCallback<UserInfoResponse> callback) {
        if (userGuid == null || userGuid.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("GUID пользователя не может быть пустым"));
            return;
        }
        
        authRepository.getUserInfoByGuid(userGuid, callback);
    }
} 