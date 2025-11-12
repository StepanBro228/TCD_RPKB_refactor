package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import com.step.tcd_rpkb.domain.model.Product;

import java.util.List;


public interface MoveRepository {

    /**
     * Получает список перемещений.
     * @param state Статус перемещения (может быть null или несколько статусов через разделитель).
     * @param startDate Начальная дата в формате YYYYMMDD (может быть null).
     * @param endDate Конечная дата в формате YYYYMMDD (может быть null).
     * @param userGuid GUID пользователя для фильтрации доступности.
     * @param useFilter Использовать ли фильтр по доступности (по умолчанию true).
     * @return MoveResponse содержащий список перемещений и метаданные.
     * @throws Exception Если происходит ошибка при загрузке данных.
     */
    void getMoveList(String state, String startDate, String endDate, String userGuid, boolean useFilter, String nomenculature, String series, RepositoryCallback<MoveResponse> callback);

    /**
     * Получает детализированную информацию о документе перемещения.
     * @param guid GUID документа перемещения.
     * @return Invoice с деталями документа.
     * @throws Exception Если происходит ошибка при загрузке данных.
     */
    void getDocumentMove(String guid, RepositoryCallback<Invoice> callback);


    /**
     * Сменяет статус перемещения.
     * @param guid GUID перемещения.
     * @param targetState Целевой статус.
     * @param userGuid GUID пользователя, выполняющего смену статуса.
     * @param callback Callback для получения результата.
     */
    void changeMoveStatus(String guid, String targetState, String userGuid, RepositoryCallback<ChangeMoveStatusResult> callback);

    /**
     * Сохраняет данные перемещения в 1С перед сменой статуса.
     * @param moveGuid GUID перемещения.
     * @param userGuid GUID пользователя.
     * @param products Список продуктов для сохранения.
     * @param callback Callback для получения результата.
     */
    void saveMoveData(String moveGuid, String userGuid, List<Product> products, RepositoryCallback<Boolean> callback);
} 