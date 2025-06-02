package com.step.tcd_rpkb.UI.movelist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.model.Invoice; // Импорт для DataProvider

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@HiltViewModel
public class MoveListViewModel extends ViewModel {

    // Ключи для SavedStateHandle
    private static final String KEY_FORMIROVAN_SENDER = "formirovanSenderFilter";
    private static final String KEY_FORMIROVAN_MOVEMENT_NUMBER = "formirovanMovementNumberFilter";
    private static final String KEY_FORMIROVAN_RECIPIENT = "formirovanRecipientFilter";
    private static final String KEY_FORMIROVAN_ASSEMBLER = "formirovanAssemblerFilter";
    private static final String KEY_FORMIROVAN_PRIORITY = "formirovanPriorityFilter";
    private static final String KEY_FORMIROVAN_RECEIVER = "formirovanReceiverFilter";
    private static final String KEY_FORMIROVAN_CPS_CHECKED = "formirovanCpsChecked";
    private static final String KEY_FORMIROVAN_AVAILABILITY_CHECKED = "formirovanAvailabilityChecked";

    private static final String KEY_KOMPLEKTUETSA_SENDER = "komplektuetsaSenderFilter";
    private static final String KEY_KOMPLEKTUETSA_MOVEMENT_NUMBER = "komplektuetsaMovementNumberFilter";
    private static final String KEY_KOMPLEKTUETSA_RECIPIENT = "komplektuetsaRecipientFilter";
    private static final String KEY_KOMPLEKTUETSA_ASSEMBLER = "komplektuetsaAssemblerFilter";
    private static final String KEY_KOMPLEKTUETSA_PRIORITY = "komplektuetsaPriorityFilter";
    private static final String KEY_KOMPLEKTUETSA_RECEIVER = "komplektuetsaReceiverFilter";
    private static final String KEY_KOMPLEKTUETSA_CPS_CHECKED = "komplektuetsaCpsChecked";
    private static final String KEY_KOMPLEKTUETSA_AVAILABILITY_CHECKED = "komplektuetsaAvailabilityChecked";

    private static final String KEY_CURRENT_TAB_POSITION = "currentTabPosition";

    private final SavedStateHandle savedStateHandle;

    // Константы статусов (из MoveList_menu)
    public static final String STATUS_FORMIROVAN = "Сформирован";
    public static final String STATUS_KOMPLEKTUETSA = "Комплектуется";

    // Константы для приоритетов (из MoveList_menu)
    public static final String PRIORITY_URGENT = "Неотложный";
    public static final String PRIORITY_HIGH = "Высокий";
    public static final String PRIORITY_MEDIUM = "Средний";
    public static final String PRIORITY_LOW = "Низкий";
    // private static final String PRIORITY_ALL = "Все"; // Понадобится для фильтров

    // LiveData для оригинальных (нефильтрованных) списков
    private final MutableLiveData<List<MoveItem>> _originalFormirovanList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> originalFormirovanList = _originalFormirovanList;

    private final MutableLiveData<List<MoveItem>> _originalKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> originalKomplektuetsaList = _originalKomplektuetsaList;

    // LiveData для отфильтрованных списков (пока просто дублируют оригинальные, фильтрация будет позже)

    private final MutableLiveData<List<MoveItem>> _filteredFormirovanList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> filteredFormirovanList = _filteredFormirovanList;

    private final MutableLiveData<List<MoveItem>> _filteredKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> filteredKomplektuetsaList = _filteredKomplektuetsaList;

    // LiveData для состояния загрузки
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    // LiveData для сообщений об ошибках
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    // --- LiveData для состояния фильтров ---

    // Текущая активная вкладка (0 - Сформирован, 1 - Комплектуется)
    public LiveData<Integer> currentTabPosition;

    // Фильтры для вкладки "Сформирован"
    public LiveData<String> formirovanSenderFilter;
    public LiveData<String> formirovanMovementNumberFilter;
    public LiveData<String> formirovanRecipientFilter;
    public LiveData<String> formirovanAssemblerFilter;
    public LiveData<String> formirovanPriorityFilter;
    public LiveData<String> formirovanReceiverFilter;
    public LiveData<Boolean> formirovanCpsChecked;
    public LiveData<Boolean> formirovanAvailabilityChecked;


    // Фильтры для вкладки "Комплектуется"
    public LiveData<String> komplektuetsaSenderFilter;
    public LiveData<String> komplektuetsaMovementNumberFilter;
    public LiveData<String> komplektuetsaRecipientFilter;
    public LiveData<String> komplektuetsaAssemblerFilter;
    public LiveData<String> komplektuetsaPriorityFilter;
    public LiveData<String> komplektuetsaReceiverFilter;
    public LiveData<Boolean> komplektuetsaCpsChecked;
    public LiveData<Boolean> komplektuetsaAvailabilityChecked;

    // --- Вспомогательный класс и LiveData для состояния отмены ---
    private static class MovedItemDataForUndo {
        final MoveItem originalItemState; // Состояние ДО перемещения
        final int originalIndex;

        MovedItemDataForUndo(MoveItem item, int index) {
            // Создаем копию, чтобы сохранить оригинальный статус
            this.originalItemState = new MoveItem(item.getMovementId(), item.getMovementDisplayText(), item.isCps(), item.getDate(), item.getNumber(), item.getComment(), item.getProductName(), item.getResponsiblePersonName(), item.getColor(), item.getPriority(), item.getAssemblerName(), item.getSigningStatus(), item.getSourceWarehouseName(), item.getDestinationWarehouseName(), item.getItemsCount(), item.getPositionsCount());
            this.originalIndex = index;
        }
    }

    private final MutableLiveData<List<MovedItemDataForUndo>> _undoStack = new MutableLiveData<>();
    // _lastMovedItems больше не нужен

    private final MutableLiveData<String> _lastMoveSourceState = new MutableLiveData<>();
    private final MutableLiveData<String> _lastMoveTargetState = new MutableLiveData<>();

    // Для сообщений UI, таких как Snackbar. Предполагается наличие класса SingleLiveEvent.
    // Если его нет, можно использовать MutableLiveData и обрабатывать события соответствующим образом в Activity/Fragment.
    // private final MutableLiveData<String> _moveOperationMessage = new MutableLiveData<>(); // ЗАКОММЕНТИРОВАНО
    // public LiveData<String> moveOperationMessage = _moveOperationMessage; // ЗАКОММЕНТИРОВАНО

    // LiveData для управления видимостью кнопки обновления
    private final MutableLiveData<Boolean> _showRefreshButton = new MutableLiveData<>(false);
    public LiveData<Boolean> showRefreshButton = _showRefreshButton;

    // Счетчик для активных фоновых загрузок данных
    private int activeDataChecks = 0;
    private final Object dataCheckLock = new Object(); // Для синхронизации доступа к activeDataChecks

    // Временные хранилища для фоново загруженных данных
    private List<MoveItem> freshFormirovanData = null;
    private List<MoveItem> freshKomplektuetsaData = null;
    private boolean hasFreshFormirovanData = false;
    private boolean hasFreshKomplektuetsaData = false;

    // Поля для хранения данных, полученных из PrixodActivity
    private String productsJsonDataFromPrixod = null;
    private boolean preserveEditedDataFromPrixod = false;

    // LiveData для UI событий
    private final MutableLiveData<SingleEvent<String>> _navigateToPrixodEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> navigateToPrixodEvent = _navigateToPrixodEvent;

    private final MutableLiveData<SingleEvent<String>> _showToastEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> showToastEvent = _showToastEvent;
    
    // LiveData для индикатора загрузки при проверке документа (отличается от _isLoading для списков)
    private final MutableLiveData<Boolean> _isProcessingItemClick = new MutableLiveData<>(false);
    public LiveData<Boolean> isProcessingItemClick = _isProcessingItemClick;

    // LiveData для индикации активных фильтров
    private final MutableLiveData<Boolean> _isAnyFilterActiveLive = new MutableLiveData<>(false);
    public LiveData<Boolean> isAnyFilterActiveLive = _isAnyFilterActiveLive;

    // Новый LiveData для управления Snackbar с отменой
    private final MutableLiveData<SingleEvent<SnackbarEvent>> _showUndoSnackbarEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<SnackbarEvent>> showUndoSnackbarEvent = _showUndoSnackbarEvent;

    // Класс-обертка для данных Snackbar
    public static class SnackbarEvent {
        public final String message;
        public final boolean showUndoAction;

        public SnackbarEvent(String message, boolean showUndoAction) {
            this.message = message;
            this.showUndoAction = showUndoAction;
        }
    }

    private final MoveRepository moveRepository;
    private final ExecutorService executorService;

    @Inject
    public MoveListViewModel(SavedStateHandle savedStateHandle, MoveRepository moveRepository) {
        this.savedStateHandle = savedStateHandle;
        this.moveRepository = moveRepository;
        this.executorService = Executors.newSingleThreadExecutor(); // Создаем пул из одного потока

        // Инициализация LiveData фильтров из SavedStateHandle
        currentTabPosition = savedStateHandle.getLiveData(KEY_CURRENT_TAB_POSITION, 0);

        formirovanSenderFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_SENDER, "");
        formirovanMovementNumberFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_MOVEMENT_NUMBER, "");
        formirovanRecipientFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_RECIPIENT, "");
        formirovanAssemblerFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_ASSEMBLER, "");
        formirovanPriorityFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_PRIORITY, "");
        formirovanReceiverFilter = savedStateHandle.getLiveData(KEY_FORMIROVAN_RECEIVER, "");
        formirovanCpsChecked = savedStateHandle.getLiveData(KEY_FORMIROVAN_CPS_CHECKED, true);
        formirovanAvailabilityChecked = savedStateHandle.getLiveData(KEY_FORMIROVAN_AVAILABILITY_CHECKED, true);

        komplektuetsaSenderFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_SENDER, "");
        komplektuetsaMovementNumberFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_MOVEMENT_NUMBER, "");
        komplektuetsaRecipientFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_RECIPIENT, "");
        komplektuetsaAssemblerFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_ASSEMBLER, "");
        komplektuetsaPriorityFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_PRIORITY, "");
        komplektuetsaReceiverFilter = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_RECEIVER, "");
        komplektuetsaCpsChecked = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_CPS_CHECKED, true);
        komplektuetsaAvailabilityChecked = savedStateHandle.getLiveData(KEY_KOMPLEKTUETSA_AVAILABILITY_CHECKED, true);


    }

    /**
     * Загружает данные о перемещениях.
     * Аналог loadMoveDataFromJson из MoveList_menu.
     */
    public void loadMoveData() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Используем значения по умолчанию для дат, если они не указаны (репозиторий сам обработает null)
        String startDate = null; // DataProvider.formatDateForRequest(DataProvider.getTwoMonthsAgo());
        String endDate = null;   // DataProvider.formatDateForRequest(DataProvider.getCurrentDate());
        String status = STATUS_FORMIROVAN + "|" + STATUS_KOMPLEKTUETSA;

        // DataProvider.getInstance().getMoveList(status, startDate, endDate, new DataProvider.DataProviderListener<MoveResponse>() {
        moveRepository.getMoveList(status, startDate, endDate, new RepositoryCallback<MoveResponse>() {
            @Override
            // public void onDataLoaded(MoveResponse response) { // Старый метод из DataProviderListener
            public void onSuccess(MoveResponse response) { // Новый метод из RepositoryCallback
                if (response != null && response.getItems() != null) {
                    List<MoveItem> formirovanList = new ArrayList<>();
                    List<MoveItem> komplektuetsaList = new ArrayList<>();

                    for (MoveItem item : response.getItems()) {
                        if (STATUS_FORMIROVAN.equals(item.getSigningStatus())) {
                            formirovanList.add(item);
                        } else if (STATUS_KOMPLEKTUETSA.equals(item.getSigningStatus())) {
                            komplektuetsaList.add(item);
                        }
                    }

                    sortByPriority(formirovanList);
                    sortByPriority(komplektuetsaList);

                    _originalFormirovanList.setValue(formirovanList);
                    _originalKomplektuetsaList.setValue(komplektuetsaList);


                    setKomplektuetsaAvailabilityChecked(true);
                    setFormirovanAvailabilityChecked(true);
                    setKomplektuetsaCpsChecked(true);
                    setFormirovanCpsChecked(true);


                    _isLoading.postValue(false);

                } else {
                        _errorMessage.setValue("Ответ сервера пуст или не содержит элементов.");

                }

            }

            @Override
            public void onError(Exception exception) { // Новый метод
                    _errorMessage.setValue("Ошибка загрузки списка перемещений: " + exception.getMessage());
                    _isLoading.setValue(false); // Восстанавливаем
            }
        });

    }

    /**
     * Сортирует список MoveItem по приоритету.
     * Скопировано из MoveList_menu.
     */
    private void sortByPriority(List<MoveItem> moveItems) {
        if (moveItems == null) return;
        Collections.sort(moveItems, new Comparator<MoveItem>() {
            @Override
            public int compare(MoveItem item1, MoveItem item2) {
                String priority1 = item1.getPriority();
                String priority2 = item2.getPriority();

                if (priority1 == null && priority2 == null ||
                    (priority1 != null && priority2 != null && priority1.equals(priority2))) {
                    return 0;
                }
                if (priority1 == null || priority1.isEmpty()) return 1;
                if (priority2 == null || priority2.isEmpty()) return -1;

                return getPriorityOrder(priority1) - getPriorityOrder(priority2);
            }

            private int getPriorityOrder(String priority) {
                if (PRIORITY_URGENT.equals(priority)) return 1;
                if (PRIORITY_HIGH.equals(priority)) return 2;
                if (PRIORITY_MEDIUM.equals(priority)) return 3;
                if (PRIORITY_LOW.equals(priority)) return 4;
                return 5;
            }
        });
    }

    // --- Методы для обновления состояния фильтров ---

    public void setCurrentTabPosition(int position) {
        if (currentTabPosition.getValue() == null || !currentTabPosition.getValue().equals(position)) {
            savedStateHandle.set(KEY_CURRENT_TAB_POSITION, position);
            triggerFilterRecalculation(); // Пересчитываем фильтры при смене вкладки
        }
    }

    // --- Сеттеры для фильтров вкладки "Сформирован" (обновляют и LiveData, и SavedStateHandle) ---
    public void setFormirovanSenderFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_SENDER, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanMovementNumberFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_MOVEMENT_NUMBER, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanRecipientFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_RECIPIENT, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanAssemblerFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_ASSEMBLER, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanPriorityFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_PRIORITY, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanReceiverFilter(String filter) { 
        savedStateHandle.set(KEY_FORMIROVAN_RECEIVER, filter);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanCpsChecked(boolean checked) { 
        savedStateHandle.set(KEY_FORMIROVAN_CPS_CHECKED, checked);
        triggerFilterRecalculation(); 
    }
    public void setFormirovanAvailabilityChecked(boolean checked) { 
        savedStateHandle.set(KEY_FORMIROVAN_AVAILABILITY_CHECKED, checked);
        triggerFilterRecalculation(); 
    }

    // --- Сеттеры для фильтров вкладки "Комплектуется" (обновляют и LiveData, и SavedStateHandle) ---
    public void setKomplektuetsaSenderFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_SENDER, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaMovementNumberFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_MOVEMENT_NUMBER, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaRecipientFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_RECIPIENT, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaAssemblerFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_ASSEMBLER, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaPriorityFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_PRIORITY, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaReceiverFilter(String filter) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_RECEIVER, filter);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaCpsChecked(boolean checked) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_CPS_CHECKED, checked);
        triggerFilterRecalculation(); 
    }
    public void setKomplektuetsaAvailabilityChecked(boolean checked) { 
        savedStateHandle.set(KEY_KOMPLEKTUETSA_AVAILABILITY_CHECKED, checked);
        triggerFilterRecalculation(); 
    }

    /**
     * Этот метод будет вызываться после каждого изменения фильтра или активной вкладки,
     * чтобы пересчитать _filteredFormirovanList и _filteredKomplektuetsaList.
     */
    private void triggerFilterRecalculation() {
        executorService.execute(() -> {
            // Логика фильтрации остается прежней, но выполняется в фоновом потоке

            List<MoveItem> currentFormirovanList = _originalFormirovanList.getValue();


            List<MoveItem> currentfilteredFormirovan = applyFiltersToList(
                    currentFormirovanList != null ? new ArrayList<>(currentFormirovanList): new ArrayList<>(), // Передаем копию
                    formirovanSenderFilter.getValue(),
                    formirovanMovementNumberFilter.getValue(),
                    formirovanRecipientFilter.getValue(),
                    formirovanAssemblerFilter.getValue(),
                    formirovanPriorityFilter.getValue(),
                    formirovanReceiverFilter.getValue(),
                    formirovanCpsChecked.getValue(),
                    formirovanAvailabilityChecked.getValue()
            );
            _filteredFormirovanList.postValue(currentfilteredFormirovan);

            List<MoveItem> currentKomplektuetsaList = _originalKomplektuetsaList.getValue();

            List<MoveItem> currentfilteredKomplektuetsa = applyFiltersToList(
                    currentKomplektuetsaList != null ? new ArrayList<>(currentKomplektuetsaList): new ArrayList<>(),
                    komplektuetsaSenderFilter.getValue(),
                    komplektuetsaMovementNumberFilter.getValue(),
                    komplektuetsaRecipientFilter.getValue(),
                    komplektuetsaAssemblerFilter.getValue(),
                    komplektuetsaPriorityFilter.getValue(),
                    komplektuetsaReceiverFilter.getValue(),
                    komplektuetsaCpsChecked.getValue(),
                    komplektuetsaAvailabilityChecked.getValue()
            );
            _filteredKomplektuetsaList.postValue(currentfilteredKomplektuetsa);


            updateIsAnyFilterActive();
        });
    }

    private void updateIsAnyFilterActive() {
        int currentTab = currentTabPosition.getValue() != null ? currentTabPosition.getValue() : 0;
        boolean isActive = false;

        if (currentTab == 0) { // Сформирован
            isActive = isFilterApplied(formirovanSenderFilter.getValue()) ||
                       isFilterApplied(formirovanMovementNumberFilter.getValue()) ||
                       isFilterApplied(formirovanRecipientFilter.getValue()) ||
                       isFilterApplied(formirovanAssemblerFilter.getValue()) ||
                       isFilterPriorityApplied(formirovanPriorityFilter.getValue()) ||
                       isFilterApplied(formirovanReceiverFilter.getValue()) ||
                       formirovanCpsChecked.getValue() == false  ||
                       formirovanAvailabilityChecked.getValue() == false;
        } else { // Комплектуется
            isActive = isFilterApplied(komplektuetsaSenderFilter.getValue()) ||
                       isFilterApplied(komplektuetsaMovementNumberFilter.getValue()) ||
                       isFilterApplied(komplektuetsaRecipientFilter.getValue()) ||
                       isFilterApplied(komplektuetsaAssemblerFilter.getValue()) ||
                       isFilterPriorityApplied(komplektuetsaPriorityFilter.getValue()) ||
                       isFilterApplied(komplektuetsaReceiverFilter.getValue()) ||
                       komplektuetsaCpsChecked.getValue() == false  ||
                       komplektuetsaAvailabilityChecked.getValue() == false ;
        }
        _isAnyFilterActiveLive.postValue(isActive); // Используем postValue для безопасности потоков
    }

    // Вспомогательный метод для проверки, применен ли текстовый фильтр
    private boolean isFilterApplied(String filterValue) {
        return filterValue != null && !filterValue.trim().isEmpty();
    }

    // Вспомогательный метод для проверки, применен ли фильтр приоритета (не "Все")
    private boolean isFilterPriorityApplied(String priorityValue) {
        return priorityValue != null && !priorityValue.trim().isEmpty() && !priorityValue.equals("Все"); // Предполагаем, что "Все" - значение по умолчанию
    }

    private List<MoveItem> applyFiltersToList(List<MoveItem> originalList,
                                              String senderFilter,
                                              String movementNumberFilter,
                                              String recipientFilter,
                                              String assemblerFilter,
                                              String priorityFilter,
                                              String receiverFilter,
                                              Boolean cpsFilterEnabled,
                                              Boolean availabilityFilterEnabled) {
        List<MoveItem> filteredList = new ArrayList<>();
        if (originalList == null) return filteredList;

        for (MoveItem item : originalList) {
            boolean matches = true;

            // Фильтр по ЦПС (если включен и у элемента нет ЦПС - не добавляем)
            if (Boolean.TRUE.equals(cpsFilterEnabled) && !item.isCps()) {
                matches = false;
            }

            // Фильтр по отправителю (Склад-отправитель)
            if (matches && senderFilter != null && !senderFilter.isEmpty()) {
                if (item.getSourceWarehouseName() == null || !item.getSourceWarehouseName().toLowerCase().contains(senderFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Фильтр по номеру перемещения
            if (matches && movementNumberFilter != null && !movementNumberFilter.isEmpty()) {
                if (item.getNumber() == null || !item.getNumber().toLowerCase().contains(movementNumberFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Фильтр по получателю (Склад-получатель)
            if (matches && recipientFilter != null && !recipientFilter.isEmpty()) {
                if (item.getDestinationWarehouseName() == null || !item.getDestinationWarehouseName().toLowerCase().contains(recipientFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Фильтр по комплектовщику
            if (matches && assemblerFilter != null && !assemblerFilter.isEmpty()) {
                if (item.getAssemblerName() == null || !item.getAssemblerName().toLowerCase().contains(assemblerFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Фильтр по ответственному
            if (matches && receiverFilter != null && !receiverFilter.isEmpty()) {
                if (item.getResponsiblePersonName() == null || !item.getResponsiblePersonName().toLowerCase().contains(receiverFilter.toLowerCase())) {
                    matches = false;
                }
            }

            // Фильтр по приоритету
            if (matches && priorityFilter != null && !priorityFilter.isEmpty()) {
                String itemPriority = item.getPriority();
                if (itemPriority == null || itemPriority.isEmpty()) { // Элементы без приоритета
                    if (PRIORITY_LOW.equals(priorityFilter)) {
                        // Если выбран фильтр "Низкий", то элементы без приоритета тоже подходят
                    } else {
                        matches = false; // Иначе не подходят
                    }
                } else if (!itemPriority.equals(priorityFilter)) {
                    matches = false;
                }
            }

            // Фильтр "Наличие товара" (availabilityFilterEnabled)
            // Если availabilityFilterEnabled == true (галочка стоит в UI), то показываем ВСЕ (фильтр не применяется).
            // Если availabilityFilterEnabled == false (галочка снята в UI), то показываем только те, где itemsCount > 0.
            if (matches && Boolean.FALSE.equals(availabilityFilterEnabled)) { // Галочка снята
                if (item.getItemsCount() <= 0) { // Если товаров нет или не указано
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }

    // Методы для обновления оригинальных списков (если потребуется извне, например, при ручном обновлении)
    public void updateOriginalFormirovanList(List<MoveItem> newList) {
        _originalFormirovanList.postValue(newList);
        triggerFilterRecalculation(); // Пересчитываем фильтры при обновлении оригинального списка
    }

    public void updateOriginalKomplektuetsaList(List<MoveItem> newList) {
        _originalKomplektuetsaList.postValue(newList);
        triggerFilterRecalculation(); // Пересчитываем фильтры
    }

    // --- Логика перемещения элементов ---

    /**
     * Перемещает выбранные элементы между состояниями "Сформирован" и "Комплектуется".
     * @param itemsToMove Список элементов для перемещения.
     * @param currentFragmentState Текущее состояние/вкладка элементов (STATUS_FORMIROVAN или STATUS_KOMPLEKTUETSA).
     * @param targetState Целевое состояние/вкладка для элементов.
     */
    public void moveItems(List<MoveItem> itemsToMove, String currentFragmentState, String targetState) {
        if (itemsToMove == null || itemsToMove.isEmpty()) {
            _showToastEvent.setValue(new SingleEvent<>("Нет элементов для перемещения."));
            return;
        }

        List<MoveItem> currentOriginalFormirovanList = new ArrayList<>(_originalFormirovanList.getValue() != null ? _originalFormirovanList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalKomplektuetsaList = new ArrayList<>(_originalKomplektuetsaList.getValue() != null ? _originalKomplektuetsaList.getValue() : Collections.emptyList());

        List<MovedItemDataForUndo> movedItemsForUndo = new ArrayList<>();
        List<MoveItem> itemsActuallyMoved = new ArrayList<>(); // Элементы, которые будут добавлены в целевой список

        List<MoveItem> sourceListReference;
        List<MoveItem> destinationListReference;
        MutableLiveData<List<MoveItem>> sourceLiveData;
        MutableLiveData<List<MoveItem>> destinationLiveData;

        if (STATUS_FORMIROVAN.equals(currentFragmentState)) {
            sourceListReference = currentOriginalFormirovanList;
            destinationListReference = currentOriginalKomplektuetsaList;
            sourceLiveData = _originalFormirovanList;
            destinationLiveData = _originalKomplektuetsaList;
        } else {
            sourceListReference = currentOriginalKomplektuetsaList;
            destinationListReference = currentOriginalFormirovanList;
            sourceLiveData = _originalKomplektuetsaList;
            destinationLiveData = _originalFormirovanList;
        }

        // Создаем список элементов для удаления с их индексами, чтобы правильно их удалить
        // Сначала собираем все элементы, которые нужно переместить, с их ОРИГИНАЛЬНЫМИ индексами из ИСХОДНОГО списка
        List<android.util.Pair<Integer, MoveItem>> itemsToRemoveWithIndexes = new ArrayList<>();
        for (MoveItem itemToMove : itemsToMove) {
            for (int i = 0; i < sourceListReference.size(); i++) {
                if (sourceListReference.get(i).getMovementId().equals(itemToMove.getMovementId())) {
                    itemsToRemoveWithIndexes.add(new android.util.Pair<>(i, sourceListReference.get(i)));
                    break;
                }
            }
        }

        // Сортируем по индексам в ОБРАТНОМ порядке для корректного удаления
        // без смещения индексов для последующих удаляемых элементов.
        itemsToRemoveWithIndexes.sort((p1, p2) -> Integer.compare(p2.first, p1.first));

        for (android.util.Pair<Integer, MoveItem> pair : itemsToRemoveWithIndexes) {
            int originalIndex = pair.first;
            MoveItem itemFromSourceList = pair.second; // Это элемент из оригинального списка

            // Сохраняем для отмены (копию оригинального элемента и его индекс)
            movedItemsForUndo.add(new MovedItemDataForUndo(itemFromSourceList, originalIndex));
            
            sourceListReference.remove(originalIndex); // Удаляем из исходного списка по ранее найденному индексу

            // Модифицируем статус элемента, который был взят из sourceListReference
            itemFromSourceList.setSigningStatus(targetState);
            itemsActuallyMoved.add(itemFromSourceList); // Добавляем измененный элемент для последующего добавления в целевой список
        }
        
        // Добавляем все перемещенные (и измененные) элементы в целевой список
        destinationListReference.addAll(itemsActuallyMoved);
        // Сортируем только целевой список, так как исходный список изменился только удалениями
        sortByPriority(destinationListReference);

        // Обновляем LiveData
        sourceLiveData.setValue(new ArrayList<>(sourceListReference)); 
        destinationLiveData.setValue(new ArrayList<>(destinationListReference));

        _undoStack.setValue(movedItemsForUndo); // Сохраняем данные для отмены
        _lastMoveSourceState.setValue(currentFragmentState); // Откуда переместили
        _lastMoveTargetState.setValue(targetState);     // Куда переместили

        triggerFilterRecalculation();
        String message = "Перемещено " + itemsToMove.size() + " элементов в " + targetState;
        _showUndoSnackbarEvent.setValue(new SingleEvent<>(new SnackbarEvent(message, true)));
    }

    /**
     * Отменяет последнее действие перемещения элементов.
     */
    public void undoMove() {
        List<MovedItemDataForUndo> itemsToRestoreData = _undoStack.getValue();
        String originalSourceState = _lastMoveSourceState.getValue(); // Куда восстанавливаем (был источником)
        String originalTargetState = _lastMoveTargetState.getValue(); // Откуда удаляем (был целью)

        // Log.d("DEBUG_UPDATE", "[ViewModel.undoMove] START. Items to restore: " + (itemsToRestoreData != null ? itemsToRestoreData.size() : "null") + ", To Source: " + originalSourceState + ", From Target: " + originalTargetState);

        if (itemsToRestoreData == null || itemsToRestoreData.isEmpty() || originalSourceState == null || originalTargetState == null) {
            _showToastEvent.setValue(new SingleEvent<>("Нет операции для отмены."));
            return;
        }

        List<MoveItem> currentOriginalFormirovanList = new ArrayList<>(_originalFormirovanList.getValue() != null ? _originalFormirovanList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalKomplektuetsaList = new ArrayList<>(_originalKomplektuetsaList.getValue() != null ? _originalKomplektuetsaList.getValue() : Collections.emptyList());

        List<MoveItem> listToRestoreTo;  // Список, КУДА будем восстанавливать элементы
        List<MoveItem> listToRemoveFrom; // Список, ОТКУДА будем удалять элементы
        MutableLiveData<List<MoveItem>> liveDataToRestoreTo;
        MutableLiveData<List<MoveItem>> liveDataToRemoveFrom;

        if (STATUS_FORMIROVAN.equals(originalSourceState)) { // Восстанавливаем в "Сформирован"
            listToRestoreTo = currentOriginalFormirovanList;
            listToRemoveFrom = currentOriginalKomplektuetsaList;
            liveDataToRestoreTo = _originalFormirovanList;
            liveDataToRemoveFrom = _originalKomplektuetsaList;
        } else { // Восстанавливаем в "Комплектуется"
            listToRestoreTo = currentOriginalKomplektuetsaList;
            listToRemoveFrom = currentOriginalFormirovanList;
            liveDataToRestoreTo = _originalKomplektuetsaList;
            liveDataToRemoveFrom = _originalFormirovanList;
        }

        // Сначала удаляем элементы из списка, куда они были перемещены (originalTargetState)
        // Используем ID из originalItemState для поиска в listToRemoveFrom
        for (MovedItemDataForUndo data : itemsToRestoreData) {
            listToRemoveFrom.removeIf(itemInTargetList -> 
                itemInTargetList.getMovementId().equals(data.originalItemState.getMovementId()));
        }

        // Теперь восстанавливаем элементы в исходный список на их оригинальные позиции.
        // Сортируем itemsToRestoreData по originalIndex в обычном порядке, чтобы вставлять последовательно.
        itemsToRestoreData.sort(Comparator.comparingInt(data -> data.originalIndex));

        for (MovedItemDataForUndo data : itemsToRestoreData) {
            // data.originalItemState уже имеет правильный (старый) статус, так как мы создали его копию.
            // Убедимся, что статус явно установлен на тот, который был в originalSourceState (на всякий случай, хотя копия должна быть верной)
            data.originalItemState.setSigningStatus(originalSourceState);

            if (data.originalIndex >= 0 && data.originalIndex <= listToRestoreTo.size()) {
                listToRestoreTo.add(data.originalIndex, data.originalItemState);
            } else {
                // Если индекс некорректен (маловероятно, но для безопасности), добавляем в конец
                listToRestoreTo.add(data.originalItemState);
            }
        }

        // После восстановления на точные места, НЕ НУЖНО вызывать sortByPriority для listToRestoreTo,
        // так как это нарушит восстановленный порядок.

        liveDataToRestoreTo.setValue(new ArrayList<>(listToRestoreTo));
        liveDataToRemoveFrom.setValue(new ArrayList<>(listToRemoveFrom));

        _undoStack.setValue(null); // Очищаем стек отмены
        _lastMoveSourceState.setValue(null);
        _lastMoveTargetState.setValue(null);

        triggerFilterRecalculation();
        _showToastEvent.setValue(new SingleEvent<>("Перемещение отменено. Восстановлено " + itemsToRestoreData.size() + " элементов."));
    }

    /**
     * Очищает сообщение об операции, чтобы оно не показывалось повторно (например, при повороте экрана).
     * Вызывать из Activity/Fragment после того, как сообщение было показано.
     */
    // public void clearMoveOperationMessage() { // МЕТОД УДАЛЕН
    //     // _moveOperationMessage.postValue(null);
    // }

    // --- Логика проверки обновлений данных и управления кнопкой Refresh ---

    public void checkForDataUpdates() {
        synchronized (dataCheckLock) {
            if (activeDataChecks > 0) {
                // Уже идет проверка, новую не начинаем
                return;
            }
            activeDataChecks = 2; // Ожидаем два колбека (для Formirovan и Komplektuetsa)
        }
        _isLoading.postValue(true); // Используем общий индикатор загрузки

        // Загрузка данных для "Сформирован"
        moveRepository.getMoveList(STATUS_FORMIROVAN, null, null, new RepositoryCallback<MoveResponse>() {
            @Override
            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> loadedItems = new ArrayList<>(response.getItems());
                    sortByPriority(loadedItems);
                    freshFormirovanData = loadedItems;
                    compareAndSignalRefresh(freshFormirovanData, _originalFormirovanList.getValue(), true);
                } else {
                    // Ошибка или пустой ответ для Сформирован, сбрасываем флаг свежих данных
                    hasFreshFormirovanData = false;
                    freshFormirovanData = null;
                    // Если для другого списка тоже нет свежих данных, кнопка не нужна
                    if (!hasFreshKomplektuetsaData) {
                        _showRefreshButton.postValue(false);
                    }
                }
                handleDataCheckCompletion();
            }

            @Override
            public void onError(Exception exception) {
                _errorMessage.postValue("Ошибка фоновой загрузки (Сформирован): " + exception.getMessage());
                hasFreshFormirovanData = false;
                freshFormirovanData = null;
                if (!hasFreshKomplektuetsaData) {
                    _showRefreshButton.postValue(false);
                }
                handleDataCheckCompletion();
            }
        });

        // Загрузка данных для "Комплектуется"
        moveRepository.getMoveList(STATUS_KOMPLEKTUETSA, null, null, new RepositoryCallback<MoveResponse>() {
            @Override
            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> loadedItems = new ArrayList<>(response.getItems());
                    sortByPriority(loadedItems);
                    freshKomplektuetsaData = loadedItems;
                    compareAndSignalRefresh(freshKomplektuetsaData, _originalKomplektuetsaList.getValue(), false);
                } else {
                    hasFreshKomplektuetsaData = false;
                    freshKomplektuetsaData = null;
                    if (!hasFreshFormirovanData) {
                        _showRefreshButton.postValue(false);
                    }
                }
                handleDataCheckCompletion();
            }

            @Override
            public void onError(Exception exception) {
                _errorMessage.postValue("Ошибка фоновой загрузки (Комплектуется): " + exception.getMessage());
                hasFreshKomplektuetsaData = false;
                freshKomplektuetsaData = null;
                if (!hasFreshFormirovanData) {
                    _showRefreshButton.postValue(false);
                }
                handleDataCheckCompletion();
            }
        });
    }

    private void handleDataCheckCompletion() {
        synchronized (dataCheckLock) {
            activeDataChecks--;
            if (activeDataChecks <= 0) {
                _isLoading.postValue(false);
                activeDataChecks = 0; // Сбрасываем на случай ошибок или непредвиденных вызовов
            }
        }
    }

    private void compareAndSignalRefresh(List<MoveItem> newList, List<MoveItem> currentList, boolean isFormirovanTab) {
        if (!areListsEqualVM(newList, currentList)) {
            if (isFormirovanTab) {
                hasFreshFormirovanData = true;
            } else {
                hasFreshKomplektuetsaData = true;
            }
            _showRefreshButton.postValue(true);
        } else {
            // Если списки равны, сбрасываем флаг наличия свежих данных для этой вкладки
            if (isFormirovanTab) {
                hasFreshFormirovanData = false;
                freshFormirovanData = null; // Очищаем, так как они идентичны текущим
            } else {
                hasFreshKomplektuetsaData = false;
                freshKomplektuetsaData = null;
            }
            // Кнопка обновления показывается, только если ЕСТЬ свежие данные хотя бы для одного списка
            if (!hasFreshFormirovanData && !hasFreshKomplektuetsaData) {
                _showRefreshButton.postValue(false);
            }
        }
    }

    public void applyPendingUpdates() {
        // _isLoading.postValue(true);
        _isLoading.setValue(true);
        boolean updated = false;
        if (hasFreshFormirovanData && freshFormirovanData != null) {
            // _originalFormirovanList.postValue(new ArrayList<>(freshFormirovanData)); // создаем копию
            _originalFormirovanList.setValue(new ArrayList<>(freshFormirovanData)); // создаем копию
            hasFreshFormirovanData = false;
            freshFormirovanData = null;
            updated = true;
        }
        if (hasFreshKomplektuetsaData && freshKomplektuetsaData != null) {
            // _originalKomplektuetsaList.postValue(new ArrayList<>(freshKomplektuetsaData)); // создаем копию
            _originalKomplektuetsaList.setValue(new ArrayList<>(freshKomplektuetsaData)); // создаем копию
            hasFreshKomplektuetsaData = false;
            freshKomplektuetsaData = null;
            updated = true;
        }

        if (updated) {
            triggerFilterRecalculation(); // Уже вызывается при postValue на _original списки, но для явности
            // _moveOperationMessage.postValue("Данные обновлены.");
            _showToastEvent.postValue(new SingleEvent<>("Данные обновлены."));
        } else {
            // _moveOperationMessage.postValue("Нет доступных обновлений.");
            _showToastEvent.postValue(new SingleEvent<>("Нет доступных обновлений."));
        }
        // _showRefreshButton.postValue(false);
        _showRefreshButton.setValue(false);
        // _isLoading.postValue(false);
        _isLoading.setValue(false);
    }

    // --- Вспомогательные методы для сравнения списков (скопировано из MoveList_menu) ---
    private boolean areListsEqualVM(List<MoveItem> list1, List<MoveItem> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        // Для простой проверки можно использовать contentEquals если порядок важен и элементы имеют хороший equals
        // Но здесь логика сложнее, основана на ID и выборочных полях.
        // Создаем копии для сортировки, чтобы не изменять оригинальные списки, если они передаются откуда-то еще.
        // Однако, если эти списки приходят из LiveData, они уже обертки, и сортировка копий - правильный подход.
        // Для сравнения без учета порядка, можно использовать Map, как было в MoveList_menu
        // или сортировать копии по уникальному ключу и затем сравнивать поэлементно.

        // Используем подход с Map для сравнения без учета порядка, как было в MoveList_menu
        Map<String, MoveItem> map1 = new HashMap<>();
        for (MoveItem item : list1) {
            if (item.getMovementId() != null) { // Проверка на null ID
                 map1.put(item.getMovementId(), item);
            }
        }
        Map<String, MoveItem> map2 = new HashMap<>();
        for (MoveItem item : list2) {
            if (item.getMovementId() != null) { // Проверка на null ID
                map2.put(item.getMovementId(), item);
            }
        }

        if (map1.size() != map2.size()) return false; // Если количество элементов с ID разное
        if (!map1.keySet().equals(map2.keySet())) return false;

        for (String id : map1.keySet()) {
            if (!areItemsEqualVM(map1.get(id), map2.get(id))) {
                return false;
            }
        }
        return true;
    }

    private boolean areItemsEqualVM(MoveItem item1, MoveItem item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;

        // Сначала сравниваем ID - если разные, то это точно разные объекты
        // (хотя на этом этапе ID уже должны совпадать из-за логики в areListsEqualVM)
        if (!safeEqualsVM(item1.getMovementId(), item2.getMovementId())) return false;

        if (!safeEqualsVM(item1.getSigningStatus(), item2.getSigningStatus())) return false;
        if (!safeEqualsVM(item1.getPriority(), item2.getPriority())) return false;
        if (!safeEqualsVM(item1.getAssemblerName(), item2.getAssemblerName())) return false;
        if (!safeEqualsVM(item1.getNumber(), item2.getNumber())) return false;
        if (!safeEqualsVM(item1.getSourceWarehouseName(), item2.getSourceWarehouseName())) return false;
        if (!safeEqualsVM(item1.getDestinationWarehouseName(), item2.getDestinationWarehouseName())) return false;
        if (!safeEqualsVM(item1.getResponsiblePersonName(), item2.getResponsiblePersonName())) return false;
        if (item1.isCps() != item2.isCps()) return false;
        if (!safeEqualsVM(item1.getDate(), item2.getDate())) return false;
        // Добавляем сравнение isKomplektuetsa и isFormirovan, если они влияют на равенство
        if (item1.getItemsCount() != item2.getItemsCount()) return false;
        // TODO: Сравнить другие важные поля, если необходимо

        return true;
    }

    private boolean safeEqualsVM(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }

    public void processMoveItemClick(MoveItem moveItem) {
        if (moveItem == null || moveItem.getMovementId() == null) {
            _errorMessage.postValue("Ошибка: Недостаточно данных для обработки клика по элементу.");
            return;
        }

        String movementId = moveItem.getMovementId();

        _isProcessingItemClick.postValue(true); // Показываем индикатор загрузки для этой операции
        moveRepository.getDocumentMove(movementId, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                _isProcessingItemClick.postValue(false);
                if (invoice == null || invoice.getProducts() == null || invoice.getProducts().isEmpty()) {
                    _showToastEvent.postValue(new SingleEvent<>("Нет комплектующих для данного перемещения"));
                } else {
                    _navigateToPrixodEvent.postValue(new SingleEvent<>(movementId));
                }
            }

            @Override
            public void onError(Exception exception) {
                _isProcessingItemClick.postValue(false);
                _errorMessage.postValue("Ошибка при проверке комплектующих: " + exception.getMessage());
            }
        });
    }

    // --- Методы для взаимодействия с PrixodActivity ---

    /**
     * Вызывается из Activity после возврата из PrixodActivity.
     * @param data Intent, полученный в onActivityResult.
     */
    public void onReturnedFromPrixod(android.content.Intent data) {
        if (data != null) {
            if (data.hasExtra("productData")) {
                productsJsonDataFromPrixod = data.getStringExtra("productData");
            }
            preserveEditedDataFromPrixod = data.getBooleanExtra("preserveEditedData", false);
            // Можно добавить логгирование полученных данных
        }
        // После возврата из Prixod, всегда проверяем наличие обновлений данных для списков
        checkForDataUpdates();
    }

    public String getProductsJsonForPrixod() {
        return productsJsonDataFromPrixod;
    }

    public boolean shouldPreserveEditedDataForPrixod() {
        return preserveEditedDataFromPrixod;
    }

    public void clearPrixodReturnData() {
        productsJsonDataFromPrixod = null;
        preserveEditedDataFromPrixod = false;
    }

    protected void onCleared() {
        super.onCleared();
        executorService.shutdown(); // Не забываем остановить ExecutorService
    }
} 