package com.step.tcd_rpkb.UI.Prixod.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.step.tcd_rpkb.base.BaseViewModel;
import com.step.tcd_rpkb.data.mapper.ProductMapper;
import com.step.tcd_rpkb.data.network.dto.ProductDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.usecase.GetPrixodDocumentUseCase;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.utils.Event;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections; // Для сортировки
import java.util.Comparator; // Для сортировки
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PrixodViewModel extends BaseViewModel {

    private final GetPrixodDocumentUseCase getPrixodDocumentUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ProductMapper productMapper;
    private final Gson gson;

    // LiveData для UI
    private final MutableLiveData<List<Product>> _productsLiveData = new MutableLiveData<>();
    public LiveData<List<Product>> productsLiveData = _productsLiveData;

    private final MutableLiveData<Invoice> _currentInvoiceLiveData = new MutableLiveData<>();
    public LiveData<Invoice> currentInvoiceLiveData = _currentInvoiceLiveData;

    private final MutableLiveData<Boolean> _isLoadingLiveData = new MutableLiveData<>();
    public LiveData<Boolean> isLoadingLiveData = _isLoadingLiveData;

    private final MutableLiveData<Event<String>> _errorLiveData = new MutableLiveData<>();
    public LiveData<Event<String>> errorLiveData = _errorLiveData;

    private final MutableLiveData<String> _userFullNameLiveData = new MutableLiveData<>();
    public LiveData<String> userFullNameLiveData = _userFullNameLiveData;

    private final MutableLiveData<String> _userRoleLiveData = new MutableLiveData<>();
    public LiveData<String> userRoleLiveData = _userRoleLiveData;

    private final MutableLiveData<Event<Integer>> _focusProductPositionLiveData = new MutableLiveData<>();
    public LiveData<Event<Integer>> focusProductPositionLiveData = _focusProductPositionLiveData;

    private final MutableLiveData<Event<String>> _productNotFoundForFocusEvent = new MutableLiveData<>();
    public LiveData<Event<String>> productNotFoundForFocusEvent = _productNotFoundForFocusEvent;

    // Список оригинальных продуктов, полученных из репозитория
    private List<Product> originalProductList = new ArrayList<>();

    // LiveData для состояния фильтров
    private final MutableLiveData<String> _nameFilterLiveData = new MutableLiveData<>("");
    public LiveData<String> nameFilterLiveData = _nameFilterLiveData;

    private final MutableLiveData<Integer> _minAmountFilterLiveData = new MutableLiveData<>(); // null означает отсутствие фильтра
    public LiveData<Integer> minAmountFilterLiveData = _minAmountFilterLiveData;

    private final MutableLiveData<Integer> _maxAmountFilterLiveData = new MutableLiveData<>(); // null означает отсутствие фильтра
    public LiveData<Integer> maxAmountFilterLiveData = _maxAmountFilterLiveData;

    private final MutableLiveData<Boolean> _untouchedFilterLiveData = new MutableLiveData<>(false);
    public LiveData<Boolean> untouchedFilterLiveData = _untouchedFilterLiveData;

    private final MutableLiveData<Boolean> _isAnyFilterActiveLiveData = new MutableLiveData<>(false);
    public LiveData<Boolean> isAnyFilterActiveLiveData = _isAnyFilterActiveLiveData;

    // LiveData для состояния сортировки
    private final MutableLiveData<SortCriteria> _sortCriteriaLiveData = new MutableLiveData<>(SortCriteria.NONE);
    public LiveData<SortCriteria> sortCriteriaLiveData = _sortCriteriaLiveData;

    private final MutableLiveData<Boolean> _isSortAscendingLiveData = new MutableLiveData<>(true);
    public LiveData<Boolean> isSortAscendingLiveData = _isSortAscendingLiveData;

    // LiveData для хранения UUID продуктов с ошибками валидации
    private final MutableLiveData<Set<String>> _validationErrorUuidsLiveData = new MutableLiveData<>(new java.util.HashSet<>());
    public LiveData<Set<String>> validationErrorUuidsLiveData = _validationErrorUuidsLiveData;

    private final MutableLiveData<Event<String>> _forceResetFiltersMessageLiveData = new MutableLiveData<>();
    public LiveData<Event<String>> forceResetFiltersMessageLiveData = _forceResetFiltersMessageLiveData;

    private String currentMoveUuid;

    @Inject
    public PrixodViewModel(GetPrixodDocumentUseCase getPrixodDocumentUseCase, 
                           GetUserUseCase getUserUseCase,
                           ProductMapper productMapper,
                           Gson gson) {
        this.getPrixodDocumentUseCase = getPrixodDocumentUseCase;
        this.getUserUseCase = getUserUseCase;
        this.productMapper = productMapper;
        this.gson = gson;
        loadUserData(); // Загружаем данные пользователя при инициализации
    }

    public void loadInitialData(String moveUuid, String productsJson, boolean preserveEditedData) {
        this.currentMoveUuid = moveUuid;
        if (moveUuid == null || moveUuid.isEmpty()) {
            _errorLiveData.setValue(new Event<>("Ошибка: не указан UUID перемещения"));
            // Возможно, здесь нужно вызвать событие для закрытия экрана
            return;
        }
        loadPrixodDocument(moveUuid, productsJson, preserveEditedData);
        loadUserData(); // Загрузка данных пользователя
    }

    private void loadPrixodDocument(String moveUuid, String productsJson, boolean preserveEditedData) {
        _isLoadingLiveData.setValue(true);
        getPrixodDocumentUseCase.execute(moveUuid, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                _isLoadingLiveData.setValue(false);
                
                if (invoice == null || invoice.getProducts() == null) {
                    _errorLiveData.setValue(new Event<>("Не удалось загрузить документ или список продуктов пуст."));
                    _productsLiveData.setValue(new ArrayList<>()); // Устанавливаем пустой список
                    _currentInvoiceLiveData.setValue(null);
                    return;
                }

                List<Product> loadedProducts = new ArrayList<>(invoice.getProducts()); // Создаем изменяемую копию

                if (productsJson != null && !productsJson.isEmpty()) {
                    try {
                        Type type = new TypeToken<List<ProductDto>>(){}.getType();
                        List<ProductDto> savedDtoProducts = gson.fromJson(productsJson, type);

                        if (savedDtoProducts != null && !savedDtoProducts.isEmpty()) {
                            List<Product> savedDomainProducts = productMapper.mapToDomainList(savedDtoProducts);
                            Map<String, Product> savedProductsMap = new HashMap<>();
                            for (Product savedProduct : savedDomainProducts) {
                                if (savedProduct != null && savedProduct.getNomenclatureUuid() != null) {
                                    savedProductsMap.put(savedProduct.getNomenclatureUuid(), savedProduct);
                                }
                            }

                            for (Product loadedProduct : loadedProducts) {
                                if (loadedProduct == null || loadedProduct.getNomenclatureUuid() == null) continue;

                                Product savedProduct = savedProductsMap.get(loadedProduct.getNomenclatureUuid());
                                if (savedProduct != null) {
                                    if (preserveEditedData) {
                                        if (savedProduct.getTaken() > 0) {
                                            loadedProduct.setTaken(savedProduct.getTaken());
                                        }
                                    } else {
                                        if (savedProduct.getTaken() > 0) { // В старом коде было (double)product.getTaken()
                                            loadedProduct.setQuantity(savedProduct.getTaken());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Логируем ошибку парсинга или маппинга сохраненных данных
                        System.err.println("Ошибка при восстановлении данных продуктов из JSON: " + e.getMessage());
                        _errorLiveData.setValue(new Event<>("Ошибка восстановления сохраненных данных."));
                        // Продолжаем с загруженными данными, но сообщаем об ошибке
                    }
                }
                
                originalProductList = new ArrayList<>(loadedProducts); // Сохраняем оригинальный список
                invoice.setProducts(originalProductList); // В Invoice тоже кладем актуальный список (хотя он будет в LiveData)
                _currentInvoiceLiveData.setValue(invoice);
                applyFiltersAndSort(); // Применяем фильтры и сортировку по умолчанию
            }

            @Override
            public void onError(Exception exception) {
                _isLoadingLiveData.setValue(false);
                _errorLiveData.setValue(new Event<>("Какая то ошибка в loadPrixodDocument"));
                _productsLiveData.setValue(null); // или emptyList()
            }


        });
    }

    private void loadUserData() {
        // Пример загрузки данных пользователя
        // getUserUseCase.execute(new RepositoryCallback<User>() {
        //     @Override
        //     public void onSuccess(User user) {
        //         if (user != null) {
        //             _userFullNameLiveData.setValue(user.getFullName());
        //             _userRoleLiveData.setValue(user.getRole());
        //         }
        //     }
        // 
        //     @Override
        //     public void onError(String message) {
        //         // Обработка ошибки загрузки пользователя, если необходимо
        //         _errorLiveData.setValue(new Event<>("Ошибка загрузки данных пользователя: " + message));
        //     }
        // });
        // Временная заглушка, пока GetUserUseCase не настроен полностью
        _userFullNameLiveData.setValue("Иванов Иван Иванович (Заглушка)");
        _userRoleLiveData.setValue("Кладовщик (Заглушка)");
    }

    public LiveData<Event<String>> getErrorLiveData() { return errorLiveData; }
    public LiveData<String> getUserFullNameLiveData() { return userFullNameLiveData; }
    public LiveData<String> getUserRoleLiveData() { return userRoleLiveData; }
    public LiveData<Event<Integer>> getFocusProductPositionLiveData() { return focusProductPositionLiveData; }
    public LiveData<Event<String>> getProductNotFoundForFocusEvent() { return productNotFoundForFocusEvent; }
    public LiveData<Event<String>> getForceResetFiltersMessageLiveData() { return _forceResetFiltersMessageLiveData; }

    private String extractUUID(String barcodeData) {
        if (barcodeData == null || barcodeData.isEmpty()) {
            return null;
        }

        // Разделяем строку по символу |
        String[] parts = barcodeData.split("\\|"); // Необходимо экранировать символ |

        // Если частей нет или последняя часть пуста, возвращаем null
        if (parts.length == 0) {
            return null;
        }

        String potentialUuid = parts[parts.length - 1].trim();

            // Проверяем, что последняя часть соответствует формату UUID
                Pattern uuidPattern = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
        if (uuidPattern.matcher(potentialUuid).matches()) {
            return potentialUuid;
        }

        // Если предыдущие проверки не прошли, значит это не тот формат, который мы ожидаем
        Log.w("PrixodViewModel", "Не удалось извлечь UUID из данных сканера: " + barcodeData + ". Последняя часть: " + potentialUuid);
        return null; // Возвращаем null, если не удалось извлечь валидный UUID
    }

    // Вспомогательный метод для поиска позиции продукта по Nomenclature UUID
    private int findProductPositionByNomenclatureUuid(List<Product> productList, String nomenclatureUuid) {
        if (productList == null || nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < productList.size(); i++) {
            Product product = productList.get(i);
            if (product != null && nomenclatureUuid.equals(product.getNomenclatureUuid())) {
                return i;
            }
        }
        return -1;
    }

    public void processBarcodeData(String barcodeData) {
        Log.d("PrixodViewModel", "Обработка данных сканера: " + barcodeData);
        String nomenclatureUuid = extractUUID(barcodeData);

        if (nomenclatureUuid == null) {
            Log.w("PrixodViewModel", "UUID не извлечен из данных сканера.");
            _productNotFoundForFocusEvent.setValue(new Event<>("Некорректный QR-код. Товар не найден."));
                        return;
                    }

        List<Product> currentProductList = _productsLiveData.getValue();
        if (currentProductList == null) { // Добавлена проверка на null
            currentProductList = new ArrayList<>(); // Инициализация пустым списком, если null
        }
        
        int position = findProductPositionByNomenclatureUuid(currentProductList, nomenclatureUuid);

        if (position != -1) {
            Log.d("PrixodViewModel", "Товар найден по NomenclatureUuid: " + nomenclatureUuid + " на позиции: " + position);
            _focusProductPositionLiveData.setValue(new Event<>(position));
        } else {
            Log.d("PrixodViewModel", "Товар НЕ найден по NomenclatureUuid: " + nomenclatureUuid + " в текущем списке.");
            // Проверяем, активны ли фильтры
            boolean filtersActive = Boolean.TRUE.equals(_isAnyFilterActiveLiveData.getValue()); // Безопасная проверка на null

            if (filtersActive) {
                Log.d("PrixodViewModel", "Фильтры активны. Сбрасываем фильтры и повторяем поиск.");
                // Сбрасываем фильтры. Это вызовет обновление _productsLiveData.
                // Повторный поиск будет в originalProductList, так как _productsLiveData обновится асинхронно.
                String messageForUser = "Товар не найден с текущими фильтрами. Фильтры сброшены. Повторный поиск...";
                _productNotFoundForFocusEvent.setValue(new Event<>(messageForUser)); // Уведомляем пользователя о сбросе
                
                resetAllFilters(); // Этот метод обновит _productsLiveData до originalProductList и применит сортировку

                // Ищем в originalProductList, так как он теперь основа для _productsLiveData после сброса
                int positionAfterReset = findProductPositionByNomenclatureUuid(originalProductList, nomenclatureUuid);

                if (positionAfterReset != -1) {
                    Log.d("PrixodViewModel", "Товар найден в originalProductList после сброса фильтров на позиции: " + positionAfterReset);
                    // Поскольку resetAllFilters() вызовет applyFiltersAndSort(), который обновит _productsLiveData,
                    // позиция в originalProductList должна совпадать с позицией в _productsLiveData (если сортировка не изменила порядок критично)
                    // Если сортировка может сильно менять порядок, то нужно дождаться обновления LiveData.
                    // Для простоты пока предполагаем, что позиция будет корректной или достаточно близкой.
                    _focusProductPositionLiveData.setValue(new Event<>(positionAfterReset));
                     // Можно добавить дополнительное сообщение, что товар найден после сброса.
                    // _productNotFoundForFocusEvent.setValue(new Event<>("Фильтры сброшены. Товар найден."));
                } else {
                    Log.d("PrixodViewModel", "Товар не найден даже после сброса фильтров.");
                    _productNotFoundForFocusEvent.setValue(new Event<>("Товар с кодом " + nomenclatureUuid + " не найден."));
                }
            } else {
                Log.d("PrixodViewModel", "Фильтры не активны. Товар не найден.");
                _productNotFoundForFocusEvent.setValue(new Event<>("Товар с кодом " + nomenclatureUuid + " не найден."));
            }
        }
    }

    // --- Методы для управления фильтрацией ---
    public void updateNameFilter(String name) {
        _nameFilterLiveData.setValue(name == null ? "" : name);
        applyFiltersAndSort();
    }

    public void updateAmountFilter(Integer min, Integer max) {
        _minAmountFilterLiveData.setValue(min);
        _maxAmountFilterLiveData.setValue(max);
        applyFiltersAndSort();
    }

    public void updateUntouchedFilter(boolean active) {
        _untouchedFilterLiveData.setValue(active);
        applyFiltersAndSort();
    }

    public void resetAllFilters() {
        _nameFilterLiveData.setValue("");
        _minAmountFilterLiveData.setValue(null);
        _maxAmountFilterLiveData.setValue(null);
        _untouchedFilterLiveData.setValue(false);
        // Сортировку не сбрасываем здесь, для этого есть clearSort или resetAllFiltersAndSort
        applyFiltersAndSort();
    }

    // --- Методы для управления сортировкой ---
    public void setSortCriteria(SortCriteria criteria) {
        if (_sortCriteriaLiveData.getValue() == criteria) {
            _isSortAscendingLiveData.setValue(!Boolean.TRUE.equals(_isSortAscendingLiveData.getValue()));
        } else {
        _sortCriteriaLiveData.setValue(criteria);
            _isSortAscendingLiveData.setValue(true);
        }
        applyFiltersAndSort();
    }

    public void clearSort() {
        _sortCriteriaLiveData.setValue(SortCriteria.NONE);
        applyFiltersAndSort();
    }
    
    public void resetAllFiltersAndSort() {
        resetAllFilters();
        clearSort(); // applyFiltersAndSort() будет вызван в clearSort()
    }

    private void applyFiltersAndSort() {
        List<Product> processedList = new ArrayList<>(originalProductList);

        // 1. Фильтрация (код фильтрации остается без изменений)
        String nameQuery = _nameFilterLiveData.getValue() != null ? _nameFilterLiveData.getValue().toLowerCase().trim() : "";
        Integer minAmount = _minAmountFilterLiveData.getValue();
        Integer maxAmount = _maxAmountFilterLiveData.getValue();
        boolean filterUntouched = Boolean.TRUE.equals(_untouchedFilterLiveData.getValue());
        boolean anyFilterActive = false;

        if (!nameQuery.isEmpty()) {
            processedList.removeIf(p -> p.getNomenclatureName() == null || !p.getNomenclatureName().toLowerCase().contains(nameQuery));
            anyFilterActive = true;
        }
        if (minAmount != null) {
            processedList.removeIf(p -> p.getQuantity() < minAmount);
            anyFilterActive = true;
        }
        if (maxAmount != null) {
            processedList.removeIf(p -> p.getQuantity() > maxAmount);
            anyFilterActive = true;
        }
        if (filterUntouched) {
            processedList.removeIf(p -> p.getTaken() > 0);
            anyFilterActive = true;
        }
        _isAnyFilterActiveLiveData.setValue(anyFilterActive);


        // 2. Сортировка
        SortCriteria currentCriteria = _sortCriteriaLiveData.getValue();
        boolean isAscending = Boolean.TRUE.equals(_isSortAscendingLiveData.getValue());

        if (currentCriteria != null && currentCriteria != SortCriteria.NONE) {
            Comparator<Product> comparator = null;
            switch (currentCriteria) {
                case NAME:
                    comparator = Comparator.comparing(Product::getNomenclatureName, Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case MEASURE:
                    comparator = Comparator.comparing(Product::getUnitName, Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case AMOUNT:
                    comparator = Comparator.comparingDouble(Product::getQuantity);
                    break;
                case STORAGE:
                    comparator = (p1, p2) -> compareStorageNames(p1.getSenderStorageName(), p2.getSenderStorageName());
                    break;
            }

            if (comparator != null) {
                if (!isAscending) {
                    comparator = comparator.reversed();
                }
                Collections.sort(processedList, comparator);
            }
        }
        _productsLiveData.setValue(processedList);
    }
    
    // Вспомогательный метод для сравнения мест хранения (ячеек)
    private int compareStorageNames(String storage1, String storage2) {
        if (storage1 == null) storage1 = "";
        if (storage2 == null) storage2 = "";

        // Простая реализация, можно доработать для числового сравнения частей "X-X-X"
        String[] parts1 = storage1.split("-");
        String[] parts2 = storage2.split("-");

        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            try {
                int num1 = Integer.parseInt(parts1[i]);
                int num2 = Integer.parseInt(parts2[i]);
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            } catch (NumberFormatException e) {
                // Если не число, сравниваем как строки
                int strComp = parts1[i].compareToIgnoreCase(parts2[i]);
                if (strComp != 0) {
                    return strComp;
                }
            }
        }
        return Integer.compare(parts1.length, parts2.length);
    }

    /**
     * Собирает текущий список продуктов (originalProductList, который должен содержать актуальные "taken"),
     * мапит его в List<ProductDto> и сериализует в JSON-строку.
     * @return JSON-строка списка продуктов для сохранения или отправки.
     */
    public String getProductsToSaveAsJson() {
        if (originalProductList == null) {
            // Возвращаем JSON для пустого списка, если originalProductList не инициализирован
            return gson.toJson(new ArrayList<ProductDto>());
        }
        if (productMapper == null) {
            Log.e("PrixodViewModel", "ProductMapper is null in getProductsToSaveAsJson. DI issue?");
            // Возвращаем JSON для пустого списка в случае ошибки с маппером
            return gson.toJson(new ArrayList<ProductDto>()); 
        }
        // Маппим доменные модели в DTO
        List<ProductDto> dtoList = productMapper.mapToDtoList(originalProductList);
        // Сериализуем список DTO в JSON
        return gson.toJson(dtoList);
    }

    /**
     * Внутренний метод для обновления значения 'taken' и состояния ошибок.
     * @param nomenclatureUuid UUID номенклатуры продукта для обновления.
     * @param newTakenValue Новое значение 'taken'.
     * @param isValidПришлоОтАдаптера Флаг валидности, полученный от адаптера.
     * @return true, если после всех проверок ViewModel считает данные валидными для этого продукта, иначе false.
     */
    private boolean updateProductTakenValueInternal(String nomenclatureUuid, int newTakenValue, boolean isValidПришлоОтАдаптера) {
        if (originalProductList == null || nomenclatureUuid == null) {
            Log.e("PrixodViewModel", "originalProductList is null or nomenclatureUuid is null in updateProductTakenValueInternal");
            return false; // Считаем невалидным, так как не можем обработать
        }

        Product productToUpdate = null;
        for (Product p : originalProductList) {
            if (nomenclatureUuid.equals(p.getNomenclatureUuid())) {
                productToUpdate = p;
                break;
            }
        }

        Set<String> currentErrors = new java.util.HashSet<>(_validationErrorUuidsLiveData.getValue() != null ? _validationErrorUuidsLiveData.getValue() : new java.util.HashSet<>());
        boolean isDataConsideredValidByViewModel = false;

        if (productToUpdate != null) {
            // Основная логика валидации ViewModel
            if (newTakenValue >= 0 && newTakenValue <= productToUpdate.getQuantity()) {
                productToUpdate.setTaken(newTakenValue);
                currentErrors.remove(nomenclatureUuid); 
                isDataConsideredValidByViewModel = true;
                Log.d("PrixodViewModel", "Product UUID: " + nomenclatureUuid + " updated. Taken: " + newTakenValue + ". Valid by ViewModel.");
            } else {
                // Значение невалидно по бизнес-логике ViewModel (например, выходит за пределы quantity)
                currentErrors.add(nomenclatureUuid);
                // productToUpdate.setTaken(productToUpdate.getTaken()); // Оставляем старое значение 'taken' или сбрасываем, если нужно
                Log.w("PrixodViewModel", "Product UUID: " + nomenclatureUuid + " value " + newTakenValue + " is out of bounds (0-" + productToUpdate.getQuantity() + "). Marked as error by ViewModel.");
                isDataConsideredValidByViewModel = false;
            }
            
            // Дополнительно учитываем флаг от адаптера, если он сказал, что невалидно, то это точно ошибка.
            // (хотя основная логика валидации уже выше)
            if (!isValidПришлоОтАдаптера && isDataConsideredValidByViewModel) {
                // Случай, когда ViewModel посчитала валидным, но адаптер почему-то нет.
                // Это странно, но для безопасности пометим как ошибку.
                currentErrors.add(nomenclatureUuid);
                Log.w("PrixodViewModel", "Product UUID: " + nomenclatureUuid + " was valid by ViewModel, but adapter reported invalid. Marked as error.");
                isDataConsideredValidByViewModel = false;
            }

            _validationErrorUuidsLiveData.setValue(currentErrors);
            // applyFiltersAndSort(); // Обновляем LiveData для UI - будет вызван из handleProductDataConfirmation
        } else {
            Log.e("PrixodViewModel", "Product with UUID: " + nomenclatureUuid + " not found in originalProductList.");
            return false; // Товар не найден, невалидно
        }
        return isDataConsideredValidByViewModel;
    }
    
    // Старый метод updateProductTakenValue можно удалить или сделать приватным, если он больше не нужен извне.
    // Для примера, переименуем его в updateProductTakenValueInternal и сделаем приватным.
    // public void updateProductTakenValue(String nomenclatureUuid, int newTakenValue, boolean isValid) { ... }

    public void handleProductDataConfirmation(String nomenclatureUuid, int newTakenValue, boolean isValidFromAdapter, boolean byEnterKey, int currentPositionInAdapter) {
        Log.d("PrixodViewModel", "Handling data confirmation for UUID: " + nomenclatureUuid + 
                           ", value: " + newTakenValue + ", isValidFromAdapter: " + isValidFromAdapter + 
                           ", byEnterKey: " + byEnterKey + ", position: " + currentPositionInAdapter);

        boolean isDataValidByViewModel = updateProductTakenValueInternal(nomenclatureUuid, newTakenValue, isValidFromAdapter);
        applyFiltersAndSort(); // Обновляем список в UI после изменения данных/ошибок

        if (byEnterKey && isDataValidByViewModel) {
            List<Product> currentDisplayedProducts = _productsLiveData.getValue();
            if (currentDisplayedProducts != null && !currentDisplayedProducts.isEmpty()) {
                int nextPosition = currentPositionInAdapter + 1;
                if (nextPosition < currentDisplayedProducts.size()) {
                    // Проверяем, что следующий элемент не тот же самый (маловероятно, но для безопасности)
                    // Product nextProduct = currentDisplayedProducts.get(nextPosition);
                    // if (!nextProduct.getNomenclatureUuid().equals(nomenclatureUuid)) { ... }
                    Log.d("PrixodViewModel", "Enter key pressed, data valid. Focusing next item at position: " + nextPosition);
                    _focusProductPositionLiveData.setValue(new Event<>(nextPosition));
                } else {
                    // Это был последний элемент в списке
                    Log.d("PrixodViewModel", "Enter key pressed on the last item. No next item to focus.");
                    // Можно инициировать снятие фокуса с RecyclerView или другое действие
                    // _clearFocusEvent.setValue(new Event<>(true)); // Пример нового LiveData для такого события
                }
            } else {
                 Log.w("PrixodViewModel", "Enter key pressed, but current product list is null or empty. Cannot determine next focus.");
            }
        } else if (byEnterKey && !isDataValidByViewModel) {
            // Если Enter нажат, но данные не валидны, фокус должен остаться на текущем элементе с ошибкой.
            // _focusProductPositionLiveData НЕ должен обновляться, чтобы фокус не перескакивал.
            // Activity/Adapter должен обеспечить, что фокус остается (или возвращается) на поле с ошибкой.
            // Можно послать событие для принудительного "встряхивания" или подсветки, если нужно.
             Log.d("PrixodViewModel", "Enter key pressed, but data is invalid. Focus should remain on current item: " + nomenclatureUuid);
             // Можно даже явно запросить фокус на текущей позиции, если есть сомнения, что он останется.
             // List<Product> currentDisplayedProducts = _productsLiveData.getValue();
             // if (currentDisplayedProducts != null) {
             //     int currentErrorItemOriginalIndex = findProductPositionByNomenclatureUuid(originalProductList, nomenclatureUuid); 
             //     if(currentErrorItemOriginalIndex != -1) { // Если товар с ошибкой вообще существует
             //         // Нужно найти его позицию в ТЕКУЩЕМ ОТОБРАЖАЕМОМ списке
             //         int positionInDisplayedList = findProductPositionByNomenclatureUuid(currentDisplayedProducts, nomenclatureUuid);
             //         if(positionInDisplayedList != -1) {
             //             _focusProductPositionLiveData.setValue(new Event<>(positionInDisplayedList));
             //         } else {
             //             // Ошибка на элементе, который не отображается (скрыт фильтром). Тогда сбрасываем фильтры и фокусируемся.
             //             requestFocusOnError(nomenclatureUuid);
             //         }
             //     }
             // }
        }
        // Если не byEnterKey, то фокус управляется стандартным поведением Android (потеря фокуса и т.д.)
    }


    public void requestFocusOnError(String errorUuid) {
        if (errorUuid == null || errorUuid.isEmpty() || originalProductList == null) {
            _productNotFoundForFocusEvent.setValue(new Event<>("Не удалось найти товар для фокусировки (нет UUID или списка)."));
            return;
        }

        boolean productFound = false;
        int positionInCurrentList = -1;

        // Сначала ищем в текущем отображаемом списке (_productsLiveData)
        List<Product> currentDisplayList = _productsLiveData.getValue();
        if (currentDisplayList != null) {
            positionInCurrentList = findProductPositionByNomenclatureUuid(currentDisplayList, errorUuid);
        }

        if (positionInCurrentList != -1) {
            // Товар видим, просто фокусируемся
            _focusProductPositionLiveData.setValue(new Event<>(positionInCurrentList));
            productFound = true;
        } else {
            // Товар не видим. Проверяем, есть ли он вообще в originalProductList
            int positionInOriginalList = findProductPositionByNomenclatureUuid(originalProductList, errorUuid);
            if (positionInOriginalList != -1) {
                // Товар есть, но скрыт фильтрами. Сбрасываем фильтры.
                _forceResetFiltersMessageLiveData.setValue(new Event<>("Фильтры сброшены для отображения товара с ошибкой."));
                resetAllFilters(); // Это вызовет applyFiltersAndSort(), который обновит _productsLiveData

                // После сброса фильтров, _productsLiveData будет равен originalProductList (с учетом текущей сортировки)
                // Нам нужно найти позицию в этом "новом" _productsLiveData
                // Поскольку applyFiltersAndSort() вызывается синхронно внутри resetAllFilters(),
                // мы можем сразу получить обновленный список.
                List<Product> listAfterReset = _productsLiveData.getValue();
                if (listAfterReset != null) {
                    int newPosition = findProductPositionByNomenclatureUuid(listAfterReset, errorUuid);
                    if (newPosition != -1) {
                        _focusProductPositionLiveData.setValue(new Event<>(newPosition));
                        productFound = true;
                    } else {
                        // Это странная ситуация: товар был в original, но после сброса фильтров и сортировки его нет
                        Log.e("PrixodViewModel", "Товар " + errorUuid + " найден в original, но не найден после resetAllFilters. Проблема с сортировкой?");
                         _productNotFoundForFocusEvent.setValue(new Event<>("Ошибка при попытке показать товар: " + errorUuid));
            }
                } else {
                     Log.e("PrixodViewModel", "_productsLiveData is null after resetAllFilters in requestFocusOnError");
                    _productNotFoundForFocusEvent.setValue(new Event<>("Ошибка при обновлении списка для показа товара."));
                }
            } else {
                // Товара нет даже в оригинальном списке
                _productNotFoundForFocusEvent.setValue(new Event<>("Товар с ошибкой (UUID: " + errorUuid + ") не найден в документе."));
            }
        }

        if (!productFound) {
            Log.w("PrixodViewModel", "Товар с ошибкой " + errorUuid + " не был найден для фокусировки.");
        }
    }

    // TODO: Добавить методы для обработки QR-кодов, фильтрации, сортировки, обновления количества и т.д.
} 