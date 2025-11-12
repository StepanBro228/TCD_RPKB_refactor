package com.step.tcd_rpkb.di;

import android.content.Context;

import com.google.gson.Gson;
import com.step.tcd_rpkb.data.datasources.LocalMoveDataSource;
import com.step.tcd_rpkb.data.datasources.RemoteMoveDataSource;
import com.step.tcd_rpkb.data.mapper.SaveMoveDataMapper;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.repository.MoveRepositoryImpl;
import com.step.tcd_rpkb.data.repository.UserRepositoryImpl;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.UserRepository;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import com.step.tcd_rpkb.domain.repository.PrixodRepository;
import com.step.tcd_rpkb.data.repository.PrixodRepositoryImpl;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class) //
public abstract class RepositoryModule {



    @Provides
    @Singleton
    public static MoveRepository provideMoveRepository(LocalMoveDataSource localDataSource,
                                                 RemoteMoveDataSource remoteDataSource,
                                                 UserSettingsRepository userSettingsRepository,
                                                 @ApplicationContext Context appContext,
                                                 com.step.tcd_rpkb.domain.util.ConnectivityChecker connectivityChecker,
                                                 MoveApiService moveApiService,
                                                 SaveMoveDataMapper saveMoveDataMapper,
                                                 Gson gson) {
        return new MoveRepositoryImpl(localDataSource, remoteDataSource, userSettingsRepository, appContext, connectivityChecker, moveApiService, saveMoveDataMapper, gson);
    }

    @Provides
    @Singleton
    public static UserRepository provideUserRepository(@ApplicationContext Context appContext) {

        return new UserRepositoryImpl(appContext);
    }

    @Binds
    @Singleton
    public abstract PrixodRepository bindPrixodRepository(PrixodRepositoryImpl impl);
}