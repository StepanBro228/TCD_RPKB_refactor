package com.step.tcd_rpkb.UI.ZnpSelection.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.step.tcd_rpkb.base.BaseViewModel;
import com.step.tcd_rpkb.domain.model.SeriesChangeItem;
import com.step.tcd_rpkb.utils.Event;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.utils.ProductsDataManager;
import com.step.tcd_rpkb.utils.SeriesDataManager;
import com.step.tcd_rpkb.utils.UuidGenerator;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.step.tcd_rpkb.domain.model.SeriesItem;

/**
 * ViewModel для страницы замены серии товаров
 */
@HiltViewModel
public class ZnpSelectionViewModel extends BaseViewModel {
    
    private static final String TAG = "ZnpSelectionViewModel";
    
    // LiveData для списка продуктов для замены серии
    private final MutableLiveData<List<SeriesChangeItem>> _seriesChangeItems = new MutableLiveData<>();
    public LiveData<List<SeriesChangeItem>> getSeriesChangeItems() {
        return _seriesChangeItems;
    }
    
    // LiveData для максимального доступного количества (сумма выбранных или свободный остаток)
    private final MutableLiveData<Double> _maxAvailableQuantity = new MutableLiveData<>(0.0);
    public LiveData<Double> getMaxAvailableQuantity() {
        return _maxAvailableQuantity;
    }
    
    // LiveData для сообщений об ошибках
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }
    
    // LiveData для уведомления о успешной замене серии
    private final MutableLiveData<Event<SeriesChangeResult>> _seriesChangeSuccessEvent = new MutableLiveData<>();
    public LiveData<Event<SeriesChangeResult>> getSeriesChangeSuccessEvent() {
        return _seriesChangeSuccessEvent;
    }
    
    // LiveData для управления состоянием загрузки
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() {
        return _isLoading;
    }
    
    // Данные о целевой серии
    private String targetSeriesUuid;
    private String targetSeriesName;
    private double targetSeriesFreeBalance;
    
    // Данные о номенклатуре и исходной серии
    private String currentNomenclatureUuid;
    private String currentMoveUuid;
    private String originalSeriesUuid;
    
    // Менеджеры данных
    private final SeriesDataManager seriesDataManager;
    private final ProductsDataManager productsDataManager;
    
    // Сохраняем оригинальные значения свободных остатков для отката
    private Map<String, Double> originalFreeBalances = new HashMap<>();
    
    // Сохраняем последний результат замены серии для передачи при сохранении
    private SeriesChangeResult lastSeriesChangeResult = null;
    
    // Класс результата замены серии
    public static class SeriesChangeResult {
        public final List<Product> updatedProducts;
        public final List<Product> newProducts;
        public final List<String> changedProductLineIds;
        
        public SeriesChangeResult(List<Product> updatedProducts, List<Product> newProducts, List<String> changedProductLineIds) {
            this.updatedProducts = updatedProducts;
            this.newProducts = newProducts;
            this.changedProductLineIds = changedProductLineIds;
        }
    }
    
    @Inject
    public ZnpSelectionViewModel(SeriesDataManager seriesDataManager, ProductsDataManager productsDataManager) {
        this.seriesDataManager = seriesDataManager;
        this.productsDataManager = productsDataManager;
    }
    
    /**
     * Загружает продукты для замены серии
     */
    public void loadProductsForSeriesChange(String nomenclatureUuid, String moveUuid, String targetSeriesUuid, String targetSeriesName, double freeBalance) {
        this.currentNomenclatureUuid = nomenclatureUuid;
        this.currentMoveUuid = moveUuid;
        this.targetSeriesUuid = targetSeriesUuid;
        this.targetSeriesName = targetSeriesName;
        this.targetSeriesFreeBalance = freeBalance;
        
        // Сохраняем оригинальные свободные остатки серий для отката
        saveOriginalFreeBalances();
        
        _isLoading.setValue(true);
        
        // Загружаем данные продуктов из файла по moveUuid
        List<Product> products = productsDataManager.loadProductsData(moveUuid);
        
        if (products.isEmpty()) {
            _isLoading.setValue(false);
            _errorMessage.setValue("Не найдены данные продуктов для замены серии");
            Log.e(TAG, "Не найдены данные продуктов для перемещения: " + moveUuid);
            return;
        }
        
        Log.d(TAG, "Загружено продуктов для замены серии: " + products.size());
        
        // Выполняем фильтрацию и преобразование
        new Thread(() -> {
            try {
                List<SeriesChangeItem> seriesChangeItems = new ArrayList<>();
                
                // Фильтруем продукты по номенклатуре и преобразуем в SeriesChangeItem
                for (Product product : products) {
                    if (nomenclatureUuid.equals(product.getNomenclatureUuid())) {
                        SeriesChangeItem item = SeriesChangeItem.fromProduct(product);
                        seriesChangeItems.add(item);
                        
                        // Определяем исходную серию из первого продукта
                        if (originalSeriesUuid == null && product.getSeriesUuid() != null) {
                            originalSeriesUuid = product.getSeriesUuid();
                        }
                    }
                }
                
                Log.d(TAG, "Создано элементов для замены серии: " + seriesChangeItems.size());

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    _isLoading.setValue(false);
                    _seriesChangeItems.setValue(seriesChangeItems);
                    updateMaxAvailableQuantity();
                });
                
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue("Ошибка при загрузке продуктов: " + e.getMessage());
                    Log.e(TAG, "Ошибка при загрузке продуктов", e);
                });
            }
        }).start();
    }
    
    /**
     * Обновляет состояние выбора элемента
     */
    public void updateItemSelection(int position, boolean isSelected) {
        List<SeriesChangeItem> currentItems = _seriesChangeItems.getValue();
        if (currentItems != null && position >= 0 && position < currentItems.size()) {
            currentItems.get(position).setSelected(isSelected);
            _seriesChangeItems.setValue(currentItems);
            updateMaxAvailableQuantity();
            Log.d(TAG, "Обновлен выбор элемента на позиции " + position + ": " + isSelected);
        }
    }
    
    /**
     * Обновляет максимальное доступное количество на основе выбранных элементов
     */
    private void updateMaxAvailableQuantity() {
        List<SeriesChangeItem> currentItems = _seriesChangeItems.getValue();
        if (currentItems == null) {
            _maxAvailableQuantity.setValue(0.0);
            return;
        }
        
        // Получаем актуальный свободный остаток из кеша
        double currentFreeBalance = getCurrentFreeBalance();
        
        // Вычисляем сумму количеств выбранных элементов
        double totalSelectedQuantity = 0.0;
        for (SeriesChangeItem item : currentItems) {
            if (item.isSelected()) {
                totalSelectedQuantity += item.getQuantityToProcure();
            }
        }

        double maxAvailable = Math.min(totalSelectedQuantity, currentFreeBalance);
        _maxAvailableQuantity.setValue(maxAvailable);
        
        Log.d(TAG, "Обновлено максимальное доступное количество: " + maxAvailable + 
                   " (выбрано: " + totalSelectedQuantity + ", свободный остаток: " + currentFreeBalance + ")");
    }
    
    /**
     * Обновляет данные после замены серии
     */
    public void refreshDataAfterSeriesChange() {
        // Обновляем локальный свободный остаток
        targetSeriesFreeBalance = getCurrentFreeBalance();

        if (currentMoveUuid != null && currentNomenclatureUuid != null) {
            Log.d(TAG, "Перезагружаем продукты из кеша после замены серии");
            

            List<Product> updatedProducts = productsDataManager.loadProductsData(currentMoveUuid);
            
            // Фильтруем и преобразуем в SeriesChangeItem
            List<SeriesChangeItem> updatedSeriesChangeItems = new ArrayList<>();
            for (Product product : updatedProducts) {
                if (currentNomenclatureUuid.equals(product.getNomenclatureUuid())) {
                    SeriesChangeItem item = SeriesChangeItem.fromProduct(product);
                    updatedSeriesChangeItems.add(item);
                }
            }
            
            Log.d(TAG, "Обновлен список продуктов после замены серии: " + updatedSeriesChangeItems.size() + " продуктов");
            

            _seriesChangeItems.setValue(updatedSeriesChangeItems);
        }
        
        // Пересчитываем максимальное доступное количество
        updateMaxAvailableQuantity();
        
        Log.d(TAG, "Данные обновлены после замены серии. Новый свободный остаток: " + targetSeriesFreeBalance);
    }
    
    /**
     * Выполняет замену серии для выбранных продуктов
     */
    public void performSeriesChange(double changeQuantity) {
        List<SeriesChangeItem> currentItems = _seriesChangeItems.getValue();
        if (currentItems == null || targetSeriesUuid == null || targetSeriesName == null) {
            _errorMessage.setValue("Недостаточно данных для замены серии");
            return;
        }

        List<SeriesChangeItem> selectedItems = new ArrayList<>();
        for (SeriesChangeItem item : currentItems) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        
        if (selectedItems.isEmpty()) {
            _errorMessage.setValue("Не выбрано ни одного продукта для замены серии");
            return;
        }
        

        double totalSelectedQuantity = 0.0;
        for (SeriesChangeItem item : selectedItems) {
            totalSelectedQuantity += item.getQuantityToProcure();
        }
        
        if (changeQuantity <= 0) {
            _errorMessage.setValue("Количество для замены должно быть больше нуля");
            return;
        }
        
        if (changeQuantity > Math.min(totalSelectedQuantity, targetSeriesFreeBalance)) {
            _errorMessage.setValue("Количество превышает доступное для замены");
            return;
        }
        
        _isLoading.setValue(true);
        

        new Thread(() -> {
            try {
                SeriesChangeResult result = processSeriesChange(selectedItems, changeQuantity);
                

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    _isLoading.setValue(false);

                    lastSeriesChangeResult = result;
                    
                    _seriesChangeSuccessEvent.setValue(new Event<>(result));
                });
                
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue("Ошибка при замене серии: " + e.getMessage());
                    Log.e(TAG, "Ошибка при замене серии", e);
                });
            }
        }).start();
    }
    
    /**
     * Обрабатывает замену серий согласно алгоритму
     */
    private SeriesChangeResult processSeriesChange(List<SeriesChangeItem> selectedItems, double changeQuantity) {
        List<Product> updatedProducts = new ArrayList<>();
        List<Product> newProducts = new ArrayList<>(); 
        List<String> changedProductLineIds = new ArrayList<>();
        
        double remainingQuantity = changeQuantity;
        
        Log.d(TAG, "Начинаем замену серии. Количество для замены: " + changeQuantity + 
                   ", выбрано продуктов: " + selectedItems.size());
        

        for (SeriesChangeItem item : selectedItems) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            double itemQuantity = item.getQuantityToProcure();
            
            if (remainingQuantity >= itemQuantity) {
                // Хватает количества чтобы заменить серию полностью
                Product updatedProduct = item.toProduct();
                updatedProduct.setSeriesName(targetSeriesName);
                updatedProduct.setSeriesUuid(targetSeriesUuid);
                
                updatedProducts.add(updatedProduct);
                changedProductLineIds.add(item.getProductLineId());
                
                remainingQuantity -= itemQuantity;
                
                Log.d(TAG, "Полная замена серии для продукта " + item.getProductLineId() + 
                           ", количество: " + itemQuantity + ", осталось: " + remainingQuantity);
                
            } else {
                // Не хватает количества - создаем копию с частичным количеством
                
                // Новый продукт с новой серией и оставшимся количеством
                Product newProduct = item.toProduct();
                newProduct.setSeriesName(targetSeriesName);
                newProduct.setSeriesUuid(targetSeriesUuid);
                newProduct.setQuantity(remainingQuantity);
                
                // Генерируем новый productLineId для копии
                String newProductLineId = generateNewProductLineId(item.getProductLineId());
                newProduct.setProductLineId(newProductLineId);

                newProduct.setParentProductLineId(item.getProductLineId());

                newProduct.setExists(false);
                
                newProducts.add(newProduct);
                changedProductLineIds.add(newProductLineId);
                
                // Обновляем оригинальный продукт - уменьшаем его количество
                Product originalProduct = item.toProduct();
                originalProduct.setQuantity(itemQuantity - remainingQuantity);
                
                updatedProducts.add(originalProduct);
                changedProductLineIds.add(item.getProductLineId());
                
                Log.d(TAG, "Частичная замена серии для продукта " + item.getProductLineId() + 
                           ". Создан новый продукт " + newProductLineId + " с количеством " + remainingQuantity +
                           " и родительским ID " + item.getProductLineId() +
                           ". Оригинальный продукт обновлен до количества " + (itemQuantity - remainingQuantity));
                
                remainingQuantity = 0; // Количество исчерпано
            }
        }
        
        Log.d(TAG, "Замена серии завершена. Обновлено продуктов: " + updatedProducts.size() +
                   ", создано новых: " + newProducts.size() + ", измененных ID: " + changedProductLineIds.size());
        
        // Обновляем свободные остатки серий
        updateSeriesFreeBalances(changeQuantity);
        
        // Сохраняем обновленные продукты
        saveUpdatedProducts(updatedProducts, newProducts);
        
        return new SeriesChangeResult(updatedProducts, newProducts, changedProductLineIds);
    }
    
    /**
     * Генерирует новый productLineId для копии продукта
     */
    private String generateNewProductLineId(String originalId) {
        return UuidGenerator.generateProductLineId();
    }
    
    /**
     * Проверяет идентичность двух продуктов (исключая УИДСтрокиТовары, УИДСтрокиТоварыРодитель, Количество и Exists)
     */
    private boolean areProductsIdentical(Product product1, Product product2) {
        if (product1 == null || product2 == null) {
            return false;
        }
        
        return Objects.equals(product1.getNomenclatureUuid(), product2.getNomenclatureUuid()) &&
               Objects.equals(product1.getNomenclatureName(), product2.getNomenclatureName()) &&
               Objects.equals(product1.getRequestedUuid(), product2.getRequestedUuid()) &&
               Objects.equals(product1.getRequestedName(), product2.getRequestedName()) &&
               Objects.equals(product1.getSeriesName(), product2.getSeriesName()) &&
               Objects.equals(product1.getSeriesUuid(), product2.getSeriesUuid()) &&
               Objects.equals(product1.getUnitName(), product2.getUnitName()) &&
               Objects.equals(product1.getUnitUuid(), product2.getUnitUuid()) &&
               Objects.equals(product1.getSenderStorageName(), product2.getSenderStorageName()) &&
               Objects.equals(product1.getSenderStorageUuid(), product2.getSenderStorageUuid()) &&
               Objects.equals(product1.getReceiverStorageName(), product2.getReceiverStorageName()) &&
               Objects.equals(product1.getReceiverStorageUuid(), product2.getReceiverStorageUuid()) &&
               Objects.equals(product1.getResponsibleReceiverName(), product2.getResponsibleReceiverName()) &&
               Objects.equals(product1.getResponsibleReceiverUuid(), product2.getResponsibleReceiverUuid()) &&
               Objects.equals(product1.getReserveDocumentName(), product2.getReserveDocumentName()) &&
               Objects.equals(product1.getReserveDocumentUuid(), product2.getReserveDocumentUuid()) &&
               Double.compare(product1.getFreeBalanceInCell(), product2.getFreeBalanceInCell()) == 0 &&
               Double.compare(product1.getFreeBalanceBySeries(), product2.getFreeBalanceBySeries()) == 0 &&
               Double.compare(product1.getFreeBalance(), product2.getFreeBalance()) == 0 &&
               Double.compare(product1.getTotalBalance(), product2.getTotalBalance()) == 0 &&
               Double.compare(product1.getTaken(), product2.getTaken()) == 0;

    }
    
    /**
     * Валидирует количество для замены серии
     */
    public boolean validateChangeQuantity(double changeQuantity) {
        if (changeQuantity <= 0) {
            _errorMessage.setValue("Количество для замены должно быть больше нуля");
            return false;
        }
        
        Double maxAvailable = _maxAvailableQuantity.getValue();
        if (maxAvailable == null || changeQuantity > maxAvailable) {
            _errorMessage.setValue("Количество превышает максимально доступное для замены");
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет есть ли выбранные продукты
     */
    public boolean hasSelectedItems() {
        List<SeriesChangeItem> currentItems = _seriesChangeItems.getValue();
        if (currentItems == null) {
            return false;
        }
        
        for (SeriesChangeItem item : currentItems) {
            if (item.isSelected()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Обновляет свободные остатки серий после замены
     */
    private void updateSeriesFreeBalances(double changeQuantity) {
        if (currentNomenclatureUuid != null && originalSeriesUuid != null && targetSeriesUuid != null) {
            // Перемещаем количество из целевой серии в исходную
            boolean success = seriesDataManager.transferQuantityBetweenSeries(
                currentNomenclatureUuid, originalSeriesUuid, targetSeriesUuid, changeQuantity);
            
            if (success) {
                Log.d(TAG, "Свободные остатки серий обновлены. Перемещено количество: " + changeQuantity +
                           " из серии " + targetSeriesUuid + " в серию " + originalSeriesUuid);
            } else {
                Log.e(TAG, "Ошибка обновления свободных остатков серий");
            }
        }
    }

    /**
     * Получить актуальный свободный остаток для целевой серии
     */
    public double getCurrentFreeBalance() {
        if (currentNomenclatureUuid != null && targetSeriesUuid != null) {
            SeriesItem target = seriesDataManager.getSeriesByUuid(currentNomenclatureUuid, targetSeriesUuid);
            if (target != null) return target.getFreeBalance();
        }
        return 0.0;
    }
    
    /**
     * Сохраняет обновленные и новые продукты в кеш
     */
    private void saveUpdatedProducts(List<Product> updatedProducts, List<Product> newProducts) {
        if (currentMoveUuid == null) {
            Log.e(TAG, "currentMoveUuid is null, невозможно сохранить продукты");
            return;
        }
        
        try {
            // Загружаем текущие продукты по moveUuid
            List<Product> allProducts = productsDataManager.loadProductsData(currentMoveUuid);
            
            Log.d(TAG, "=== НАЧАЛО СОХРАНЕНИЯ ОБНОВЛЕННЫХ ПРОДУКТОВ ===");
            Log.d(TAG, "MoveUUID: " + currentMoveUuid);
            Log.d(TAG, "Загружено существующих продуктов: " + allProducts.size());
            Log.d(TAG, "Обновленных продуктов: " + updatedProducts.size());
            Log.d(TAG, "Новых продуктов: " + newProducts.size());
            
            // Логируем
            for (int i = 0; i < updatedProducts.size(); i++) {
                Product product = updatedProducts.get(i);
                Log.d(TAG, "Обновляемый продукт #" + (i + 1) + ": " +
                          "ID=" + product.getProductLineId() +
                          ", parentID=" + product.getParentProductLineId() +
                          ", nomenclature=" + product.getNomenclatureName() +
                          ", series=" + product.getSeriesName() +
                          ", quantity=" + product.getQuantity() +
                          ", exists=" + product.getExists());
            }
            
            // Логируем
            for (int i = 0; i < newProducts.size(); i++) {
                Product product = newProducts.get(i);
                Log.d(TAG, "Новый продукт #" + (i + 1) + ": " +
                          "ID=" + product.getProductLineId() +
                          ", parentID=" + product.getParentProductLineId() +
                          ", nomenclature=" + product.getNomenclatureName() +
                          ", series=" + product.getSeriesName() +
                          ", quantity=" + product.getQuantity() +
                          ", exists=" + product.getExists());
            }
            
            // Обновляем продукты
            for (Product updatedProduct : updatedProducts) {
                for (int i = 0; i < allProducts.size(); i++) {
                    if (java.util.Objects.equals(allProducts.get(i).getProductLineId(), updatedProduct.getProductLineId())) {
                        Log.d(TAG, "Заменяем продукт с ID: " + updatedProduct.getProductLineId());
                        allProducts.set(i, updatedProduct);
                        break;
                    }
                }
            }
            
            // Добавляем новые продукты
            allProducts.addAll(newProducts);
            Log.d(TAG, "Общее количество продуктов после добавления: " + allProducts.size());
            
            // Слияние продуктов с одинаковыми полями
            mergeIdenticalProducts(allProducts);
            Log.d(TAG, "Количество продуктов после слияния: " + allProducts.size());
            
            // Сохраняем обновленный список по moveUuid
            productsDataManager.saveProductsData(currentMoveUuid, allProducts);
            
            Log.d(TAG, "Продукты сохранены в кеш для moveUuid: " + currentMoveUuid + 
                       ". Обновлено: " + updatedProducts.size() + 
                       ", добавлено новых: " + newProducts.size());
            Log.d(TAG, "=== КОНЕЦ СОХРАНЕНИЯ ОБНОВЛЕННЫХ ПРОДУКТОВ ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения продуктов: " + e.getMessage(), e);
        }
    }

    /**
     * Выполняет слияние продуктов с одинаковыми полями
     * Удаляет продукты с exists = false и добавляет их количество к продуктам с exists = true
     */
    private void mergeIdenticalProducts(List<Product> allProducts) {
        Log.d(TAG, "Начинаем слияние идентичных продуктов. Всего продуктов до слияния: " + allProducts.size());
        
        List<Product> productsToRemove = new ArrayList<>();
        Map<Product, Product> mergeMap = new HashMap<>();
        
        // Находим пары продуктов для слияния
        for (int i = 0; i < allProducts.size(); i++) {
            Product product1 = allProducts.get(i);
            
            if (productsToRemove.contains(product1)) {
                continue;
            }
            
            for (int j = i + 1; j < allProducts.size(); j++) {
                Product product2 = allProducts.get(j);
                
                if (productsToRemove.contains(product2)) {
                    continue;
                }
                
                // Проверяем идентичность продуктов
                if (areProductsIdentical(product1, product2)) {
                    Product toRemove, toKeep;
                    
                    if (!product1.getExists() && product2.getExists()) {
                        toRemove = product1;
                        toKeep = product2;
                    } else if (product1.getExists() && !product2.getExists()) {
                        toRemove = product2;
                        toKeep = product1;
                    } else {
                        toRemove = product2;
                        toKeep = product1;
                    }
                    
                    // Добавляем количество от удаляемого к сохраняемому
                    toKeep.setQuantity(toKeep.getQuantity() + toRemove.getQuantity());

                    productsToRemove.add(toRemove);
                    mergeMap.put(toRemove, toKeep);
                    
                    Log.d(TAG, "Слияние продуктов: " + toRemove.getProductLineId() + 
                               " (exists=" + toRemove.getExists() + ", кол=" + toRemove.getQuantity() + ")" +
                               " -> " + toKeep.getProductLineId() + 
                               " (exists=" + toKeep.getExists() + ", новое кол=" + toKeep.getQuantity() + ")");
                    
                    break;
                }
            }
        }

        allProducts.removeAll(productsToRemove);
        
        Log.d(TAG, "Слияние завершено. Удалено продуктов: " + productsToRemove.size() + 
                   ", осталось: " + allProducts.size());
    }

    /**
     * Сохраняет оригинальные свободные остатки серий при загрузке
     */
    private void saveOriginalFreeBalances() {
        if (currentNomenclatureUuid != null) {
            originalFreeBalances.clear();
            
            // Сохраняем свободный остаток целевой серии
            if (targetSeriesUuid != null) {
                SeriesItem targetSeries = seriesDataManager.getSeriesByUuid(currentNomenclatureUuid, targetSeriesUuid);
                if (targetSeries != null) {
                    originalFreeBalances.put(targetSeriesUuid, targetSeries.getFreeBalance());
                    Log.d(TAG, "Сохранен оригинальный свободный остаток целевой серии " + targetSeriesUuid + ": " + targetSeries.getFreeBalance());
                }
            }
            
            // Определяем исходную серию из первого продукта данной номенклатуры
            List<Product> products = productsDataManager.loadProductsData(currentMoveUuid);
            for (Product product : products) {
                if (currentNomenclatureUuid.equals(product.getNomenclatureUuid()) && product.getSeriesUuid() != null) {
                    String seriesUuid = product.getSeriesUuid();
                    if (!originalFreeBalances.containsKey(seriesUuid)) {
                        SeriesItem series = seriesDataManager.getSeriesByUuid(currentNomenclatureUuid, seriesUuid);
                        if (series != null) {
                            originalFreeBalances.put(seriesUuid, series.getFreeBalance());
                            Log.d(TAG, "Сохранен оригинальный свободный остаток исходной серии " + seriesUuid + ": " + series.getFreeBalance());
                        }
                    }
                }
            }
        }
    }

    /**
     * Откатывает изменения свободных остатков серий
     */
    public void rollbackFreeBalanceChanges() {
        if (currentNomenclatureUuid == null || originalFreeBalances.isEmpty()) {
            Log.w(TAG, "Нет данных для отката изменений свободных остатков");
            return;
        }
        
        // Восстанавливаем оригинальные свободные остатки
        for (Map.Entry<String, Double> entry : originalFreeBalances.entrySet()) {
            String seriesUuid = entry.getKey();
            Double originalBalance = entry.getValue();
            
            if (seriesDataManager.updateSeriesFreeBalance(currentNomenclatureUuid, seriesUuid, originalBalance)) {
                Log.d(TAG, "Восстановлен свободный остаток серии " + seriesUuid + ": " + originalBalance);
            } else {
                Log.e(TAG, "Ошибка восстановления свободного остатка серии " + seriesUuid);
            }
        }

        originalFreeBalances.clear();
    }

    /**
     * Возвращает последний результат замены серии для передачи при сохранении
     */
    public SeriesChangeResult getLastSeriesChangeResult() {
        if (lastSeriesChangeResult != null) {
            return lastSeriesChangeResult;
        } else {
            return new SeriesChangeResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Сбрасывает сохраненный результат замены серии
     */
    public void clearLastSeriesChangeResult() {
        lastSeriesChangeResult = null;
    }


} 