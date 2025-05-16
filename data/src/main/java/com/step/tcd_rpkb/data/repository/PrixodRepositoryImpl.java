package com.step.tcd_rpkb.data.repository;

// import com.step.tcd_rpkb.data.network.dto.InvoiceDto; // Больше не нужен прямой доступ к DTO здесь
import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
// import com.step.tcd_rpkb.network.DataProvider; // УДАЛЯЕМ ЗАВИСИМОСТЬ
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.repository.PrixodRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.repository.MoveRepository; // <-- ДОБАВЛЯЕМ ЗАВИСИМОСТЬ

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrixodRepositoryImpl implements PrixodRepository {

    // private final DataProvider dataProvider; // УДАЛЯЕМ
    private final MoveRepository moveRepository; // <-- ЗАМЕНЯЕМ НА MoveRepository
    private final InvoiceMapper invoiceMapper; // Оставляем, если все еще нужен для какой-то специфичной логики Prixod, но скорее всего нет, если MoveRepository уже возвращает Invoice

    @Inject
    // public PrixodRepositoryImpl(DataProvider dataProvider, InvoiceMapper invoiceMapper) { // СТАРАЯ СИГНАТУРА
    public PrixodRepositoryImpl(MoveRepository moveRepository, InvoiceMapper invoiceMapper) { // НОВАЯ СИГНАТУРА
        // this.dataProvider = dataProvider;
        this.moveRepository = moveRepository;
        this.invoiceMapper = invoiceMapper; // Если MoveRepository.getDocumentMove УЖЕ возвращает domain Invoice, то invoiceMapper здесь не нужен.
                                            // Пока оставим, но это кандидат на удаление.
    }

    @Override
    public void getPrixodDocument(String moveUuid, RepositoryCallback<Invoice> callback) {
        // Делегируем вызов MoveRepository.getDocumentMove
        // MoveRepository уже обрабатывает онлайн/оффлайн логику и маппинг.
        moveRepository.getDocumentMove(moveUuid, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                // Проверяем, нужна ли дополнительная обработка или маппинг специфичный для Prixod.
                // Если нет, просто передаем результат.
                if (invoice != null) {
                    callback.onSuccess(invoice);
                } else {
                    // Это условие маловероятно, если MoveRepository.getDocumentMove корректно обрабатывает null
                    callback.onError(new Exception("MoveRepository вернул null Invoice для UUID: " + moveUuid));
                }
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception); // Просто передаем ошибку
            }
        });
    }
} 