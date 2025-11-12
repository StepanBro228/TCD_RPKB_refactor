package com.step.tcd_rpkb.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.step.tcd_rpkb.data.datasources.ZnpLocalDataSource;
import com.step.tcd_rpkb.data.mapper.ZnpDataMapper;
import com.step.tcd_rpkb.data.network.dto.ZnpSeriesDataDto;
import com.step.tcd_rpkb.domain.model.ZnpSeriesData;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.ZnpRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * Реализация репозитория ЗНП
 */
public class ZnpRepositoryImpl implements ZnpRepository {
    
    private final ZnpLocalDataSource localDataSource;
    private final Executor executor;
    private final Handler mainHandler;
    
    @Inject
    public ZnpRepositoryImpl(ZnpLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
        this.executor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public void getZnpDataForSeries(String seriesUuid, RepositoryCallback<ZnpSeriesData> callback) {
        executor.execute(() -> {
            try {
                ZnpSeriesDataDto dto = localDataSource.getZnpDataForSeries(seriesUuid);
                ZnpSeriesData domainData = ZnpDataMapper.mapToDomain(dto);
                
                mainHandler.post(() -> {
                    if (domainData != null) {
                        callback.onSuccess(domainData);
                    } else {
                        callback.onError(new Exception("Данные ЗНП для серии не найдены"));
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    @Override
    public void saveZnpReservation(String seriesUuid, ZnpSeriesData znpSeriesData, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                ZnpSeriesDataDto dto = ZnpDataMapper.mapToDto(znpSeriesData);
                boolean success = localDataSource.saveZnpReservation(seriesUuid, dto);
                
                mainHandler.post(() -> callback.onSuccess(success));
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
} 