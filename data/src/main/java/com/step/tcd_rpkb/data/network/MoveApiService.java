package com.step.tcd_rpkb.data.network;

import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MoveApiService {

    // Базовый URL будет указан при создании экземпляра Retrofit
    // Эндпоинты из NetworkUtils:
    // private static final String MOVE_LIST_ENDPOINT = "movelist";
    // private static final String DOCUMENT_MOVE_ENDPOINT = "documentmove";

    @GET("movelist")
    Call<MoveResponseDto> getMoveList(
        @Query("state") String state,
        @Query("startdate") String startDate,
        @Query("enddate") String endDate
    );

    @GET("movelist") // Перегруженный метод без параметров дат/состояния, если API поддерживает
    Call<MoveResponseDto> getMoveList(
    );

    @GET("documentmove")
    Call<InvoiceDto> getDocumentMove(
        @Query("guid") String guid
    );
} 