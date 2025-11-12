package com.step.tcd_rpkb.data.repository;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.step.tcd_rpkb.data.datasources.DataSourceCallback;
import com.step.tcd_rpkb.data.datasources.LocalMoveDataSource;
import com.step.tcd_rpkb.data.datasources.RemoteMoveDataSource;
import com.step.tcd_rpkb.data.mapper.SaveMoveDataMapper;
import com.step.tcd_rpkb.data.network.MoveApiService;
import com.step.tcd_rpkb.data.network.dto.SaveMoveDataRequestDto;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import com.step.tcd_rpkb.domain.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MoveRepositoryImpl implements MoveRepository {

    private static final String TAG = "MoveRepositoryImpl";

    private final LocalMoveDataSource localDataSource;
    private final RemoteMoveDataSource remoteDataSource;
    private final UserSettingsRepository userSettingsRepository;
    private final Context appContext;
    private final ConnectivityChecker connectivityChecker;
    private final MoveApiService moveApiService;
    private final SaveMoveDataMapper saveMoveDataMapper;
    private final Gson gson;

    @Inject
    public MoveRepositoryImpl(LocalMoveDataSource localDataSource,
                              RemoteMoveDataSource remoteDataSource,
                              UserSettingsRepository userSettingsRepository,
                              @ApplicationContext Context appContext,
                              ConnectivityChecker connectivityChecker,
                              MoveApiService moveApiService,
                              SaveMoveDataMapper saveMoveDataMapper,
                              Gson gson) {
        this.localDataSource = localDataSource;
        this.remoteDataSource = remoteDataSource;
        this.userSettingsRepository = userSettingsRepository;
        this.appContext = appContext;
        this.connectivityChecker = connectivityChecker;
        this.moveApiService = moveApiService;
        this.saveMoveDataMapper = saveMoveDataMapper;
        this.gson = gson;
    }

    @Override
    public void getMoveList(String state, String startDate, String endDate, String userGuid, boolean useFilter, String nomenculature, String series, RepositoryCallback<MoveResponse> callback) {
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
            // Попытка удаленной загрузки
            System.err.println("MoveRepositoryImpl: Попытка удаленной загрузки getMoveList.");
            remoteDataSource.getMoveList(state, finalStartDate, finalEndDate, userGuid, useFilter, nomenculature, series, new DataSourceCallback<MoveResponse>() {
                @Override
                public void onSuccess(MoveResponse data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(Exception exception) {
                    System.err.println("MoveRepositoryImpl: Ошибка при удаленной загрузке getMoveList: " + exception.getMessage());
                    callback.onError(exception);
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
            System.err.println("MoveRepositoryImpl: Попытка удаленной загрузки getDocumentMove для guid: " + guid);
            remoteDataSource.getDocumentMove(guid, new DataSourceCallback<Invoice>() {
                @Override
                public void onSuccess(Invoice data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onError(Exception exception) {
                    System.err.println("MoveRepositoryImpl: Ошибка при удаленной загрузке getDocumentMove: " + exception.getMessage());
                    callback.onError(exception);
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


    @Override
    public void changeMoveStatus(String guid, String targetState, String userGuid, RepositoryCallback<ChangeMoveStatusResult> callback) {
        boolean onlineMode = userSettingsRepository.isOnlineMode();
        boolean networkAvailable = connectivityChecker.isNetworkAvailable();
        if (onlineMode && networkAvailable) {
            remoteDataSource.changeMoveStatus(guid, targetState, userGuid, new DataSourceCallback<ChangeMoveStatusResult>() {
                @Override
                public void onSuccess(ChangeMoveStatusResult data) {
                    callback.onSuccess(data);
                }
                @Override
                public void onError(Exception exception) {
                    callback.onError(exception);
                }
            });
        } else {
            // Оффлайн-режим: имитация ошибки
            callback.onSuccess(new ChangeMoveStatusResult(false,  null, null, new ArrayList<>()));
        }
    }

    @Override
    public void saveMoveData(String moveGuid, String userGuid, List<Product> products, RepositoryCallback<Boolean> callback) {
        if (moveGuid == null || moveGuid.isEmpty() || userGuid == null || userGuid.isEmpty()) {
            callback.onError(new IllegalArgumentException("MoveGuid и UserGuid не могут быть пустыми"));
            return;
        }
        
        if (products == null || products.isEmpty()) {
            Log.w(TAG, "Список продуктов пуст для перемещения " + moveGuid);
            callback.onSuccess(true); // Считаем успехом если нет данных для сохранения
            return;
        }
        
        try {
            // Создаем запрос для сохранения данных
            SaveMoveDataRequestDto request = saveMoveDataMapper.createSaveRequest(moveGuid, userGuid, products);
            
            // Создаем JSON вручную чтобы обеспечить правильный порядок полей
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"ГУИДПеремещения\":\"").append(moveGuid).append("\",");
            jsonBuilder.append("\"ГУИДПользователя\":\"").append(userGuid).append("\",");
            jsonBuilder.append("\"Данные\":[");
            
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                if (i > 0) jsonBuilder.append(",");
                
                jsonBuilder.append("{");
                jsonBuilder.append("\"УИДСтрокиТовары\":\"").append(product.getProductLineId()).append("\"");
                
                // Добавляем УИДСтрокиТоварыРодитель только если он не null
                if (product.getParentProductLineId() != null) {
                    jsonBuilder.append(",\"УИДСтрокиТоварыРодитель\":\"").append(product.getParentProductLineId()).append("\"");
                }
                
                // НоменклатураГУИД
                if (product.getNomenclatureUuid() != null) {
                    jsonBuilder.append(",\"НоменклатураГУИД\":\"").append(product.getNomenclatureUuid()).append("\"");
                }
                
                // СерияГУИД
                if (product.getSeriesUuid() != null) {
                    jsonBuilder.append(",\"СерияГУИД\":\"").append(product.getSeriesUuid()).append("\"");
                }
                
                // ДокументРезерваГУИД
                if (product.getReserveDocumentUuid() != null) {
                    jsonBuilder.append(",\"ДокументРезерваГУИД\":\"").append(product.getReserveDocumentUuid()).append("\"");
                }
                
                // Exist
                jsonBuilder.append(",\"Exist\":\"").append(product.getExists()).append("\"");
                
                // Количество - отправляем значение taken,
                jsonBuilder.append(",\"Количество\":").append(product.getTaken());
                
                jsonBuilder.append("}");
            }
            
            jsonBuilder.append("]}");
            String requestJson = jsonBuilder.toString();
            

            Log.d(TAG, "Создан JSON размером: " + requestJson.length() + " символов");
            
            // Проверяем валидность JSON
            try {
                org.json.JSONObject testJson = new org.json.JSONObject(requestJson);
                org.json.JSONArray dataArray = testJson.getJSONArray("Данные");
                Log.d(TAG, "JSON валиден, содержит " + dataArray.length() + " элементов");
            } catch (Exception e) {
                Log.e(TAG, "ОШИБКА: JSON невалиден! " + e.getMessage());
            }
            
            // Логируем
            try {
                org.json.JSONObject jsonObj = new org.json.JSONObject(requestJson);
                String prettyJson = jsonObj.toString(2); // отступ в 2 пробела
                

                Log.d(TAG, "=== СОХРАНЕНИЕ ДАННЫХ ПЕРЕМЕЩЕНИЯ В 1С ===");
                Log.d(TAG, "MoveGuid: " + moveGuid);
                Log.d(TAG, "UserGuid: " + userGuid);
                Log.d(TAG, "Количество продуктов: " + products.size());
                Log.d(TAG, "URL: documentmove_save");
                Log.d(TAG, "Тело запроса (форматированный JSON):");
                Log.d(TAG, prettyJson);
            } catch (Exception e) {
                Log.d(TAG, "=== СОХРАНЕНИЕ ДАННЫХ ПЕРЕМЕЩЕНИЯ В 1С ===");
                Log.d(TAG, "MoveGuid: " + moveGuid);
                Log.d(TAG, "UserGuid: " + userGuid);
                Log.d(TAG, "Количество продуктов: " + products.size());
                Log.d(TAG, "URL: documentmove_save");
                Log.d(TAG, "Тело запроса (JSON):");
                Log.d(TAG, requestJson);
                Log.e(TAG, "Ошибка форматирования JSON для логов: " + e.getMessage());
            }
            
            Log.d(TAG, "Подготовка к отправке " + products.size() + " продуктов в 1С");

            RequestBody requestBody;
            try {

                byte[] jsonBytes = requestJson.getBytes("UTF-8");
                requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), 
                    jsonBytes
                );
                Log.d(TAG, "RequestBody создан из байтов UTF-8, размер: " + jsonBytes.length + " байт");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при создании RequestBody из байтов, используем строку: " + e.getMessage());

                requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), 
                    requestJson
                );
            }
            

            Log.d(TAG, "RequestBody создан, размер: " + requestBody.contentLength() + " байт");
            

            
            Call<Void> call = moveApiService.saveMoveData(requestBody);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Данные перемещения успешно сохранены в 1С для moveGuid: " + moveGuid);
                        callback.onSuccess(true);
                    } else {

                        String fullErrorText = "Ошибка сохранения данных в 1С. Код: " + response.code();
                        
                        try {
                            if (response.errorBody() != null) {
                                String errorBodyString = response.errorBody().string();
                                Log.d(TAG, "Тело ошибки от сервера: " + errorBodyString);
                                
                                // Парсим JSON ответ для извлечения ТекстОшибки
                                if (errorBodyString != null && !errorBodyString.trim().isEmpty()) {
                                    try {
                                        com.google.gson.JsonObject jsonObject = gson.fromJson(errorBodyString, com.google.gson.JsonObject.class);
                                        if (jsonObject != null && jsonObject.has("ТекстОшибки")) {
                                            String serverErrorText = jsonObject.get("ТекстОшибки").getAsString();
                                            if (serverErrorText != null && !serverErrorText.trim().isEmpty()) {
                                                fullErrorText = serverErrorText;
                                                Log.d(TAG, "Извлечен ТекстОшибки: " + fullErrorText);
                                            }
                                        }
                                    } catch (Exception parseException) {
                                        Log.w(TAG, "Не удалось распарсить JSON ошибки: " + parseException.getMessage());
                                        // Используем исходное сообщение об ошибке
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Ошибка при чтении тела ответа: " + e.getMessage());
                            // Используем исходное сообщение об ошибке
                        }
                        
                        Log.e(TAG, "Полная ошибка сохранения данных: " + fullErrorText);

                        callback.onError(new com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException(
                            fullErrorText, 
                            com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException.ErrorType.CHANGE_MOVE_STATUS
                        ));
                    }
                }
                
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    String errorMsg = "Ошибка сети при сохранении данных в 1С: " + t.getMessage();
                    Log.e(TAG, errorMsg, t);
                    callback.onError(new Exception(errorMsg, t));
                }
            });
            
        } catch (Exception e) {
            String errorMsg = "Ошибка при подготовке запроса сохранения данных: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            callback.onError(new Exception(errorMsg, e));
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