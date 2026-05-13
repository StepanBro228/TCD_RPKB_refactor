package com.step.tcd_rpkb.data.datasources;

import android.util.Log;

import com.step.tcd_rpkb.data.local.realm.MoveMetadataRealm;
import com.step.tcd_rpkb.data.local.realm.ProductRealm;
import com.step.tcd_rpkb.data.local.realm.SeriesItemRealm;
import com.step.tcd_rpkb.data.mapper.MoveMetadataRealmMapper;
import com.step.tcd_rpkb.data.mapper.ProductRealmMapper;
import com.step.tcd_rpkb.data.mapper.SeriesItemRealmMapper;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.realm.Realm;
import io.realm.RealmResults;

@Singleton
public class LocalRealmDataSource {
    
    private static final String TAG = "LocalRealmDataSource";
    
    private final Realm realm;
    private final ProductRealmMapper productMapper;
    private final SeriesItemRealmMapper seriesMapper;
    private final MoveMetadataRealmMapper metadataMapper;
    
    @Inject
    public LocalRealmDataSource(
        Realm realm,
        ProductRealmMapper productMapper,
        SeriesItemRealmMapper seriesMapper,
        MoveMetadataRealmMapper metadataMapper
    ) {
        this.realm = realm;
        this.productMapper = productMapper;
        this.seriesMapper = seriesMapper;
        this.metadataMapper = metadataMapper;
    }
    
    // =====================================================
    // PRODUCTS - с detached копиями и error handling
    // =====================================================
    
    /**
     * Сохранение всех продуктов перемещения (batch)
     * Используется при начальной загрузке с сервера
     */
    public void saveProducts(String moveUuid, List<Product> products) {
        try {
            realm.executeTransaction(bgRealm -> {
                for (Product product : products) {
                    ProductRealm realmProduct = productMapper.toRealm(product, moveUuid);
                    bgRealm.copyToRealmOrUpdate(realmProduct);
                }
                Log.d(TAG, "Сохранено " + products.size() + " продуктов для перемещения " + moveUuid);
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении продуктов: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранение одного продукта (для автосохранения при изменении)
     * Вызывается при каждом изменении taken, quantity и т.д.
     */
    public void saveProduct(String moveUuid, Product product) {
        try {
            realm.executeTransaction(bgRealm -> {
                ProductRealm realmProduct = productMapper.toRealm(product, moveUuid);
                bgRealm.copyToRealmOrUpdate(realmProduct);
            });
            Log.d(TAG, "Автосохранение продукта " + product.getProductLineId() + " для перемещения " + moveUuid);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка автосохранения продукта: " + e.getMessage(), e);
        }
    }
    
    /**
     * Загрузка продуктов перемещения
     * КРИТИЧНО: Возвращает detached копии для thread safety!
     */
    public List<Product> loadProducts(String moveUuid) {
        try {
            RealmResults<ProductRealm> results = realm.where(ProductRealm.class)
                .equalTo("moveUuid", moveUuid)
                .findAll();
            
            // КРИТИЧНО: Создаем detached копии!
            List<ProductRealm> detachedList = realm.copyFromRealm(results);
            
            Log.d(TAG, "Загружено " + detachedList.size() + " продуктов для перемещения " + moveUuid);
            return productMapper.toDomainList(detachedList);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки продуктов: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Удаление одного продукта
     * Используется при удалении строки пользователем
     */
    public void deleteProduct(String moveUuid, String productLineId) {
        try {
            realm.executeTransaction(bgRealm -> {
                ProductRealm product = bgRealm.where(ProductRealm.class)
                    .equalTo("moveUuid", moveUuid)
                    .equalTo("productLineId", productLineId)
                    .findFirst();
                
                if (product != null) {
                    product.deleteFromRealm();
                    Log.d(TAG, "Удален продукт " + productLineId);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка удаления продукта: " + e.getMessage(), e);
        }
    }
    
    // =====================================================
    // SERIES - с detached копиями
    // =====================================================
    
    public void saveSeries(String moveUuid, String nomenclatureUuid, List<SeriesItem> seriesItems) {
        try {
            realm.executeTransaction(bgRealm -> {
                for (SeriesItem item : seriesItems) {
                    String id = moveUuid + "_" + nomenclatureUuid + "_" + item.getSeriesUuid();
                    SeriesItemRealm realmItem = seriesMapper.toRealm(item, moveUuid, nomenclatureUuid, id);
                    bgRealm.copyToRealmOrUpdate(realmItem);
                }
                Log.d(TAG, "Сохранено " + seriesItems.size() + " серий для " + nomenclatureUuid);
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения серий: " + e.getMessage(), e);
        }
    }
    
    public List<SeriesItem> loadSeries(String moveUuid, String nomenclatureUuid) {
        try {
            String idPrefix = moveUuid + "_" + nomenclatureUuid + "_";
            RealmResults<SeriesItemRealm> results = realm.where(SeriesItemRealm.class)
                .beginsWith("id", idPrefix)
                .findAll();
            
            // Detached копии
            List<SeriesItemRealm> detachedList = realm.copyFromRealm(results);
            
            return seriesMapper.toDomainList(detachedList);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки серий: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
    
    // =====================================================
    // MOVE METADATA - для отслеживания статусов
    // =====================================================
    
    /**
     * Сохранение метаданных перемещения
     * Вызывается:
     * 1. При загрузке списка перемещений (MoveListViewModel)
     * 2. При смене статуса перемещения
     */
    public void saveMoveMetadata(MoveItem moveItem) {
        try {
            realm.executeTransaction(bgRealm -> {
                MoveMetadataRealm metadata = metadataMapper.toRealm(moveItem);
                bgRealm.copyToRealmOrUpdate(metadata);
            });
            Log.d(TAG, "Сохранены метаданные для перемещения " + moveItem.getMovementId() + 
                      ", статус: " + moveItem.getSigningStatus());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения метаданных: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получение старого статуса перемещения из Realm
     * Используется для определения нужно ли очищать данные при смене статуса
     */
    public String getOldSigningStatus(String moveUuid) {
        try {
            MoveMetadataRealm metadata = realm.where(MoveMetadataRealm.class)
                .equalTo("moveUuid", moveUuid)
                .findFirst();
            
            return metadata != null ? metadata.getSigningStatus() : null;
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения старого статуса: " + e.getMessage(), e);
            return null;
        }
    }
    
    // =====================================================
    // CLEANUP - очистка данных
    // =====================================================
    
    /**
     * Удаление всех данных перемещения
     * Вызывается при смене статуса ИЗ "Комплектуется"
     */
    public void deleteMoveData(String moveUuid) {
        try {
            realm.executeTransaction(bgRealm -> {
                // Удаляем продукты
                RealmResults<ProductRealm> products = bgRealm.where(ProductRealm.class)
                    .equalTo("moveUuid", moveUuid)
                    .findAll();
                int productsCount = products.size();
                products.deleteAllFromRealm();
                
                // Удаляем серии
                RealmResults<SeriesItemRealm> series = bgRealm.where(SeriesItemRealm.class)
                    .equalTo("moveUuid", moveUuid)
                    .findAll();
                int seriesCount = series.size();
                series.deleteAllFromRealm();
                
                // Удаляем метаданные
                MoveMetadataRealm metadata = bgRealm.where(MoveMetadataRealm.class)
                    .equalTo("moveUuid", moveUuid)
                    .findFirst();
                if (metadata != null) {
                    metadata.deleteFromRealm();
                }
                
                Log.d(TAG, "Удалены все данные для перемещения " + moveUuid + 
                          ": " + productsCount + " продуктов, " + seriesCount + " серий");
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка удаления данных перемещения: " + e.getMessage(), e);
        }
    }
    
    /**
     * Очистка данных перемещений НЕ в статусе "Комплектуется"
     * Вызывается при старте приложения (в MainApplication или первой Activity)
     */
    public int cleanupNonActiveMovements() {
        int deletedCount = 0;
        try {
            realm.executeTransaction(bgRealm -> {
                RealmResults<MoveMetadataRealm> nonActiveMoves = bgRealm.where(MoveMetadataRealm.class)
                    .notEqualTo("signingStatus", "Комплектуется")
                    .findAll();
                
                for (MoveMetadataRealm metadata : nonActiveMoves) {
                    String moveUuid = metadata.getMoveUuid();
                    deleteMoveData(moveUuid);
                }
            });
            
            Log.d(TAG, "Очищено " + deletedCount + " неактивных перемещений");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка cleanup неактивных перемещений: " + e.getMessage(), e);
        }
        return deletedCount;
    }
}



