package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import com.step.tcd_rpkb.data.datasources.DataSourceCallback;
import com.step.tcd_rpkb.data.datasources.LocalMoveDataSource;
import com.step.tcd_rpkb.data.datasources.RemoteMoveDataSource;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import com.step.tcd_rpkb.domain.util.ConnectivityChecker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.inject.Inject;
import dagger.hilt.android.qualifiers.ApplicationContext;

public class MoveRepositoryImpl implements MoveRepository {

    private final LocalMoveDataSource localDataSource;
    private final RemoteMoveDataSource remoteDataSource;
    private final UserSettingsRepository userSettingsRepository;
    private final Context appContext;
    private final ConnectivityChecker connectivityChecker;

    @Inject
    public MoveRepositoryImpl(LocalMoveDataSource localDataSource,
                              RemoteMoveDataSource remoteDataSource,
                              UserSettingsRepository userSettingsRepository,
                              @ApplicationContext Context appContext,
                              ConnectivityChecker connectivityChecker) {
        this.localDataSource = localDataSource;
        this.remoteDataSource = remoteDataSource;
        this.userSettingsRepository = userSettingsRepository;
        this.appContext = appContext;
        this.connectivityChecker = connectivityChecker;
    }

    @Override
    public void getMoveList(String state, String startDate, String endDate, RepositoryCallback<MoveResponse> callback) {
        boolean onlineMode = userSettingsRepository.isOnlineMode();
        boolean networkAvailable = connectivityChecker.isNetworkAvailable();

        boolean needSetDates = (startDate == null || startDate.isEmpty()) && 
                               (endDate == null || endDate.isEmpty());
        
        String finalStartDate = startDate;
        String finalEndDate = endDate;
        if (needSetDates) {
            finalStartDate = formatDateForRequest(getTwoMonthsAgoDate());
            finalEndDate = formatDateForRequest(getCurrentDate());
        }

        if (onlineMode && networkAvailable) {
            // TODO: RemoteDataSource.getMoveList все еще с проблемами маппинга POJO->DTO
            // Попытка удаленной загрузки
            System.err.println("MoveRepositoryImpl: Попытка удаленной загрузки getMoveList.");
            remoteDataSource.getMoveList(state, finalStartDate, finalEndDate, new DataSourceCallback<MoveResponse>() {
                @Override
                public void onSuccess(MoveResponse data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(Exception exception) {
                    System.err.println("MoveRepositoryImpl: Ошибка при удаленной загрузке getMoveList, используем локальные: " + exception.getMessage());
                    try {
                        callback.onSuccess(localDataSource.getMoveList()); // Fallback
                    } catch (Exception localEx) {
                        callback.onError(localEx);
                    }
                }
            });
        } else {
            System.out.println("MoveRepositoryImpl: Оффлайн/нет сети, загрузка getMoveList из локального источника.");
            try {
                callback.onSuccess(localDataSource.getMoveList());
            } catch (Exception e) {
                callback.onError(e);
            }
        }
    }

    @Override
    public void getDocumentMove(String guid, RepositoryCallback<Invoice> callback) {
        boolean onlineMode = userSettingsRepository.isOnlineMode();
        boolean networkAvailable = connectivityChecker.isNetworkAvailable();

        if (onlineMode && networkAvailable) {
            System.err.println("MoveRepositoryImpl: Попытка удаленной загрузки getDocumentMove.");
            remoteDataSource.getDocumentMove(guid, new DataSourceCallback<Invoice>() {
                @Override
                public void onSuccess(Invoice data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(Exception exception) {
                    System.err.println("MoveRepositoryImpl: Ошибка при удаленной загрузке getDocumentMove, используем локальные: " + exception.getMessage());
                    try {
                        callback.onSuccess(localDataSource.getDocumentMove(guid)); // Fallback
                    } catch (Exception localEx) {
                        callback.onError(localEx);
                    }
                }
            });
        } else {
            System.out.println("MoveRepositoryImpl: Оффлайн/нет сети, загрузка getDocumentMove из локального источника.");
            try {
                callback.onSuccess(localDataSource.getDocumentMove(guid));
            } catch (Exception e) {
                callback.onError(e);
            }
        }
    }

    private String formatDateForRequest(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date);
    }

    private Date getTwoMonthsAgoDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -2);
        return calendar.getTime();
    }

    private Date getCurrentDate() {
        return new Date();
    }
} 