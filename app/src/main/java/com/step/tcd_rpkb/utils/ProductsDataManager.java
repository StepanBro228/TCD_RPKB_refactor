package com.step.tcd_rpkb.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.data.network.dto.ProductDto;
import com.step.tcd_rpkb.data.mapper.ProductMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Управляет временными данными продуктов для передачи между активностями
 */
public class ProductsDataManager {
    
    private static final String TAG = "ProductsDataManager";
    private static final String PRODUCTS_FILE_PREFIX = "products_data_";
    private static final String FILE_EXTENSION = ".json";
    
    private final Context context;
    private final Gson gson;
    private final ProductMapper productMapper;
    
    public ProductsDataManager(Context context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .disableHtmlEscaping() // Отключаем HTML экранирование для корректной работы с кириллицей
                .serializeNulls() // Сериализуем null значения
                .setLenient() // Позволяем более гибкий парсинг JSON
                .create();
        this.productMapper = new ProductMapper();
    }
    
    /**
     * Сохраняет список продуктов во временный файл
     * @param nomenclatureUuid UUID номенклатуры для создания уникального имени файла
     * @param products список продуктов для сохранения
     * @return true если сохранение прошло успешно
     */
    public boolean saveProductsData(String nomenclatureUuid, List<Product> products) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty() || products == null) {
            Log.w(TAG, "Не удается сохранить данные продуктов: некорректные параметры");
            return false;
        }
        
        try {
            String fileName = PRODUCTS_FILE_PREFIX + nomenclatureUuid + FILE_EXTENSION;
            File tempFile = new File(context.getCacheDir(), fileName);
            
            // Преобразуем Product в ProductDto для правильной сериализации с аннотациями @SerializedName
            List<ProductDto> productDtos = productMapper.mapToDtoList(products);
            String jsonData = gson.toJson(productDtos);
            

            if (!validateJsonData(jsonData)) {
                Log.e(TAG, "Ошибка: JSON не прошел проверку целостности, сохранение отменено");
                return false;
            }
            

            if (jsonData.contains("\uFFFD")) {
                Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: JSON содержит некорректные UTF-8 символы!");
                return false;
            }
            
            // Детальное логирование сохраняемого JSON
            Log.d(TAG, "=== СОХРАНЕНИЕ ПРОДУКТОВ ===");
            Log.d(TAG, "Файл: " + fileName);
            Log.d(TAG, "UUID: " + nomenclatureUuid);
            Log.d(TAG, "Количество продуктов: " + products.size());
            Log.d(TAG, "Размер JSON: " + jsonData.length() + " символов");
            
            // Логируем краткую информацию о каждом продукте ПЕРЕД маппингом
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                Log.d(TAG, "Продукт #" + (i + 1) + " ПЕРЕД маппингом: " +
                          "ID=" + product.getProductLineId() +
                          ", parentID=" + product.getParentProductLineId() +
                          ", nomenclature=" + product.getNomenclatureName() +
                          ", series=" + product.getSeriesName() +
                          ", quantity=" + product.getQuantity() +
                          ", taken=" + product.getTaken() +
                          ", exists=" + product.getExists());
            }
            
            // Логируем краткую информацию о каждом DTO ПОСЛЕ маппинга
            for (int i = 0; i < productDtos.size(); i++) {
                ProductDto dto = productDtos.get(i);
                Log.d(TAG, "DTO #" + (i + 1) + " ПОСЛЕ маппинга: " +
                          "ID=" + dto.getProductLineId() +
                          ", parentID=" + dto.getParentProductLineId() +
                          ", nomenclature=" + dto.getNomenclatureName() +
                          ", series=" + dto.getSeriesName() +
                          ", quantity=" + dto.getQuantity() +
                          ", taken=" + dto.getTaken() +
                          ", exists=" + dto.getExists());
            }
            
            // Логируем полный JSON
            if (jsonData.length() < 5000) { // Если JSON небольшой
                Log.d(TAG, "Полный JSON: " + jsonData);
            } else { // Если JSON большой, логируем частично
                Log.d(TAG, "JSON (первые 1000 символов): " + jsonData.substring(0, 1000) + "...");
                Log.d(TAG, "JSON (последние 500 символов): ..." + jsonData.substring(jsonData.length() - 500));
            }
            

            writeUtf8File(tempFile, jsonData);
            

            try {
                String verificationData = readUtf8File(tempFile);
                if (!validateJsonData(verificationData)) {
                    Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Данные повреждены после записи в файл!");

                    writeUtf8File(tempFile, jsonData);
                    

                    String secondVerification = readUtf8File(tempFile);
                    if (!validateJsonData(secondVerification)) {
                        Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: Повторная запись также привела к повреждению данных!");
                        return false;
                    } else {
                        Log.w(TAG, "Повторная запись исправила проблему с данными");
                    }
                } else {
                    Log.d(TAG, "Проверка целостности после записи: УСПЕШНО");
                }
            } catch (IOException e) {
                Log.w(TAG, "Не удалось выполнить проверку целостности после записи: " + e.getMessage());

            }
            
            Log.d(TAG, "Данные продуктов успешно сохранены в файл: " + fileName);
            Log.d(TAG, "=== КОНЕЦ СОХРАНЕНИЯ ПРОДУКТОВ ===");
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при сохранении данных продуктов", e);
            return false;
        }
    }
    
    /**
     * Загружает список продуктов из временного файла
     * @param nomenclatureUuid UUID номенклатуры для поиска файла
     * @return список продуктов или пустой список при ошибке
     */
    public List<Product> loadProductsData(String nomenclatureUuid) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            Log.w(TAG, "Не удается загрузить данные продуктов: некорректный UUID номенклатуры");
            return new ArrayList<>();
        }
        
        try {
            String fileName = PRODUCTS_FILE_PREFIX + nomenclatureUuid + FILE_EXTENSION;
            File tempFile = new File(context.getCacheDir(), fileName);
            
            if (!tempFile.exists()) {
                Log.w(TAG, "Файл с данными продуктов не найден: " + fileName);
                return new ArrayList<>();
            }
            

            String jsonData = readUtf8File(tempFile);
            

            Type listType = new TypeToken<List<ProductDto>>(){}.getType();
            List<ProductDto> productDtos = gson.fromJson(jsonData, listType);
            

            if (!validateJsonData(jsonData)) {
                Log.e(TAG, "Ошибка: Загруженный JSON поврежден, возвращаем пустой список");
                return new ArrayList<>();
            }
            
            // Преобразуем ProductDto в Product через маппер
            List<Product> products = new ArrayList<>();
            if (productDtos != null) {
                for (ProductDto dto : productDtos) {
                    Product product = productMapper.mapToDomain(dto);
                    if (product != null) {
                        products.add(product);
                    }
                }
            }
            
            // логирование
            Log.d(TAG, "=== ЗАГРУЗКА ПРОДУКТОВ ===");
            Log.d(TAG, "Файл: " + fileName);
            Log.d(TAG, "UUID: " + nomenclatureUuid);
            Log.d(TAG, "Размер JSON: " + jsonData.length() + " символов");
            Log.d(TAG, "Количество загруженных продуктов: " + products.size());
            
            // Логируем краткую информацию о каждом загруженном DTO ПЕРЕД маппингом
            if (productDtos != null) {
                for (int i = 0; i < productDtos.size(); i++) {
                    ProductDto dto = productDtos.get(i);
                    Log.d(TAG, "Загруженный DTO #" + (i + 1) + " ПЕРЕД маппингом: " +
                              "ID=" + dto.getProductLineId() +
                              ", parentID=" + dto.getParentProductLineId() +
                              ", nomenclature=" + dto.getNomenclatureName() +
                              ", series=" + dto.getSeriesName() +
                              ", quantity=" + dto.getQuantity() +
                              ", taken=" + dto.getTaken() +
                              ", exists=" + dto.getExists());
                }
            }
            
            // Логируем краткую информацию о каждом загруженном продукте ПОСЛЕ маппинга
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                Log.d(TAG, "Загруженный продукт #" + (i + 1) + " ПОСЛЕ маппинга: " +
                          "ID=" + product.getProductLineId() +
                          ", parentID=" + product.getParentProductLineId() +
                          ", nomenclature=" + product.getNomenclatureName() +
                          ", series=" + product.getSeriesName() +
                          ", quantity=" + product.getQuantity() +
                          ", taken=" + product.getTaken() +
                          ", exists=" + product.getExists());
            }
            
            // Логируем загруженный JSON (с ограничением по размеру)
            if (jsonData.length() < 5000) { // Если JSON небольшой
                Log.d(TAG, "Загруженный JSON: " + jsonData);
            } else { //
                Log.d(TAG, "Загруженный JSON (первые 1000 символов): " + jsonData.substring(0, 1000) + "...");
                Log.d(TAG, "Загруженный JSON (последние 500 символов): ..." + jsonData.substring(jsonData.length() - 500));
            }
            
            Log.d(TAG, "=== КОНЕЦ ЗАГРУЗКИ ПРОДУКТОВ ===");
            return products;
            
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при загрузке данных продуктов", e);
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при парсинге данных продуктов", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Удаляет временный файл с данными продуктов
     * @param nomenclatureUuid UUID номенклатуры для поиска файла
     * @return true если файл был удален или не существовал
     */
    public boolean deleteProductsData(String nomenclatureUuid) {
        if (nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            return false;
        }
        
        try {
            String fileName = PRODUCTS_FILE_PREFIX + nomenclatureUuid + FILE_EXTENSION;
            File tempFile = new File(context.getCacheDir(), fileName);
            
            if (!tempFile.exists()) {
                return true;
            }
            
            boolean deleted = tempFile.delete();
            Log.d(TAG, "Временный файл с данными продуктов " + 
                      (deleted ? "удален" : "не удален") + ": " + fileName);
            return deleted;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении временного файла с данными продуктов", e);
            return false;
        }
    }
    
    /**
     * Проверяет целостность UTF-8 строки
     * @param text строка для проверки
     * @param fieldName имя поля для логирования
     * @return true если строка корректна
     */
    private boolean validateUtf8String(String text, String fieldName) {
        if (text == null) {
            return true;
        }
        
        // Проверяем на наличие некорректных символов
        if (text.contains("\uFFFD")) {
            Log.w(TAG, "validateUtf8String: Найден некорректный UTF-8 символ в поле " + fieldName + ": " + text);
            return false;
        }
        
        // Проверяем на аномальные пробелы в кириллических словах
        if (text.matches(".*[А-Яа-я]\\s{2,}[А-Яа-я].*")) {
            Log.w(TAG, "validateUtf8String: Найдены аномальные пробелы в кириллическом тексте в поле " + fieldName + ": " + text);
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет целостность JSON данных путем попытки десериализации
     * @param jsonData JSON строка для проверки
     * @return true если JSON корректен
     */
    private boolean validateJsonData(String jsonData) {
        try {
            Type listType = new TypeToken<List<ProductDto>>(){}.getType();
            List<ProductDto> testList = gson.fromJson(jsonData, listType);
            
            if (testList == null) {
                Log.e(TAG, "validateJsonData: Десериализация вернула null");
                return false;
            }
            
            // Проверяем что все важные поля не null и UTF-8 строки корректны
            for (ProductDto dto : testList) {
                if (dto.getProductLineId() == null || dto.getProductLineId().isEmpty()) {
                    Log.e(TAG, "validateJsonData: Найден DTO с пустым productLineId");
                    return false;
                }
                
                // Проверяем целостность UTF-8 строк
                if (!validateUtf8String(dto.getNomenclatureName(), "nomenclatureName")) {
                    return false;
                }
                if (!validateUtf8String(dto.getSeriesName(), "seriesName")) {
                    return false;
                }
                if (!validateUtf8String(dto.getSenderStorageName(), "senderStorageName")) {
                    return false;
                }
                if (!validateUtf8String(dto.getReceiverStorageName(), "receiverStorageName")) {
                    return false;
                }
                if (!validateUtf8String(dto.getResponsibleReceiverName(), "responsibleReceiverName")) {
                    return false;
                }
                if (!validateUtf8String(dto.getReserveDocumentName(), "reserveDocumentName")) {
                    return false;
                }
            }
            
            Log.d(TAG, "validateJsonData: JSON прошел проверку целостности, количество объектов: " + testList.size());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "validateJsonData: Ошибка при проверке JSON", e);
            return false;
        }
    }
    
    /**
     * Безопасно записывает UTF-8 текст в файл
     * @param file файл для записи
     * @param content содержимое для записи
     * @throws IOException если произошла ошибка записи
     */
    private void writeUtf8File(File file, String content) throws IOException {
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(content);
            writer.flush();
        }
    }
    
    /**
     * Безопасно читает UTF-8 текст из файла
     * @param file файл для чтения
     * @return содержимое файла
     * @throws IOException если произошла ошибка чтения
     */
    private String readUtf8File(File file) throws IOException {

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Очищает все временные файлы с данными продуктов старше указанного возраста
     * @param maxAgeMillis максимальный возраст файла в миллисекундах
     * @return количество удаленных файлов
     */
    public int cleanupOldProductsFiles(long maxAgeMillis) {
        int deletedCount = 0;
        
        try {
            File cacheDir = context.getCacheDir();
            File[] files = cacheDir.listFiles();
            
            if (files != null) {
                long currentTime = System.currentTimeMillis();
                
                for (File file : files) {
                    if (file.getName().startsWith(PRODUCTS_FILE_PREFIX) && 
                        file.getName().endsWith(FILE_EXTENSION)) {
                        
                        long fileAge = currentTime - file.lastModified();
                        if (fileAge > maxAgeMillis) {
                            if (file.delete()) {
                                deletedCount++;
                                Log.d(TAG, "Удален старый файл с данными продуктов: " + file.getName());
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке старых файлов с данными продуктов", e);
        }
        
        Log.d(TAG, "Очищено старых файлов с данными продуктов: " + deletedCount);
        return deletedCount;
    }
} 