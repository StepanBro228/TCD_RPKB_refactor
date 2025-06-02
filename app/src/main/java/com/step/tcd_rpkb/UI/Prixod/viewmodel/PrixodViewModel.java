package com.step.tcd_rpkb.UI.Prixod.viewmodel;

import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet; // Добавляем HashSet
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
    private final MutableLiveData<SortCriteria> _sortCriteriaLiveData = new MutableLiveData<>(SortCriteria.STORAGE);
    public LiveData<SortCriteria> sortCriteriaLiveData = _sortCriteriaLiveData;

    private final MutableLiveData<Boolean> _isSortAscendingLiveData = new MutableLiveData<>(true);
    public LiveData<Boolean> isSortAscendingLiveData = _isSortAscendingLiveData;

    // LiveData для хранения UUID продуктов с ошибками валидации
    private final MutableLiveData<Set<String>> _validationErrorUuidsLiveData = new MutableLiveData<>(new HashSet<>()); // Используем HashSet
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
       ; // Загружаем данные пользователя при инициализации
    }

    public void loadInitialData(String moveUuid, String productsJson, boolean preserveEditedData) {
        this.currentMoveUuid = moveUuid;
        if (moveUuid == null || moveUuid.isEmpty()) {
            _errorLiveData.setValue(new Event<>("Ошибка: не указан UUID перемещения"));
            // Возможно, здесь нужно вызвать событие для закрытия экрана
            return;
        }
        loadPrixodDocument(moveUuid, productsJson, preserveEditedData);

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



    public LiveData<Event<String>> getErrorLiveData() { return errorLiveData; }

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

    // Вспомогательный метод для поиска позиции продукта по Series UUID
    private int findProductPositionBySeriesUuid(List<Product> productList, String seriesUuid) {
        if (productList == null || seriesUuid == null || seriesUuid.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < productList.size(); i++) {
            Product product = productList.get(i);
            // Предполагается, что у Product есть метод getSeriesUuid()
            if (product != null && seriesUuid.equals(product.getSeriesUuid())) { 
                return i;
            }
        }
        return -1;
    }

    public void processBarcodeData(String barcodeData) {
        Log.d("PrixodViewModel", "Обработка данных сканера: " + barcodeData);
        final String seriesUuid = extractUUID(barcodeData); // Теперь это seriesUuid

        if (seriesUuid == null) {
            Log.w("PrixodViewModel", "Series UUID не извлечен из данных сканера.");
            new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Некорректный QR-код. Товар не найден по серийному номеру.")));
            return;
        }

        List<Product> currentProductList = _productsLiveData.getValue();
        if (currentProductList == null) {
            currentProductList = new ArrayList<>();
        }
        
        int position = findProductPositionBySeriesUuid(currentProductList, seriesUuid);

        if (position != -1) {
            Log.d("PrixodViewModel", "Товар найден по SeriesUuid: " + seriesUuid + " на позиции: " + position);
            final int finalPosition = position;
            new Handler(Looper.getMainLooper()).post(() -> _focusProductPositionLiveData.setValue(new Event<>(finalPosition)));
        } else {
            Log.d("PrixodViewModel", "Товар НЕ найден по SeriesUuid: " + seriesUuid + " в текущем списке.");
            boolean filtersActive = Boolean.TRUE.equals(_isAnyFilterActiveLiveData.getValue());

            if (filtersActive) {
                Log.d("PrixodViewModel", "Фильтры активны. Сбрасываем фильтры и повторяем поиск по SeriesUuid.");
                new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Товар не найден с текущими фильтрами. Фильтры сброшены. Повторный поиск...")));
                
                resetAllFilters(); 

                int positionAfterReset = findProductPositionBySeriesUuid(originalProductList, seriesUuid);

                if (positionAfterReset != -1) {
                    Log.d("PrixodViewModel", "Товар найден по SeriesUuid в originalProductList после сброса фильтров на позиции: " + positionAfterReset);
                    new Handler(Looper.getMainLooper()).post(() -> _focusProductPositionLiveData.setValue(new Event<>(positionAfterReset)));
                } else {
                    Log.d("PrixodViewModel", "Товар с SeriesUuid " + seriesUuid + " не найден даже после сброса фильтров.");
                    new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Товар с серийным номером " + seriesUuid + " не найден.")));
                }
            } else {
                Log.d("PrixodViewModel", "Фильтры не активны. Товар с SeriesUuid " + seriesUuid + " не найден.");
                new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Товар с серийным номером " + seriesUuid + " не найден.")));
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
        resetAllFilters(); // Сначала сбрасываем все фильтры
        clearSort();
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
     * Обновляет значение 'taken' для продукта и управляет списком ошибок.
     * @param nomenclatureUuid UUID продукта.
     * @param newTakenValue Новое значение 'taken'.
     * @param isValidFromAdapter Валидность, сообщенная адаптером (true, если адаптер считает значение корректным).
     * @return true, если значение 'taken' было успешно обновлено и считается валидным; false в противном случае.
     */
    private boolean updateProductTakenValueAndErrorState(String nomenclatureUuid, int newTakenValue, boolean isValidFromAdapter) {
        if (originalProductList == null || nomenclatureUuid == null) {
            Log.e("PrixodViewModel", "originalProductList или nomenclatureUuid is null в updateProductTakenValueAndErrorState");
            return false;
        }

        Product productToUpdate = null;
        for (Product p : originalProductList) {
            if (nomenclatureUuid.equals(p.getNomenclatureUuid())) {
                productToUpdate = p;
                break;
            }
        }

        Set<String> currentErrors = new HashSet<>(_validationErrorUuidsLiveData.getValue() != null ? _validationErrorUuidsLiveData.getValue() : new HashSet<>());
        boolean isConsideredValidByViewModel = false;

        if (productToUpdate != null) {
            // Логика валидации ViewModel: значение должно быть >= 0 и <= quantity
            if (newTakenValue >= 0 && newTakenValue <= productToUpdate.getQuantity()) {
                productToUpdate.setTaken(newTakenValue);
                currentErrors.remove(nomenclatureUuid); 
                isConsideredValidByViewModel = true;
                Log.d("PrixodViewModel", "Продукт UUID: " + nomenclatureUuid + " обновлен. Taken: " + newTakenValue + ". Валидно по ViewModel.");
            } else {
                // Невалидно по бизнес-логике ViewModel (например, выходит за пределы quantity)
                // Не меняем productToUpdate.setTaken(newTakenValue); здесь, чтобы не сохранять невалидное значение
                currentErrors.add(nomenclatureUuid);
                Log.w("PrixodViewModel", "Продукт UUID: " + nomenclatureUuid + " значение " + newTakenValue + " вне допустимых пределов (0-" + productToUpdate.getQuantity() + "). Помечено как ошибка ViewModel.");
                isConsideredValidByViewModel = false;
            }
            
            // Если адаптер сообщил о невалидности, это приоритет, даже если ViewModel считает валидным.
            if (!isValidFromAdapter && isConsideredValidByViewModel) {
                currentErrors.add(nomenclatureUuid);
                Log.w("PrixodViewModel", "Продукт UUID: " + nomenclatureUuid + " был валиден по ViewModel, но адаптер сообщил о невалидности. Помечено как ошибка.");
                isConsideredValidByViewModel = false; // Окончательное решение - невалидно
            }

            _validationErrorUuidsLiveData.setValue(currentErrors);
        } else {
            Log.e("PrixodViewModel", "Продукт с UUID: " + nomenclatureUuid + " не найден в originalProductList.");
            return false; // Товар не найден, невалидно
        }
        return isConsideredValidByViewModel;
    }
    

    public void handleProductDataConfirmation(String nomenclatureUuid, int newTakenValue, boolean isValidFromAdapter, boolean byEnterKey, int currentPositionInAdapter) {
        Log.d("PrixodViewModel", "Обработка подтверждения данных для UUID: " + nomenclatureUuid + 
                           ", значение: " + newTakenValue + ", isValidFromAdapter: " + isValidFromAdapter + 
                           ", byEnterKey: " + byEnterKey + ", позиция: " + currentPositionInAdapter);

        boolean isDataValidByViewModel = updateProductTakenValueAndErrorState(nomenclatureUuid, newTakenValue, isValidFromAdapter);
        applyFiltersAndSort(); // Обновляем список в UI после изменения данных/ошибок

        if (!isDataValidByViewModel) {
            // Если данные НЕ валидны, фокус должен остаться на текущем элементе.
            // Адаптер уже должен был показать анимацию тряски и фон ошибки.
            // Мы просто не переводим фокус.
            Log.d("PrixodViewModel", "Данные не валидны для UUID: " + nomenclatureUuid + ". Фокус остается на текущем элементе.");
            // Можно дополнительно отправить событие, если нужно специфическое поведение UI при ошибке и Enter.
            // Например, _forceShakeEvent.setValue(new Event<>(nomenclatureUuid));
            return; // Прерываем дальнейшую обработку перехода фокуса
        }

        // Если данные валидны и был нажат Enter
        if (byEnterKey) {
            // Откладываем установку _focusProductPositionLiveData, чтобы RecyclerView успел обработать notifyDataSetChanged
            new Handler(Looper.getMainLooper()).post(() -> {
            List<Product> currentDisplayedProducts = _productsLiveData.getValue();
            if (currentDisplayedProducts != null && !currentDisplayedProducts.isEmpty()) {
                    // Ищем текущий продукт в ОТОБРАЖАЕМОМ списке, чтобы найти его актуальный индекс
                    int currentIndexInDisplayedList = -1;
                    for(int i=0; i < currentDisplayedProducts.size(); i++){
                        if(nomenclatureUuid.equals(currentDisplayedProducts.get(i).getNomenclatureUuid())){
                            currentIndexInDisplayedList = i;
                            break;
                        }
                    }

                    if (currentIndexInDisplayedList != -1) {
                        int nextPosition = currentIndexInDisplayedList + 1;
                if (nextPosition < currentDisplayedProducts.size()) {
                            Log.d("PrixodViewModel", "Нажат Enter, данные валидны. Фокус на следующий элемент на позиции: " + nextPosition + " (posted)");
                    _focusProductPositionLiveData.setValue(new Event<>(nextPosition));
                        } else {
                            Log.d("PrixodViewModel", "Нажат Enter на последнем элементе. Следующего элемента нет. (posted)");
                            // Можно инициировать снятие фокуса с RecyclerView или другое действие
                        }
                    } else {
                         Log.w("PrixodViewModel", "Нажат Enter, но текущий продукт UUID: " + nomenclatureUuid + " не найден в отображаемом списке. Невозможно определить следующий фокус. (posted)");
                    }
                } else {
                     Log.w("PrixodViewModel", "Нажат Enter, но текущий список продуктов null или пуст. Невозможно определить следующий фокус. (posted)");
                }
            });
        }
        // Если не byEnterKey и данные валидны, фокус управляется стандартным поведением Android 
        // (потеря фокуса и т.д.), ViewModel не вмешивается.
    }


    public void requestFocusOnError(String errorUuid) {
        if (errorUuid == null || errorUuid.isEmpty() || originalProductList == null) {
            new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Не удалось найти товар для фокусировки (нет UUID или списка).")));
            return;
        }

        boolean productFound = false;
        int positionInCurrentList = -1;

        // Сначала ищем в текущем отображаемом списке (_productsLiveData)
        // Важно: requestFocusOnError вызывается с nomenclatureUuid (т.к. ошибки валидации привязаны к номенклатуре)
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