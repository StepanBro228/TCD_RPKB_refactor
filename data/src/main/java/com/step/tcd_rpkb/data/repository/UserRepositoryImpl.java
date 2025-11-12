package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.UserRepository;

import javax.inject.Inject;

public class UserRepositoryImpl implements UserRepository {

    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String PREF_USER_FULL_NAME = "user_full_name";
    private static final String PREF_USER_ROLE = "user_role";
    private static final String PREF_USER_GUID = "user_guid";

    private final SharedPreferences sharedPreferences;

    @Inject
    public UserRepositoryImpl(Context appContext) {
        this.sharedPreferences = appContext.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void saveUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (user != null) {
            editor.putString(PREF_USER_FULL_NAME, user.getFullName());
            editor.putString(PREF_USER_ROLE, user.getRole());
            editor.putString(PREF_USER_GUID, user.getUserGuid());
        } else {
            // Очистка данных пользователя при выходе
            editor.remove(PREF_USER_FULL_NAME);
            editor.remove(PREF_USER_ROLE);
            editor.remove(PREF_USER_GUID);
        }
        editor.apply();
    }

    @Override
    public User getUser() {
        String fullName = sharedPreferences.getString(PREF_USER_FULL_NAME, null);
        String role = sharedPreferences.getString(PREF_USER_ROLE, null);
        String userGuid = sharedPreferences.getString(PREF_USER_GUID, null);

        // Проверяем, есть ли полные данные авторизованного пользователя
        if (fullName != null && role != null && userGuid != null && 
            !fullName.trim().isEmpty() && !role.trim().isEmpty() && !userGuid.trim().isEmpty()) {
            // Возвращаем авторизованного пользователя
            return new User(fullName, role, userGuid);
        } else {
            // Возвращаем null если пользователь не авторизован
            return null;
        }
    }
} 