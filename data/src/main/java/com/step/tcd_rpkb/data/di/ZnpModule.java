package com.step.tcd_rpkb.data.di;

import android.content.Context;

import com.google.gson.Gson;
import com.step.tcd_rpkb.data.datasources.ZnpLocalDataSource;
import com.step.tcd_rpkb.data.repository.ZnpRepositoryImpl;
import com.step.tcd_rpkb.domain.repository.ZnpRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Модуль Dagger Hilt для инъекции зависимостей ЗНП
 */
@Module
@InstallIn(SingletonComponent.class)
public class ZnpModule {
    
    @Provides
    @Singleton
    public ZnpLocalDataSource provideZnpLocalDataSource(
            @ApplicationContext Context context,
            Gson gson) {
        return new ZnpLocalDataSource(context, gson);
    }
    
    @Provides
    @Singleton
    public ZnpRepository provideZnpRepository(ZnpLocalDataSource localDataSource) {
        return new ZnpRepositoryImpl(localDataSource);
    }
} 