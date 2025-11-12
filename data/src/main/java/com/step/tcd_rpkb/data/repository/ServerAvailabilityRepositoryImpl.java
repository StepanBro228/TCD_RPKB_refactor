package com.step.tcd_rpkb.data.repository;

import android.content.Context;
// import android.net.ConnectivityManager; // Больше не используется напрямую здесь
// import android.net.NetworkInfo;       // Больше не используется напрямую здесь
import android.util.Log;

import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.domain.repository.ServerAvailabilityCallback;
import com.step.tcd_rpkb.domain.repository.ServerAvailabilityRepository;
// import com.step.tcd_rpkb.network.NetworkUtils; // УДАЛЯЕМ
import com.step.tcd_rpkb.domain.util.ConnectivityChecker; // <-- ДОБАВЛЯЕМ

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerAvailabilityRepositoryImpl implements ServerAvailabilityRepository {
    private static final String TAG = "ServerAvailabilityRepo";

    private final MoveApiService moveApiService;
    private final ConnectivityChecker connectivityChecker;

    @Inject
    public ServerAvailabilityRepositoryImpl(MoveApiService moveApiService, ConnectivityChecker connectivityChecker) {
        this.moveApiService = moveApiService;
        // this.appContext = appContext;
        this.connectivityChecker = connectivityChecker;
    }

    @Override
    public void checkServerAvailability(ServerAvailabilityCallback domainCallback) {
        if (!connectivityChecker.isNetworkAvailable()) {
            Log.d(TAG, "❌ Сеть недоступна, сервер считается недоступным");
            domainCallback.onResult(false);
            return;
        }

        Call<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> call = moveApiService.getMoveList();

        call.enqueue(new Callback<com.step.tcd_rpkb.data.network.dto.MoveResponseDto>() {
            @Override
            public void onResponse(Call<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> call, Response<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> response) {
                Log.d(TAG, "✅ Сервер доступен, получен ответ с кодом: " + response.code());
                if (response.code() == 401) {
                    Log.d(TAG, "⚠️ Предупреждение: Ошибка авторизации (401): проверьте имя пользователя и пароль");
                } else if (response.code() == 403) {
                    Log.d(TAG, "⚠️ Предупреждение: Доступ запрещен (403): у пользователя нет прав доступа");
                }
                domainCallback.onResult(true);
            }

            @Override
            public void onFailure(Call<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> call, Throwable t) {
                Log.e(TAG, "❌ Ошибка при проверке доступности сервера: " + t.getMessage(), t);
                domainCallback.onResult(false);
            }
        });
    }
} 