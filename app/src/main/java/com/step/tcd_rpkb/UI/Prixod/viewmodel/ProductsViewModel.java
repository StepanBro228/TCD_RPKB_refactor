package com.step.tcd_rpkb.UI.Prixod.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.step.tcd_rpkb.base.BaseViewModel;
import com.step.tcd_rpkb.data.mapper.ProductMapper;
import com.step.tcd_rpkb.data.network.dto.ProductDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.usecase.GetPrixodDocumentUseCase;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.utils.Event;
import com.step.tcd_rpkb.utils.ProductsDataManager;
import com.step.tcd_rpkb.utils.SeriesDataManager;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections; // Для сортировки
import java.util.Comparator; // Для сортировки
import java.util.Date;
import java.util.HashSet; // Добавляем HashSet
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Set;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProductsViewModel extends BaseViewModel {

    private final GetPrixodDocumentUseCase getPrixodDocumentUseCase;

    private final ProductMapper productMapper;
    private final Gson gson;
    private final ProductsDataManager productsDataManager;
    private final SeriesDataManager seriesDataManager;

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

    //  событие для фокуса после сканирования с автоматическим выбором
    private final MutableLiveData<Event<Integer>> _focusAndSelectProductPositionLiveData = new MutableLiveData<>();
    public LiveData<Event<Integer>> focusAndSelectProductPositionLiveData = _focusAndSelectProductPositionLiveData;

    // Событие для прокрутки к позиции без установки фокуса (после отметки товаров как скомплектованных)
    private final MutableLiveData<Event<Integer>> _scrollToPositionEvent = new MutableLiveData<>();
    public LiveData<Event<Integer>> scrollToPositionEvent = _scrollToPositionEvent;

    private final MutableLiveData<Event<String>> _productNotFoundForFocusEvent = new MutableLiveData<>();
    public LiveData<Event<String>> productNotFoundForFocusEvent = _productNotFoundForFocusEvent;

    private final MutableLiveData<Event<String>> _shakeViewEvent = new MutableLiveData<>();
    public LiveData<Event<String>> shakeViewEvent = _shakeViewEvent;

    // LiveData для сообщений об ошибках валидации
    private final MutableLiveData<Event<String>> _validationErrorMessageEvent = new MutableLiveData<>();
    public LiveData<Event<String>> validationErrorMessageEvent = _validationErrorMessageEvent;

    // LiveData для уведомления Activity об изменениях в originalProductList
    private final MutableLiveData<Event<Boolean>> _originalProductListUpdatedEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> originalProductListUpdatedEvent = _originalProductListUpdatedEvent;

    // Список оригинальных продуктов
    private List<Product> originalProductList = new ArrayList<>();

    // LiveData для состояния фильтров
    private final MutableLiveData<String> _nameFilterLiveData = new MutableLiveData<>("");
    public LiveData<String> nameFilterLiveData = _nameFilterLiveData;

    private final MutableLiveData<Integer> _minAmountFilterLiveData = new MutableLiveData<>();
    public LiveData<Integer> minAmountFilterLiveData = _minAmountFilterLiveData;

    private final MutableLiveData<Integer> _maxAmountFilterLiveData = new MutableLiveData<>();
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

    // LiveData для хранения productLineId продуктов с ошибками валидации
    private final MutableLiveData<Set<String>> _validationErrorUuidsLiveData = new MutableLiveData<>(new HashSet<>()); // Используем HashSet
    public LiveData<Set<String>> validationErrorUuidsLiveData = _validationErrorUuidsLiveData;

    private final MutableLiveData<Event<String>> _forceResetFiltersMessageLiveData = new MutableLiveData<>();
    public LiveData<Event<String>> forceResetFiltersMessageLiveData = _forceResetFiltersMessageLiveData;

    private String currentMoveUuid;

    // LiveData для хранения информации о перемещении
    private final MutableLiveData<MoveItem> _moveItemLiveData = new MutableLiveData<>();
    public LiveData<MoveItem> moveItemLiveData = _moveItemLiveData;

    // Флаг синхронизации с 1С
    private final MutableLiveData<Boolean> _isSyncedWith1C = new MutableLiveData<>(false);
    public LiveData<Boolean> isSyncedWith1C = _isSyncedWith1C;


    // Событие для возврата в меню с сохранением данных (обычный возврат)
    private final MutableLiveData<Event<String>> _returnToMenuWithDataEvent = new MutableLiveData<>();
    public LiveData<Event<String>> returnToMenuWithDataEvent = _returnToMenuWithDataEvent;
    
    // Событие для возврата в меню с изменением статуса на "Подготовлен"
    private final MutableLiveData<Event<String>> _returnToMenuWithStatusChangeEvent = new MutableLiveData<>();
    public LiveData<Event<String>> returnToMenuWithStatusChangeEvent = _returnToMenuWithStatusChangeEvent;
    
    // Событие для показа диалога о сохраненных изменениях при выходе
    private final MutableLiveData<Event<Boolean>> _showDataSavedDialogEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> showDataSavedDialogEvent = _showDataSavedDialogEvent;

    // Событие для уведомления об успешном использовании переданных данных
    private final MutableLiveData<Event<Boolean>> _dataSuccessfullyLoadedEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> dataSuccessfullyLoadedEvent = _dataSuccessfullyLoadedEvent;

    // Enum для типов поиска продукта
    public enum SearchType {
        NOMENCLATURE_UUID,
        SERIES_UUID
    }

    // LiveData для режима "Только чтение" (для перемещений со статусами "Подготовлен" и "Сформированно")
    private final MutableLiveData<Boolean> _isReadOnlyMode = new MutableLiveData<>(false);
    public LiveData<Boolean> isReadOnlyMode = _isReadOnlyMode;

    // Константы для статусов перемещений
    private static final String STATUS_PODGOTOVLEN = "Подготовлен";
    private static final String STATUS_SFORMIROVANNO = "Сформированно";

    // LiveData для диалога подтверждения синхронизации
    private final MutableLiveData<Event<String>> _syncConfirmationMessage = new MutableLiveData<>();
    public LiveData<Event<String>> syncConfirmationMessage = _syncConfirmationMessage;

    // LiveData для диалога выбора товаров по серии
    private final MutableLiveData<Event<MultipleProductsData>> _multipleProductsFoundEvent = new MutableLiveData<>();
    public LiveData<Event<MultipleProductsData>> multipleProductsFoundEvent = _multipleProductsFoundEvent;

    // LiveData для события успешной отметки товаров как скомплектованные
    private final MutableLiveData<Event<Boolean>> _productsMarkedAsCompletedEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> productsMarkedAsCompletedEvent = _productsMarkedAsCompletedEvent;

    // LiveData для диалога подтверждения статистики перед отправкой в "Подготовлено"
    private final MutableLiveData<Event<ProductsStatistics>> _showStatisticsConfirmationEvent = new MutableLiveData<>();
    public LiveData<Event<ProductsStatistics>> showStatisticsConfirmationEvent = _showStatisticsConfirmationEvent;

    // Класс для передачи статистики по товарам
    public static class ProductsStatistics {
        private final int totalProducts;           // Всего контейнеров
        private final int fullyCompleted;         // Полностью скомплектованные (taken == quantity)
        private final int partiallyCompleted;     // Частично скомплектованные (taken != quantity и taken != 0)
        private final int notCompleted;           // Нескомплектованные (taken == 0)
        
        public ProductsStatistics(int totalProducts, int fullyCompleted, int partiallyCompleted, int notCompleted) {
            this.totalProducts = totalProducts;
            this.fullyCompleted = fullyCompleted;
            this.partiallyCompleted = partiallyCompleted;
            this.notCompleted = notCompleted;
        }
        
        public int getTotalProducts() {
            return totalProducts;
        }
        
        public int getFullyCompleted() {
            return fullyCompleted;
        }
        
        public int getPartiallyCompleted() {
            return partiallyCompleted;
        }
        
        public int getNotCompleted() {
            return notCompleted;
        }
        

    }

    // Класс для передачи данных о множестве найденных товаров
    public static class MultipleProductsData {
        private final List<Product> products;
        private final int totalCount;
        private final String senderStorage;
        
        public MultipleProductsData(List<Product> products, int totalCount, String senderStorage) {
            this.products = products;
            this.totalCount = totalCount;
            this.senderStorage = senderStorage;
        }
        
        public List<Product> getProducts() {
            return products;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public String getSenderStorage() {
            return senderStorage;
        }
    }

    @Inject
    public ProductsViewModel(GetPrixodDocumentUseCase getPrixodDocumentUseCase,
                             GetUserUseCase getUserUseCase,
                             ProductMapper productMapper,
                             Gson gson,
                             MoveRepository moveRepository,
                             ProductsDataManager productsDataManager,
                             SeriesDataManager seriesDataManager) {
        this.getPrixodDocumentUseCase = getPrixodDocumentUseCase;
        this.productMapper = productMapper;
        this.gson = gson;
        this.productsDataManager = productsDataManager;
        this.seriesDataManager = seriesDataManager;
    }

    /**
     * Загружает начальные данные для активити
     * 
     * @param moveUuid UUID перемещения
     * @param productsJson JSON с сохраненными данными продуктов (null если нет)
     * @param preserveEditedData флаг для сохранения отредактированных данных
     * @param moveItem полный объект MoveItem с информацией о перемещении (может быть null)
     */
    public void loadInitialData(String moveUuid, String productsJson, boolean preserveEditedData, MoveItem moveItem) {
        currentMoveUuid = moveUuid;
        
        // Если передан MoveItem, используем его данные
        if (moveItem != null) {
            _moveItemLiveData.setValue(moveItem);
            updateReadOnlyMode(moveItem.getSigningStatus());
            Log.d("PrixodViewModel", "Получен MoveItem: номер=" + moveItem.getNumber() + 
                  ", дата=" + moveItem.getDate() + 
                  ", статус=" + moveItem.getSigningStatus());
        }
        

        if (_isSyncedWith1C.getValue() == null) {
            _isSyncedWith1C.setValue(true);
        }
        
        loadPrixodDocument(moveUuid, productsJson, preserveEditedData, moveItem);
    }



    private void loadPrixodDocument(String moveUuid, String productsJson, boolean preserveEditedData, MoveItem moveItem) {
        _isLoadingLiveData.setValue(true);
        
        Log.d("PrixodViewModel", "loadPrixodDocument: moveUuid=" + moveUuid + 
                              ", productsJson=" + (productsJson != null && !productsJson.isEmpty() ? "заполнен" : "пуст") + 
                              ", preserveEditedData=" + preserveEditedData + 
                              ", moveItem=" + (moveItem != null ? "передан" : "null"));
        

        List<Product> cachedProducts = productsDataManager.loadProductsData(moveUuid);
        if (!cachedProducts.isEmpty()) {
            Log.d("PrixodViewModel", "Найдены кэшированные продукты для перемещения " + moveUuid + ": " + cachedProducts.size() + " продуктов");
            

            Invoice cachedInvoice = new Invoice(moveUuid, cachedProducts);
            

            processLoadedDataFromCache(cachedInvoice, productsJson, preserveEditedData, moveItem);
            return;
        }
        
        // Если кеша нет - загружаем с сервера
        Log.d("PrixodViewModel", "Кэш продуктов пуст, загружаем с сервера для перемещения " + moveUuid);
        getPrixodDocumentUseCase.execute(moveUuid, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                _isLoadingLiveData.setValue(false);
                
                if (invoice == null || invoice.getProducts() == null) {
                    _errorLiveData.setValue(new Event<>("Не удалось загрузить документ или список продуктов пуст."));
                    _productsLiveData.setValue(new ArrayList<>());
                    _currentInvoiceLiveData.setValue(null);
                    return;
                }

                List<Product> loadedProducts = new ArrayList<>(invoice.getProducts());
                Log.d("PrixodViewModel", "Загружено " + loadedProducts.size() + " продуктов из документа");


                if (productsJson != null && !productsJson.isEmpty()) {
                    try {
                        Type type = new TypeToken<List<ProductDto>>(){}.getType();
                        List<ProductDto> savedDtoProducts = gson.fromJson(productsJson, type);

                        if (savedDtoProducts != null && !savedDtoProducts.isEmpty()) {
                            Log.d("PrixodViewModel", "Восстановлено " + savedDtoProducts.size() + " продуктов из JSON для сопоставления по УИДСтрокиТовары");
                            
                            List<Product> savedDomainProducts = productMapper.mapToDomainList(savedDtoProducts);
                            

                            Map<String, Product> savedProductsByLineId = new HashMap<>();
                            
                            for (Product savedProduct : savedDomainProducts) {
                                if (savedProduct != null && savedProduct.getProductLineId() != null && !savedProduct.getProductLineId().isEmpty()) {
                                    savedProductsByLineId.put(savedProduct.getProductLineId(), savedProduct);
                                    Log.d("PrixodViewModel", "Сохранен продукт с УИДСтрокиТовары=" + savedProduct.getProductLineId() + 
                                          " и taken=" + savedProduct.getTaken() + " для восстановления");
                                }
                            }

                            // Счетчики для логирования
                            int restoredCount = 0;
                            int notFoundCount = 0;
                            int emptyLineIdCount = 0;
                            
                            for (Product loadedProduct : loadedProducts) {
                                if (loadedProduct == null) continue;

                                // Проверяем наличие УИДСтрокиТовары у загруженного продукта
                                String productLineId = loadedProduct.getProductLineId();
                                if (productLineId == null || productLineId.isEmpty()) {
                                    emptyLineIdCount++;
                                    Log.d("PrixodViewModel", "У продукта " + loadedProduct.getNomenclatureName() + 
                                          " отсутствует УИДСтрокиТовары, пропускаем восстановление");
                                    continue;
                                }

                                // Ищем сохраненный продукт по УИДСтрокиТовары
                                Product savedProduct = savedProductsByLineId.get(productLineId);
                                    
                                    if (savedProduct != null) {
                                    // Восстанавливаем значение taken из сохраненных данных
                                    double takenValue = savedProduct.getTaken();
                                    loadedProduct.setTaken(takenValue);
                                    
                                        restoredCount++;
                                        Log.d("PrixodViewModel", "Восстановлено значение taken=" + takenValue + 
                                              " для продукта " + loadedProduct.getNomenclatureName() +
                                          " по УИДСтрокиТовары=" + productLineId);
                                } else {
                                    notFoundCount++;
                                    Log.d("PrixodViewModel", "Продукт " + loadedProduct.getNomenclatureName() + 
                                          " с УИДСтрокиТовары=" + productLineId + " не найден в сохраненных данных");
                                }
                            }
                            
                            Log.d("PrixodViewModel", "Итоги восстановления по УИДСтрокиТовары: восстановлено=" + restoredCount + 
                                  ", не найдено=" + notFoundCount + ", без УИДСтрокиТовары=" + emptyLineIdCount);
                        } else {
                            Log.w("PrixodViewModel", "JSON парсинг не дал результатов или вернул пустой список");
                        }
                    } catch (Exception e) {
                        // Логируем
                        Log.e("PrixodViewModel", "Ошибка при восстановлении данных продуктов из JSON: " + e.getMessage(), e);
                        _errorLiveData.setValue(new Event<>("Ошибка восстановления сохраненных данных."));
                    }
                } else {
                    Log.d("PrixodViewModel", "Нет сохраненных данных для восстановления");
                }
                
                originalProductList = new ArrayList<>(loadedProducts); // Сохраняем оригинальный список
                invoice.setProducts(originalProductList); // В Invoice  кладем актуальный список
                _currentInvoiceLiveData.setValue(invoice);
                applyFiltersAndSort(); // Применяем фильтры и сортировку по умолчанию
                
                // Уведомляем об обновлении оригинального списка продуктов
                _originalProductListUpdatedEvent.setValue(new Event<>(true));
                
                // Проверяем наличие ошибок валидации в загруженных данных
                validateAllProducts();
                
                // Повторно применяем режим "только чтение"
                String currentStatus = getCurrentMoveStatus();
                if (currentStatus != null) {
                    Log.d("PrixodViewModel", "Повторно применяем режим 'только чтение' после загрузки данных с сервера для статуса: " + currentStatus);
                    updateReadOnlyMode(currentStatus);
                }
                
                // Сохраняем продукты в кеш при успешной загрузке с сервера
                productsDataManager.saveProductsData(moveUuid, loadedProducts);
                Log.d("PrixodViewModel", "Продукты сохранены в кеш для перемещения: " + moveUuid);

                _dataSuccessfullyLoadedEvent.setValue(new Event<>(true));
            }

            @Override
            public void onError(Exception exception) {
                _isLoadingLiveData.setValue(false);
                Log.e("PrixodViewModel", "Ошибка при загрузке данных с сервера: " + exception.getMessage(), exception);
                

                if (productsJson != null && !productsJson.isEmpty()) {
                    try {
                        Log.d("PrixodViewModel", "Сервер недоступен, восстанавливаем данные из кэша");
                        
                        Type type = new TypeToken<List<ProductDto>>(){}.getType();
                        List<ProductDto> savedDtoProducts = gson.fromJson(productsJson, type);
                        
                        if (savedDtoProducts != null && !savedDtoProducts.isEmpty()) {
                            // Преобразуем DTO в доменные модели
                            List<Product> cachedProducts = productMapper.mapToDomainList(savedDtoProducts);
                            

                            Invoice cachedInvoice = new Invoice(
                                moveUuid,
                                cachedProducts
                            );
                            

                            originalProductList = new ArrayList<>(cachedProducts);
                            _currentInvoiceLiveData.setValue(cachedInvoice);
                            applyFiltersAndSort();
                            

                            _originalProductListUpdatedEvent.setValue(new Event<>(true));
                            

                            validateAllProducts();
                            
                            // Повторно применяем режим "только чтение"
                            String currentStatus = getCurrentMoveStatus();
                            if (currentStatus != null) {
                                Log.d("PrixodViewModel", "Повторно применяем режим 'только чтение' после загрузки данных из кэша для статуса: " + currentStatus);
                                updateReadOnlyMode(currentStatus);
                            }
                            

                            _errorLiveData.setValue(new Event<>("Ответ от сервера не получен. Используются сохраненные данные."));
                            

                            _dataSuccessfullyLoadedEvent.setValue(new Event<>(true));
                            
                            Log.d("PrixodViewModel", "Данные успешно восстановлены из кэша после ошибки сервера. Продуктов: " + cachedProducts.size());
                            return;
                        } else {
                            Log.w("PrixodViewModel", "Кэшированные данные пусты или некорректны");
                        }
                    } catch (Exception cacheException) {
                        Log.e("PrixodViewModel", "Ошибка при восстановлении данных из кэша: " + cacheException.getMessage(), cacheException);
                    }
                }
                

                _errorLiveData.setValue(new Event<>("Ошибка при загрузке документа: " + exception.getMessage()));
                _productsLiveData.setValue(new ArrayList<>());
            }
        });
    }
    
    /**
     * Обрабатывает данные продуктов из кеша без сопоставления по УИДСтрокиТовары
     */
    private void processLoadedDataFromCache(Invoice invoice, String productsJson, boolean preserveEditedData, 
                                          MoveItem moveItem) {
        _isLoadingLiveData.setValue(false);
        
        if (invoice == null || invoice.getProducts() == null) {
            _errorLiveData.setValue(new Event<>("Не удалось загрузить документ или список продуктов пуст."));
            _productsLiveData.setValue(new ArrayList<>());
            _currentInvoiceLiveData.setValue(null);
            return;
        }

        List<Product> loadedProducts = new ArrayList<>(invoice.getProducts());
        Log.d("PrixodViewModel", "Загружено " + loadedProducts.size() + " продуктов из кеша");

        // Сохраняем оригинальный список продуктов
        originalProductList = new ArrayList<>(loadedProducts);
        _currentInvoiceLiveData.setValue(invoice);
        applyFiltersAndSort();
        
        // Уведомляем об обновлении оригинального списка продуктов
        _originalProductListUpdatedEvent.setValue(new Event<>(true));
        
        // Проверяем наличие ошибок валидации в загруженных данных
        validateAllProducts();
        
        // Применяем режим "только чтение"
        String currentStatus = getCurrentMoveStatus();
        if (currentStatus != null) {
            Log.d("PrixodViewModel", "Применяем режим 'только чтение' после загрузки данных из кеша для статуса: " + currentStatus);
            updateReadOnlyMode(currentStatus);
        }
        

        productsDataManager.saveProductsData(invoice.getUuid(), loadedProducts);
        Log.d("PrixodViewModel", "Продукты обновлены в кеше для перемещения: " + invoice.getUuid());
        

        _dataSuccessfullyLoadedEvent.setValue(new Event<>(true));
        
        Log.d("PrixodViewModel", "Данные успешно загружены из кеша. Продуктов: " + loadedProducts.size());
    }


    private void validateAllProducts() {
        if (originalProductList == null || originalProductList.isEmpty()) return;
        
        Set<String> errorUuids = new HashSet<>();
        for (Product product : originalProductList) {
            if (product != null && product.getProductLineId() != null) {
                if (product.getTaken() > 0) {
                    boolean isValid = validateTakenValueByProductLineId(product.getProductLineId(), product.getTaken());
                    if (!isValid) {
                        errorUuids.add(product.getProductLineId());
                    }
                }
            }
        }
        
        if (!errorUuids.isEmpty()) {
            _validationErrorUuidsLiveData.setValue(errorUuids);
            Log.d("PrixodViewModel", "Обнаружено " + errorUuids.size() + " ошибок валидации после загрузки данных");
        }
    }


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


        Log.w("PrixodViewModel", "Не удалось извлечь UUID из данных сканера: " + barcodeData + ". Последняя часть: " + potentialUuid);
        return null;
    }

    /**
     * Универсальный метод поиска позиции продукта по различным критериям
     * @param productList список продуктов для поиска
     * @param uuid UUID для поиска (номенклатура или серия)
     * @param searchType тип поиска (по какому полю искать)
     * @return индекс найденного продукта или -1, если не найден
     */
    private int findProductPosition(List<Product> productList, String uuid, SearchType searchType) {
        if (productList == null || uuid == null || uuid.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < productList.size(); i++) {
            Product product = productList.get(i);
            if (product != null) {
                switch (searchType) {
                    case NOMENCLATURE_UUID:
                        if (uuid.equals(product.getNomenclatureUuid())) {
                            return i;
                        }
                        break;
                    case SERIES_UUID:
                        if (uuid.equals(product.getSeriesUuid())) {
                            return i;
                        }
                        break;
                }
            }
        }
        return -1;
    }



    public void processBarcodeData(String barcodeData) {
        if (TextUtils.isEmpty(barcodeData)) {
            _errorLiveData.setValue(new Event<>("Данные сканирования пусты"));
            return;
        }

        // Обрабатываем данные штрих-кода, чтобы извлечь UUID серии
        String seriesUuid = extractUUID(barcodeData);
        if (TextUtils.isEmpty(seriesUuid)) {
            _errorLiveData.setValue(new Event<>("Не удалось распознать код как UUID серии"));
            return;
        }

        // Ищем все товары с данной серией
        List<Product> foundProducts = findProductsBySeriesUuid(seriesUuid);
        
        if (foundProducts.isEmpty()) {
            // Проверяем, активны ли фильтры
            boolean filtersActive = Boolean.TRUE.equals(_isAnyFilterActiveLiveData.getValue());
            
            if (filtersActive) {
                // Если фильтры активны, сбрасываем их и повторяем поиск
                _productNotFoundForFocusEvent.setValue(new Event<>("Товар не найден с текущими фильтрами. Фильтры сброшены. Повторный поиск..."));
                resetAllFilters();
                
                // Повторный поиск после сброса фильтров
                foundProducts = findProductsBySeriesUuid(seriesUuid);
                
                if (foundProducts.isEmpty()) {

                    _productNotFoundForFocusEvent.setValue(new Event<>("Товар не найден по серии: " + seriesUuid));
                } else if (foundProducts.size() == 1) {

                    processSingleProductFound(foundProducts.get(0));
                } else {

                    handleMultipleProductsFound(foundProducts, seriesUuid);
                }
            } else {
                // Фильтры не активны, товар просто не найден
                _productNotFoundForFocusEvent.setValue(new Event<>("Товар не найден по серии: " + seriesUuid));
            }
        } else if (foundProducts.size() == 1) {

            processSingleProductFound(foundProducts.get(0));
        } else {

            handleMultipleProductsFound(foundProducts, seriesUuid);
        }
    }

    /**
     * Обрабатывает случай, когда найден один товар по серии
     * @param product найденный товар
     */
    private void processSingleProductFound(Product product) {
        if (product == null) return;

        List<Product> currentDisplayList = _productsLiveData.getValue();
        if (currentDisplayList == null) {
            Log.w("PrixodViewModel", "processSingleProductFound: currentDisplayList is null");
            return;
        }
        
        int foundPosition = findProductPosition(currentDisplayList, 
                                              product.getSeriesUuid(),
                                              SearchType.SERIES_UUID);
        
        if (foundPosition != -1) {
            Log.d("PrixodViewModel", "processSingleProductFound: найден товар '" + 
                  product.getNomenclatureName() + "' на позиции " + foundPosition + " в отображаемом списке");
            

            if (isCurrentlyReadOnly()) {
                _focusProductPositionLiveData.setValue(new Event<>(foundPosition));
            } else {

                _focusAndSelectProductPositionLiveData.setValue(new Event<>(foundPosition));
            }
        } else {

            Log.d("PrixodViewModel", "processSingleProductFound: товар '" + 
                  product.getNomenclatureName() + "' скрыт фильтрами, сбрасываем фильтры");
            
            // Сбрасываем фильтры для отображения товара
            _productNotFoundForFocusEvent.setValue(new Event<>("Фильтры сброшены для отображения найденного товара"));
            resetAllFilters();
            

            List<Product> listAfterReset = _productsLiveData.getValue();
            if (listAfterReset != null) {
                int newPosition = findProductPosition(listAfterReset,
                        product.getSeriesUuid(),
                        SearchType.SERIES_UUID);
                if (newPosition != -1) {
                    Log.d("PrixodViewModel", "processSingleProductFound: после сброса фильтров найден товар на позиции " + newPosition);
                    

                    if (isCurrentlyReadOnly()) {
                        _focusProductPositionLiveData.setValue(new Event<>(newPosition));
                    } else {

                        _focusAndSelectProductPositionLiveData.setValue(new Event<>(newPosition));
                    }
                } else {
                    Log.e("PrixodViewModel", "processSingleProductFound: товар не найден даже после сброса фильтров");
                    _productNotFoundForFocusEvent.setValue(new Event<>("Ошибка при отображении найденного товара"));
                }
            }
        }
    }

    /**
     * Обрабатывает случай, когда по серии найдено несколько товаров
     * @param foundProducts список найденных товаров
     * @param seriesUuid UUID серии
     */
    private void handleMultipleProductsFound(List<Product> foundProducts, String seriesUuid) {
        if (isCurrentlyReadOnly()) {
            if (!foundProducts.isEmpty()) {
                processSingleProductFound(foundProducts.get(0));
            }
            return;
        }
        

        List<Product> currentDisplayList = _productsLiveData.getValue();
        if (currentDisplayList == null || currentDisplayList.isEmpty()) {
            Log.w("PrixodViewModel", "handleMultipleProductsFound: currentDisplayList is null or empty");
            _productNotFoundForFocusEvent.setValue(new Event<>("Фильтры сброшены для отображения найденных товаров"));
            resetAllFilters();
            currentDisplayList = _productsLiveData.getValue();
        }
        

        List<Product> visibleFoundProducts = new ArrayList<>();
        for (Product foundProduct : foundProducts) {

            boolean isVisible = currentDisplayList.stream()
                .anyMatch(p -> p.getNomenclatureUuid().equals(foundProduct.getNomenclatureUuid()));
            if (isVisible) {
                visibleFoundProducts.add(foundProduct);
            }
        }
        
        // Если после фильтрации не осталось видимых товаров, сбрасываем фильтры
        if (visibleFoundProducts.isEmpty()) {
            Log.d("PrixodViewModel", "handleMultipleProductsFound: все найденные товары скрыты фильтрами, сбрасываем фильтры");
            _productNotFoundForFocusEvent.setValue(new Event<>("Фильтры сброшены для отображения найденных товаров"));
            resetAllFilters();
            visibleFoundProducts = foundProducts;
        }
        
        // Если остался только один видимый товар, обрабатываем как одиночный
        if (visibleFoundProducts.size() == 1) {
            processSingleProductFound(visibleFoundProducts.get(0));
            return;
        }
        
        // Считаем общее количество для видимых товаров
        int totalQuantity = 0;
        for (Product product : visibleFoundProducts) {
            totalQuantity += product.getQuantity();
        }
        
        // Определяем место хранения отправителя
        String senderStorage = "";
        if (!visibleFoundProducts.isEmpty()) {
            senderStorage = visibleFoundProducts.get(0).getSenderStorageName();
            if (TextUtils.isEmpty(senderStorage)) {
                senderStorage = "Не указано";
            }
        }
        
        // Отправляем событие для отображения диалога с видимыми товарами
        _multipleProductsFoundEvent.setValue(
            new Event<>(new MultipleProductsData(visibleFoundProducts, totalQuantity, senderStorage))
        );
    }

    /**
     * Находит все продукты по UUID серии
     * @param seriesUuid UUID серии
     * @return список найденных продуктов
     */
    private List<Product> findProductsBySeriesUuid(String seriesUuid) {
        List<Product> result = new ArrayList<>();
        
        if (originalProductList == null || TextUtils.isEmpty(seriesUuid)) {
            return result;
        }
        
        for (Product product : originalProductList) {
            if (seriesUuid.equals(product.getSeriesUuid())) {
                result.add(product);
            }
        }
        
        return result;
    }



    /**
     * Отмечает все найденные товары как полностью скомплектованные
     * @param products список товаров для отметки
     * @param shouldScrollToFirst если true, после отметки прокрутить к первому товару серии
     * @param seriesUuid UUID серии для определения первого товара (если shouldScrollToFirst = true)
     */
    public void markAllProductsAsCompleted(List<Product> products, boolean shouldScrollToFirst, String seriesUuid) {
        if (products == null || products.isEmpty()) {
            return;
        }
        
        Log.d("PrixodViewModel", "markAllProductsAsCompleted: Начинаем обработку " + products.size() + " товаров");
        
        boolean anyUpdated = false;
        
        // Собираем все уникальные UUID серий из переданных товаров
        Set<String> seriesUuids = new HashSet<>();
        for (Product product : products) {
            if (product.getSeriesUuid() != null && !product.getSeriesUuid().isEmpty()) {
                seriesUuids.add(product.getSeriesUuid());
                Log.d("PrixodViewModel", "markAllProductsAsCompleted: Добавлена серия " + product.getSeriesUuid() + 
                      " для товара " + product.getNomenclatureName());
            }
        }
        
        Log.d("PrixodViewModel", "markAllProductsAsCompleted: Найдено " + seriesUuids.size() + " уникальных серий");
        
        // Для каждой серии находим все товары и устанавливаем максимальные значения
        for (String series : seriesUuids) {

            List<Product> productsWithSeries = findProductsBySeriesUuid(series);
            Log.d("PrixodViewModel", "markAllProductsAsCompleted: Для серии " + series +
                  " найдено " + productsWithSeries.size() + " товаров");
            
            for (Product product : productsWithSeries) {
                double maxQuantity = product.getQuantity();
                
                Log.d("PrixodViewModel", "markAllProductsAsCompleted: Обрабатываем товар " + 
                      product.getNomenclatureName() + " (UUID: " + product.getNomenclatureUuid() + 
                      "), текущий taken=" + product.getTaken() + ", целевой=" + maxQuantity);
                

                if (product.getTaken() == maxQuantity) {
                    Log.d("PrixodViewModel", "markAllProductsAsCompleted: Пропускаем товар " + 
                          product.getNomenclatureName() + " - уже скомплектован");
                    continue;
                }
                

                double oldTakenValue = product.getTaken();
                

                product.setTaken(maxQuantity);
                

                if (oldTakenValue != maxQuantity) {
                    anyUpdated = true;
                    Log.d("PrixodViewModel", "Обновлен товар: " + product.getNomenclatureName() + 
                          " (UUID: " + product.getNomenclatureUuid() + "), установлено значение taken = " + maxQuantity);
                }

                Set<String> currentErrors = _validationErrorUuidsLiveData.getValue() != null ? 
                                          new HashSet<>(_validationErrorUuidsLiveData.getValue()) : 
                                          new HashSet<>();
                

                boolean isValid = validateTakenValueByProductLineId(product.getProductLineId(), maxQuantity);
                
                if (!isValid) {
                    currentErrors.add(product.getProductLineId());
                } else {
                    currentErrors.remove(product.getProductLineId());
                }
                

                if (!currentErrors.equals(_validationErrorUuidsLiveData.getValue())) {
                    _validationErrorUuidsLiveData.setValue(currentErrors);
                }
            }
        }
        

        if (anyUpdated) {
            Log.d("PrixodViewModel", "markAllProductsAsCompleted: Были обновления, обновляем UI");
            

            _originalProductListUpdatedEvent.setValue(new Event<>(true));
            

            new Handler(Looper.getMainLooper()).post(() -> {

                List<Product> updatedList = new ArrayList<>(originalProductList);
                

                applyFiltersAndSort();
                

                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    List<Product> currentList = _productsLiveData.getValue();
                    if (currentList != null) {

                        List<Product> refreshedList = new ArrayList<>(currentList);
                        _productsLiveData.setValue(refreshedList);
                    }
                    

                    _productsMarkedAsCompletedEvent.setValue(new Event<>(true));
                    
                    Log.d("PrixodViewModel", "Принудительное обновление UI выполнено");
                }, 100);
            });
            
            Log.d("PrixodViewModel", "Все найденные товары отмечены как скомплектованные, UI обновлен");
            
            // Если нужно прокрутить к первому товару серии
            if (shouldScrollToFirst && seriesUuid != null) {
                scrollToFirstProductOfSeries(seriesUuid);
            }
        } else {
            Log.d("PrixodViewModel", "Не было изменений при отметке товаров как скомплектованных");
            

            _productsMarkedAsCompletedEvent.setValue(new Event<>(false));

            if (shouldScrollToFirst && seriesUuid != null) {
                scrollToFirstProductOfSeries(seriesUuid);
            }
        }
    }

    /**
     * Прокручивает к первому товару указанной серии в отображаемом списке
     * @param seriesUuid UUID серии
     */
    private void scrollToFirstProductOfSeries(String seriesUuid) {
        if (seriesUuid == null || seriesUuid.isEmpty()) {
            Log.w("PrixodViewModel", "scrollToFirstProductOfSeries: seriesUuid is null or empty");
            return;
        }
        
        List<Product> currentDisplayList = _productsLiveData.getValue();
        if (currentDisplayList == null || currentDisplayList.isEmpty()) {
            Log.w("PrixodViewModel", "scrollToFirstProductOfSeries: currentDisplayList is null or empty");
            return;
        }
        
        // Ищем первый товар с указанной серией в отображаемом списке
        int firstPositionOfSeries = -1;
        for (int i = 0; i < currentDisplayList.size(); i++) {
            Product product = currentDisplayList.get(i);
            if (product != null && seriesUuid.equals(product.getSeriesUuid())) {
                firstPositionOfSeries = i;
                break;
            }
        }
        
        if (firstPositionOfSeries != -1) {
            Log.d("PrixodViewModel", "scrollToFirstProductOfSeries: найден первый товар серии " + 
                  seriesUuid + " на позиции " + firstPositionOfSeries);
            
            // Отправляем событие прокрутки
            int finalFirstPositionOfSeries = firstPositionOfSeries;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                _scrollToPositionEvent.setValue(new Event<>(finalFirstPositionOfSeries));
            }, 200);
        } else {
            Log.w("PrixodViewModel", "scrollToFirstProductOfSeries: товар с серией " + 
                  seriesUuid + " не найден в отображаемом списке");
        }
    }

    // Методы для управления фильтрацией
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
        applyFiltersAndSort();
    }

    // Методы для управления сортировкой
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
    

    private void applyFiltersAndSort() {
        List<Product> processedList = new ArrayList<>(originalProductList);

        // 1. Фильтрация
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

            processedList.removeIf(p -> p.getTaken() == p.getQuantity());
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
    
    // Вспомогательный метод для сравнения мест хранения
    private int compareStorageNames(String storage1, String storage2) {
        if (storage1 == null) storage1 = "";
        if (storage2 == null) storage2 = "";


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
     * Собирает текущий список продуктов (originalProductList, который содержит актуальные "taken"),
     * мапит его в List<ProductDto> и сериализует в JSON-строку.
     * @return JSON-строка списка продуктов для сохранения или отправки.
     */
    public String getProductsToSaveAsJson() {
        if (originalProductList == null) {

            Log.e("PrixodViewModel", "originalProductList is null в getProductsToSaveAsJson");
            return gson.toJson(new ArrayList<ProductDto>());
        }
        if (productMapper == null) {
            Log.e("PrixodViewModel", "ProductMapper is null в getProductsToSaveAsJson. DI issue?");

            return gson.toJson(new ArrayList<ProductDto>()); 
        }
        

        List<ProductDto> dtoList = productMapper.mapToDtoList(originalProductList);
        
        // Логируем
        int nonZeroTakenCount = 0;
        int zeroTakenCount = 0;
        int totalProductsCount = originalProductList.size();
        
        for (Product product : originalProductList) {
            if (product.getTaken() > 0) {
                nonZeroTakenCount++;
                Log.d("PrixodViewModel", "Сохраняем продукт " + product.getNomenclatureName() + 
                      " с taken = " + product.getTaken());
            } else {
                zeroTakenCount++;
                Log.d("PrixodViewModel", "Сохраняем продукт " + product.getNomenclatureName() + 
                      " с нулевым/пустым taken = " + product.getTaken());
            }
        }
        
        Log.d("PrixodViewModel", "Итоги сохранения: всего продуктов = " + totalProductsCount +
              ", с заполненными значениями = " + nonZeroTakenCount + 
              ", с нулевыми значениями = " + zeroTakenCount);
              
        // Сериализуем список DTO в JSON
        String jsonResult = gson.toJson(dtoList);
        
        // Детальное логирование JSON
        Log.d("PrixodViewModel", "=== ГЕНЕРАЦИЯ JSON ДЛЯ СОХРАНЕНИЯ ===");
        Log.d("PrixodViewModel", "Количество DTO объектов: " + dtoList.size());
        Log.d("PrixodViewModel", "Размер JSON: " + jsonResult.length() + " символов");
        
        // Логируем краткую информацию о каждом DTO
        for (int i = 0; i < dtoList.size(); i++) {
            com.step.tcd_rpkb.data.network.dto.ProductDto dto = dtoList.get(i);
            Log.d("PrixodViewModel", "DTO #" + (i + 1) + ": " +
                      "ID=" + dto.getProductLineId() +
                      ", parentID=" + dto.getParentProductLineId() +
                      ", nomenclature=" + dto.getNomenclatureName() +
                      ", series=" + dto.getSeriesName() +
                      ", quantity=" + dto.getQuantity() +
                      ", taken=" + dto.getTaken() +
                      ", exists=" + dto.getExists());
        }
        
        // Логируем полный JSON
        if (jsonResult.length() < 5000) {
            Log.d("PrixodViewModel", "Полный JSON для сохранения: " + jsonResult);
        } else {
            Log.d("PrixodViewModel", "JSON (первые 1000 символов): " + jsonResult.substring(0, 1000) + "...");
            Log.d("PrixodViewModel", "JSON (последние 500 символов): ..." + jsonResult.substring(jsonResult.length() - 500));
        }
        
        Log.d("PrixodViewModel", "=== КОНЕЦ ГЕНЕРАЦИИ JSON ===");
        
        return jsonResult;
    }

    /**
     * Вспомогательный метод для быстрого поиска продукта по productLineId.
     */
    private Product findProductByProductLineId(String productLineId) {
        if (originalProductList == null || TextUtils.isEmpty(productLineId)) {
            return null;
        }
        
        for (Product product : originalProductList) {
            if (java.util.Objects.equals(productLineId, product.getProductLineId())) {
                return product;
            }
        }
        
        return null;
    }

    /**
     * Централизованный метод для валидации вводимого значения taken по productLineId.
     * @param productLineId UUID строки товара (УИДСтрокиТовары)
     * @param takenValue проверяемое значение
     * @return true, если значение валидно; false в противном случае
     */
    public boolean validateTakenValueByProductLineId(String productLineId, double takenValue) {
        if (originalProductList == null || productLineId == null) {
            Log.e("PrixodViewModel", "originalProductList или productLineId is null в validateTakenValueByProductLineId");
            return false;
        }

        // Быстрый поиск продукта по productLineId
        Product product = findProductByProductLineId(productLineId);

        if (product != null) {

            boolean isValid = takenValue >= 0 && takenValue <= product.getQuantity();
            
            if (!isValid) {
                Log.d("PrixodViewModel", "Невалидное значение: " + takenValue + 
                      " для продукта " + product.getNomenclatureName() + 
                      " (productLineId=" + productLineId + ")" +
                      " (допустимые пределы: 0-" + product.getQuantity() + ")");
            }
            
            return isValid;
        } else {
            Log.e("PrixodViewModel", "Продукт с productLineId: " + productLineId + 
                  " не найден в originalProductList.");
            return false;
        }
    }

    /**
     * Возвращает оригинальный (полный) список продуктов
     * @return Полный список продуктов без учета фильтров
     */
    public List<Product> getOriginalProductList() {
        return originalProductList;
    }
    
    /**
     * Обновляет значение 'taken' для продукта и управляет списком ошибок.
     * @param productLineId UUID строки товара (УИДСтрокиТовары).
     * @param newTakenValue Новое значение 'taken'.
     * @param isValidFromAdapter Валидность, сообщенная адаптером (true, если адаптер считает значение корректным).
     * @return true, если значение 'taken' было успешно обновлено и считается валидным; false в противном случае.
     */
    private boolean updateProductTakenValueAndErrorState(String productLineId, double newTakenValue, boolean isValidFromAdapter) {
        if (TextUtils.isEmpty(productLineId)) {
            Log.w("PrixodViewModel", "Не указан productLineId");
            return false;
        }

        // Находим продукт по productLineId
        Product targetProduct = findProductByProductLineId(productLineId);
        
        if (targetProduct != null) {

            double oldTakenValue = targetProduct.getTaken();

            targetProduct.setTaken(newTakenValue);

            if (oldTakenValue != newTakenValue) {
                Log.d("PrixodViewModel", "Значение taken изменено с " + oldTakenValue + " на " + newTakenValue + " для productLineId " + productLineId);
            }

            boolean isValid = validateTakenValueByProductLineId(productLineId, newTakenValue);

            Set<String> currentErrors = _validationErrorUuidsLiveData.getValue() != null ? 
                                      new HashSet<>(_validationErrorUuidsLiveData.getValue()) : 
                                      new HashSet<>();
            
            if (!isValid) {
                currentErrors.add(productLineId);
                Log.w("PrixodViewModel", "Ошибка валидации для productLineId " + productLineId + ", taken = " + newTakenValue);
            } else {
                currentErrors.remove(productLineId);
            }

            if (!currentErrors.equals(_validationErrorUuidsLiveData.getValue())) {
                _validationErrorUuidsLiveData.setValue(currentErrors);
            }
            
            return isValid;
        } else {
            Log.w("PrixodViewModel", "Продукт с productLineId " + productLineId + " не найден");
            return false;
        }
    }
    

    public void handleProductDataConfirmation(String productLineId, double newTakenValue, boolean isValidFromAdapter, boolean byEnterKey, int currentPositionInAdapter) {
        Log.d("PrixodViewModel", "Обработка подтверждения данных для productLineId: " + productLineId + 
                           ", значение: " + newTakenValue + ", isValidFromAdapter: " + isValidFromAdapter + 
                           ", byEnterKey: " + byEnterKey + ", позиция: " + currentPositionInAdapter);

        // Проверяем режим "только чтение"
        if (isCurrentlyReadOnly()) {
            Log.d("PrixodViewModel", "Режим 'только чтение' активен. Изменения данных заблокированы.");
            _validationErrorMessageEvent.setValue(new Event<>("Редактирование недоступно в режиме просмотра"));
            return;
        }

        // Проверяем, было ли изменение значения
        boolean valueChanged = false;
        boolean errorStatusChanged = false;
        
        // Проверяем текущее значение продукта
        double currentTakenValue = 0;
        for (Product p : originalProductList) {
            if (java.util.Objects.equals(productLineId, p.getProductLineId())) {
                currentTakenValue = p.getTaken();
                break;
            }
        }

        boolean progressRelevantChange = (currentTakenValue == 0 && newTakenValue > 0) || 
                                         (currentTakenValue > 0 && newTakenValue == 0);
        

        boolean isDataValidByViewModel = updateProductTakenValueAndErrorState(productLineId, newTakenValue, isValidFromAdapter);
        
        // Проверяем, изменилось ли значение
        for (Product p : originalProductList) {
            if (java.util.Objects.equals(productLineId, p.getProductLineId())) {
                if (p.getTaken() != currentTakenValue) {
                    valueChanged = true;

                    if (progressRelevantChange) {
                        _originalProductListUpdatedEvent.setValue(new Event<>(true));
                    }
                }
                break;
            }
        }
        
        // Если данные не валидны, отправляем событие для анимации тряски и сообщение об ошибке
        if (!isDataValidByViewModel) {
            _shakeViewEvent.setValue(new Event<>(productLineId));
            

            String errorMessage = "Введено некорректное значение";
            _validationErrorMessageEvent.setValue(new Event<>(errorMessage));

            if (byEnterKey) {
                Log.d("PrixodViewModel", "Нажат Enter на невалидном поле, фокусируемся на текущем поле");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    _focusProductPositionLiveData.setValue(new Event<>(currentPositionInAdapter));
                }, 50);
            }
        }

        if (valueChanged) {
            Log.d("PrixodViewModel", "Произошли изменения, обновляем UI (valueChanged=" + valueChanged + ")");
                  
            // Отладочное логирование после изменения данных
            logAllTakenValues("ПОСЛЕ_ИЗМЕНЕНИЯ_ДАННЫХ_" + productLineId);
                  
            new Handler(Looper.getMainLooper()).post(() -> {

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    applyFiltersAndSort();
                }, 150);
            });
        } else {
            Log.d("PrixodViewModel", "Изменений не произошло, UI не обновляется");
        }

        if (!isDataValidByViewModel) {

            Log.d("PrixodViewModel", "Данные не валидны для productLineId: " + productLineId + ". Фокус остается на текущем элементе.");
            return;
        }

        if (byEnterKey) {
            Log.d("PrixodViewModel", "Нажат Enter, данные валидны. Переходим к следующему полю.");
            handleFocusMoveToNextField(currentPositionInAdapter);
        }
    }


    public void requestFocusOnError(String errorUuid) {
        if (errorUuid == null || errorUuid.isEmpty() || originalProductList == null) {
            new Handler(Looper.getMainLooper()).post(() -> _productNotFoundForFocusEvent.setValue(new Event<>("Не удалось найти товар для фокусировки (нет UUID или списка).")));
            return;
        }

        boolean productFound = false;
        int positionInCurrentList = -1;


        List<Product> currentDisplayList = _productsLiveData.getValue();
        if (currentDisplayList != null) {
            positionInCurrentList = findProductPosition(currentDisplayList, errorUuid, SearchType.NOMENCLATURE_UUID);
        }

        if (positionInCurrentList != -1) {

            _focusProductPositionLiveData.setValue(new Event<>(positionInCurrentList));
            productFound = true;
        } else {
            int positionInOriginalList = findProductPosition(originalProductList, errorUuid, SearchType.NOMENCLATURE_UUID);
            if (positionInOriginalList != -1) {
                _forceResetFiltersMessageLiveData.setValue(new Event<>("Фильтры сброшены для отображения товара с ошибкой."));
                resetAllFilters();


                List<Product> listAfterReset = _productsLiveData.getValue();
                if (listAfterReset != null) {
                    int newPosition = findProductPosition(listAfterReset, errorUuid, SearchType.NOMENCLATURE_UUID);
                    if (newPosition != -1) {
                        _focusProductPositionLiveData.setValue(new Event<>(newPosition));
                        productFound = true;
                    } else {
                        Log.e("PrixodViewModel", "Товар " + errorUuid + " найден в original, но не найден после resetAllFilters. Проблема с сортировкой?");
                         _productNotFoundForFocusEvent.setValue(new Event<>("Ошибка при попытке показать товар: " + errorUuid));
            }
                } else {
                     Log.e("PrixodViewModel", "_productsLiveData is null after resetAllFilters in requestFocusOnError");
                    _productNotFoundForFocusEvent.setValue(new Event<>("Ошибка при обновлении списка для показа товара."));
                }
            } else {
                _productNotFoundForFocusEvent.setValue(new Event<>("Товар с ошибкой (UUID: " + errorUuid + ") не найден в документе."));
            }
        }

        if (!productFound) {
            Log.w("PrixodViewModel", "Товар с ошибкой " + errorUuid + " не был найден для фокусировки.");
        }
    }

    public void handleFocusMoveToNextField(int currentPosition) {
        Log.d("PrixodViewModel", "Запрос на перемещение фокуса к следующему полю от позиции " + currentPosition);
        
        // Получаем текущий список продуктов
        List<Product> currentProductList = _productsLiveData.getValue();
        if (currentProductList == null || currentProductList.isEmpty()) {
            Log.d("PrixodViewModel", "Список продуктов пуст, невозможно переместить фокус");
            return;
        }
        
        Log.d("PrixodViewModel", "Размер текущего списка продуктов: " + currentProductList.size());
        
        // Проверяем, корректна ли текущая позиция
        if (currentPosition < 0 || currentPosition >= currentProductList.size()) {
            Log.d("PrixodViewModel", "Некорректная текущая позиция: " + currentPosition + ", размер списка: " + currentProductList.size());
            return;
        }
        
        // Переходим к следующей позиции
        int nextPosition = currentPosition + 1;
        Log.d("PrixodViewModel", "Вычислена следующая позиция: " + nextPosition);

        if (nextPosition < currentProductList.size()) {
            Log.d("PrixodViewModel", "Перемещаем фокус к позиции " + nextPosition + " (позиция корректна, в пределах списка размером " + currentProductList.size() + ")");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                _focusProductPositionLiveData.setValue(new Event<>(nextPosition));
            }, 100);
        } else {
            Log.d("PrixodViewModel", "Достигнут конец списка: следующая позиция " + nextPosition + " >= размера списка " + currentProductList.size() + ", невозможно переместить фокус дальше");
        }
    }

    /**
     * Возвращает форматированную дату для отображения
     */
    public String getFormattedMoveDate() {
        MoveItem moveItem = _moveItemLiveData.getValue();
        Log.d("PrixodViewModel", "getFormattedMoveDate: moveItem = " + (moveItem != null ? "доступен" : "null"));
        if (moveItem != null && moveItem.getDate() != null && !moveItem.getDate().isEmpty()) {
            String dateStr = moveItem.getDate();
            Log.d("PrixodViewModel", "getFormattedMoveDate: moveItem.getDate() = '" + dateStr + "'");
            
            // Конвертируем дату из формата "yyyy-MM-dd" или "yyyy-MM-dd'T'HH:mm:ss" в "dd.MM.yyyy HH:mm"
            try {
                java.text.SimpleDateFormat inputFormat;
                

                if (dateStr.contains("T")) {
                    inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                } else {
                    inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                }

                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault());
                
                java.util.Date date = inputFormat.parse(dateStr);
                if (date != null) {
                    String formattedDate = outputFormat.format(date);
                    Log.d("PrixodViewModel", "getFormattedMoveDate: отформатированная дата = '" + formattedDate + "'");
                    return formattedDate;
                }
            } catch (Exception e) {
                Log.e("PrixodViewModel", "Ошибка парсинга даты: " + moveItem.getDate(), e);
            }
        }
        Log.d("PrixodViewModel", "getFormattedMoveDate: возвращаем пустую строку");
        return "";
    }
    
    /**
     * Возвращает номер перемещения
     */
    public String getMoveNumber() {
        MoveItem moveItem = _moveItemLiveData.getValue();
        Log.d("PrixodViewModel", "getMoveNumber: moveItem = " + (moveItem != null ? "доступен" : "null"));
        if (moveItem != null) {
            String number = moveItem.getNumber();
            Log.d("PrixodViewModel", "getMoveNumber: moveItem.getNumber() = '" + number + "'");
            if (number != null && !number.isEmpty()) {
                return number;
            }
            
            // Если номер не задан, пробуем извлечь из movementDisplayText
            String displayText = moveItem.getMovementDisplayText();
            Log.d("PrixodViewModel", "getMoveNumber: movementDisplayText = '" + displayText + "'");
            if (displayText != null && !displayText.isEmpty()) {
                // формат: "Перемещение P000123456 от 01.01.2023"
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("P\\d+");
                java.util.regex.Matcher matcher = pattern.matcher(displayText);
                if (matcher.find()) {
                    String extractedNumber = matcher.group();
                    Log.d("PrixodViewModel", "getMoveNumber: извлечен номер из displayText = '" + extractedNumber + "'");
                    return extractedNumber;
                }
            }
        }
        Log.d("PrixodViewModel", "getMoveNumber: возвращаем пустую строку");
        return "";
    }
    
    /**
     * Обрабатывает нажатие кнопки "Отпр. в \"Подготовленно\""
     */
    public void handleSendTo1C() {
        Log.d("PrixodViewModel", "Обработка нажатия кнопки 'Отпр. в \"Подготовленно\"'");
        
        // Сначала показываем диалог со статистикой
        ProductsStatistics statistics = calculateProductsStatistics();
        _showStatisticsConfirmationEvent.setValue(new Event<>(statistics));
    }
    
    /**
     * Подтверждение отправки в "Подготовлено" после показа статистики
     */
    public void confirmSendTo1C() {
        Log.d("PrixodViewModel", "Подтверждена отправка в 'Подготовлено'");
        
        // Логируем текущие значения taken перед отправкой
        logAllTakenValues("ПОДТВЕРЖДЕНИЕ_ОТПРАВКИ_В_1С");
        returnToMenuWithStatusChange();
    }

    /**
     * Подсчитывает статистику по товарам для диалога подтверждения
     * @return объект со статистикой по товарам
     */
    private ProductsStatistics calculateProductsStatistics() {
        if (originalProductList == null || originalProductList.isEmpty()) {
            return new ProductsStatistics(0, 0, 0, 0);
        }
        
        int totalProducts = originalProductList.size();
        int fullyCompleted = 0;      // taken == quantity
        int partiallyCompleted = 0;  // taken != quantity && taken != 0
        int notCompleted = 0;        // taken == 0
        
        for (Product product : originalProductList) {
            if (product == null) continue;
            
            double taken = product.getTaken();
            double quantity = product.getQuantity();
            
            if (taken == 0) {
                notCompleted++;
            } else if (taken == quantity) {
                fullyCompleted++;
            } else {
                partiallyCompleted++;
            }
        }
        
        Log.d("PrixodViewModel", "Статистика товаров: всего=" + totalProducts + 
              ", полностью=" + fullyCompleted + 
              ", частично=" + partiallyCompleted + 
              ", нескомплектовано=" + notCompleted);
        
        return new ProductsStatistics(totalProducts, fullyCompleted, partiallyCompleted, notCompleted);
    }
    
    /**
     * Возвращается в меню с флагом для смены статуса на "Подготовлен"
     */
    private void returnToMenuWithStatusChange() {
        Log.d("PrixodViewModel", "Возвращаемся в меню для смены статуса на 'Подготовлен'");
        
        // Сначала сохраняем продукты во временный файл
        // чтобы актуальные значения taken попали в JSON для document_save
        if (currentMoveUuid != null && !currentMoveUuid.isEmpty() && originalProductList != null) {
            boolean saved = productsDataManager.saveProductsData(currentMoveUuid, originalProductList);
            if (saved) {
                Log.d("PrixodViewModel", "Продукты сохранены во временный файл перед сменой статуса для moveUuid: " + 
                      currentMoveUuid + ", количество: " + originalProductList.size());
                      

                logAllTakenValues("ПЕРЕД_СМЕНОЙ_СТАТУСА");
            } else {
                Log.e("PrixodViewModel", "КРИТИЧЕСКАЯ ОШИБКА: Не удалось сохранить продукты во временный файл перед сменой статуса!");
            }
        } else {
            Log.w("PrixodViewModel", "Не удается сохранить продукты во временный файл: отсутствуют необходимые данные");
        }
        
        // Теперь формируем JSON
        String productsJson = getProductsToSaveAsJson();
        Log.d("PrixodViewModel", "Размер JSON для смены статуса: " + 
              (productsJson != null ? productsJson.length() : 0));

        logCurrentStateAsJson("ПЕРЕД_ОТПРАВКОЙ_В_1С");
        
        _returnToMenuWithStatusChangeEvent.setValue(new Event<>(productsJson));
    }
    

    
    /**
     * Обрабатывает нажатие кнопки "Вернуться в меню"
     */
    public void handleBackToMenu() {
        Log.d("PrixodViewModel", "Обработка возврата в меню");
        if (isDataModified()) {
            Log.d("PrixodViewModel", "Обнаружены изменения в продуктах, показываем диалог");
            _showDataSavedDialogEvent.setValue(new Event<>(true));
        } else {
            Log.d("PrixodViewModel", "Изменений в продуктах не обнаружено, возвращаемся в меню");
            returnToMenuWithSavedData();
        }
    }
    
    /**
     * Продолжает возврат в меню после показа диалога о сохраненных данных
     */
    public void proceedWithBackToMenu() {
        Log.d("PrixodViewModel", "Продолжение возврата в меню после подтверждения диалога");
        returnToMenuWithSavedData();
    }
    

    /**
     * Возвращается в меню с сохранением всех данных
     */
    private void returnToMenuWithSavedData() {
        String productsJson = getProductsToSaveAsJson();
        Log.d("PrixodViewModel", "Возвращаемся в меню с сохраненными данными, размер JSON: " + 
              (productsJson != null ? productsJson.length() : 0));

        if (currentMoveUuid != null && !currentMoveUuid.isEmpty() && originalProductList != null) {
            boolean saved = productsDataManager.saveProductsData(currentMoveUuid, originalProductList);
            if (saved) {
                Log.d("PrixodViewModel", "Продукты сохранены в кеш для moveUuid: " + currentMoveUuid + 
                      ", количество: " + originalProductList.size());
            } else {
                Log.e("PrixodViewModel", "Ошибка сохранения продуктов в кеш");
            }
        }

        logCurrentStateAsJson("ДО_СОХРАНЕНИЯ");
        
        _returnToMenuWithDataEvent.setValue(new Event<>(productsJson));
    }
    

    
    /**
     * Отладочный метод для логирования всех значений taken в originalProductList
     */
    public void logAllTakenValues(String context) {
        if (originalProductList == null) {
            Log.d("PrixodViewModel", "[" + context + "] originalProductList is null");
            return;
        }
        
        Log.d("PrixodViewModel", "[" + context + "] Состояние всех значений taken:");
        int nonZeroCount = 0;
        int zeroCount = 0;
        
        for (int i = 0; i < originalProductList.size(); i++) {
            Product product = originalProductList.get(i);
            if (product != null) {
                double takenValue = product.getTaken();
                Log.d("PrixodViewModel", "[" + context + "] [" + i + "] " + 
                      product.getNomenclatureName() + " -> taken=" + takenValue);
                
                if (takenValue > 0) {
                    nonZeroCount++;
                } else {
                    zeroCount++;
                }
            }
        }
        
        Log.d("PrixodViewModel", "[" + context + "] Итого: ненулевых=" + nonZeroCount + 
              ", нулевых=" + zeroCount + ", всего=" + originalProductList.size());
    }
    
    /**
     * Отладочный метод для немедленного получения JSON состояния для отладки
     */
    public void logCurrentStateAsJson(String context) {
        String jsonState = getProductsToSaveAsJson();
        Log.d("PrixodViewModel", "[" + context + "] Текущее состояние в JSON (размер=" + 
              jsonState.length() + " символов):");
        Log.d("PrixodViewModel", "[" + context + "] JSON: " + jsonState);
    }

    /**
     * Обновляет режим "только чтение" на основе статуса перемещения
     * @param status статус перемещения
     */
    private void updateReadOnlyMode(String status) {
        // Детальное логирование для отладки
        Log.d("PrixodViewModel", "updateReadOnlyMode вызван со статусом: '" + status + "'");
        Log.d("PrixodViewModel", "STATUS_PODGOTOVLEN: '" + STATUS_PODGOTOVLEN + "'");
        Log.d("PrixodViewModel", "STATUS_SFORMIROVANNO: '" + STATUS_SFORMIROVANNO + "'");
        Log.d("PrixodViewModel", "Проверка STATUS_PODGOTOVLEN.equals(status): " + STATUS_PODGOTOVLEN.equals(status));
        Log.d("PrixodViewModel", "Проверка STATUS_SFORMIROVANNO.equals(status): " + STATUS_SFORMIROVANNO.equals(status));
        
        boolean readOnlyMode = STATUS_PODGOTOVLEN.equals(status) || STATUS_SFORMIROVANNO.equals(status);
        

        Boolean currentValue = _isReadOnlyMode.getValue();
        boolean valueChanged = currentValue == null || currentValue != readOnlyMode;
        

        _isReadOnlyMode.setValue(readOnlyMode);
        
        Log.d("PrixodViewModel", "Режим 'только чтение' " + (readOnlyMode ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН") + 
              " для статуса: '" + status + "'" + (valueChanged ? " (значение изменено)" : " (значение не изменилось)"));
    }
    
    /**
     * Проверяет, включен ли режим "только чтение"
     * @return true если включен режим "только чтение"
     */
    public boolean isCurrentlyReadOnly() {
        return Boolean.TRUE.equals(isReadOnlyMode.getValue());
    }
    
    /**
     * Возвращает текущий статус перемещения
     * @return статус перемещения или null
     */
    public String getCurrentMoveStatus() {
        MoveItem moveItem = _moveItemLiveData.getValue();
        return moveItem != null ? moveItem.getSigningStatus() : null;
    }

    /**
     * Проверяет, были ли данные изменены пользователем
     * @return true, если хотя бы один товар имеет ненулевое значение taken или были изменения серий
     */
    public boolean isDataModified() {
        if (originalProductList == null || originalProductList.isEmpty()) {
            return false;
        }
        
        for (Product product : originalProductList) {
            // Проверяем изменения количества (taken > 0)
            if (product.getTaken() > 0) {
                return true;
            }
            
            // Проверяем наличие новых продуктов с exists = false
            if (!product.getExists()) {
                return true;
            }
            
            // Проверяем наличие продуктов с parentProductLineId (результат замены серии)
            if (product.getParentProductLineId() != null && !product.getParentProductLineId().isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Возвращает копию списка всех продуктов для использования в других активностях
     * @return список всех продуктов
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(originalProductList);
    }
    
    /**
     * Применяет результат замены серии к списку продуктов
     * @param updatedProducts обновленные продукты (измененные серии)
     * @param newProducts новые продукты (копии с новыми сериями)
     * @param changedProductLineIds ID продуктов которые были изменены (для выделения)
     * @return true если изменения были применены успешно
     */
    public boolean applySeriesChangeResult(List<Product> updatedProducts, List<Product> newProducts, List<String> changedProductLineIds) {
        if (originalProductList == null) {
            Log.e("PrixodViewModel", "originalProductList is null, невозможно применить изменения");
            return false;
        }
        
        Log.d("PrixodViewModel", "Применяем результат замены серии. Обновлено: " + updatedProducts.size() + 
                               ", новых: " + newProducts.size() + ", измененных ID: " + changedProductLineIds.size());
        
        boolean anyChanges = false;
        
        // 1. Обновляем существующие продукты
        for (Product updatedProduct : updatedProducts) {
            String productLineId = updatedProduct.getProductLineId();
            for (int i = 0; i < originalProductList.size(); i++) {
                Product originalProduct = originalProductList.get(i);
                if (java.util.Objects.equals(productLineId, originalProduct.getProductLineId())) {
                    // Заменяем продукт в списке
                    originalProductList.set(i, updatedProduct);
                    anyChanges = true;
                    Log.d("PrixodViewModel", "Обновлен продукт с ID: " + productLineId + 
                                           ", новая серия: " + updatedProduct.getSeriesName() + 
                                           ", количество: " + updatedProduct.getQuantity());
                    break;
                }
            }
        }
        
        // 2. Добавляем новые продукты в конец списка
        for (Product newProduct : newProducts) {
            originalProductList.add(newProduct);
            anyChanges = true;
            Log.d("PrixodViewModel", "Добавлен новый продукт с ID: " + newProduct.getProductLineId() + 
                                   ", серия: " + newProduct.getSeriesName() + 
                                   ", количество: " + newProduct.getQuantity());
        }
        
        if (anyChanges) {
            // 3. Обновляем UI
            Log.d("PrixodViewModel", "Применены изменения замены серии, обновляем UI");
            

            _isSyncedWith1C.setValue(false);
            Log.d("PrixodViewModel", "Данные помечены как несинхронизированные после замены серий");
            

            Invoice currentInvoice = _currentInvoiceLiveData.getValue();
            if (currentInvoice != null) {
                currentInvoice.setProducts(new ArrayList<>(originalProductList));
                _currentInvoiceLiveData.setValue(currentInvoice);
            }
            

            _originalProductListUpdatedEvent.setValue(new Event<>(true));
            
            // Применяем фильтры и сортировку
            applyFiltersAndSort();
            

            
            // 4. Сохраняем обновленные продукты в кеш
            if (currentMoveUuid != null && !currentMoveUuid.isEmpty()) {
                productsDataManager.saveProductsData(currentMoveUuid, originalProductList);
                Log.d("PrixodViewModel", "Обновленные продукты сохранены в кеш для moveUuid: " + currentMoveUuid);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Обновляет данные продуктов из кеша
     */
    public void refreshProductsFromCache() {
        if (currentMoveUuid == null) {
            Log.e("PrixodViewModel", "currentMoveUuid is null, невозможно обновить данные из кеша");
            return;
        }
        
        Log.d("PrixodViewModel", "Обновляем данные продуктов из кеша для moveUuid: " + currentMoveUuid);
        

        List<Product> cachedProducts = productsDataManager.loadProductsData(currentMoveUuid);
        if (cachedProducts != null && !cachedProducts.isEmpty()) {
            // Обновляем оригинальный список
            originalProductList = new ArrayList<>(cachedProducts);
            

            Invoice currentInvoice = _currentInvoiceLiveData.getValue();
            if (currentInvoice != null) {
                currentInvoice.setProducts(new ArrayList<>(originalProductList));
                _currentInvoiceLiveData.setValue(currentInvoice);
            }
            

            _originalProductListUpdatedEvent.setValue(new Event<>(true));
            
            // Применяем фильтры и сортировку
            applyFiltersAndSort();
            
            Log.d("PrixodViewModel", "Данные продуктов успешно обновлены из кеша. Количество продуктов: " + cachedProducts.size());
        } else {
            Log.w("PrixodViewModel", "Данные продуктов в кеше не найдены для moveUuid: " + currentMoveUuid);
        }
    }
} 