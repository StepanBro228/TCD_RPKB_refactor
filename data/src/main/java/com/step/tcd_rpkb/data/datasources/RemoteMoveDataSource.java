package com.step.tcd_rpkb.data.datasources;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
import com.step.tcd_rpkb.data.mapper.MoveResponseMapper;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import com.step.tcd_rpkb.data.network.dto.ChangeMoveStatusResponseDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import com.step.tcd_rpkb.data.mapper.ChangeMoveStatusMapper;
import com.step.tcd_rpkb.data.exceptions.ServerErrorException;
import com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException;

import java.io.IOException;
import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RemoteMoveDataSource {

    private static final String TAG = "RemoteMoveDataSource";


    private final MoveResponseMapper moveResponseMapper;
    private final InvoiceMapper invoiceMapper;
    private final MoveApiService moveApiService;
    private final ChangeMoveStatusMapper changeMoveStatusMapper;

    @Inject
    public RemoteMoveDataSource(
            MoveApiService moveApiService, 
            MoveResponseMapper moveResponseMapper, 
            InvoiceMapper invoiceMapper,
            ChangeMoveStatusMapper changeMoveStatusMapper
    ) {
        this.moveApiService = moveApiService;
        this.moveResponseMapper = moveResponseMapper;
        this.invoiceMapper = invoiceMapper;
        this.changeMoveStatusMapper = changeMoveStatusMapper;
    }

    /**
     * Получает список перемещений с сервера.
     * Этот метод должен быть асинхронным в реальном приложении.
     * NetworkUtils.fetchMoveList уже использует колбэк, нужно это адаптировать.
     * Пока что для простоты сделаем "синхронную обертку" с ожиданием, что крайне не рекомендуется.
     * Либо изменим сигнатуру на использование колбэка/возвращение Future/CompletableFuture.
     */
    public void getMoveList(String state, String startDate, String endDate, String userGuid, boolean useFilter, String nomenculature, String series, DataSourceCallback<MoveResponse> callback) {
        Log.d(TAG, "Выполняется getMoveList со параметрами: state=" + state + ", startDate=" + startDate + ", endDate=" + endDate + ", userGuid=" + userGuid + ", useFilter=" + useFilter);
        Call<MoveResponseDto> call = moveApiService.getMoveList(state, startDate, endDate, userGuid, useFilter, nomenculature, series);
        
        call.enqueue(new Callback<MoveResponseDto>() {
            @Override
            public void onResponse(Call<MoveResponseDto> call, Response<MoveResponseDto> response) {
                MoveResponseDto dto = response.body();
                
                // Если есть body в response.body(), обрабатываем его
                if (dto != null) {
                    processResponseDto(dto, callback);
                } else {
                    // Если body пустой, пытаемся получить JSON из errorBody
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyString = response.errorBody().string();
                            Log.d(TAG, "Пытаемся распарсить JSON из errorBody: " + errorBodyString);
                            
                            // Пытаемся распарсить JSON из errorBody
                            Gson gson = new GsonBuilder()
                                    .disableHtmlEscaping()
                                    .create();
                            dto = gson.fromJson(errorBodyString, MoveResponseDto.class);
                            
                            if (dto != null) {
                                Log.d(TAG, "Успешно распарсили DTO из errorBody");
                                processResponseDto(dto, callback);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге errorBody как JSON", e);
                    }
                    
                    // Если не удалось распарсить JSON из errorBody, обрабатываем как обычную ошибку
                    if (response.isSuccessful()) {
                        Log.e(TAG, "getMoveList: DTO равен null при успешном ответе");
                        callback.onError(new IOException("Не удалось получить список перемещений (пустой ответ)."));
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
        try {
            Call<InvoiceDto> call = moveApiService.getDocumentMove(guid);
            
            // Логируем
            String requestUrl = call.request().url().toString();
            Log.d(TAG, "getDocumentMove URL: " + requestUrl);
            
            call.enqueue(new Callback<InvoiceDto>() {
                @Override
                public void onResponse(Call<InvoiceDto> call, Response<InvoiceDto> response) {
                    Log.d(TAG, "getDocumentMove onResponse: Code=" + response.code() + ", isSuccessful=" + response.isSuccessful());
                    
                    InvoiceDto dto = response.body();
                    
                    // Если есть body, пытаемся его обработать независимо от статус кода
                    if (dto != null) {
                        processDocumentResponseDto(dto, callback);
                        return;
                    }
                    
                    // Если body пустой, но статус не успешный, пытаемся получить JSON из errorBody
                    if (!response.isSuccessful() && response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.d(TAG, "getDocumentMove: попытка парсинга JSON из errorBody: " + errorBodyString);
                            
                            Gson gson = new GsonBuilder()
                                    .disableHtmlEscaping()
                                    .create();
                            InvoiceDto errorDto = gson.fromJson(errorBodyString, InvoiceDto.class);
                            
                            if (errorDto != null) {
                                processDocumentResponseDto(errorDto, callback);
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getDocumentMove: ошибка парсинга JSON из errorBody: " + e.getMessage());
                        }
                    }
                    
                    // Если все попытки не удались, возвращаем стандартную ошибку
                    String errorMsg = "Ошибка при загрузке документа GUID: " + guid + ", Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg += ", Body: " + errorBody;
                            Log.e(TAG, "getDocumentMove errorBody: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new IOException(errorMsg));
                }

                @Override
                public void onFailure(Call<InvoiceDto> call, Throwable t) {
                    Log.e(TAG, "getDocumentMove: Сбой сети для GUID: " + guid, t);
                    callback.onError(new IOException("Сбой при загрузке документа GUID: " + guid + ", Error: " + t.getMessage(), t));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Исключение при выполнении getDocumentMove: " + e.getMessage(), e);
            callback.onError(new IOException("Исключение при подготовке запроса: " + e.getMessage(), e));
        }
    }

    public void changeMoveStatus(String guid, String targetState, String userGuid, DataSourceCallback<ChangeMoveStatusResult> callback) {
        Log.d(TAG, "Выполняется changeMoveStatus: guid=" + guid + ", state=" + targetState + ", userGuid=" + userGuid);
        Call<ChangeMoveStatusResponseDto> call = moveApiService.changeMoveStatus(guid, targetState, userGuid);
        call.enqueue(new Callback<ChangeMoveStatusResponseDto>() {
            @Override
            public void onResponse(Call<ChangeMoveStatusResponseDto> call, Response<ChangeMoveStatusResponseDto> response) {
                ChangeMoveStatusResponseDto dto = response.body();
                String rawErrorBody = null;
                
                // Если response.body() пустой, пытаемся получить JSON из errorBody
                if (dto == null && response.errorBody() != null) {
                    try {
                        rawErrorBody = response.errorBody().string();
                        Log.d(TAG, "changeMoveStatus: попытка парсинга JSON из errorBody: " + rawErrorBody);
                        Gson gson = new GsonBuilder()
                                .disableHtmlEscaping()
                                .create();
                        dto = gson.fromJson(rawErrorBody, ChangeMoveStatusResponseDto.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при парсинге errorBody как JSON в changeMoveStatus", e);
                    }
                }
                
                // Обрабатываем DTO если получен
                if (dto != null) {
                    processChangeMoveStatusResponseDto(dto, callback);
                } else {
                    // Если DTO не получен, возвращаем общую ошибку
                    String errorMsg = "Ошибка смены статуса, Code: " + response.code();
                    if (rawErrorBody != null && !rawErrorBody.isEmpty()) {
                        errorMsg += ", Body: " + rawErrorBody;
                    } else if (response.errorBody() != null) {
                        try {
                            String fallbackErrorBody = response.errorBody().string();
                            if (!fallbackErrorBody.isEmpty()) {
                                errorMsg += ", Body: " + fallbackErrorBody;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка чтения errorBody для changeMoveStatus (fallback)", e);
                        }
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new IOException(errorMsg));
                }
            }
            @Override
            public void onFailure(Call<ChangeMoveStatusResponseDto> call, Throwable t) {
                Log.e(TAG, "changeMoveStatus: Сбой сети или другая ошибка Retrofit", t);
                callback.onError(new IOException("Сбой при смене статуса: " + t.getMessage(), t));
            }
        });
    }

    /**
     * Обрабатывает DTO ответа, проверяет поле "Результат" и выполняет соответствующие действия
     */
    private void processResponseDto(MoveResponseDto dto, DataSourceCallback<MoveResponse> callback) {
        // Проверяем поле "Результат"
        if (!dto.isResult()) {
            // Если результат false, возвращаем серверную ошибку с текстом из "ТекстОшибки"
            String errorText = dto.getErrorText();
            if (errorText == null || errorText.trim().isEmpty()) {
                errorText = "Произошла ошибка при выполнении запроса";
            }
            Log.e(TAG, "getMoveList: Результат false, ТекстОшибки: " + errorText);
            callback.onError(new ServerErrorWithTypeException(errorText, ServerErrorWithTypeException.ErrorType.MOVE_LIST));
            return;
        }
        
        // Если результат true, обрабатываем как обычно
        MoveResponse domainResponse = moveResponseMapper.mapToDomain(dto);
        if (domainResponse != null) {
            Log.d(TAG, "Успешно получен и смаплен список перемещений.");
            callback.onSuccess(domainResponse);
        } else {
            Log.e(TAG, "getMoveList: Результат маппинга null");
            callback.onError(new IOException("Не удалось получить список перемещений (ошибка маппинга)."));
        }
    }
    
    /**
     * Обрабатывает DTO ответа документа, проверяет поле "Результат" и выполняет соответствующие действия
     */
    private void processDocumentResponseDto(InvoiceDto dto, DataSourceCallback<Invoice> callback) {
        // Проверяем поле "Результат"
        if (!dto.isResult()) {
            // Если результат false, возвращаем серверную ошибку с текстом из "ТекстОшибки"
            String errorText = dto.getErrorText();
            if (errorText == null || errorText.trim().isEmpty()) {
                errorText = "Произошла ошибка при получении документа";
            }
            Log.e(TAG, "getDocumentMove: Результат false, ТекстОшибки: " + errorText);
            callback.onError(new ServerErrorWithTypeException(errorText, ServerErrorWithTypeException.ErrorType.DOCUMENT_MOVE));
            return;
        }
        
        // Если результат true, обрабатываем как обычно
        try {
            Invoice invoice = invoiceMapper.mapToDomain(dto);
            if (invoice != null) {
                Log.d(TAG, "getDocumentMove: Успешно получен и смаплен документ");
                callback.onSuccess(invoice);
            } else {
                Log.e(TAG, "getDocumentMove: Результат маппинга равен null");
                callback.onError(new IOException("Не удалось преобразовать данные документа"));
            }
        } catch (Exception e) {
            Log.e(TAG, "getDocumentMove: Ошибка при маппинге данных: " + e.getMessage());
            callback.onError(new IOException("Ошибка при обработке данных документа: " + e.getMessage(), e));
        }
    }
    
    /**
     * Обрабатывает DTO ответа смены статуса, проверяет поле "Результат" и выполняет соответствующие действия
     */
    private void processChangeMoveStatusResponseDto(ChangeMoveStatusResponseDto dto, DataSourceCallback<ChangeMoveStatusResult> callback) {
        // Проверяем поле "Результат"
        if (!dto.isResult()) {
            // Если результат false, возвращаем серверную ошибку с текстом из "ТекстОшибки"
            String errorText = dto.getErrorText();
            if (errorText == null || errorText.trim().isEmpty()) {
                errorText = "Произошла ошибка при смене статуса";
            }
            Log.e(TAG, "changeMoveStatus: Результат false, ТекстОшибки: " + errorText);
            callback.onError(new ServerErrorWithTypeException(errorText, ServerErrorWithTypeException.ErrorType.CHANGE_MOVE_STATUS));
            return;
        }
        
        // Если результат true, обрабатываем как обычно
        try {
            ChangeMoveStatusResult result = changeMoveStatusMapper.mapToDomain(dto);
            if (result != null) {
                Log.d(TAG, "changeMoveStatus: Успешно получен результат смены статуса");
                callback.onSuccess(result);
            } else {
                Log.e(TAG, "changeMoveStatus: Результат маппинга равен null");
                callback.onError(new IOException("Не удалось обработать результат смены статуса"));
            }
        } catch (Exception e) {
            Log.e(TAG, "changeMoveStatus: Ошибка при маппинге данных: " + e.getMessage());
            callback.onError(new IOException("Ошибка при обработке данных смены статуса: " + e.getMessage(), e));
        }
    }

} 