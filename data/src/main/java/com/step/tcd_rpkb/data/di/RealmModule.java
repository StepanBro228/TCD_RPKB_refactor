package com.step.tcd_rpkb.data.di;

import android.content.Context;

import com.step.tcd_rpkb.data.datasources.LocalRealmDataSource;
import com.step.tcd_rpkb.data.mapper.MoveMetadataRealmMapper;
import com.step.tcd_rpkb.data.mapper.ProductRealmMapper;
import com.step.tcd_rpkb.data.mapper.SeriesItemRealmMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.realm.Realm;
import io.realm.RealmConfiguration;

@Module
@InstallIn(SingletonComponent.class)
public class RealmModule {
    
    @Provides
    @Singleton
    public Realm provideRealm(@ApplicationContext Context context) {
        // Realm.init() уже вызван в MainApplication.onCreate()
        
        RealmConfiguration config = new RealmConfiguration.Builder()
            .name("tcd_rpkb_moves.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded() // Только на начальном этапе разработки!
            .build();
        
        return Realm.getInstance(config);
    }
    
    @Provides
    @Singleton
    public LocalRealmDataSource provideLocalRealmDataSource(
        Realm realm,
        ProductRealmMapper productMapper,
        SeriesItemRealmMapper seriesMapper,
        MoveMetadataRealmMapper metadataMapper
    ) {
        return new LocalRealmDataSource(realm, productMapper, seriesMapper, metadataMapper);
    }
}



