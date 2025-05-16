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
    // TODO: Реализовать логику фильтрации для этих LiveData
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

    // --- LiveData для управления состоянием отмены ---
    private final MutableLiveData<Boolean> _undoAvailable = new MutableLiveData<>(false);
    public LiveData<Boolean> undoAvailable = _undoAvailable;

    private final MutableLiveData<List<MoveItem>> _lastMovedItems = new MutableLiveData<>();
    // Не делаем public LiveData для _lastMovedItems, т.к. это внутреннее состояние для undo

    private final MutableLiveData<String> _lastMoveSourceState = new MutableLiveData<>();
    // Не делаем public LiveData, внутреннее состояние

    private final MutableLiveData<String> _lastMoveTargetState = new MutableLiveData<>();
    // Не делаем public LiveData, внутреннее состояние

    // Для сообщений UI, таких как Snackbar. Предполагается наличие класса SingleLiveEvent.
    // Если его нет, можно использовать MutableLiveData и обрабатывать события соответствующим образом в Activity/Fragment.
    private final MutableLiveData<String> _moveOperationMessage = new MutableLiveData<>(); // Замена на MutableLiveData, если SingleLiveEvent нет
    public LiveData<String> moveOperationMessage = _moveOperationMessage;

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

    private final MoveRepository moveRepository;

    @Inject
    public MoveListViewModel(SavedStateHandle savedStateHandle, MoveRepository moveRepository) {
        this.savedStateHandle = savedStateHandle;
        this.moveRepository = moveRepository;

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

        // Начальная загрузка данных при создании ViewModel
        // Проверяем, есть ли уже данные в _originalFormirovanList, чтобы не загружать повторно при пересоздании ViewModel
        if (_originalFormirovanList.getValue() == null || _originalFormirovanList.getValue().isEmpty()) {
            loadMoveData();
        }
    }

    /**
     * Загружает данные о перемещениях.
     * Аналог loadMoveDataFromJson из MoveList_menu.
     */
    public void loadMoveData() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null); // Сбрасываем предыдущую ошибку

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

                    _originalFormirovanList.postValue(formirovanList);
                    _originalKomplektuetsaList.postValue(komplektuetsaList);

                    // TODO: После реализации фильтров, обновлять _filtered...List здесь на основе _original...List и текущих фильтров.
                    // Пока что просто копируем.
                    _filteredFormirovanList.postValue(new ArrayList<>(formirovanList));
                    _filteredKomplektuetsaList.postValue(new ArrayList<>(komplektuetsaList));

                } else {
                    _errorMessage.postValue("Ошибка загрузки данных: пустой ответ");
                }
                _isLoading.postValue(false);
            }

            @Override
            // public void onError(String errorMsg) { // Старый метод
            public void onError(Exception exception) { // Новый метод
                // _errorMessage.postValue("Ошибка при загрузке данных: " + errorMsg);
                _errorMessage.postValue("Ошибка при загрузке данных: " + exception.getMessage());
                _isLoading.postValue(false);
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
        // Эта функция теперь будет вызываться после каждого изменения фильтра
        // или смены вкладки.
        // Здесь мы применим фильтры к оригинальным спискам и обновим _filtered...List.

        int currentTab = currentTabPosition.getValue() != null ? currentTabPosition.getValue() : 0;
        List<MoveItem> originalList;
        MutableLiveData<List<MoveItem>> targetFilteredList;

        String sender, movementNumber, recipient, assembler, priority, receiver;
        Boolean cpsChecked, availabilityChecked;

        if (currentTab == 0) { // Сформирован
            originalList = _originalFormirovanList.getValue();
            targetFilteredList = _filteredFormirovanList;
            sender = formirovanSenderFilter.getValue();
            movementNumber = formirovanMovementNumberFilter.getValue();
            recipient = formirovanRecipientFilter.getValue();
            assembler = formirovanAssemblerFilter.getValue();
            priority = formirovanPriorityFilter.getValue();
            receiver = formirovanReceiverFilter.getValue();
            cpsChecked = formirovanCpsChecked.getValue();
            availabilityChecked = formirovanAvailabilityChecked.getValue();
        } else { // Комплектуется
            originalList = _originalKomplektuetsaList.getValue();
            targetFilteredList = _filteredKomplektuetsaList;
            sender = komplektuetsaSenderFilter.getValue();
            movementNumber = komplektuetsaMovementNumberFilter.getValue();
            recipient = komplektuetsaRecipientFilter.getValue();
            assembler = komplektuetsaAssemblerFilter.getValue();
            priority = komplektuetsaPriorityFilter.getValue();
            receiver = komplektuetsaReceiverFilter.getValue();
            cpsChecked = komplektuetsaCpsChecked.getValue();
            availabilityChecked = komplektuetsaAvailabilityChecked.getValue();
        }

        if (originalList == null) {
            originalList = new ArrayList<>();
        }

        List<MoveItem> filteredList = applyFiltersToList(
                new ArrayList<>(originalList), // Передаем копию для фильтрации
                sender, movementNumber, recipient, assembler, priority, receiver,
                cpsChecked, availabilityChecked
        );

        // Обновляем LiveData отфильтрованного списка
        targetFilteredList.setValue(filteredList);

        // Обновляем индикатор активных фильтров
        updateIsAnyFilterActive();
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
                       (formirovanCpsChecked.getValue() != null && formirovanCpsChecked.getValue()) ||
                       (formirovanAvailabilityChecked.getValue() != null && formirovanAvailabilityChecked.getValue());
        } else { // Комплектуется
            isActive = isFilterApplied(komplektuetsaSenderFilter.getValue()) ||
                       isFilterApplied(komplektuetsaMovementNumberFilter.getValue()) ||
                       isFilterApplied(komplektuetsaRecipientFilter.getValue()) ||
                       isFilterApplied(komplektuetsaAssemblerFilter.getValue()) ||
                       isFilterPriorityApplied(komplektuetsaPriorityFilter.getValue()) ||
                       isFilterApplied(komplektuetsaReceiverFilter.getValue()) ||
                       (komplektuetsaCpsChecked.getValue() != null && komplektuetsaCpsChecked.getValue()) ||
                       (komplektuetsaAvailabilityChecked.getValue() != null && komplektuetsaAvailabilityChecked.getValue());
        }
        _isAnyFilterActiveLive.setValue(isActive);
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
            _moveOperationMessage.postValue("Нет элементов для перемещения.");
            return;
        }

        List<MoveItem> currentOriginalFormirovanList = new ArrayList<>(_originalFormirovanList.getValue() != null ? _originalFormirovanList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalKomplektuetsaList = new ArrayList<>(_originalKomplektuetsaList.getValue() != null ? _originalKomplektuetsaList.getValue() : Collections.emptyList());

        List<MoveItem> movedItemsCopy = new ArrayList<>(); // Для сохранения состояния отмены

        for (MoveItem item : itemsToMove) {
            movedItemsCopy.add(item); // Сохраняем копию для отмены

            boolean removedSuccessfully = false; // Флаг для отслеживания успешного удаления

            if (STATUS_FORMIROVAN.equals(currentFragmentState) && STATUS_KOMPLEKTUETSA.equals(targetState)) {
                removedSuccessfully = currentOriginalFormirovanList.removeIf(
                    listItem -> listItem.getMovementId() != null &&
                                item.getMovementId() != null &&
                                listItem.getMovementId().equals(item.getMovementId())
                );
                if (removedSuccessfully) {
                    item.setSigningStatus(STATUS_KOMPLEKTUETSA); // Меняем статус ПОСЛЕ успешного удаления
                    currentOriginalKomplektuetsaList.add(item);
                }
            } else if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState) && STATUS_FORMIROVAN.equals(targetState)) {
                removedSuccessfully = currentOriginalKomplektuetsaList.removeIf(
                    listItem -> listItem.getMovementId() != null &&
                                item.getMovementId() != null &&
                                listItem.getMovementId().equals(item.getMovementId())
                );
                if (removedSuccessfully) {
                    item.setSigningStatus(STATUS_FORMIROVAN); // Меняем статус ПОСЛЕ успешного удаления
                    currentOriginalFormirovanList.add(item);
                }
            }
            // Если removedSuccessfully == false, элемент не был найден в ожидаемом списке.
            // Можно добавить логгирование или другую обработку, если это необходимо.
            // В текущей логике, если элемент не найден, его статус не меняется и он не перемещается.
        }

        sortByPriority(currentOriginalFormirovanList);
        sortByPriority(currentOriginalKomplektuetsaList);

        _originalFormirovanList.postValue(currentOriginalFormirovanList);
        _originalKomplektuetsaList.postValue(currentOriginalKomplektuetsaList);

        // Сохраняем состояние для возможной отмены
        _lastMovedItems.postValue(movedItemsCopy); // Сохраняем КОПИИ оригинальных состояний
        _lastMoveSourceState.postValue(currentFragmentState);
        _lastMoveTargetState.postValue(targetState);
        _undoAvailable.postValue(true);

        triggerFilterRecalculation(); // Обновляем фильтрованные списки

        String message = "Перемещено " + itemsToMove.size() + " элементов в " + targetState ;
        _moveOperationMessage.postValue(message);
    }

    /**
     * Отменяет последнее действие перемещения элементов.
     */
    public void undoMove() {
        List<MoveItem> itemsToRestore = _lastMovedItems.getValue();
        String sourceState = _lastMoveSourceState.getValue(); // Куда вернуть (бывший currentFragmentState)
        String targetState = _lastMoveTargetState.getValue(); // Откуда вернуть (бывший targetState)

        if (itemsToRestore == null || itemsToRestore.isEmpty() || sourceState == null || targetState == null) {
            _moveOperationMessage.postValue("Нет операции для отмены.");
            return;
        }

        List<MoveItem> currentOriginalFormirovanList = new ArrayList<>(_originalFormirovanList.getValue() != null ? _originalFormirovanList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalKomplektuetsaList = new ArrayList<>(_originalKomplektuetsaList.getValue() != null ? _originalKomplektuetsaList.getValue() : Collections.emptyList());

        // Логика восстановления обратна moveItems
        for (MoveItem originalItemState : itemsToRestore) {
             // Находим актуальный элемент в текущих списках по ID или уникальному ключу, чтобы обновить его.
             // Предполагаем, что MoveItem имеет getId() или аналогичный уникальный идентификатор.
             // Если нет, то нужно будет найти по совпадению других полей или передавать актуальные объекты.
             // Для простоты, сейчас будем предполагать, что мы можем найти и удалить/добавить сам объект.
             // Однако, правильнее было бы найти объект по ID и обновить его поля из originalItemState.

            if (STATUS_FORMIROVAN.equals(sourceState)) { // Элементы были в "Сформирован", вернулись туда
                // Удаляем из "Комплектуется" (куда они были перемещены)
                // boolean removed = currentOriginalKomplektuetsaList.removeIf(item -> item.getInternalDocumentNumber().equals(originalItemState.getInternalDocumentNumber())); // Пример уникального идентификатора
                boolean removed = currentOriginalKomplektuetsaList.removeIf(
                    listItem -> listItem.getMovementId() != null &&
                                originalItemState.getMovementId() != null &&
                                listItem.getMovementId().equals(originalItemState.getMovementId())
                );
                if(removed){
                    currentOriginalFormirovanList.add(originalItemState); // Добавляем оригинальное состояние
                }
            } else if (STATUS_KOMPLEKTUETSA.equals(sourceState)) { // Элементы были в "Комплектуется", вернулись туда
                // Удаляем из "Сформирован"
                // boolean removed = currentOriginalFormirovanList.removeIf(item -> item.getInternalDocumentNumber().equals(originalItemState.getInternalDocumentNumber())); // Пример уникального идентификатора
                 boolean removed = currentOriginalFormirovanList.removeIf(
                    listItem -> listItem.getMovementId() != null &&
                                originalItemState.getMovementId() != null &&
                                listItem.getMovementId().equals(originalItemState.getMovementId())
                 );
                 if(removed){
                    currentOriginalKomplektuetsaList.add(originalItemState); // Добавляем оригинальное состояние
                 }
            }
        }

        sortByPriority(currentOriginalFormirovanList);
        sortByPriority(currentOriginalKomplektuetsaList);

        _originalFormirovanList.postValue(currentOriginalFormirovanList);
        _originalKomplektuetsaList.postValue(currentOriginalKomplektuetsaList);

        // Сбрасываем состояние отмены
        _undoAvailable.postValue(false);
        _lastMovedItems.postValue(null);
        _lastMoveSourceState.postValue(null);
        _lastMoveTargetState.postValue(null);

        triggerFilterRecalculation();
        _moveOperationMessage.postValue("Перемещение отменено. Восстановлено " + itemsToRestore.size() + " элементов.");
    }

    /**
     * Очищает сообщение об операции, чтобы оно не показывалось повторно (например, при повороте экрана).
     * Вызывать из Activity/Fragment после того, как сообщение было показано.
     */
    public void clearMoveOperationMessage() {
        _moveOperationMessage.postValue(null);
    }

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
        _isLoading.postValue(true);
        boolean updated = false;
        if (hasFreshFormirovanData && freshFormirovanData != null) {
            _originalFormirovanList.postValue(new ArrayList<>(freshFormirovanData)); // создаем копию
            hasFreshFormirovanData = false;
            freshFormirovanData = null;
            updated = true;
        }
        if (hasFreshKomplektuetsaData && freshKomplektuetsaData != null) {
            _originalKomplektuetsaList.postValue(new ArrayList<>(freshKomplektuetsaData)); // создаем копию
            hasFreshKomplektuetsaData = false;
            freshKomplektuetsaData = null;
            updated = true;
        }

        if (updated) {
            triggerFilterRecalculation(); // Уже вызывается при postValue на _original списки, но для явности
            _moveOperationMessage.postValue("Данные обновлены.");
        } else {
            _moveOperationMessage.postValue("Нет доступных обновлений.");
        }
        _showRefreshButton.postValue(false);
        _isLoading.postValue(false);
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
} 