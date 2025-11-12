package com.step.tcd_rpkb.data.di;

import android.content.Context;

import com.google.gson.Gson;
import com.step.tcd_rpkb.data.datasources.RemoteSeriesDataSource;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.repository.SeriesRepositoryImpl;
import com.step.tcd_rpkb.domain.repository.SeriesRepository;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import com.step.tcd_rpkb.domain.util.ConnectivityChecker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Модуль Dagger Hilt для инъекции зависимостей серий товаров
 */
@Module
@InstallIn(SingletonComponent.class)
public class SeriesModule {
    
    @Provides
    @Singleton
    public RemoteSeriesDataSource provideRemoteSeriesDataSource(MoveApiService moveApiService) {
        return new RemoteSeriesDataSource(moveApiService);
    }
    
    @Provides
    @Singleton
    public SeriesRepository provideSeriesRepository(
            @ApplicationContext Context context,
            Gson gson,
            UserSettingsRepository userSettingsRepository,
            ConnectivityChecker connectivityChecker,
            RemoteSeriesDataSource remoteSeriesDataSource) {
        return new SeriesRepositoryImpl(
                context, 
                gson, 
                userSettingsRepository, 
                connectivityChecker, 
                remoteSeriesDataSource
        );
    }
} 