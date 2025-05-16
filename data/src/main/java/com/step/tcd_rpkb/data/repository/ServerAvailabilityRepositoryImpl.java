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
    // private final Context appContext; // Больше не нужен здесь, т.к. ConnectivityChecker его инкапсулирует
    private final ConnectivityChecker connectivityChecker; // <-- ДОБАВЛЯЕМ

    @Inject
    // public ServerAvailabilityRepositoryImpl(MoveApiService moveApiService, Context appContext) { // СТАРАЯ СИГНАТУРА
    public ServerAvailabilityRepositoryImpl(MoveApiService moveApiService, ConnectivityChecker connectivityChecker) { // НОВАЯ СИГНАТУРА
        this.moveApiService = moveApiService;
        // this.appContext = appContext;
        this.connectivityChecker = connectivityChecker; // <-- СОХРАНЯЕМ
    }

    @Override
    public void checkServerAvailability(ServerAvailabilityCallback domainCallback) {
        // Используем NetworkUtils для проверки доступности сети
        // if (!NetworkUtils.isNetworkAvailable(appContext)) {
        if (!connectivityChecker.isNetworkAvailable()) { // <-- ИСПОЛЬЗУЕМ НОВЫЙ ЧЕКЕР
            Log.d(TAG, "❌ Сеть недоступна, сервер считается недоступным");
            domainCallback.onResult(false);
            return;
        }

        // Используем moveApiService.getMoveList() без параметров, 
        // так как нам нужен просто GET запрос к /movelist для проверки доступности сервера.
        // AuthInterceptor добавит необходимые заголовки.
        // Убедимся, что MoveApiService.getMoveList() возвращает Call<MoveResponseDto>, а не Call<Object>.
        // Если сервер на /movelist без параметров возвращает не MoveResponseDto, а что-то другое (или пустой ответ),
        // то Call<Void> или Call<ResponseBody> может быть более подходящим.
        // Но для простой проверки доступности, если /movelist ожидает параметры и не может быть вызван без них,
        // тогда moveApiService.getMoveList(null, null, null) было бы правильнее, если сервер это допускает.
        // Старый DataProvider делал GET на /movelist БЕЗ query params. Значит, второй метод в MoveApiService (без query) - то что нужно.

        Call<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> call = moveApiService.getMoveList(); // Используем версию без параметров

        call.enqueue(new Callback<com.step.tcd_rpkb.data.network.dto.MoveResponseDto>() { // Тип изменен на MoveResponseDto
            @Override
            public void onResponse(Call<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> call, Response<com.step.tcd_rpkb.data.network.dto.MoveResponseDto> response) {
                // Любой ответ от сервера (даже ошибка 4xx или 5xx) означает, что сервер жив.
                // Retrofit выдаст onFailure только при сетевых проблемах или проблемах с парсингом,
                // которые здесь не так важны, как сам факт ответа.
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

    // Локальная реализация isNetworkAvailable УДАЛЕНА
    /*
    private boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    */
} 