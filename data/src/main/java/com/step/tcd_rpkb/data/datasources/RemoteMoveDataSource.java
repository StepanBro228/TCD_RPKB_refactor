package com.step.tcd_rpkb.data.datasources;

import android.util.Log;

import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
import com.step.tcd_rpkb.data.mapper.MoveResponseMapper;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveResponse;

import java.io.IOException;
import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// TODO: Заменить NetworkUtils на Retrofit в будущем.
// TODO: Обработка ошибок и асинхронность должны быть улучшены.
public class RemoteMoveDataSource {

    private static final String TAG = "RemoteMoveDataSource";

    // NetworkUtils статический, поэтому не внедряем его экземпляры.
    // Зависимости от мапперов пока не нужны здесь, так как NetworkUtils
    // возвращает уже DTO-подобные структуры или сырой JSON, который парсится в нем же.
    // Когда перейдем на Retrofit, он будет возвращать DTO, и мапперы понадобятся здесь или в репозитории.

    // Однако, если мы хотим, чтобы DataSource возвращал сразу Domain модели, то мапперы нужны.
    // Давайте сделаем так для консистентности с LocalDataSource.
    private final MoveResponseMapper moveResponseMapper;
    private final InvoiceMapper invoiceMapper;
    private final MoveApiService moveApiService;

    @Inject
    public RemoteMoveDataSource(
            MoveApiService moveApiService, 
            MoveResponseMapper moveResponseMapper, 
            InvoiceMapper invoiceMapper
    ) {
        this.moveApiService = moveApiService;
        this.moveResponseMapper = moveResponseMapper;
        this.invoiceMapper = invoiceMapper;
    }

    /**
     * Получает список перемещений с сервера.
     * Этот метод должен быть асинхронным в реальном приложении.
     * NetworkUtils.fetchMoveList уже использует колбэк, нужно это адаптировать.
     * Пока что для простоты сделаем "синхронную обертку" с ожиданием, что крайне не рекомендуется.
     * Либо изменим сигнатуру на использование колбэка/возвращение Future/CompletableFuture.
     */
    public void getMoveList(String state, String startDate, String endDate, DataSourceCallback<MoveResponse> callback) {
        Log.d(TAG, "Выполняется getMoveList со параметрами: state=" + state + ", startDate=" + startDate + ", endDate=" + endDate);
        Call<MoveResponseDto> call = moveApiService.getMoveList(state, startDate, endDate);
        
        call.enqueue(new Callback<MoveResponseDto>() {
            @Override
            public void onResponse(Call<MoveResponseDto> call, Response<MoveResponseDto> response) {
                if (response.isSuccessful()) {
                    MoveResponseDto dto = response.body();
                    MoveResponse domainResponse = moveResponseMapper.mapToDomain(dto);
                    if (domainResponse != null) {
                        Log.d(TAG, "Успешно получен и смаплен список перемещений.");
                        callback.onSuccess(domainResponse);
                    } else {
                        Log.e(TAG, "getMoveList: DTO или результат маппинга null");
                        callback.onError(new IOException("Не удалось получить список перемещений (null)."));
                    }
                } else {
                    String errorMsg = "Ошибка при загрузке списка перемещений, Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ", Body: " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody для getMoveList", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new IOException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<MoveResponseDto> call, Throwable t) {
                Log.e(TAG, "getMoveList: Сбой сети или другая ошибка Retrofit", t);
                callback.onError(new IOException("Сбой при загрузке списка перемещений: " + t.getMessage(), t));
            }
        });
    }

    public void getDocumentMove(String guid, DataSourceCallback<Invoice> callback) {
        Log.d(TAG, "Выполняется getDocumentMove для GUID: " + guid);
        Call<InvoiceDto> call = moveApiService.getDocumentMove(guid);
        call.enqueue(new Callback<InvoiceDto>() {
            @Override
            public void onResponse(Call<InvoiceDto> call, Response<InvoiceDto> response) {
                if (response.isSuccessful()) {
                    InvoiceDto dto = response.body();
                    Invoice domainInvoice = invoiceMapper.mapToDomain(dto);
                    if (domainInvoice != null) {
                        Log.d(TAG, "Успешно получен и смаплен документ перемещения.");
                        callback.onSuccess(domainInvoice);
                    } else {
                        Log.e(TAG, "getDocumentMove: DTO или результат маппинга null для GUID: " + guid);
                        callback.onError(new IOException("Не удалось получить данные документа (null): " + guid));
                    }
                } else {
                    String errorMsg = "Ошибка при загрузке документа GUID: " + guid + ", Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ", Body: " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new IOException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<InvoiceDto> call, Throwable t) {
                Log.e(TAG, "getDocumentMove: Сбой сети для GUID: " + guid, t);
                callback.onError(new IOException("Сбой при загрузке документа GUID: " + guid + ", Error: " + t.getMessage(), t));
            }
        });
    }

    // Старые заглушки mapOld...Dto и NetworkUtils...simulation УДАЛЕНЫ, так как они больше не нужны
    // или будут заменены полной реализацией getMoveList.
} 