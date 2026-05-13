package com.step.tcd_rpkb.data.repository;

import android.util.Log;
import com.google.gson.GsonBuilder;

import com.step.tcd_rpkb.data.mapper.UserInfoMapper;
import com.step.tcd_rpkb.data.network.AuthApiService;
import com.step.tcd_rpkb.data.network.UserAuthApiService;
import com.step.tcd_rpkb.data.network.UserAuthInterceptor;
import com.step.tcd_rpkb.data.network.dto.UserInfoResponseDto;
import com.step.tcd_rpkb.domain.model.AuthenticationResult;
import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.model.UserInfoResponse;
import com.step.tcd_rpkb.domain.repository.AuthRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Реализация AuthRepository для работы с авторизацией
 */
public class AuthRepositoryImpl implements AuthRepository {
    
    private static final String TAG = "AuthRepositoryImpl";
    
    private final AuthApiService authApiService;
    private final UserAuthApiService userAuthApiService;
    private final UserAuthInterceptor userAuthInterceptor;
    private final UserSettingsRepository userSettingsRepository;
    
    @Inject
    public AuthRepositoryImpl(AuthApiService authApiService, 
                             UserAuthApiService userAuthApiService,
                             UserAuthInterceptor userAuthInterceptor,
                             UserSettingsRepository userSettingsRepository) {
        this.authApiService = authApiService;
        this.userAuthApiService = userAuthApiService;
        this.userAuthInterceptor = userAuthInterceptor;
        this.userSettingsRepository = userSettingsRepository;
    }
    
    @Override
    public void getUserInfoByGuid(String userGuid, RepositoryCallback<UserInfoResponse> callback) {
        Log.d(TAG, "Запрос информации о пользователе с GUID: " + userGuid);
        

        Call<UserInfoResponseDto> call = authApiService.getUserInfoByGuid(userGuid);
        call.enqueue(new Callback<UserInfoResponseDto>() {
            @Override
            public void onResponse(Call<UserInfoResponseDto> call, Response<UserInfoResponseDto> response) {
                UserInfoResponseDto dto = response.body();
                String rawErrorBody = null;
                
                // Если response.body() пустой, пытаемся получить JSON из errorBody
                if (dto == null && response.errorBody() != null) {
                    try {
                        rawErrorBody = response.errorBody().string();
                        Log.d(TAG, "getUserInfoByGuid: попытка парсинга JSON из errorBody: " + rawErrorBody);
                        com.google.gson.Gson gson = new GsonBuilder()
                                .disableHtmlEscaping()
                                .create();
                        dto = gson.fromJson(rawErrorBody, UserInfoResponseDto.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге errorBody как JSON в getUserInfoByGuid", e);
                    }
                }
                
                // Обрабатываем DTO
                if (dto != null) {
                    processUserInfoResponseDto(dto, callback);
                } else {
                    // Если DTO не получен
                    String errorMessage = "Пользователь с указанным GUID не найден, Code: " + response.code();
                    if (rawErrorBody != null && !rawErrorBody.isEmpty()) {
                        errorMessage += ", Body: " + rawErrorBody;
                    } else if (response.errorBody() != null) {
                        try {
                            String fallbackErrorBody = response.errorBody().string();
                            if (!fallbackErrorBody.isEmpty()) {
                                errorMessage += ", Body: " + fallbackErrorBody;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка чтения errorBody для getUserInfoByGuid (fallback)", e);
                        }
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(new Exception(errorMessage));
                }
            }
            
            @Override
            public void onFailure(Call<UserInfoResponseDto> call, Throwable t) {
                String errorMessage = "Ошибка связи с сервером: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                callback.onError(new Exception(errorMessage));
            }
        });
    }
    
    @Override
    public void authenticateUser(String login, String password, RepositoryCallback<AuthenticationResult> callback) {
        Log.d(TAG, "Начало двухэтапной авторизации пользователя: " + login);
        
        // Проверяем логин через новый эндпоинт
        Call<UserInfoResponseDto> loginCheckCall = authApiService.checkUserLogin(login);
        loginCheckCall.enqueue(new Callback<UserInfoResponseDto>() {
            @Override
            public void onResponse(Call<UserInfoResponseDto> call, Response<UserInfoResponseDto> response) {
                UserInfoResponseDto dto = response.body();
                String rawErrorBody = null;
                
                // Если response.body() пустой, пытаемся получить JSON из errorBody
                if (dto == null && response.errorBody() != null) {
                    try {
                        rawErrorBody = response.errorBody().string();
                        Log.d(TAG, "checkUserLogin: попытка парсинга JSON из errorBody: " + rawErrorBody);
                        com.google.gson.Gson gson = new GsonBuilder()
                                .disableHtmlEscaping()
                                .create();
                        dto = gson.fromJson(rawErrorBody, UserInfoResponseDto.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге errorBody как JSON в checkUserLogin", e);
                    }
                }
                
                // Обрабатываем DTO если получен
                if (dto != null) {
//                    if (dto.hasError()) {
//                        String errorMessage = dto.getError();
//                        Log.w(TAG, "Сервер вернул ошибку: " + errorMessage);
//                        callback.onSuccess(new AuthenticationResult("Пользователь не найден"));
//                        return;
//                    }
                    
                    // Проверяем поле "Результат"
//                    if (!dto.isResult()) {
//                        String errorText = dto.getErrorText();
//                        if (errorText == null || errorText.trim().isEmpty()) {
//                            errorText = "Неверный логин";
//                        }
//                        Log.w(TAG, "Логин не найден или ошибка: " + errorText);
//                        callback.onSuccess(new AuthenticationResult(errorText));
//                        return;
//                    }
                    
                    // Проверяем, что получены обязательные данные пользователя
                    if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                        Log.w(TAG, "Получены некорректные данные пользователя: name=" + dto.getName() + ", fullName=" + dto.getFullName());
                        callback.onSuccess(new AuthenticationResult("Пользователь не найден"));
                        return;
                    }
                    

                    Log.d(TAG, "Логин найден, получена информация: name=" + dto.getName() + ", fullName=" + dto.getFullName());
                    
                    //Проверяем пароль через hs/rpkb_autorization с пользовательскими учетными данными
                    checkPasswordWithAuthorization(login, password, dto, callback);
                } else {
                    // Если DTO не получен, считаем логин неверным
                    Log.w(TAG, "Логин не найден: " + login + ", код ответа: " + response.code());
                    callback.onSuccess(new AuthenticationResult("Неверный логин"));
                }
            }
            
            @Override
            public void onFailure(Call<UserInfoResponseDto> call, Throwable t) {
                String errorMessage = "Ошибка связи с сервером при проверке логина: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                callback.onError(new Exception(errorMessage));
            }
        });
    }
    
    /**
     * Проверяет пароль пользователя через эндпоинт hs/rpkb_autorization с пользовательскими учетными данными
     * @param login Логин пользователя
     * @param password Пароль пользователя  
     * @param userDto Информация о пользователе, полученная на первом этапе
     * @param callback Callback для результата авторизации
     */
    private void checkPasswordWithAuthorization(String login, String password, UserInfoResponseDto userDto, RepositoryCallback<AuthenticationResult> callback) {
        Log.d(TAG, "ЭТАП 2: Проверка пароля через hs/rpkb_autorization для пользователя: " + login);
        
        // Устанавливаем пользовательские учетные данные в интерсептор
        userAuthInterceptor.setCredentials(login, password);
        
        long startTime = System.currentTimeMillis();
        
        // Используем запрос hs/rpkb_autorization с пользовательскими учетными данными
        Call<UserInfoResponseDto> call = userAuthApiService.checkUserPassword(login);
        call.enqueue(new Callback<UserInfoResponseDto>() {
            @Override
            public void onResponse(Call<UserInfoResponseDto> call, Response<UserInfoResponseDto> response) {
                long duration = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Запрос проверки пароля выполнен за " + duration + " мс");
                
                UserInfoResponseDto dto = response.body();
                String rawErrorBody = null;
                
                // Если response.body() пустой, пытаемся получить JSON из errorBody
                if (dto == null && response.errorBody() != null) {
                    try {
                        rawErrorBody = response.errorBody().string();
                        Log.d(TAG, "checkUserPassword: попытка парсинга JSON из errorBody: " + rawErrorBody);
                        com.google.gson.Gson gson = new GsonBuilder()
                                .disableHtmlEscaping()
                                .create();
                        dto = gson.fromJson(rawErrorBody, UserInfoResponseDto.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге errorBody как JSON в checkUserPassword", e);
                    }
                }
                
                // Обрабатываем DTO если получен
                if (dto != null) {
//                    if (dto.hasError()) {
//                        String errorMessage = dto.getError();
//                        Log.w(TAG, "Сервер вернул ошибку при проверке пароля: " + errorMessage);
//
//
//                        userAuthInterceptor.clearCredentials();
//                        userSettingsRepository.saveCredentials(new Credentials("", "", ""));
//
//                        callback.onSuccess(new AuthenticationResult("Неверный пароль"));
//                        return;
//                    }
                    

//                    if (!dto.isResult()) {
//                        String errorText = dto.getErrorText();
//                        if (errorText == null || errorText.trim().isEmpty()) {
//                            errorText = "Неверный пароль";
//                        }
//
//                        // Пароль неверный - очищаем данные
//                        userAuthInterceptor.clearCredentials();
//                        userSettingsRepository.saveCredentials(new Credentials("", "", ""));
//
//                        Log.w(TAG, "Неверный пароль или ошибка (время: " + duration + " мс): " + errorText);
//                        callback.onSuccess(new AuthenticationResult(errorText));
//                        return;
//                    }
                    
                    // Пароль верный - успешная авторизация
                    Log.d(TAG, "Успешная авторизация пользователя (время: " + duration + " мс)");
                    
                    // Сохраняем учетные данные
                    Credentials credentials = new Credentials(login, password, "");
                    userSettingsRepository.saveCredentials(credentials);
                    
                    // Создаем пользователя с полной информацией
                    User user = new User(
                        userDto.getFullName() != null ? userDto.getFullName() : "Авторизованный пользователь",
                        "Пользователь", // Роль по умолчанию, можно расширить в будущем
                        userDto.getGuid() != null ? userDto.getGuid() : null // Используем GUID из первого запроса
                    );
                    
                    AuthenticationResult result = new AuthenticationResult(user);
                    Log.d(TAG, "Успешная авторизация пользователя: " + user.getFullName() + " (" + user.getUserGuid() + ")");
                    callback.onSuccess(result);
                } else {
                    // Если DTO не получен, считаем пароль неверным
                    userAuthInterceptor.clearCredentials();
                    userSettingsRepository.saveCredentials(new Credentials("", "", ""));
                    
                    Log.w(TAG, "Неверный пароль (время: " + duration + " мс), код: " + response.code());
                    callback.onSuccess(new AuthenticationResult("Неверный пароль"));
                }
            }
            
            @Override
            public void onFailure(Call<UserInfoResponseDto> call, Throwable t) {
                // Неуспешная авторизация - очищаем данные
                userAuthInterceptor.clearCredentials();
                userSettingsRepository.saveCredentials(new Credentials("", "", ""));
                
                String errorMessage = "Ошибка связи с сервером при проверке пароля: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                callback.onError(new Exception(errorMessage));
            }
        });
    }
    
    /**
     * Обрабатывает DTO ответа получения информации о пользователе
     */
    private void processUserInfoResponseDto(UserInfoResponseDto dto, RepositoryCallback<UserInfoResponse> callback) {
//        if (dto.hasError()) {
//            String errorMessage = dto.getError();
//            Log.e(TAG, "getUserInfoByGuid: Сервер вернул ошибку: " + errorMessage);
//            callback.onError(new com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException("Пользователь с указанным QR-кодом не найден", com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException.ErrorType.USER_INFO));
//            return;
//        }
//
//        // Проверяем поле "Результат"
//        if (!dto.isResult()) {
//
//            String errorText = dto.getErrorText();
//            if (errorText == null || errorText.trim().isEmpty()) {
//                errorText = "Пользователь с указанным GUID не найден";
//            }
//            Log.e(TAG, "getUserInfoByGuid: Результат false, ТекстОшибки: " + errorText);
//            callback.onError(new com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException(errorText, com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException.ErrorType.USER_INFO));
//            return;
//        }
        

        try {
            Log.d(TAG, "Получен DTO от сервера: name=" + dto.getName() + 
                  ", fullName=" + dto.getFullName() + 
                  ", guid=" + dto.getGuid());
            

            UserInfoResponse userInfo = UserInfoMapper.mapToDomain(dto);
            Log.d(TAG, "Маппинг завершен: name=" + userInfo.getName() + 
                  ", fullName=" + userInfo.getFullName() + 
                  ", userGuid=" + userInfo.getUserGuid());
            
            // Проверяем GUID
            if (userInfo.getUserGuid() == null || userInfo.getUserGuid().trim().isEmpty()) {
                String errorMessage = "GUID пользователя отсутствует в ответе сервера";
                Log.e(TAG, errorMessage);
                callback.onError(new Exception(errorMessage));
                return;
            }
            
            callback.onSuccess(userInfo);
        } catch (Exception e) {
            Log.e(TAG, "getUserInfoByGuid: Ошибка при обработке данных: " + e.getMessage());
            callback.onError(new Exception("Ошибка при обработке информации о пользователе: " + e.getMessage(), e));
        }
    }
} 