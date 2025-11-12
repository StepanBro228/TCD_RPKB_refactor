package com.step.tcd_rpkb.UI.Serias.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.step.tcd_rpkb.base.BaseViewModel;
import com.step.tcd_rpkb.domain.model.SeriesItem;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.usecase.GetSeriesForNomenclatureUseCase;
import com.step.tcd_rpkb.domain.usecase.SaveSeriesAllocationUseCase;
import com.step.tcd_rpkb.utils.Event;
import com.step.tcd_rpkb.utils.SeriesDataManager;
import com.step.tcd_rpkb.utils.ProductsDataManager;
import com.step.tcd_rpkb.domain.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel для окна подбора серий товара
 */
@HiltViewModel
public class SeriesSelectionViewModel extends BaseViewModel {

    private static final String TAG = "SeriesSelectionViewModel";

    private final GetSeriesForNomenclatureUseCase getSeriesForNomenclatureUseCase;
    private final SaveSeriesAllocationUseCase saveSeriesAllocationUseCase;
    private final SeriesDataManager seriesDataManager;
    private final ProductsDataManager productsDataManager;

    // LiveData для хранения списка серий
    private final MutableLiveData<List<SeriesItem>> _seriesItems = new MutableLiveData<>();
    public LiveData<List<SeriesItem>> getSeriesItems() {
        return _seriesItems;
    }

    // LiveData для хранения количества в документе
    private final MutableLiveData<Double> _documentQuantity = new MutableLiveData<>(0.0);
    public LiveData<Double> getDocumentQuantity() {
        return _documentQuantity;
    }

    // LiveData для хранения распределенного количества
    private final MutableLiveData<Double> _allocatedQuantity = new MutableLiveData<>(0.0);
    public LiveData<Double> getAllocatedQuantity() {
        return _allocatedQuantity;
    }

    // LiveData для сообщений об ошибках
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    // LiveData для уведомления о успешном сохранении
    private final MutableLiveData<Event<Boolean>> _saveSuccessEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> getSaveSuccessEvent() {
        return _saveSuccessEvent;
    }

    // LiveData для управления состоянием загрузки
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() {
        return _isLoading;
    }

    private String currentNomenclatureUuid;
    private String currentMoveUuid;

    // Кеш данных о сериях
    private Map<String, SeriesItem> seriesItemsMap = new HashMap<>();
    
    // Сохранение оригинальных данных для отката изменений
    private List<SeriesItem> originalSeriesData = new ArrayList<>();
    private boolean hasOriginalData = false;
    
    // Флаг для отслеживания несохраненных изменений
    private boolean hasUnsavedChanges = false;

    @Inject
    public SeriesSelectionViewModel(
            GetSeriesForNomenclatureUseCase getSeriesForNomenclatureUseCase,
            SaveSeriesAllocationUseCase saveSeriesAllocationUseCase,
            SeriesDataManager seriesDataManager,
            ProductsDataManager productsDataManager) {
        this.getSeriesForNomenclatureUseCase = getSeriesForNomenclatureUseCase;
        this.saveSeriesAllocationUseCase = saveSeriesAllocationUseCase;
        this.seriesDataManager = seriesDataManager;
        this.productsDataManager = productsDataManager;
    }

    /**
     * Загружает данные о сериях для товара в перемещении
     * Сначала проверяет кеш, и только при его отсутствии делает запрос на сервер
     * ВСЕГДА пересчитывает "Количество в документе" для каждой серии на основе фактических данных перемещения
     * 
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     * @param productLineId УИД строки товара (для онлайн режима)
     * @param totalDocumentQuantity Общее количество в документе (игнорируется, рассчитывается автоматически)
     */
    public void loadSeriesData(String nomenclatureUuid, String moveUuid, String productLineId, double totalDocumentQuantity) {

        this.currentNomenclatureUuid = nomenclatureUuid;
        this.currentMoveUuid = moveUuid;
        

        _isLoading.setValue(true);
        

        _seriesItems.setValue(new ArrayList<>());
        seriesItemsMap.clear();
        

        _documentQuantity.setValue(0.0);
        _allocatedQuantity.setValue(0.0);
        
        // Сначала проверяем кеш
        if (seriesDataManager.hasSeriesData(nomenclatureUuid)) {
            Log.d(TAG, "Загружаем данные серий из кеша для номенклатуры: " + nomenclatureUuid);
            List<SeriesItem> cachedSeries = seriesDataManager.loadSeriesData(nomenclatureUuid);
            
            if (!cachedSeries.isEmpty()) {
                // Пересчитываем количество в документе для каждой серии на основе текущих продуктов
                recalculateDocumentQuantitiesFromProducts(cachedSeries, nomenclatureUuid);
                
                processLoadedSeries(cachedSeries);
                _isLoading.setValue(false);
                Log.d(TAG, "Данные серий успешно загружены из кеша");
                return;
            }
        }
        
        // Если кеша нет или он пустой - загружаем с сервера
        Log.d(TAG, "Загружаем данные серий с сервера для номенклатуры: " + nomenclatureUuid);
        getSeriesForNomenclatureUseCase.execute(nomenclatureUuid, moveUuid, productLineId, new RepositoryCallback<List<SeriesItem>>() {
            @Override
            public void onSuccess(List<SeriesItem> seriesList) {
                _isLoading.setValue(false);
                
                if (seriesList == null || seriesList.isEmpty()) {
                    _errorMessage.setValue("Для данной номенклатуры не найдено серий");
                    return;
                }
                

                recalculateDocumentQuantitiesFromProducts(seriesList, nomenclatureUuid);
                

                seriesDataManager.saveSeriesData(nomenclatureUuid, seriesList);
                
                processLoadedSeries(seriesList);
                
                Log.d(TAG, "Данные серий загружены с сервера, пересчитаны количества в документе и сохранены в кеш для номенклатуры: " + nomenclatureUuid);
            }
            
            @Override
            public void onError(Exception exception) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Ошибка при загрузке серий: " + exception.getMessage());
                Log.e(TAG, "Ошибка при загрузке серий: " + exception.getMessage());
            }
        });
    }

    
    /**
     * Пересчитывает общее распределенное количество (сумма documentQuantity из всех серий)
     */
    private void recalculateAllocatedQuantity() {
        double totalAllocated = 0;
        for (SeriesItem item : seriesItemsMap.values()) {
            totalAllocated += item.getDocumentQuantity(); // Используем documentQuantity согласно требованию
        }
        _allocatedQuantity.setValue(totalAllocated);
    }

    /**
     * Сохраняет распределение серий с указанным UUID номенклатуры и перемещения
     *
     * @param nomenclatureUuid UUID номенклатуры
     * @param moveUuid UUID перемещения
     */
    public void saveAllocation(String nomenclatureUuid, String moveUuid) {
        if (nomenclatureUuid == null || moveUuid == null) {
            _errorMessage.setValue("Отсутствует информация о номенклатуре или перемещении");
            return;
        }

        List<SeriesItem> seriesList = _seriesItems.getValue();
        if (seriesList == null || seriesList.isEmpty()) {
            _errorMessage.setValue("Нет данных для сохранения");
            return;
        }

        // Обновляем кеш перед сохранением
        seriesDataManager.saveSeriesData(nomenclatureUuid, seriesList);


        _isLoading.setValue(true);


        saveSeriesAllocationUseCase.execute(
            nomenclatureUuid,
            moveUuid,
            seriesList,
            new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    _isLoading.setValue(false);
                    if (success) {

                        clearUnsavedChanges();
                        _saveSuccessEvent.setValue(new Event<>(true));
                        Log.d(TAG, "Распределение серий успешно сохранено");
                    } else {
                        _errorMessage.setValue("Не удалось сохранить распределение серий");
                    }
                }

                @Override
                public void onError(Exception exception) {
                    _isLoading.setValue(false);
                    _errorMessage.setValue("Ошибка при сохранении: " + exception.getMessage());
                    Log.e(TAG, "Ошибка при сохранении распределения серий: " + exception.getMessage());
                }
            }
        );
    }
    
    /**
     * Обрабатывает загруженные серии (из кеша или с сервера)
     */
    private void processLoadedSeries(List<SeriesItem> seriesList) {

        if (!hasOriginalData) {
            saveOriginalSeriesData(seriesList);
        }

        seriesItemsMap.clear();
        
        for (SeriesItem series : seriesList) {
            seriesItemsMap.put(series.getSeriesUuid(), series);
        }
        _seriesItems.setValue(seriesList);

        recalculateAllocatedQuantity();
        
        Log.d(TAG, "Обработано " + seriesList.size() + " серий");
    }
    
    /**
     * КЛЮЧЕВОЙ МЕТОД: Пересчитывает количество в документе для каждой серии на основе фактических данных перемещения
     * Вызывается ВСЕГДА - как при загрузке из кеша, так и при первой загрузке с сервера
     * Игнорирует значения из ответа сервера и рассчитывает реальные количества по продуктам
     */
    private void recalculateDocumentQuantitiesFromProducts(List<SeriesItem> seriesList, String nomenclatureUuid) {
        Log.d(TAG, "НАЧАЛО пересчета количества в документе для серий номенклатуры: " + nomenclatureUuid);
        Log.d(TAG, "Количество серий для пересчета: " + seriesList.size());
        
        // Загружаем продукты из кеша по moveUuid
        List<Product> products = productsDataManager.loadProductsData(currentMoveUuid);
        if (products.isEmpty()) {
            Log.w(TAG, "Нет продуктов для пересчета количества в документе (moveUuid: " + currentMoveUuid + ")");
            return;
        }
        
        Log.d(TAG, "Загружено " + products.size() + " продуктов для анализа");
        

        Map<String, Double> seriesQuantityMap = new HashMap<>();
        

        for (SeriesItem series : seriesList) {
            seriesQuantityMap.put(series.getSeriesUuid(), 0.0);
        }
        
        // Суммируем количества продуктов по сериям только для нужной номенклатуры
        double totalDocumentQuantity = 0.0;
        for (Product product : products) {
            if (nomenclatureUuid.equals(product.getNomenclatureUuid())) {
                String seriesUuid = product.getSeriesUuid();
                if (seriesUuid != null && seriesQuantityMap.containsKey(seriesUuid)) {
                    double currentQuantity = seriesQuantityMap.get(seriesUuid);
                    seriesQuantityMap.put(seriesUuid, currentQuantity + product.getQuantity());
                }
                totalDocumentQuantity += product.getQuantity();
            }
        }
        
        // Обновляем количество в документе для каждой серии
        for (SeriesItem series : seriesList) {
            String seriesUuid = series.getSeriesUuid();
            Double newQuantity = seriesQuantityMap.get(seriesUuid);
            if (newQuantity != null) {
                series.setDocumentQuantity(newQuantity);
                Log.d(TAG, "Обновлено количество в документе для серии " + seriesUuid + ": " + newQuantity);
            }
        }
        
        // Обновляем общее количество в документе
        _documentQuantity.setValue(totalDocumentQuantity);

        Log.d(TAG, "ЗАВЕРШЕН пересчет количества в документе для номенклатуры: " + nomenclatureUuid);
        Log.d(TAG, "Общее количество в документе: " + totalDocumentQuantity);
        Log.d(TAG, "Обработано серий: " + seriesList.size());
    }

    /**
     * Сохраняет оригинальные данные серий для возможности отката
     */
    private void saveOriginalSeriesData(List<SeriesItem> seriesList) {
        originalSeriesData.clear();
        for (SeriesItem series : seriesList) {

            SeriesItem copy = createSeriesCopy(series);
            originalSeriesData.add(copy);
        }
        hasOriginalData = true;
        hasUnsavedChanges = false;
        Log.d(TAG, "Сохранены оригинальные данные " + originalSeriesData.size() + " серий");
    }
    
    /**
     * Создает глубокую копию объекта SeriesItem
     */
    private SeriesItem createSeriesCopy(SeriesItem original) {
        SeriesItem copy = new SeriesItem(
            original.getSeriesUuid(),
            original.getSeriesName(),
            original.getExpiryDate(),
            original.getFreeBalance(),
            original.getReservedByOthers(),
            original.getDocumentQuantity()
        );
        copy.setAllocatedQuantity(original.getAllocatedQuantity());
        return copy;
    }
    
    /**
     * Восстанавливает оригинальные данные серий
     */
    public void restoreOriginalSeriesData() {
        if (!hasOriginalData || originalSeriesData.isEmpty()) {
            Log.w(TAG, "Нет оригинальных данных для восстановления");
            return;
        }
        

        if (currentNomenclatureUuid != null) {
            List<SeriesItem> restoredSeries = new ArrayList<>();
            for (SeriesItem original : originalSeriesData) {
                SeriesItem copy = createSeriesCopy(original);
                restoredSeries.add(copy);
            }
            

            seriesDataManager.saveSeriesData(currentNomenclatureUuid, restoredSeries);
            

            seriesItemsMap.clear();
            for (SeriesItem series : restoredSeries) {
                seriesItemsMap.put(series.getSeriesUuid(), series);
            }
            
            // Обновляем LiveData
            _seriesItems.setValue(restoredSeries);
            
            // Пересчитываем распределенное количество
            recalculateAllocatedQuantity();
            
            hasUnsavedChanges = false;
            Log.d(TAG, "Данные серий восстановлены из оригинальных значений");
        }
    }
    
    /**
     * Проверяет есть ли несохраненные изменения
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    /**
     * Отмечает что есть несохраненные изменения
     */
    public void markAsChanged() {
        hasUnsavedChanges = true;
    }
    
    /**
     * Сбрасывает флаг несохраненных изменений
     */
    public void clearUnsavedChanges() {
        hasUnsavedChanges = false;
    }
} 