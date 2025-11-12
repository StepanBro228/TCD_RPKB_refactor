package com.step.tcd_rpkb.utils;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Менеджер для работы с фильтрами пользователей по умолчанию
 * Сохраняет фильтры в файл на устройстве и загружает их при необходимости
 */
public class DefaultFiltersManager {

    private static final String TAG = "DefaultFiltersManager";
    private static final String DEFAULT_FILTERS_FILE_NAME = "default_filters.json";


    private final Gson gson;
    private final File defaultFiltersFile;

    public DefaultFiltersManager(Context context) {
        this.gson = new GsonBuilder()
                .disableHtmlEscaping() // Отключаем HTML экранирование для корректной работы с кириллицей
                .serializeNulls() // Сериализуем null значения
                .create();
        this.defaultFiltersFile = new File(context.getFilesDir(), DEFAULT_FILTERS_FILE_NAME);

        Log.d(TAG, "DefaultFiltersManager создан, файл: " + defaultFiltersFile.getAbsolutePath());
    }

    /**
     * Сохраняет фильтры по умолчанию для пользователя
     *
     * @param userGuid    GUID пользователя
     * @param userName    имя пользователя
     * @param filtersData данные фильтров для сохранения
     * @return true если сохранение прошло успешно
     */
    public boolean saveDefaultFilters(String userGuid, String userName, DefaultFiltersData filtersData) {
        if (userGuid == null || userGuid.trim().isEmpty()) {
            Log.e(TAG, "Не удается сохранить фильтры: userGuid пустой");
            return false;
        }

        try {
            // Загружаем существующие фильтры
            List<DefaultFiltersData> allFilters = loadAllDefaultFilters();

            // Устанавливаем данные пользователя
            filtersData.setUserGuid(userGuid);
            filtersData.setUserName(userName);

            // Ищем существующую запись для этого пользователя и удаляем её
            Iterator<DefaultFiltersData> iterator = allFilters.iterator();
            while (iterator.hasNext()) {
                DefaultFiltersData existingData = iterator.next();
                if (userGuid.equals(existingData.getUserGuid())) {
                    iterator.remove();
                    Log.d(TAG, "Удалена существующая запись фильтров для пользователя: " + userName);
                    break;
                }
            }

            // Добавляем новую запись
            allFilters.add(filtersData);

            // Сохраняем в файл
            String json = gson.toJson(allFilters);
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new FileOutputStream(defaultFiltersFile), StandardCharsets.UTF_8)) {
                writer.write(json);
                writer.flush();
            }

            Log.d(TAG, "Фильтры по умолчанию сохранены для пользователя: " + userName +
                    " (GUID: " + userGuid + "), общее количество пользователей: " + allFilters.size());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении фильтров по умолчанию", e);
            return false;
        }
    }

    /**
     * Загружает фильтры по умолчанию для указанного пользователя
     *
     * @param userGuid GUID пользователя
     * @return фильтры пользователя или null если не найдены
     */
    public DefaultFiltersData loadDefaultFiltersForUser(String userGuid) {
        if (userGuid == null || userGuid.trim().isEmpty()) {
            Log.d(TAG, "Не удается загрузить фильтры: userGuid пустой");
            return null;
        }

        try {
            List<DefaultFiltersData> allFilters = loadAllDefaultFilters();

            for (DefaultFiltersData filtersData : allFilters) {
                if (userGuid.equals(filtersData.getUserGuid())) {
                    Log.d(TAG, "Найдены фильтры по умолчанию для пользователя: " + filtersData.getUserName() +
                            " (GUID: " + userGuid + ")");
                    return filtersData;
                }
            }

            Log.d(TAG, "Фильтры по умолчанию не найдены для пользователя с GUID: " + userGuid);
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке фильтров по умолчанию для пользователя: " + userGuid, e);
            return null;
        }
    }


    /**
     * Загружает все сохраненные фильтры всех пользователей
     *
     * @return список всех фильтров
     */
    private List<DefaultFiltersData> loadAllDefaultFilters() {
        List<DefaultFiltersData> allFilters = new ArrayList<>();

        if (!defaultFiltersFile.exists()) {
            Log.d(TAG, "Файл с фильтрами по умолчанию не существует: " + defaultFiltersFile.getAbsolutePath());
            return allFilters;
        }

        try (java.io.InputStreamReader reader = new java.io.InputStreamReader(
                new FileInputStream(defaultFiltersFile), StandardCharsets.UTF_8)) {

            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
            String json = sb.toString();

            Type listType = new TypeToken<List<DefaultFiltersData>>() {
            }.getType();
            List<DefaultFiltersData> loadedFilters = gson.fromJson(json, listType);

            if (loadedFilters != null) {
                allFilters = loadedFilters;
                Log.d(TAG, "Загружено " + allFilters.size() + " записей фильтров по умолчанию");
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке всех фильтров по умолчанию", e);
        }

        return allFilters;
    }
}