package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveResponse;
// import java.util.List; // Если бы getMoveList возвращал List<MoveItem>

// TODO: Адаптировать возвращаемые типы для асинхронной работы (например, Flow, Single, или использовать suspend функции с Kotlin)

// Используем тот же DataSourceCallback или определим свой для Repository уровня,
// если нужна другая гранулярность ошибок или данных.
// Для простоты пока можно использовать общий.
// import com.step.tcd_rpkb.data.datasources.DataSourceCallback; 
// Если DataSourceCallback останется в data, то domain не должен о нем знать.
// Правильнее определить RepositoryCallback в domain.

public interface MoveRepository {

    /**
     * Получает список перемещений.
     * @param state Статус перемещения (может быть null или несколько статусов через разделитель).
     * @param startDate Начальная дата в формате YYYYMMDD (может быть null).
     * @param endDate Конечная дата в формате YYYYMMDD (может быть null).
     * @return MoveResponse содержащий список перемещений и метаданные.
     * @throws Exception Если происходит ошибка при загрузке данных.
     */
    void getMoveList(String state, String startDate, String endDate, RepositoryCallback<MoveResponse> callback);

    /**
     * Получает детализированную информацию о документе перемещения.
     * @param guid GUID документа перемещения.
     * @return Invoice с деталями документа.
     * @throws Exception Если происходит ошибка при загрузке данных.
     */
    void getDocumentMove(String guid, RepositoryCallback<Invoice> callback);

} 