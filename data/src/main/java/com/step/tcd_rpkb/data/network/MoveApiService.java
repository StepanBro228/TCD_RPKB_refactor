package com.step.tcd_rpkb.data.network;

import com.step.tcd_rpkb.data.network.dto.InvoiceDto;
import com.step.tcd_rpkb.data.network.dto.MoveResponseDto;
import com.step.tcd_rpkb.data.network.dto.ChangeMoveStatusResponseDto;
import com.step.tcd_rpkb.data.network.dto.ProductSeriesDto;
import com.step.tcd_rpkb.data.network.dto.ProductSeriesResponseDto;
import com.step.tcd_rpkb.data.network.dto.SaveMoveDataRequestDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface MoveApiService {


    @GET("movelist")
    Call<MoveResponseDto> getMoveList(
        @Query("state") String state,
        @Query("startdate") String startDate,
        @Query("enddate") String endDate,
        @Query("userguid") String userGuid,
        @Query("usefilter") boolean useFilter,
        @Query("product") String nomenculature,
        @Query("series") String series

    );

    @GET("movelist") // Перегруженный метод без параметров дат/состояния, если API поддерживает
    Call<MoveResponseDto> getMoveList(
    );

    @GET("documentmove")
    Call<InvoiceDto> getDocumentMove(
        @Query("guid") String guid
    );

    @GET("documentmove")
    Call<ChangeMoveStatusResponseDto> changeMoveStatus(
        @Query("guid") String guid,
        @Query("state") String state,
        @Query("userguid") String userGuid
    );

    @GET("ProductSeries")
    Call<ProductSeriesResponseDto> getProductSeries(
        @Query("guid") String moveGuid,
        @Query("lineguid") String lineGuid
    );
    
    /**
     * Сохраняет данные перемещения в 1С перед сменой статуса
     */
    @POST("documentmove_save")
    Call<Void> saveMoveData(
        @Body okhttp3.RequestBody requestBody
    );
} 