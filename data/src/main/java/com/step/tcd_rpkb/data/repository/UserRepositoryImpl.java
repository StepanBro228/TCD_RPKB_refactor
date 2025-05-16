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

    // Дефолтные значения, если пользователь не сохранен (соответствуют старым значениям из UserManager)
    private static final String DEFAULT_FULL_NAME = "Гавров Константин Е.";
    private static final String DEFAULT_ROLE = "Руководитель проектов";

    private final SharedPreferences sharedPreferences;

    @Inject
    public UserRepositoryImpl(Context appContext) {
        this.sharedPreferences = appContext.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void saveUser(User user) {
        if (user == null) {
            // Можно либо очистить, либо ничего не делать
            // Давайте очистим, если передали null
            sharedPreferences.edit()
                    .remove(PREF_USER_FULL_NAME)
                    .remove(PREF_USER_ROLE)
                    .apply();
            return;
        }
        sharedPreferences.edit()
                .putString(PREF_USER_FULL_NAME, user.getFullName())
                .putString(PREF_USER_ROLE, user.getRole())
                .apply();
    }

    @Override
    public User getUser() {
        String fullName = sharedPreferences.getString(PREF_USER_FULL_NAME, null);
        String role = sharedPreferences.getString(PREF_USER_ROLE, null);

        if (fullName == null && role == null) {
            // Если ничего не сохранено, возвращаем значения по умолчанию
            // Это имитирует поведение старого UserManager, который всегда имел дефолтные значения
            // Если нужно возвращать null, если ничего не найдено, уберите эту логику.
            return new User(DEFAULT_FULL_NAME, DEFAULT_ROLE);
        }
        // Если что-то одно null, а другое нет, можно вернуть User с одним null полем или дефолтным значением для null поля.
        // Для простоты, если хотя бы одно поле сохранено, а другое нет, вернем то, что есть, 
        // а для отсутствующего поля будет null в объекте User.
        // Однако, для консистентности с дефолтными значениями, если одно null, другое тоже должно быть null или дефолтным.
        // Текущая логика: если оба null - дефолт. Если хотя бы одно не null - используем сохраненные (одно из них может быть null).
        // Чтобы всегда иметь User с не-null полями (как было в UserManager), можно сделать так:
        if (fullName == null) fullName = DEFAULT_FULL_NAME; // или оставить null, если User.fullName может быть null
        if (role == null) role = DEFAULT_ROLE;       // или оставить null, если User.role может быть null
        
        return new User(fullName, role);
    }
} 