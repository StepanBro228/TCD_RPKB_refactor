package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.AuthenticationResult;
import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.AuthRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.UserRepository;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import javax.inject.Inject;

public class AuthenticateUserUseCase {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public AuthenticateUserUseCase(UserRepository userRepository, 
                                   AuthRepository authRepository,
                                   UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    /**
     * Выполняет авторизацию пользователя (ручная авторизация)
     * @param login Логин пользователя
     * @param password Пароль пользователя
     * @param callback Callback для получения результата
     */
    public void execute(String login, String password, RepositoryCallback<AuthenticationResult> callback) {
        execute(login, password, null, callback);
    }
    
    /**
     * Выполняет авторизацию пользователя с дополнительной информацией
     * @param login Логин пользователя
     * @param password Пароль пользователя
     * @param userInfo Информация о пользователе (полученная из QR-кода), null для ручной авторизации
     * @param callback Callback для получения результата
     */
    public void execute(String login, String password, com.step.tcd_rpkb.domain.model.UserInfoResponse userInfo, RepositoryCallback<AuthenticationResult> callback) {
        authRepository.authenticateUser(login, password, new RepositoryCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                if (result.isSuccess()) {
                    User userToSave;
                    
                    if (userInfo != null) {
                        // QR-АВТОРИЗАЦИЯ
                        String userGuid = userInfo.getUserGuid();
                        
                        //  логирование
                        System.out.println("AuthenticateUserUseCase: QR-авторизация - UserInfo: name=" + userInfo.getName() + 
                                         ", fullName=" + userInfo.getFullName() + 
                                         ", userGuid=" + userGuid);
                        

                        if (userGuid == null || userGuid.trim().isEmpty()) {
                            String errorMessage = "GUID пользователя отсутствует в ответе сервера";
                            System.err.println("AuthenticateUserUseCase: " + errorMessage);
                            callback.onError(new Exception(errorMessage));
                            return;
                        }
                        
                        userToSave = new User(
                            userInfo.getFullName(),
                            "Должность", // Фиксированная должность
                            userGuid
                        );
                        

                        userSettingsRepository.saveCredentials(new Credentials(login, password, ""));
                    } else {
                        // РУЧНАЯ АВТОРИЗАЦИЯ
                        userToSave = result.getUser();
                        
                        System.out.println("AuthenticateUserUseCase: Ручная авторизация - Пользователь: name=" + userToSave.getUserGuid() + 
                                         ", fullName=" + userToSave.getFullName() + 
                                         ", role=" + userToSave.getRole());
                    }
                    
                    // Сохраняем пользователя в UserRepository
                    userRepository.saveUser(userToSave);

                    AuthenticationResult updatedResult = new AuthenticationResult(userToSave);
                    callback.onSuccess(updatedResult);
                } else {
                    // Ошибка авторизации
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * @deprecated Используйте execute(String, String, RepositoryCallback) для новой логики авторизации
     */
    @Deprecated
    public void execute(String login, String password) {

        User authenticatedUser = new User(
            "Пользователь", 
            "Роль не определена",
            login
        );
        userRepository.saveUser(authenticatedUser);
        userSettingsRepository.saveCredentials(new Credentials(login, password, ""));
    }

    public void logout() {
        userRepository.saveUser(null);
        userSettingsRepository.saveCredentials(new Credentials("", "", ""));
    }
} 