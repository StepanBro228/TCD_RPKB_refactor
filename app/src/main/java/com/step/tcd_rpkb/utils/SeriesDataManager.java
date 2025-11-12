package com.step.tcd_rpkb.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.step.tcd_rpkb.domain.model.SeriesItem;
import com.step.tcd_rpkb.domain.model.Product;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Менеджер для управления кешированными данными серий товаров
 */
public class SeriesDataManager {
    
    private static final String TAG = "SeriesDataManager";
    private static final String SERIES_DIR_NAME = "series_cache";
    private static final String SERIES_FILE_PREFIX = "series_";
    private static final String FILE_EXTENSION = ".json";
    
    private final Context context;
    private final Gson gson;
    private final File seriesDir;
    
    // Кеш в памяти для быстрого доступа
    private final Map<String, List<SeriesItem>> memoryCache = new HashMap<>();
    
    public SeriesDataManager(Context context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping() // Отключаем HTML экранирование для корректной работы с кириллицей
                .setPrettyPrinting()
                .serializeNulls() // Сериализуем null значения
                .create();
        
        // Создаем папку для кеша серий
        this.seriesDir = new File(context.getCacheDir(), SERIES_DIR_NAME);
        if (!seriesDir.exists()) {
            boolean created = seriesDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Не удалось создать директорию для кеша серий");
            }
        }
    }
    
    /**
     * Сохраняет данные серий для номенклатуры
     */
    public boolean saveSeriesData(String nomenclatureUuid, List<SeriesItem> seriesItems) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty() || seriesItems == null) {
            Log.e(TAG, "Некорректные параметры для сохранения данных серий");
            return false;
        }
        
        try {
            File file = getSeriesFile(nomenclatureUuid);
            
            // Сохраняем в память
            memoryCache.put(nomenclatureUuid, new ArrayList<>(seriesItems));
            
            // Сохраняем в файл
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(seriesItems, writer);
                Log.d(TAG, "Данные серий сохранены для номенклатуры: " + nomenclatureUuid + 
                           ", количество серий: " + seriesItems.size());
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сохранения данных серий для номенклатуры " + nomenclatureUuid, e);
            return false;
        }
    }
    
    /**
     * Загружает данные серий для номенклатуры
     */
    public List<SeriesItem> loadSeriesData(String nomenclatureUuid) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            Log.e(TAG, "Некорректный UUID номенклатуры");
            return new ArrayList<>();
        }
        
        // Сначала проверяем кеш в памяти
        if (memoryCache.containsKey(nomenclatureUuid)) {
            Log.d(TAG, "Данные серий найдены в кеше памяти для номенклатуры: " + nomenclatureUuid);
            return new ArrayList<>(memoryCache.get(nomenclatureUuid));
        }
        
        // Загружаем из файла
        File file = getSeriesFile(nomenclatureUuid);
        if (!file.exists()) {
            Log.d(TAG, "Файл с данными серий не найден для номенклатуры: " + nomenclatureUuid);
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<SeriesItem>>() {}.getType();
            List<SeriesItem> seriesItems = gson.fromJson(reader, listType);
            
            if (seriesItems == null) {
                seriesItems = new ArrayList<>();
            }
            
            // Сохраняем в кеш памяти
            memoryCache.put(nomenclatureUuid, new ArrayList<>(seriesItems));
            
            Log.d(TAG, "Данные серий загружены из файла для номенклатуры: " + nomenclatureUuid + 
                       ", количество серий: " + seriesItems.size());
            return seriesItems;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки данных серий для номенклатуры " + nomenclatureUuid, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Проверяет, есть ли кешированные данные для номенклатуры
     */
    public boolean hasSeriesData(String nomenclatureUuid) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            return false;
        }
        
        // Проверяем кеш в памяти
        if (memoryCache.containsKey(nomenclatureUuid)) {
            return true;
        }
        
        // Проверяем наличие файла
        File file = getSeriesFile(nomenclatureUuid);
        return file.exists();
    }
    
    /**
     * Обновляет свободный остаток для серии
     */
    public boolean updateSeriesFreeBalance(String nomenclatureUuid, String seriesUuid, double newFreeBalance) {
        List<SeriesItem> seriesItems = loadSeriesData(nomenclatureUuid);
        
        boolean updated = false;
        for (SeriesItem item : seriesItems) {
            if (seriesUuid.equals(item.getSeriesUuid())) {
                item.setFreeBalance(newFreeBalance);
                updated = true;
                Log.d(TAG, "Обновлен свободный остаток для серии " + seriesUuid + ": " + newFreeBalance);
                break;
            }
        }
        
        if (updated) {
            return saveSeriesData(nomenclatureUuid, seriesItems);
        }
        
        Log.w(TAG, "Серия не найдена для обновления: " + seriesUuid);
        return false;
    }
    
    /**
     * Перемещает количество между сериями (замена серии)
     */
    public boolean transferQuantityBetweenSeries(String nomenclatureUuid, 
                                                String fromSeriesUuid, 
                                                String toSeriesUuid, 
                                                double quantity) {
        List<SeriesItem> seriesItems = loadSeriesData(nomenclatureUuid);
        
        SeriesItem fromSeries = null;
        SeriesItem toSeries = null;
        
        // Находим обе серии
        for (SeriesItem item : seriesItems) {
            if (fromSeriesUuid.equals(item.getSeriesUuid())) {
                fromSeries = item;
            } else if (toSeriesUuid.equals(item.getSeriesUuid())) {
                toSeries = item;
            }
        }
        
        if (fromSeries == null || toSeries == null) {
            Log.e(TAG, "Не найдены серии для перемещения количества. From: " + fromSeriesUuid + ", To: " + toSeriesUuid);
            return false;
        }
        
        // Проверяем, что у целевой серии достаточно свободного остатка
        if (toSeries.getFreeBalance() < quantity) {
            Log.e(TAG, "Недостаточно свободного остатка в целевой серии. Требуется: " + quantity + 
                       ", доступно: " + toSeries.getFreeBalance());
            return false;
        }
        
        // Перемещаем количество
        fromSeries.setFreeBalance(fromSeries.getFreeBalance() + quantity);
        toSeries.setFreeBalance(toSeries.getFreeBalance() - quantity);
        
        Log.d(TAG, "Перемещено количество " + quantity + " из серии " + fromSeriesUuid + 
                   " в серию " + toSeriesUuid);
        
        return saveSeriesData(nomenclatureUuid, seriesItems);
    }

    
    /**
     * Получает серию по UUID
     */
    public SeriesItem getSeriesByUuid(String nomenclatureUuid, String seriesUuid) {
        List<SeriesItem> seriesItems = loadSeriesData(nomenclatureUuid);
        
        for (SeriesItem item : seriesItems) {
            if (seriesUuid.equals(item.getSeriesUuid())) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * Удаляет данные серий для номенклатуры
     */
    public boolean deleteSeriesData(String nomenclatureUuid) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            return false;
        }
        

        memoryCache.remove(nomenclatureUuid);
        
        // Удаляем файл
        File file = getSeriesFile(nomenclatureUuid);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Данные серий удалены для номенклатуры: " + nomenclatureUuid);
            } else {
                Log.e(TAG, "Не удалось удалить файл данных серий для номенклатуры: " + nomenclatureUuid);
            }
            return deleted;
        }
        
        return true;
    }

    
    /**
     * Очищает кеш серий при изменении статуса перемещения
     */
    public void clearSeriesCacheForMove(String moveUuid) {
        if (moveUuid == null || moveUuid.isEmpty()) {
            return;
        }
        
        // Загружаем продукты данного перемещения чтобы узнать какие номенклатуры были использованы
        ProductsDataManager productsDataManager = new ProductsDataManager(context);
        List<Product> products = productsDataManager.loadProductsData(moveUuid);
        
        // Собираем уникальные UUID номенклатур из продуктов
        Set<String> nomenclatureUuids = new HashSet<>();
        for (Product product : products) {
            if (product.getNomenclatureUuid() != null) {
                nomenclatureUuids.add(product.getNomenclatureUuid());
            }
        }
        
        // Удаляем файлы серий только для этих номенклатур
        int deletedCount = 0;
        for (String nomenclatureUuid : nomenclatureUuids) {
            boolean deleted = deleteSeriesData(nomenclatureUuid);
                        if (deleted) {
                deletedCount++;
                Log.d(TAG, "Удален кеш серий для номенклатуры: " + nomenclatureUuid);
            }
        }
        
        // Очищаем из кеша в памяти только серии для этих номенклатур
        for (String nomenclatureUuid : nomenclatureUuids) {
            memoryCache.remove(nomenclatureUuid);
        }
        
        Log.d(TAG, "Кеш серий очищен для перемещения: " + moveUuid + 
                   ", удалено файлов серий: " + deletedCount + " из " + nomenclatureUuids.size() + " номенклатур");
    }

    private File getSeriesFile(String nomenclatureUuid) {
        String fileName = SERIES_FILE_PREFIX + nomenclatureUuid + FILE_EXTENSION;
        return new File(seriesDir, fileName);
    }
} 