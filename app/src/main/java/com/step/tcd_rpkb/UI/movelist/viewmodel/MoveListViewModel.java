package com.step.tcd_rpkb.UI.movelist.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.MoveResponse;
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;
import com.step.tcd_rpkb.domain.model.Invoice; // Импорт для DataProvider
import com.step.tcd_rpkb.utils.SingleEvent;
import com.step.tcd_rpkb.domain.usecase.GetOnlineModeUseCase;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.domain.usecase.ChangeMoveStatusUseCase;
import com.step.tcd_rpkb.domain.model.ChangeMoveStatusResult;
import com.step.tcd_rpkb.utils.DefaultFiltersManager;
import com.step.tcd_rpkb.utils.DefaultFiltersData;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.repository.ProductsRepository;
import com.step.tcd_rpkb.data.datasources.LocalRealmDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@HiltViewModel
public class MoveListViewModel extends AndroidViewModel {

    // Константы статусов (из MoveList_menu)
    public static final String STATUS_FORMIROVAN = "Сформирован";
    public static final String STATUS_KOMPLEKTUETSA = "Комплектуется";
    public static final String STATUS_PODGOTOVLEN = "Подготовлен";

    // Константы для приоритетов (из MoveList_menu)
    public static final String PRIORITY_URGENT = "Неотложный";
    public static final String PRIORITY_HIGH = "Высокий";
    public static final String PRIORITY_MEDIUM = "Средний";
    public static final String PRIORITY_LOW = "Низкий";

    // Ключи для SavedStateHandle - единые фильтры для всех вкладок
    private static final String KEY_SENDER = "senderFilter";
    private static final String KEY_MOVEMENT_NUMBER = "movementNumberFilter";
    private static final String KEY_NOMENCULATURE = "nomenculatureFilter";
    private static final String KEY_SERIES = "seriesFilter";

    private static final String KEY_RECIPIENT = "recipientFilter";
    private static final String KEY_ASSEMBLER = "assemblerFilter";
    private static final String KEY_PRIORITY = "priorityFilter";
    private static final String KEY_RECEIVER = "receiverFilter";
    private static final String KEY_CPS_CHECKED = "cpsChecked";
    private static final String KEY_AVAILABILITY_CHECKED = "availabilityChecked";

    private static final String KEY_CURRENT_TAB_POSITION = "currentTabPosition";

    private final SavedStateHandle savedStateHandle;

    // LiveData для оригинальных (нефильтрованных) списков
    private final MutableLiveData<List<MoveItem>> _originalFormirovanList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> originalFormirovanList = _originalFormirovanList;

    private final MutableLiveData<List<MoveItem>> _originalKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> originalKomplektuetsaList = _originalKomplektuetsaList;

    private final MutableLiveData<List<MoveItem>> _originalPodgotovlenList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> originalPodgotovlenList = _originalPodgotovlenList;

    // LiveData для отфильтрованных списков

    private final MutableLiveData<List<MoveItem>> _filteredFormirovanList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> filteredFormirovanList = _filteredFormirovanList;

    private final MutableLiveData<List<MoveItem>> _filteredKomplektuetsaList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> filteredKomplektuetsaList = _filteredKomplektuetsaList;

    private final MutableLiveData<List<MoveItem>> _filteredPodgotovlenList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<MoveItem>> filteredPodgotovlenList = _filteredPodgotovlenList;

    // LiveData для состояния загрузки
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    // LiveData для сообщений об ошибках
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    // LiveData для состояния фильтров

    // Текущая активная вкладка
    public LiveData<Integer> currentTabPosition;

    // Единые фильтры для всех вкладок
    public LiveData<String> senderFilter;
    public LiveData<String> movementNumberFilter;
    public LiveData<String> nomenculatureFilter;
    public LiveData<String> seriesFilter;

    public LiveData<String> recipientFilter;
    public LiveData<String> assemblerFilter;
    public LiveData<String> priorityFilter;
    public LiveData<String> receiverFilter;
    public LiveData<Boolean> cpsChecked;
    public LiveData<Boolean> availabilityChecked;

    private String lastLoadedNomenculature = "";
    private String lastLoadedSeries = "";
    private boolean lastLoadedAvailability = true;

    // LiveData для управления видимостью кнопки обновления
    private final MutableLiveData<Boolean> _showRefreshButton = new MutableLiveData<>(false);
    public LiveData<Boolean> showRefreshButton = _showRefreshButton;

    // Счетчик для активных фоновых загрузок данных
    private int activeDataChecks = 0;
    private final Object dataCheckLock = new Object(); // Для синхронизации доступа к activeDataChecks

    // Временные хранилища для фоново загруженных данных
    private List<MoveItem> freshFormirovanData = null;
    private List<MoveItem> freshKomplektuetsaData = null;
    private List<MoveItem> freshPodgotovlenData = null;
    private boolean hasFreshFormirovanData = false;
    private boolean hasFreshKomplektuetsaData = false;
    private boolean hasFreshPodgotovlenData = false;


    // LiveData для UI событий
    private final MutableLiveData<SingleEvent<String>> _navigateToPrixodEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> navigateToPrixodEvent = _navigateToPrixodEvent;

    private final MutableLiveData<SingleEvent<String>> _showToastEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> showToastEvent = _showToastEvent;
    
    // Событие для показа ошибки с пустыми УИДСтрокиТовары
    private final MutableLiveData<SingleEvent<String>> _showEmptyProductLineIdErrorEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> showEmptyProductLineIdErrorEvent = _showEmptyProductLineIdErrorEvent;
    
    // Событие для показа ошибки с пустым перемещением
    private final MutableLiveData<SingleEvent<String>> _showEmptyMovementErrorEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> showEmptyMovementErrorEvent = _showEmptyMovementErrorEvent;
    
    // LiveData для индикатора загрузки при проверке документа (отличается от _isLoading для списков)
    private final MutableLiveData<Boolean> _isProcessingItemClick = new MutableLiveData<>(false);
    public LiveData<Boolean> isProcessingItemClick = _isProcessingItemClick;

    // LiveData для индикации активных фильтров
    private final MutableLiveData<Boolean> _isAnyFilterActiveLive = new MutableLiveData<>(false);
    public LiveData<Boolean> isAnyFilterActiveLive = _isAnyFilterActiveLive;

    private final MoveRepository moveRepository;
    private final ExecutorService executorService;

    // Кэш сохраненных данных перемещений
    private final Map<String, String> cachedMovementData = new HashMap<>();
    private final Map<String, Boolean> cachedPreserveEditedData = new HashMap<>();
    
    // Событие для передачи кэшированных данных в PrixodActivity
    private final MutableLiveData<SingleEvent<CachedMovementData>> _loadCachedDataEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<CachedMovementData>> loadCachedDataEvent = _loadCachedDataEvent;
    
    /**
     * Класс для передачи кэшированных данных
     */
    public static class CachedMovementData {
        public final String moveUuid;
        public final String productsJson;
        public final boolean preserveEditedData;
        
        public CachedMovementData(String moveUuid, String productsJson, boolean preserveEditedData) {
            this.moveUuid = moveUuid;
            this.productsJson = productsJson;
            this.preserveEditedData = preserveEditedData;
        }
    }

    private final GetOnlineModeUseCase getOnlineModeUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ChangeMoveStatusUseCase changeMoveStatusUseCase;

    private final DefaultFiltersManager defaultFiltersManager;
    private final ProductsRepository productsRepository;
    private final LocalRealmDataSource localRealmDataSource;

    // LiveData для показа диалога ошибки
    private final MutableLiveData<SingleEvent<ErrorDialogData>> _showErrorDialogEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<ErrorDialogData>> showErrorDialogEvent = _showErrorDialogEvent;
    
    // LiveData для показа диалога успешной смены статуса
    private final MutableLiveData<SingleEvent<SuccessStatusChangeEvent>> _showSuccessStatusChangeEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<SuccessStatusChangeEvent>> showSuccessStatusChangeEvent = _showSuccessStatusChangeEvent;
    
    // Класс для события успешной смены статуса
    public static class SuccessStatusChangeEvent {
        public final String message;
        public final String targetState;
        public final String moveGuid;
        
        public SuccessStatusChangeEvent(String message, String targetState, String moveGuid) {
            this.message = message;
            this.targetState = targetState;
            this.moveGuid = moveGuid;
        }
    }
    
    // LiveData для событий работы с фильтрами по умолчанию
    private final MutableLiveData<SingleEvent<String>> _showDefaultFiltersMessageEvent = new MutableLiveData<>();
    public LiveData<SingleEvent<String>> showDefaultFiltersMessageEvent = _showDefaultFiltersMessageEvent;

    // Массовая онлайн обработак смены статуса перемещения
    private List<MoveItem> multiMoveQueue = null;
    private int multiMoveIndex = 0;
    private List<String> multiMoveSuccessGuids = null;
    private String multiMoveTargetState = null;
    private String multiMoveSourceState = null;
    private boolean multiMoveActive = false;
    private boolean multiMoveHadSuccess = false;
    private boolean multiMoveHadError = false;
    private int multiMoveErrorCount = 0;



    @Inject
    public MoveListViewModel(Application application, SavedStateHandle savedStateHandle, MoveRepository moveRepository, GetOnlineModeUseCase getOnlineModeUseCase, GetUserUseCase getUserUseCase, ChangeMoveStatusUseCase changeMoveStatusUseCase, ProductsRepository productsRepository, LocalRealmDataSource localRealmDataSource, @dagger.hilt.android.qualifiers.ApplicationContext android.content.Context appContext) {
        super(application);
        this.savedStateHandle = savedStateHandle;
        this.moveRepository = moveRepository;
        this.getOnlineModeUseCase = getOnlineModeUseCase;
        this.getUserUseCase = getUserUseCase;
        this.changeMoveStatusUseCase = changeMoveStatusUseCase;
        this.productsRepository = productsRepository;
        this.localRealmDataSource = localRealmDataSource;
        this.defaultFiltersManager = new DefaultFiltersManager(appContext);
        this.executorService = Executors.newSingleThreadExecutor();


        currentTabPosition = savedStateHandle.getLiveData(KEY_CURRENT_TAB_POSITION, 0);

        senderFilter = savedStateHandle.getLiveData(KEY_SENDER, "");
        movementNumberFilter = savedStateHandle.getLiveData(KEY_MOVEMENT_NUMBER, "");
        nomenculatureFilter = savedStateHandle.getLiveData(KEY_NOMENCULATURE, "");
        seriesFilter = savedStateHandle.getLiveData(KEY_SERIES, "");
        recipientFilter = savedStateHandle.getLiveData(KEY_RECIPIENT, "");
        assemblerFilter = savedStateHandle.getLiveData(KEY_ASSEMBLER, "");
        priorityFilter = savedStateHandle.getLiveData(KEY_PRIORITY, "");
        receiverFilter = savedStateHandle.getLiveData(KEY_RECEIVER, "");
        cpsChecked = savedStateHandle.getLiveData(KEY_CPS_CHECKED, true);
        availabilityChecked = savedStateHandle.getLiveData(KEY_AVAILABILITY_CHECKED, true);

    }

    /**
     * Загружает данные о перемещениях.
     * Аналог loadMoveDataFromJson из MoveList_menu.
     */
    public void loadMoveData() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        // Используем значения по умолчанию для дат, если они не указаны
        String startDate = null;
        String endDate = null;

        // Получаем значение фильтра по номенкулатуре и серии
        String nomenculature = nomenculatureFilter.getValue();
        String series = seriesFilter.getValue();

        String status = STATUS_FORMIROVAN + "|" + STATUS_KOMPLEKTUETSA + "|" + STATUS_PODGOTOVLEN;

        // Получаем GUID текущего пользователя
        String userGuid = getCurrentUserGuid();
        
        // Получаем значение фильтра доступности (по умолчанию true)
        Boolean availabilityFilterValue = availabilityChecked.getValue();
        boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;


        moveRepository.getMoveList(status, startDate, endDate, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
            @Override

            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> formirovanList = new ArrayList<>();
                    List<MoveItem> komplektuetsaList = new ArrayList<>();
                    List<MoveItem> podgotovlenList = new ArrayList<>();

                    for (MoveItem item : response.getItems()) {
                        if (STATUS_FORMIROVAN.equals(item.getSigningStatus())) {
                            formirovanList.add(item);
                        } else if (STATUS_KOMPLEKTUETSA.equals(item.getSigningStatus())) {
                            komplektuetsaList.add(item);
                        } else if (STATUS_PODGOTOVLEN.equals(item.getSigningStatus())) {
                            podgotovlenList.add(item);
                        }
                    }

                    sortByPriority(formirovanList);
                    sortByPriority(komplektuetsaList);
                    sortByPriority(podgotovlenList);

                    _originalFormirovanList.setValue(formirovanList);
                    _originalKomplektuetsaList.setValue(komplektuetsaList);
                    _originalPodgotovlenList.setValue(podgotovlenList);

                    lastLoadedNomenculature = nomenculature != null ? nomenculature : "";
                    lastLoadedSeries = series != null ? series : "";
                    lastLoadedAvailability = useFilter;
                    // Применяем текущие фильтры к загруженным данным
                    triggerFilterRecalculation();

                    _isLoading.postValue(false);

                } else {
                        _errorMessage.setValue("Ответ сервера пуст или не содержит элементов.");
                }
            }

            @Override
            public void onError(Exception exception) { // Новый метод
                    List<MoveItem> formirovanList = new ArrayList<>();
                    List<MoveItem> komplektuetsaList = new ArrayList<>();
                    List<MoveItem> podgotovlenList = new ArrayList<>();
                    _originalFormirovanList.setValue(formirovanList);
                    _originalKomplektuetsaList.setValue(komplektuetsaList);
                    _originalPodgotovlenList.setValue(podgotovlenList);

                    // Применяем текущие фильтры к загруженным данным
                    triggerFilterRecalculation();

                    String errorMessage = exception.getMessage();
                    
                    // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                    if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {
                        com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                            (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                    } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                    } else {

                        _errorMessage.setValue("Ошибка загрузки списка перемещений: " + errorMessage);
                    }
                    _isLoading.setValue(false);
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

    // Методы для обновления состояния фильтров

    public void setCurrentTabPosition(int position) {
        if (currentTabPosition.getValue() == null || !currentTabPosition.getValue().equals(position)) {
            savedStateHandle.set(KEY_CURRENT_TAB_POSITION, position);
            triggerFilterRecalculation();
        }
    }

    //Сеттеры для единых фильтров
    public void setSenderFilter(String filter) { 
        savedStateHandle.set(KEY_SENDER, filter);
    }
    public void setMovementNumberFilter(String filter) { 
        savedStateHandle.set(KEY_MOVEMENT_NUMBER, filter);
    }


    public void setNomenculatureFilter(String filter) {
        savedStateHandle.set(KEY_NOMENCULATURE, filter);
    }

    public void setSeriesFilter(String filter) {
        savedStateHandle.set(KEY_SERIES, filter);
    }

    public void setRecipientFilter(String filter) {
        savedStateHandle.set(KEY_RECIPIENT, filter);
    }
    public void setAssemblerFilter(String filter) { 
        savedStateHandle.set(KEY_ASSEMBLER, filter);
    }
    public void setPriorityFilter(String filter) { 
        savedStateHandle.set(KEY_PRIORITY, filter);
    }
    public void setReceiverFilter(String filter) { 
        savedStateHandle.set(KEY_RECEIVER, filter);
    }
    public void setCpsChecked(boolean checked) { 
        savedStateHandle.set(KEY_CPS_CHECKED, checked);
    }
    public void setAvailabilityChecked(boolean checked) {
        savedStateHandle.set(KEY_AVAILABILITY_CHECKED, checked);
    }

    public void applyFilters(){
        String currentNomenculature = nomenculatureFilter.getValue();
        String currentSeries = seriesFilter.getValue();
        Boolean currentAvailability = availabilityChecked.getValue();

        // Проверяем, изменились ли серверные фильтры
        boolean serverFiltersChanged =
                !safeEqualsVM(lastLoadedNomenculature, currentNomenculature) ||
                        !safeEqualsVM(lastLoadedSeries, currentSeries) ||
                        lastLoadedAvailability != (currentAvailability != null ? currentAvailability : true);

        if (serverFiltersChanged) {
            // Нужен запрос к серверу
            loadMoveData(); // он обновит lastLoaded значения
        } else {
            // Только фильтры без необходимости делать запрос  фильтрация
            triggerFilterRecalculation();
        }

    }

    /**
     * Этот метод будет вызываться после каждого изменения фильтра или активной вкладки,
     * чтобы пересчитать отфильтрованные списки. Теперь единые фильтры применяются ко всем вкладкам.
     */
    private void triggerFilterRecalculation() {
        executorService.execute(() -> {
            // Применяем единые фильтры ко всем спискам

            List<MoveItem> currentFormirovanList = _originalFormirovanList.getValue();
            List<MoveItem> currentfilteredFormirovan = applyFiltersToList(
                    currentFormirovanList != null ? new ArrayList<>(currentFormirovanList): new ArrayList<>(), // Передаем копию
                    senderFilter.getValue(),
                    movementNumberFilter.getValue(),
                    nomenculatureFilter.getValue(),
                    seriesFilter.getValue(),
                    recipientFilter.getValue(),
                    assemblerFilter.getValue(),
                    priorityFilter.getValue(),
                    receiverFilter.getValue(),
                    cpsChecked.getValue(),
                    availabilityChecked.getValue()
            );
            _filteredFormirovanList.postValue(currentfilteredFormirovan);

            List<MoveItem> currentKomplektuetsaList = _originalKomplektuetsaList.getValue();
            List<MoveItem> currentfilteredKomplektuetsa = applyFiltersToList(
                    currentKomplektuetsaList != null ? new ArrayList<>(currentKomplektuetsaList): new ArrayList<>(),
                    senderFilter.getValue(),
                    movementNumberFilter.getValue(),
                    nomenculatureFilter.getValue(),
                    seriesFilter.getValue(),
                    recipientFilter.getValue(),
                    assemblerFilter.getValue(),
                    priorityFilter.getValue(),
                    receiverFilter.getValue(),
                    cpsChecked.getValue(),
                    availabilityChecked.getValue()
            );
            _filteredKomplektuetsaList.postValue(currentfilteredKomplektuetsa);

            List<MoveItem> currentPodgotovlenList = _originalPodgotovlenList.getValue();
            List<MoveItem> currentfilteredPodgotovlen = applyFiltersToList(
                    currentPodgotovlenList != null ? new ArrayList<>(currentPodgotovlenList): new ArrayList<>(),
                    senderFilter.getValue(),
                    movementNumberFilter.getValue(),
                    nomenculatureFilter.getValue(),
                    seriesFilter.getValue(),
                    recipientFilter.getValue(),
                    assemblerFilter.getValue(),
                    priorityFilter.getValue(),
                    receiverFilter.getValue(),
                    cpsChecked.getValue(),
                    availabilityChecked.getValue()
            );
            _filteredPodgotovlenList.postValue(currentfilteredPodgotovlen);

            updateIsAnyFilterActive();
        });
    }

    private void updateIsAnyFilterActive() {
        // Теперь проверяем только единые фильтры
        boolean isActive = isFilterApplied(senderFilter.getValue()) ||
                isFilterApplied(movementNumberFilter.getValue()) ||
                isFilterApplied(nomenculatureFilter.getValue()) ||
                isFilterApplied(seriesFilter.getValue()) ||
                isFilterApplied(recipientFilter.getValue()) ||
                isFilterApplied(assemblerFilter.getValue()) ||
                isFilterPriorityApplied(priorityFilter.getValue()) ||
                isFilterApplied(receiverFilter.getValue()) ||
                cpsChecked.getValue() == false  ||
                availabilityChecked.getValue() == false;
        
        _isAnyFilterActiveLive.postValue(isActive);
    }

    // Вспомогательный метод для проверки, применен ли текстовый фильтр
    private boolean isFilterApplied(String filterValue) {
        return filterValue != null && !filterValue.trim().isEmpty();
    }

    // Вспомогательный метод для проверки, применен ли фильтр приоритета
    private boolean isFilterPriorityApplied(String priorityValue) {
        return priorityValue != null && !priorityValue.trim().isEmpty() && !priorityValue.equals("Все");
    }

    private List<MoveItem> applyFiltersToList(List<MoveItem> originalList,
                                              String senderFilter,
                                              String movementNumberFilter,
                                              String nomenculatureFilter,
                                              String seriesFilter,
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
                if (itemPriority == null || itemPriority.isEmpty()) {
                    if (PRIORITY_LOW.equals(priorityFilter)) {
                    } else {
                        matches = false;
                    }
                } else if (!itemPriority.equals(priorityFilter)) {
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }


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

        // Проверяем онлайн-режим
        boolean isOnline = getOnlineModeUseCase.execute();
        if (isOnline) {
            if (itemsToMove.size() > 1) {
                // Массовая онлайн обработка
                multiMoveQueue = new ArrayList<>(itemsToMove);
                multiMoveIndex = 0;
                multiMoveSuccessGuids = new ArrayList<>();
                multiMoveTargetState = targetState;
                multiMoveSourceState = currentFragmentState;
                multiMoveActive = true;
                multiMoveHadSuccess = false;
                multiMoveHadError = false;
                multiMoveErrorCount = 0;
                processSingleMoveInMulti();
                return;
            } else {
                // Одиночная оналйн обработка
                MoveItem item = itemsToMove.get(0);
                String guid = item.getMovementId();
                String userguid = getCurrentUserGuid();
                
                // Проверяем авторизацию пользователя
                if (userguid == null) {
                    _showToastEvent.setValue(new SingleEvent<>("Ошибка: пользователь не авторизован"));
                    return;
                }
                
                _isLoading.postValue(true);
                
                // Проверяем нужно ли синхронизировать данные с 1С (только при переходе с "Комплектуется" на "Подготовлен")
                boolean needDataSync = STATUS_KOMPLEKTUETSA.equals(currentFragmentState) && STATUS_PODGOTOVLEN.equals(targetState);
                
                if (needDataSync) {
                    // Загружаем продукты из Realm для сохранения в 1С
                    List<Product> products = localRealmDataSource.loadProducts(guid);

                    changeMoveStatusUseCase.executeWithDataSave(guid, targetState, userguid, products, new RepositoryCallback<ChangeMoveStatusResult>() {
                    @Override
                    public void onSuccess(ChangeMoveStatusResult resultObj) {
                        if (resultObj.isResult()) {
                            String moveNumber = item.getNumber();
                            
                            // Удаляем все сохраненные данные при смене статуса
                            deleteAllDataForMovement(guid);
                            
                            if (STATUS_FORMIROVAN.equals(currentFragmentState)) {
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    _originalFormirovanList.setValue(removeByGuid(_originalFormirovanList.getValue(), guid));
                                    triggerFilterRecalculation();
                                });
                            } else if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState)) {
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    _originalKomplektuetsaList.setValue(removeByGuid(_originalKomplektuetsaList.getValue(), guid));
                                    triggerFilterRecalculation();
                                });
                            } else if (STATUS_PODGOTOVLEN.equals(currentFragmentState)) {
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    _originalPodgotovlenList.setValue(removeByGuid(_originalPodgotovlenList.getValue(), guid));
                                    triggerFilterRecalculation();
                                });
                            }
                            String userGuid = getCurrentUserGuid();
                            Boolean availabilityFilterValue = availabilityChecked.getValue();
                            boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;

                            // Получаем значение фильтра по номенкулатуре и серии
                            String nomenculature = nomenculatureFilter.getValue();
                            String series = seriesFilter.getValue();

                            moveRepository.getMoveList(targetState, null, null, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
                                @Override
                                public void onSuccess(MoveResponse response) {
                                    if (response != null && response.getItems() != null) {
                                        List<MoveItem> sortedItems = new ArrayList<>(response.getItems());
                                        sortByPriority(sortedItems);
                                        if (STATUS_FORMIROVAN.equals(targetState)) {
                                            _originalFormirovanList.postValue(sortedItems);
                                        } else if (STATUS_KOMPLEKTUETSA.equals(targetState)) {
                                            _originalKomplektuetsaList.postValue(sortedItems);
                                        } else if (STATUS_PODGOTOVLEN.equals(targetState)) {
                                            _originalPodgotovlenList.postValue(sortedItems);
                                        }
                                                String successMessage = "Статус для '" + moveNumber + "' успешно изменён на '" + targetState + "' и данные синхронизированы с 1С.";
                                        _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                                            new SuccessStatusChangeEvent(successMessage, targetState, guid)
                                        ));
                                    }
                                    _isLoading.postValue(false);
                                }
                                @Override
                                public void onError(Exception exception) {
                                    _errorMessage.postValue("Ошибка при обновлении списка: " + exception.getMessage());
                                    _isLoading.postValue(false);
                                }
                            });
                        } else {

                            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", resultObj.getErrorText())));
                            _isLoading.postValue(false);
                        }
                    }
                    @Override
                    public void onError(Exception exception) {
                        String errorMessage = exception.getMessage();
                        
                        // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                        if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                            com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                                (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                        } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                        } else {

                            _errorMessage.postValue("Ошибка смены статуса: " + errorMessage);
                        }
                        _isLoading.postValue(false);
                    }
                });
                } else {
                    // Обычная смена статуса без синхронизации с 1С
                    changeMoveStatusUseCase.execute(guid, targetState, userguid, new RepositoryCallback<ChangeMoveStatusResult>() {
                        @Override
                        public void onSuccess(ChangeMoveStatusResult resultObj) {
                            if (resultObj.isResult()) {
                                String moveNumber = item.getNumber();
                                
                                // Удаляем все сохраненные данные при смене статуса
                                deleteAllDataForMovement(guid);
                                
                                if (STATUS_FORMIROVAN.equals(currentFragmentState)) {
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        _originalFormirovanList.setValue(removeByGuid(_originalFormirovanList.getValue(), guid));
                                        triggerFilterRecalculation();
                                    });
                                } else if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState)) {
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        _originalKomplektuetsaList.setValue(removeByGuid(_originalKomplektuetsaList.getValue(), guid));
                                        triggerFilterRecalculation();
                                    });
                                } else if (STATUS_PODGOTOVLEN.equals(currentFragmentState)) {
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        _originalPodgotovlenList.setValue(removeByGuid(_originalPodgotovlenList.getValue(), guid));
                                        triggerFilterRecalculation();
                                    });
                                }
                                String userGuid = getCurrentUserGuid();
                                Boolean availabilityFilterValue = availabilityChecked.getValue();
                                boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;

                                // Получаем значение фильтра по номенкулатуре и серии
                                String nomenculature = nomenculatureFilter.getValue();
                                String series = seriesFilter.getValue();

                                moveRepository.getMoveList(targetState, null, null, userGuid, useFilter, nomenculature, series,  new RepositoryCallback<MoveResponse>() {
                                    @Override
                                    public void onSuccess(MoveResponse response) {
                                        if (response != null && response.getItems() != null) {
                                            List<MoveItem> sortedItems = new ArrayList<>(response.getItems());
                                            sortByPriority(sortedItems);
                                            if (STATUS_FORMIROVAN.equals(targetState)) {
                                                _originalFormirovanList.postValue(sortedItems);
                                            } else if (STATUS_KOMPLEKTUETSA.equals(targetState)) {
                                                _originalKomplektuetsaList.postValue(sortedItems);
                                            } else if (STATUS_PODGOTOVLEN.equals(targetState)) {
                                                _originalPodgotovlenList.postValue(sortedItems);
                                            }
                                            String successMessage = "Статус для '" + moveNumber + "' успешно изменён на '" + targetState + "'.";
                                            _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                                                new SuccessStatusChangeEvent(successMessage, targetState, guid)
                                            ));
                                        }
                                        _isLoading.postValue(false);
                                    }
                                    @Override
                                    public void onError(Exception exception) {
                                        _errorMessage.postValue("Ошибка при обновлении списка: " + exception.getMessage());
                                        _isLoading.postValue(false);
                                    }
                                });
                        } else {

                            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", resultObj.getErrorText())));
                                _isLoading.postValue(false);
                        }
                        }
                        @Override
                        public void onError(Exception exception) {
                            String errorMessage = exception.getMessage();
                            
                            // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                            if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                                com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                                    (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                                _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                            } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                                _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                            } else {

                                _errorMessage.postValue("Ошибка смены статуса: " + errorMessage);
                            }
                        _isLoading.postValue(false);
                    }
                });
                }
            }
        } else {
        // Оффлайн режим

        List<MoveItem> currentOriginalFormirovanList = new ArrayList<>(_originalFormirovanList.getValue() != null ? _originalFormirovanList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalKomplektuetsaList = new ArrayList<>(_originalKomplektuetsaList.getValue() != null ? _originalKomplektuetsaList.getValue() : Collections.emptyList());
        List<MoveItem> currentOriginalPodgotovlenList = new ArrayList<>(_originalPodgotovlenList.getValue() != null ? _originalPodgotovlenList.getValue() : Collections.emptyList());

        List<MoveItem> itemsActuallyMoved = new ArrayList<>();

        List<MoveItem> sourceListReference;
        List<MoveItem> destinationListReference;
        MutableLiveData<List<MoveItem>> sourceLiveData;
        MutableLiveData<List<MoveItem>> destinationLiveData;

        if (STATUS_FORMIROVAN.equals(currentFragmentState) && STATUS_KOMPLEKTUETSA.equals(targetState)) {
            // Перемещение из "Сформирован" в "Комплектуется"
            sourceListReference = currentOriginalFormirovanList;
            destinationListReference = currentOriginalKomplektuetsaList;
            sourceLiveData = _originalFormirovanList;
            destinationLiveData = _originalKomplektuetsaList;
        } else if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState) && STATUS_FORMIROVAN.equals(targetState)) {
            // Перемещение из "Комплектуется" в "Сформирован"
            sourceListReference = currentOriginalKomplektuetsaList;
            destinationListReference = currentOriginalFormirovanList;
            sourceLiveData = _originalKomplektuetsaList;
            destinationLiveData = _originalFormirovanList;
        } else if (STATUS_PODGOTOVLEN.equals(currentFragmentState) && STATUS_KOMPLEKTUETSA.equals(targetState)) {
            // Перемещение из "Подготовлен" в "Комплектуется"
            sourceListReference = currentOriginalPodgotovlenList;
            destinationListReference = currentOriginalKomplektuetsaList;
            sourceLiveData = _originalPodgotovlenList;
            destinationLiveData = _originalKomplektuetsaList;
        } else if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState) && STATUS_PODGOTOVLEN.equals(targetState)) {
            // Перемещение из "Комплектуется" в "Подготовлен"
            sourceListReference = currentOriginalKomplektuetsaList;
            destinationListReference = currentOriginalPodgotovlenList;
            sourceLiveData = _originalKomplektuetsaList;
            destinationLiveData = _originalPodgotovlenList;
        } else {
            _showToastEvent.setValue(new SingleEvent<>("Неподдерживаемое перемещение: " + currentFragmentState + " -> " + targetState));
            return;
        }

        // Создаем список элементов для удаления с их индексами, чтобы правильно их удалить
        List<android.util.Pair<Integer, MoveItem>> itemsToRemoveWithIndexes = new ArrayList<>();
        for (MoveItem itemToMove : itemsToMove) {
            for (int i = 0; i < sourceListReference.size(); i++) {
                if (sourceListReference.get(i).getMovementId().equals(itemToMove.getMovementId())) {
                    itemsToRemoveWithIndexes.add(new android.util.Pair<>(i, sourceListReference.get(i)));
                    break;
                }
            }
        }


        itemsToRemoveWithIndexes.sort((p1, p2) -> Integer.compare(p2.first, p1.first));

        for (android.util.Pair<Integer, MoveItem> pair : itemsToRemoveWithIndexes) {
            int originalIndex = pair.first;
            MoveItem itemFromSourceList = pair.second; // Это элемент из оригинального списка


            if (STATUS_KOMPLEKTUETSA.equals(currentFragmentState)) {
                String moveUuid = itemFromSourceList.getMovementId();
                deleteAllDataForMovement(moveUuid);
                Log.d("MoveListViewModel", "Очищен кеш для перемещения " + moveUuid + 
                      " при смене статуса из 'Комплектуется' в '" + targetState + "' (оффлайн)");
            }
            
            sourceListReference.remove(originalIndex); // Удаляем из исходного списка по ранее найденному индексу


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

        triggerFilterRecalculation();
        
        String successMessage = "Статус для '" +  "' успешно изменён на '" + targetState + "' и данные синхронизированы с 1С.";
        _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                new SuccessStatusChangeEvent(successMessage, targetState, "878")
        ));
        }
    }


    // Логика проверки обновлений данных и управления кнопкой Refresh

    /**
     * Проверяет обновления данных на сервере
     * @param showLoading показывать ли индикатор загрузки (false для фоновых проверок)
     */
    public void checkForDataUpdates(boolean showLoading) {
        synchronized (dataCheckLock) {
            if (activeDataChecks > 0) {
                // Уже идет проверка, новую не начинаем
                return;
            }
            activeDataChecks = 3;
        }
        
        if (showLoading) {
            _isLoading.postValue(true);
        }

        // Получаем общие параметры для всех запросов
        String userGuid = getCurrentUserGuid();
        Boolean availabilityFilterValue = availabilityChecked.getValue();
        boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;
        // Получаем значение фильтра по номенкулатуре и серии
        String nomenculature = nomenculatureFilter.getValue();
        String series = seriesFilter.getValue();

        // Загрузка данных для "Сформирован"
        moveRepository.getMoveList(STATUS_FORMIROVAN, null, null, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
            @Override
            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> loadedItems = new ArrayList<>(response.getItems());
                    sortByPriority(loadedItems);
                    freshFormirovanData = loadedItems;
                    compareAndSignalRefresh(freshFormirovanData, _originalFormirovanList.getValue(), 0);
                } else {
                    hasFreshFormirovanData = false;
                    freshFormirovanData = null;
                    if (!hasFreshKomplektuetsaData && !hasFreshPodgotovlenData) {
                        _showRefreshButton.postValue(false);
                    }
                }
                handleDataCheckCompletion(showLoading);
            }

            @Override
            public void onError(Exception exception) {
                String errorMessage = exception.getMessage();
                
                // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                } else {

                    _errorMessage.postValue("Ошибка фоновой загрузки (Сформирован): " + errorMessage);
                }
                
                hasFreshFormirovanData = false;
                freshFormirovanData = null;
                if (!hasFreshKomplektuetsaData && !hasFreshPodgotovlenData) {
                    _showRefreshButton.postValue(false);
                }
                handleDataCheckCompletion(showLoading);
            }
        });

        // Загрузка данных для "Комплектуется"
        moveRepository.getMoveList(STATUS_KOMPLEKTUETSA, null, null, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
            @Override
            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> loadedItems = new ArrayList<>(response.getItems());
                    sortByPriority(loadedItems);
                    freshKomplektuetsaData = loadedItems;
                    compareAndSignalRefresh(freshKomplektuetsaData, _originalKomplektuetsaList.getValue(), 1);
                } else {
                    hasFreshKomplektuetsaData = false;
                    freshKomplektuetsaData = null;
                    if (!hasFreshFormirovanData && !hasFreshPodgotovlenData) {
                        _showRefreshButton.postValue(false);
                    }
                }
                handleDataCheckCompletion(showLoading);
            }

            @Override
            public void onError(Exception exception) {
                String errorMessage = exception.getMessage();
                
                // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                } else {
                    _errorMessage.postValue("Ошибка фоновой загрузки (Комплектуется): " + errorMessage);
                }
                
                hasFreshKomplektuetsaData = false;
                freshKomplektuetsaData = null;
                if (!hasFreshFormirovanData && !hasFreshPodgotovlenData) {
                    _showRefreshButton.postValue(false);
                }
                handleDataCheckCompletion(showLoading);
            }
        });

        // Загрузка данных для "Подготовлен"
        moveRepository.getMoveList(STATUS_PODGOTOVLEN, null, null, userGuid, useFilter, nomenculature, series,  new RepositoryCallback<MoveResponse>() {
            @Override
            public void onSuccess(MoveResponse response) {
                if (response != null && response.getItems() != null) {
                    List<MoveItem> loadedItems = new ArrayList<>(response.getItems());
                    sortByPriority(loadedItems);
                    freshPodgotovlenData = loadedItems;
                    compareAndSignalRefresh(freshPodgotovlenData, _originalPodgotovlenList.getValue(), 2);
                } else {
                    hasFreshPodgotovlenData = false;
                    freshPodgotovlenData = null;
                    if (!hasFreshFormirovanData && !hasFreshKomplektuetsaData) {
                        _showRefreshButton.postValue(false);
                    }
                }
                handleDataCheckCompletion(showLoading);
            }

            @Override
            public void onError(Exception exception) {
                String errorMessage = exception.getMessage();
                
                // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                } else {

                    _errorMessage.postValue("Ошибка фоновой загрузки (Подготовлен): " + errorMessage);
                }
                
                hasFreshPodgotovlenData = false;
                freshPodgotovlenData = null;
                if (!hasFreshFormirovanData && !hasFreshKomplektuetsaData) {
                    _showRefreshButton.postValue(false);
                }
                handleDataCheckCompletion(showLoading);
            }
        });
    }
    

    private void handleDataCheckCompletion(boolean showLoading) {
        synchronized (dataCheckLock) {
            activeDataChecks--;
            if (activeDataChecks <= 0) {
                if (showLoading) {
                    _isLoading.postValue(false); // Скрываем индикатор только если он был показан
                }
                activeDataChecks = 0; // Сбрасываем на случай ошибок или непредвиденных вызовов
            }
        }
    }

    private void compareAndSignalRefresh(List<MoveItem> newList, List<MoveItem> currentList, int tabIndex) {
        String tabName = (tabIndex == 0) ? "Сформирован" : (tabIndex == 1) ? "Комплектуется" : "Подготовлен";
        
        if (!areListsEqualVM(newList, currentList)) {

            logDetailedChanges(newList, currentList, tabName);
            
            Log.d("MoveListViewModel", "Обнаружены изменения в статусе: " + tabName + 
                  ". Новых элементов: " + (newList != null ? newList.size() : 0) + 
                  ", текущих: " + (currentList != null ? currentList.size() : 0));
                  
            if (tabIndex == 0) {
                hasFreshFormirovanData = true;
            } else if (tabIndex == 1) {
                hasFreshKomplektuetsaData = true;
            } else {
                hasFreshPodgotovlenData = true;
            }
            _showRefreshButton.postValue(true);
        } else {
            Log.d("MoveListViewModel", "Нет изменений в статусе: " + tabName);
            // Если списки равны, сбрасываем флаг наличия свежих данных для этой вкладки
            if (tabIndex == 0) {
                hasFreshFormirovanData = false;
                freshFormirovanData = null;
            } else if (tabIndex == 1) {
                hasFreshKomplektuetsaData = false;
                freshKomplektuetsaData = null;
            } else {
                hasFreshPodgotovlenData = false;
                freshPodgotovlenData = null;
            }

            if (!hasFreshFormirovanData && !hasFreshKomplektuetsaData && !hasFreshPodgotovlenData) {
                _showRefreshButton.postValue(false);
            }
        }
    }
    
    /**
     * Детальное логирование изменений между списками
     */
    private void logDetailedChanges(List<MoveItem> newList, List<MoveItem> currentList, String tabName) {
        if (newList == null && currentList == null) return;
        

        Map<String, MoveItem> newMap = new HashMap<>();
        if (newList != null) {
            for (MoveItem item : newList) {
                if (item != null && item.getMovementId() != null) {
                    newMap.put(item.getMovementId(), item);
                }
            }
        }
        
        Map<String, MoveItem> currentMap = new HashMap<>();
        if (currentList != null) {
            for (MoveItem item : currentList) {
                if (item != null && item.getMovementId() != null) {
                    currentMap.put(item.getMovementId(), item);
                }
            }
        }
        
        // Ищем новые перемещения
        for (String id : newMap.keySet()) {
            if (!currentMap.containsKey(id)) {
                MoveItem newItem = newMap.get(id);
                Log.d("MoveListViewModel", "[" + tabName + "] НОВОЕ перемещение: " + 
                      newItem.getNumber() + " (ID: " + id + ")");
            }
        }
        
        // Ищем удаленные перемещения
        for (String id : currentMap.keySet()) {
            if (!newMap.containsKey(id)) {
                MoveItem deletedItem = currentMap.get(id);
                Log.d("MoveListViewModel", "[" + tabName + "] УДАЛЕНО перемещение: " + 
                      deletedItem.getNumber() + " (ID: " + id + ")");
            }
        }
        
        // Ищем измененные перемещения
        for (String id : newMap.keySet()) {
            if (currentMap.containsKey(id)) {
                MoveItem newItem = newMap.get(id);
                MoveItem currentItem = currentMap.get(id);
                if (!areItemsEqualVM(newItem, currentItem)) {
                    Log.d("MoveListViewModel", "[" + tabName + "] ИЗМЕНЕНО перемещение: " + 
                          newItem.getNumber() + " (ID: " + id + ")");

                }
            }
        }
    }

    public void applyPendingUpdates() {
        _isLoading.setValue(true);
        boolean updated = false;
        if (hasFreshFormirovanData && freshFormirovanData != null) {
            _originalFormirovanList.setValue(new ArrayList<>(freshFormirovanData)); // создаем копию
            hasFreshFormirovanData = false;
            freshFormirovanData = null;
            updated = true;
        }
        if (hasFreshKomplektuetsaData && freshKomplektuetsaData != null) {
            _originalKomplektuetsaList.setValue(new ArrayList<>(freshKomplektuetsaData)); // создаем копию
            hasFreshKomplektuetsaData = false;
            freshKomplektuetsaData = null;
            updated = true;
        }
        if (hasFreshPodgotovlenData && freshPodgotovlenData != null) {
            _originalPodgotovlenList.setValue(new ArrayList<>(freshPodgotovlenData)); // создаем копию
            hasFreshPodgotovlenData = false;
            freshPodgotovlenData = null;
            updated = true;
        }

        if (updated) {
            triggerFilterRecalculation();
            _showToastEvent.postValue(new SingleEvent<>("Данные обновлены."));
        } else {
            _showToastEvent.postValue(new SingleEvent<>("Нет доступных обновлений."));
        }
        _showRefreshButton.setValue(false);
        _isLoading.setValue(false);
    }

    // Вспомогательные методы для сравнения списков
    private boolean areListsEqualVM(List<MoveItem> list1, List<MoveItem> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;


        Map<String, MoveItem> map1 = new HashMap<>();
        for (MoveItem item : list1) {
            if (item.getMovementId() != null) {
                map1.put(item.getMovementId(), item);
            }
        }
        
        Map<String, MoveItem> map2 = new HashMap<>();
        for (MoveItem item : list2) {
            if (item.getMovementId() != null) {
                map2.put(item.getMovementId(), item);
            }
        }

        // Сначала проверяем, что у нас одинаковые ключи (ID перемещений)
        if (!map1.keySet().equals(map2.keySet())) {
            return false;
        }

        // Затем сравниваем каждое перемещение по полям
        for (String movementId : map1.keySet()) {
            MoveItem item1 = map1.get(movementId);
            MoveItem item2 = map2.get(movementId);
            if (!areItemsEqualVM(item1, item2)) {
                return false;
            }
        }

        return true;
    }

    private boolean areItemsEqualVM(MoveItem item1, MoveItem item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;

        // Сравниваем все важные поля перемещения
        // Основной ключ - movementId, но проверяем и другие поля на предмет изменений
        return safeEqualsVM(item1.getMovementId(), item2.getMovementId()) &&
               safeEqualsVM(item1.getNumber(), item2.getNumber()) &&
               safeEqualsVM(item1.getDate(), item2.getDate()) &&
               safeEqualsVM(item1.getSigningStatus(), item2.getSigningStatus()) &&
               safeEqualsVM(item1.getPriority(), item2.getPriority()) &&
               safeEqualsVM(item1.getSourceWarehouseName(), item2.getSourceWarehouseName()) &&
               safeEqualsVM(item1.getDestinationWarehouseName(), item2.getDestinationWarehouseName()) &&
               safeEqualsVM(item1.getAssemblerName(), item2.getAssemblerName()) &&
               safeEqualsVM(item1.getResponsiblePersonName(), item2.getResponsiblePersonName()) &&
               safeEqualsVM(item1.getMovementDisplayText(), item2.getMovementDisplayText()) &&
               item1.getPositionsCount() == item2.getPositionsCount() &&
               Double.compare(item1.getItemsCount(), item2.getItemsCount()) == 0 &&
               item1.isCps() == item2.isCps();
    }

    private boolean safeEqualsVM(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }

    public void processMoveItemClick(MoveItem moveItem) {
        if (moveItem == null || moveItem.getMovementId() == null) {
            _errorMessage.setValue("Некорректные данные перемещения");
            return;
        }

        String moveUuid = moveItem.getMovementId();
        String signingStatus = moveItem.getSigningStatus();
        Log.d("MoveListViewModel", "Клик по перемещению с UUID: " + moveUuid + ", статус: " + signingStatus);


        if (STATUS_KOMPLEKTUETSA.equals(signingStatus)) {
            // Сначала проверяем кеш в TempDataManager (временные данные)
            boolean hasTempCache = cachedMovementData.containsKey(moveUuid);
            
            // Затем проверяем кеш продуктов в Realm через LocalRealmDataSource
            List<Product> cachedProducts = localRealmDataSource.loadProducts(moveUuid);
            boolean hasProductsCache = cachedProducts != null && !cachedProducts.isEmpty();
            
            if (hasTempCache || hasProductsCache) {
                if (hasTempCache) {
                    String cachedJson = cachedMovementData.get(moveUuid);
                    boolean preserveData = cachedPreserveEditedData.getOrDefault(moveUuid, false);
                    
                    Log.d("MoveListViewModel", "Найдены кэшированные данные в TempDataManager для перемещения " + moveUuid + 
                          " в статусе 'Комплектуется', размер JSON: " + (cachedJson != null ? cachedJson.length() : 0) + 
                          ", preserveEditedData: " + preserveData);
                    
                    // Передаем кэшированные данные через событие
                    CachedMovementData cachedData = new CachedMovementData(moveUuid, cachedJson, preserveData);
                    _loadCachedDataEvent.setValue(new SingleEvent<>(cachedData));
                    return;
                } else {
                    Log.d("MoveListViewModel", "Найдены кэшированные продукты в Realm для перемещения " + moveUuid + 
                          " в статусе 'Комплектуется', количество: " + cachedProducts.size());
                    

                    CachedMovementData cachedData = new CachedMovementData(moveUuid, null, false);
                    _loadCachedDataEvent.setValue(new SingleEvent<>(cachedData));
                    return;
                }
            } else {
                Log.d("MoveListViewModel", "Перемещение " + moveUuid + " в статусе 'Комплектуется', но кэшированных данных нет - загружаем с сервера");
            }
        }

        // Для статусов "Сформирован" и "Подготовлен" всегда загружаем данные с сервера
        if (STATUS_FORMIROVAN.equals(signingStatus) || STATUS_PODGOTOVLEN.equals(signingStatus)) {
            Log.d("MoveListViewModel", "Перемещение " + moveUuid + " в статусе '" + signingStatus + 
                  "' - всегда загружаем данные с сервера");
        } else if (!STATUS_KOMPLEKTUETSA.equals(signingStatus)) {
            Log.d("MoveListViewModel", "Неизвестный статус '" + signingStatus + "' для перемещения " + moveUuid + " - загружаем с сервера");
        }
        
        _isProcessingItemClick.setValue(true);


        moveRepository.getDocumentMove(moveUuid, new RepositoryCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice invoice) {
                _isProcessingItemClick.setValue(false);
                
                // СНАЧАЛА проверяем наличие товаров в перемещении
                String emptyMovementError = checkForEmptyMovement(invoice);
                if (emptyMovementError != null) {
                    // Показываем ошибку о пустом перемещении
                    _showEmptyMovementErrorEvent.setValue(new SingleEvent<>(emptyMovementError));
                    Log.w("MoveListViewModel", "Переход в PrixodActivity отменен: " + emptyMovementError);
                    return;
                }
                
                // Затем проверяем товары на пустые УИДСтрокиТовары
                String emptyProductLineIdError = checkForEmptyProductLineIds(invoice);
                if (emptyProductLineIdError != null) {
                    // Показываем ошибку вместо перехода в PrixodActivity
                    _showEmptyProductLineIdErrorEvent.setValue(new SingleEvent<>(emptyProductLineIdError));
                    Log.w("MoveListViewModel", "Переход в PrixodActivity отменен: " + emptyProductLineIdError);
                    return;
                }
                
                Log.d("MoveListViewModel", "Проверка УИДСтрокиТовары пройдена для перемещения " + moveUuid + 
                      ", все товары имеют корректные идентификаторы");
                
                // Навигация к PrixodActivity без передачи данных
                _navigateToPrixodEvent.setValue(new SingleEvent<>(moveUuid));
            }

            @Override
            public void onError(Exception exception) {
                _isProcessingItemClick.setValue(false);
                String errorMessage = exception.getMessage();
                
                // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка", errorMessage)));
                } else {

                    String fullErrorMessage = "Ошибка при доступе к документу: " + errorMessage;
                    _errorMessage.setValue(fullErrorMessage);
                }
                
                Log.e("MoveListViewModel", "Ошибка при доступе к документу: " + errorMessage, exception);
            }
        });
    }

    // Методы для взаимодействия с PrixodActivity

    /**
     * Обрабатывает данные, возвращаемые из PrixodActivity
     */
    public void onReturnedFromPrixod(android.content.Intent data) {
        String moveUuid = null;
        String productsJsonData = null;
        boolean changeStatusToPodgotovlen = false;
        
        if (data != null) {
            // Получаем moveUuid из Intent
            moveUuid = data.getStringExtra("moveUuid");
            
            // Проверяем флаг смены статуса на "Подготовлен"
            changeStatusToPodgotovlen = data.getBooleanExtra("changeStatusToPodgotovlen", false);
            

            if (data.hasExtra("productData")) {
                productsJsonData = data.getStringExtra("productData");
                Log.d("MoveListViewModel", "onReturnedFromPrixod: получен productData, длина: " + 
                      (productsJsonData != null ? productsJsonData.length() : 0));
            } else if (data.hasExtra("productsData")) {
                productsJsonData = data.getStringExtra("productsData");
                Log.d("MoveListViewModel", "onReturnedFromPrixod: получен productsData, длина: " + 
                      (productsJsonData != null ? productsJsonData.length() : 0));
            }
            
            // Если нужно изменить статус на "Подготовлен", выполняем это действие
            if (changeStatusToPodgotovlen && moveUuid != null) {
                Log.d("MoveListViewModel", "Обнаружен флаг changeStatusToPodgotovlen для перемещения: " + moveUuid);
                changeSingleMoveStatusToPodgotovlen(moveUuid);
                return;
            }
            
            // Сохраняем данные в кэше только для статуса "Комплектуется"
            if (moveUuid != null && productsJsonData != null) {
                String signingStatus = getSigningStatusForMove(moveUuid);
                if (STATUS_KOMPLEKTUETSA.equals(signingStatus)) {
                    cachedMovementData.put(moveUuid, productsJsonData);
                    cachedPreserveEditedData.put(moveUuid, true); // Всегда true при возврате из PrixodActivity
                    
                    Log.d("MoveListViewModel", "Данные перемещения " + moveUuid + " в статусе 'Комплектуется' сохранены в кэше, размер: " + 
                          productsJsonData.length() + " символов");
                } else {
                    Log.d("MoveListViewModel", "Перемещение " + moveUuid + " в статусе '" + signingStatus + 
                          "' - данные НЕ сохраняются в кэше согласно новой логике");
                }
            }
            

            Log.d("MoveListViewModel", "onReturnedFromPrixod: данные перемещения сохранены в кэше по UUID: " + moveUuid);
        }
        
        // Запускаем фоновую проверку обновлений БЕЗ показа экрана загрузки
        Log.d("MoveListViewModel", "Запуск фоновой проверки обновлений после возврата из PrixodActivity");
        checkForDataUpdates(false);
    }

    /**
     * Выполняет смену статуса одного перемещения на "Подготовлен"
     * Аналогично btnFinishWork в MoveList_menu
     */
    private void changeSingleMoveStatusToPodgotovlen(String moveUuid) {
        Log.d("MoveListViewModel", "Начинаем смену статуса перемещения " + moveUuid + " на 'Подготовлен'");
        
        // Получаем GUID пользователя
        String userguid = getCurrentUserGuid();
        if (userguid == null) {
            _showToastEvent.setValue(new SingleEvent<>("Ошибка: пользователь не авторизован"));
            return;
        }
        
        // Проверяем онлайн-режим
        boolean isOnline = getOnlineModeUseCase.execute();
        if (!isOnline) {
            _showToastEvent.setValue(new SingleEvent<>("Смена статуса возможна только в онлайн режиме"));
            return;
        }
        
        // Найдем перемещение в списке "Комплектуется"
        MoveItem moveItem = findMoveItemInKomplektuetsaList(moveUuid);
        if (moveItem == null) {
            _showToastEvent.setValue(new SingleEvent<>("Перемещение не найдено в списке 'Комплектуется'"));
            return;
        }
        
        // Показываем индикатор загрузки
        _isLoading.postValue(true);
        
        // Загружаем данные продуктов из Realm для сохранения в 1С
        List<Product> products = localRealmDataSource.loadProducts(moveUuid);
        
        // Выполняем сохранение данных в 1С и затем смену статуса
        changeMoveStatusUseCase.executeWithDataSave(moveUuid, STATUS_PODGOTOVLEN, userguid, products, new RepositoryCallback<ChangeMoveStatusResult>() {
            @Override
            public void onSuccess(ChangeMoveStatusResult resultObj) {
                if (resultObj.isResult()) {
                    String moveNumber = moveItem.getNumber();
                    
                    // Удаляем все сохраненные данные при смене статуса
                    deleteAllDataForMovement(moveUuid);
                    
                    // Удаляем из списка "Комплектуется"
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        _originalKomplektuetsaList.setValue(removeByGuid(_originalKomplektuetsaList.getValue(), moveUuid));
                        triggerFilterRecalculation();
                    });
                    
                    // Обновляем список "Подготовлен"
                    String userGuid = getCurrentUserGuid();
                    Boolean availabilityFilterValue = availabilityChecked.getValue();
                    boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;
                    // Получаем значение фильтра по номенкулатуре и серии
                    String nomenculature = nomenculatureFilter.getValue();
                    String series = seriesFilter.getValue();

                    moveRepository.getMoveList(STATUS_PODGOTOVLEN, null, null, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
                        @Override
                        public void onSuccess(MoveResponse response) {
                            if (response != null && response.getItems() != null) {
                                List<MoveItem> sortedItems = new ArrayList<>(response.getItems());
                                sortByPriority(sortedItems);
                                _originalPodgotovlenList.postValue(sortedItems);
                            }
                            
                            String successMessage = "Статус для '" + moveNumber + "' успешно изменён на '" + STATUS_PODGOTOVLEN + "' и данные синхронизированы с 1С.";
                            _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                                new SuccessStatusChangeEvent(successMessage, STATUS_PODGOTOVLEN, moveUuid)
                            ));
                            _isLoading.postValue(false);
                        }
                        
                        @Override
                        public void onError(Exception exception) {
                            String errorMsg = "Ошибка при обновлении списка после смены статуса: " + exception.getMessage();
                            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка обновления", errorMsg)));
                            _isLoading.postValue(false);
                        }
                    });
                    
                } else {

                    String error = resultObj.getErrorText() != null && !resultObj.getErrorText().trim().isEmpty() 
                        ? resultObj.getErrorText() 
                        : "Неизвестная ошибка смены статуса на 'Подготовлен'";
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", error)));
                    _isLoading.postValue(false);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                String errorMessage = exception.getMessage();
                
                // Проверяем тип исключения - серверные ошибки (result = false) показываем в диалоге
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {

                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", errorMessage)));
                } else {

                    String fullErrorMessage = "Ошибка при смене статуса перемещения: " + errorMessage;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", fullErrorMessage)));
                }
                
                _isLoading.postValue(false);
                Log.e("MoveListViewModel", "Ошибка при смене статуса перемещения: " + errorMessage, exception);
            }
        });
    }
    
    /**
     * Находит перемещение в списке "Комплектуется" по UUID
     */
    private MoveItem findMoveItemInKomplektuetsaList(String moveUuid) {
        List<MoveItem> komplektuetsaList = _originalKomplektuetsaList.getValue();
        if (komplektuetsaList != null) {
            for (MoveItem item : komplektuetsaList) {
                if (moveUuid.equals(item.getMovementId())) {
                    return item;
                }
            }
        }
        return null;
    }

    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }



    
    /**
     * Очищает кэшированные данные для указанного перемещения
     */
    public void clearCachedDataForMovement(String moveUuid) {
        if (moveUuid != null) {
            cachedMovementData.remove(moveUuid);
            cachedPreserveEditedData.remove(moveUuid);
            Log.d("MoveListViewModel", "Кэшированные данные для перемещения " + moveUuid + " очищены");
        }
    }
    
    /**
     * Удаляет все сохраненные данные для указанного перемещения
     * (как в памяти, так и во временных файлах)
     * Используется при смене статуса перемещения
     */
    private void deleteAllDataForMovement(String moveUuid) {
        if (moveUuid == null || moveUuid.isEmpty()) {
            return;
        }
        
        // Удаляем данные из кэша в памяти
        clearCachedDataForMovement(moveUuid);
        

        
        // Удаляем все данные перемещения из Realm (продукты, серии, метаданные)
        localRealmDataSource.deleteMoveData(moveUuid);
        
        Log.d("MoveListViewModel", "Удаление всех сохраненных данных для перемещения " + moveUuid + 
              " из Realm: кэш очищен, серии и продукты удалены");
    }

    /**
     * Возвращает статус подписи перемещения по его UUID
     * @param moveUuid UUID перемещения
     * @return статус подписи или null, если не найден
     */
    public String getSigningStatusForMove(String moveUuid) {
        if (moveUuid == null || moveUuid.isEmpty()) {
            return null;
        }
        
        // Проверяем все три списка перемещений
        List<MoveItem> formirovanList = _originalFormirovanList.getValue();
        if (formirovanList != null) {
            for (MoveItem item : formirovanList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден статус подписи '" + item.getSigningStatus() + 
                          "' для перемещения " + moveUuid + " в списке 'Сформирован'");
                    return item.getSigningStatus();
                }
            }
        }
        
        List<MoveItem> komplektuetsaList = _originalKomplektuetsaList.getValue();
        if (komplektuetsaList != null) {
            for (MoveItem item : komplektuetsaList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден статус подписи '" + item.getSigningStatus() + 
                          "' для перемещения " + moveUuid + " в списке 'Комплектуется'");
                    return item.getSigningStatus();
                }
            }
        }
        
        List<MoveItem> podgotovlenList = _originalPodgotovlenList.getValue();
        if (podgotovlenList != null) {
            for (MoveItem item : podgotovlenList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден статус подписи '" + item.getSigningStatus() + 
                          "' для перемещения " + moveUuid + " в списке 'Подготовлен'");
                    return item.getSigningStatus();
                }
            }
        }
        
        Log.w("MoveListViewModel", "Не найден статус подписи для перемещения " + moveUuid);
        return null;
    }



    /**
     * Получает полный объект MoveItem по UUID перемещения
     * @param moveUuid UUID перемещения
     * @return MoveItem если найден, иначе null
     */
    public MoveItem getMoveItemByUuid(String moveUuid) {
        if (moveUuid == null || moveUuid.isEmpty()) {
            return null;
        }

        // Проверяем все три списка перемещений
        List<MoveItem> formirovanList = _originalFormirovanList.getValue();
        if (formirovanList != null) {
            for (MoveItem item : formirovanList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден MoveItem для перемещения " + moveUuid + " в списке 'Сформирован'");
                    return item;
                }
            }
        }

        List<MoveItem> komplektuetsaList = _originalKomplektuetsaList.getValue();
        if (komplektuetsaList != null) {
            for (MoveItem item : komplektuetsaList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден MoveItem для перемещения " + moveUuid + " в списке 'Комплектуется'");
                    return item;
                }
            }
        }

        List<MoveItem> podgotovlenList = _originalPodgotovlenList.getValue();
        if (podgotovlenList != null) {
            for (MoveItem item : podgotovlenList) {
                if (item != null && moveUuid.equals(item.getMovementId())) {
                    Log.d("MoveListViewModel", "Найден MoveItem для перемещения " + moveUuid + " в списке 'Подготовлен'");
                    return item;
                }
            }
        }

        Log.w("MoveListViewModel", "Не найден MoveItem для перемещения " + moveUuid);
        return null;
    }

    // Вспомогательный метод для удаления перемещения по guid
    private List<MoveItem> removeByGuid(List<MoveItem> list, String guid) {
        if (list == null) return null;
        List<MoveItem> newList = new ArrayList<>(list);
        newList.removeIf(item -> guid.equals(item.getMovementId()));
        return newList;
    }
    
    /**
     * Находит позицию элемента в отфильтрованном списке по GUID
     * @param guid GUID элемента для поиска
     * @param targetState статус (вкладка) для поиска
     * @return позицию элемента или -1 если не найден
     */
    public int findItemPositionByGuid(String guid, String targetState) {
        List<MoveItem> targetList = null;
        
        if (STATUS_FORMIROVAN.equals(targetState)) {
            targetList = _filteredFormirovanList.getValue();
        } else if (STATUS_KOMPLEKTUETSA.equals(targetState)) {
            targetList = _filteredKomplektuetsaList.getValue();
        } else if (STATUS_PODGOTOVLEN.equals(targetState)) {
            targetList = _filteredPodgotovlenList.getValue();
        }
        
        if (targetList == null || guid == null) {
            return -1;
        }
        
        for (int i = 0; i < targetList.size(); i++) {
            MoveItem item = targetList.get(i);
            if (item != null && guid.equals(item.getMovementId())) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Продолжить массовую обработку после закрытия диалога (успех/ошибка)
     */
    public void continueMultiMove() {
        if (!multiMoveActive || multiMoveQueue == null) return;
        multiMoveIndex++;
        if (multiMoveIndex < multiMoveQueue.size()) {
            // Сбрасываем индикатор загрузки перед обработкой следующего элемента
            _isLoading.postValue(false);
            // Запустить обработку следующего элемента
            processSingleMoveInMulti();
        } else {

            multiMoveActive = false;
            if (multiMoveSuccessGuids != null && !multiMoveSuccessGuids.isEmpty()) {

                String userGuid = getCurrentUserGuid();
                Boolean availabilityFilterValue = availabilityChecked.getValue();
                boolean useFilter = availabilityFilterValue != null ? availabilityFilterValue : true;

                // Получаем значение фильтра по номенкулатуре и серии
                String nomenculature = nomenculatureFilter.getValue();
                String series = seriesFilter.getValue();

                moveRepository.getMoveList(multiMoveTargetState, null, null, userGuid, useFilter, nomenculature, series, new RepositoryCallback<MoveResponse>() {
                    @Override
                    public void onSuccess(MoveResponse response) {
                        if (response != null && response.getItems() != null) {
                            List<MoveItem> sortedItems = new ArrayList<>(response.getItems());
                            sortByPriority(sortedItems);
                            if (STATUS_FORMIROVAN.equals(multiMoveTargetState)) {
                                _originalFormirovanList.postValue(sortedItems);
                            } else if (STATUS_KOMPLEKTUETSA.equals(multiMoveTargetState)) {
                                _originalKomplektuetsaList.postValue(sortedItems);
                            } else if (STATUS_PODGOTOVLEN.equals(multiMoveTargetState)) {
                                _originalPodgotovlenList.postValue(sortedItems);
                            }
                            
                            String firstGuid = multiMoveSuccessGuids.get(0);
                            int successCount = multiMoveSuccessGuids.size();
                            int totalCount = multiMoveQueue != null ? multiMoveQueue.size() : 0;
                            
                            // Проверяем была ли синхронизация с 1С
                            boolean wasDataSynced = STATUS_KOMPLEKTUETSA.equals(multiMoveSourceState) && STATUS_PODGOTOVLEN.equals(multiMoveTargetState);
                            String syncMessage = wasDataSynced ? " и данные синхронизированы с 1С" : "";
                            
                            String msg;
                            if (multiMoveErrorCount > 0) {
                                msg = "Массовая обработка завершена:\n" +
                                      "Успешно обработано: " + successCount + " перемещений\n" +
                                      "Неудачно обработано: " + multiMoveErrorCount + " перемещений\n" +
                                      "Статус успешно обработанных изменён на '" + multiMoveTargetState + "'" + syncMessage + ".";
                            } else {
                                msg = "Массовая обработка завершена:\n" +
                                      "Статус для всех выбранных перемещений (" + successCount + ") успешно изменён на '" + multiMoveTargetState + "'" + syncMessage + ".";
                            }
                            
                            _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                                new SuccessStatusChangeEvent(msg, multiMoveTargetState, firstGuid)
                            ));
                        }
                        _isLoading.postValue(false);
                        clearMultiMoveState();
                    }
                    @Override
                    public void onError(Exception exception) {
                        _errorMessage.postValue("Ошибка при обновлении списка: " + exception.getMessage());
                        _isLoading.postValue(false);
                        clearMultiMoveState();
                    }
                });
            } else {
                // Если не было успешных, показываем диалог об ошибке
                _isLoading.postValue(false);
                int totalCount = multiMoveQueue != null ? multiMoveQueue.size() : 0;
                String errorMsg = "Массовая обработка завершена:\n" +
                                  "Успешно обработано: 0 перемещений\n" +
                                  "Неудачно обработано: " + totalCount + " перемещений\n" +
                                  "Не удалось изменить статус ни у одного из выбранных перемещений.";
                _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка массовой смены статуса", errorMsg)));
                clearMultiMoveState();
            }
        }
    }
    
    /**
     * Очистка состояния массовой обработки
     */
    private void clearMultiMoveState() {
        multiMoveQueue = null;
        multiMoveSuccessGuids = null;
        multiMoveTargetState = null;
        multiMoveSourceState = null;
        multiMoveIndex = 0;
        multiMoveHadSuccess = false;
        multiMoveHadError = false;
        multiMoveErrorCount = 0;
    }

    /**
     * Получает GUID текущего пользователя
     * @return GUID пользователя или null если пользователь не авторизован
     */
    private String getCurrentUserGuid() {
        User currentUser = getUserUseCase.execute();
        if (currentUser != null && currentUser.getUserGuid() != null && !currentUser.getUserGuid().trim().isEmpty()) {
            Log.d("MoveListViewModel", "Используется GUID текущего пользователя: " + currentUser.getUserGuid());
            return currentUser.getUserGuid();
        }
        
        Log.e("MoveListViewModel", "Пользователь не авторизован или GUID отсутствует");
        return null;
    }

    /**
     * Внутренний запуск одного элемента из очереди массовой обработки
     */
    private void processSingleMoveInMulti() {
        if (multiMoveQueue == null || multiMoveIndex >= multiMoveQueue.size()) return;
        MoveItem item = multiMoveQueue.get(multiMoveIndex);
        String guid = item.getMovementId();
        String userguid = getCurrentUserGuid();
        
        // Проверяем авторизацию пользователя
        if (userguid == null) {
            Log.e("MoveListViewModel", "Прерывание массовой обработки: пользователь не авторизован");
            multiMoveHadError = true;
            multiMoveErrorCount = multiMoveQueue.size();
            
            // Показываем ошибку пользователю и завершаем массовую обработку
            _isLoading.postValue(false);
            String errorMsg = "Массовая обработка прервана:\n" +
                             "Пользователь не авторизован\n" +
                             "Для выполнения операции необходимо войти в систему.";
            _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка авторизации", errorMsg)));
            clearMultiMoveState();
            return;
        }
        
        _isLoading.postValue(true);
        
        // Проверяем нужно ли синхронизировать данные с 1С (только при переходе с "Комплектуется" на "Подготовлен")
        boolean needDataSync = STATUS_KOMPLEKTUETSA.equals(multiMoveSourceState) && STATUS_PODGOTOVLEN.equals(multiMoveTargetState);
        
        if (needDataSync) {
            // Загружаем данные продуктов из Realm для сохранения в 1С
            List<Product> products = localRealmDataSource.loadProducts(guid);

            changeMoveStatusUseCase.executeWithDataSave(guid, multiMoveTargetState, userguid, products, new RepositoryCallback<ChangeMoveStatusResult>() {
            @Override
            public void onSuccess(ChangeMoveStatusResult resultObj) {
                if (resultObj.isResult()) {
                    multiMoveHadSuccess = true;
                    if (multiMoveSuccessGuids != null) multiMoveSuccessGuids.add(guid);
                    
                    // Удаляем все сохраненные данные при смене статуса
                    deleteAllDataForMovement(guid);
                    
                    // Удалить из исходного списка
                    if (STATUS_FORMIROVAN.equals(multiMoveSourceState)) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            _originalFormirovanList.setValue(removeByGuid(_originalFormirovanList.getValue(), guid));
                            triggerFilterRecalculation();
                        });
                    } else if (STATUS_KOMPLEKTUETSA.equals(multiMoveSourceState)) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            _originalKomplektuetsaList.setValue(removeByGuid(_originalKomplektuetsaList.getValue(), guid));
                            triggerFilterRecalculation();
                        });
                    } else if (STATUS_PODGOTOVLEN.equals(multiMoveSourceState)) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            _originalPodgotovlenList.setValue(removeByGuid(_originalPodgotovlenList.getValue(), guid));
                            triggerFilterRecalculation();
                        });
                    }

                    String successMessage = "Статус для '" + item.getNumber() + "' успешно изменён на '" + multiMoveTargetState + "' и данные синхронизированы с 1С.";
                    _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                        new SuccessStatusChangeEvent(successMessage, multiMoveTargetState, guid)
                    ));

                } else {
                    multiMoveHadError = true;
                    multiMoveErrorCount++;
                    String error = resultObj.getErrorText() != null && !resultObj.getErrorText().trim().isEmpty() 
                        ? resultObj.getErrorText() 
                        : "Неизвестная ошибка сохранения данных в 1С";
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", error)));
                }
                
                // Переходим к следующему элементу
                continueMultiMove();
            }
            @Override
            public void onError(Exception exception) {
                multiMoveHadError = true;
                multiMoveErrorCount++;
                
                String errorMessage = exception.getMessage();
                
                // ИСПРАВЛЕНИЕ: Проверяем тип исключения для правильного отображения серверных ошибок
                if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                    com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                        (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка массовой смены статуса", errorMessage)));
                } else {

                    String fullErrorMessage = "Ошибка при смене статуса или сохранении данных в 1С: " + errorMessage;
                    _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка массовой смены статуса", fullErrorMessage)));
                }

                continueMultiMove();
                
                Log.e("MoveListViewModel", "Ошибка при массовой смене статуса", exception);
            }
        });
        } else {
            // Обычная смена статуса без синхронизации с 1С
            changeMoveStatusUseCase.execute(guid, multiMoveTargetState, userguid, new RepositoryCallback<ChangeMoveStatusResult>() {
                @Override
                public void onSuccess(ChangeMoveStatusResult resultObj) {
                    if (resultObj.isResult()) {
                        multiMoveHadSuccess = true;
                        if (multiMoveSuccessGuids != null) multiMoveSuccessGuids.add(guid);
                        
                        // Удаляем все сохраненные данные при смене статуса
                        deleteAllDataForMovement(guid);
                        
                        if (STATUS_FORMIROVAN.equals(multiMoveSourceState)) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                _originalFormirovanList.setValue(removeByGuid(_originalFormirovanList.getValue(), guid));
                                triggerFilterRecalculation();
                            });
                        } else if (STATUS_KOMPLEKTUETSA.equals(multiMoveSourceState)) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                _originalKomplektuetsaList.setValue(removeByGuid(_originalKomplektuetsaList.getValue(), guid));
                                triggerFilterRecalculation();
                            });
                        } else if (STATUS_PODGOTOVLEN.equals(multiMoveSourceState)) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                _originalPodgotovlenList.setValue(removeByGuid(_originalPodgotovlenList.getValue(), guid));
                                triggerFilterRecalculation();
                            });
                        }
                        
                        // Показываем диалог успеха (без сообщения о синхронизации)
                        String successMessage = "Статус для '" + item.getNumber() + "' успешно изменён на '" + multiMoveTargetState + "'.";
                        _showSuccessStatusChangeEvent.postValue(new SingleEvent<>(
                            new SuccessStatusChangeEvent(successMessage, multiMoveTargetState, guid)
                        ));
                    } else {
                        multiMoveHadError = true;
                        multiMoveErrorCount++;
                        String error = resultObj.getErrorText() != null && !resultObj.getErrorText().trim().isEmpty() 
                            ? resultObj.getErrorText() 
                            : "Неизвестная ошибка смены статуса";
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка смены статуса перемещения", error)));
                    }

                    continueMultiMove();
                }
                @Override
                public void onError(Exception exception) {
                    multiMoveHadError = true;
                    multiMoveErrorCount++;
                    
                    String errorMessage = exception.getMessage();
                    
                    // ИСПРАВЛЕНИЕ: Проверяем тип исключения для правильного отображения серверных ошибок
                    if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) {

                        com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException serverError = 
                            (com.step.tcd_rpkb.data.exceptions.ServerErrorWithTypeException) exception;
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData(serverError.getDialogTitle(), errorMessage)));
                    } else if (exception instanceof com.step.tcd_rpkb.data.exceptions.ServerErrorException) {
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка массовой смены статуса", errorMessage)));
                    } else {
                        String fullErrorMessage = "Ошибка при смене статуса: " + errorMessage;
                        _showErrorDialogEvent.postValue(new SingleEvent<>(new ErrorDialogData("Ошибка массовой смены статуса", fullErrorMessage)));
                    }
                    

                    continueMultiMove();
                    
                    Log.e("MoveListViewModel", "Ошибка при массовой смене статуса: " + errorMessage, exception);
                }
            });
        }
    }

    /**
     * Проверяет товары в документе на пустые УИДСтрокиТовары
     * @param invoice документ для проверки
     * @return сообщение об ошибке если найдены товары с пустыми УИДСтрокиТовары, иначе null
     */
    private String checkForEmptyProductLineIds(Invoice invoice) {
        if (invoice == null || invoice.getProducts() == null || invoice.getProducts().isEmpty()) {
            Log.d("MoveListViewModel", "checkForEmptyProductLineIds: документ пуст или товаров нет");
            return null;
        }
        
        List<Product> products = invoice.getProducts();
        int emptyProductLineIdCount = 0;
        String firstEmptyProductName = null;
        
        for (Product product : products) {
            if (product == null) continue;
            
            String productLineId = product.getProductLineId();
            if (productLineId == null || productLineId.trim().isEmpty()) {
                emptyProductLineIdCount++;
                if (firstEmptyProductName == null) {
                    firstEmptyProductName = product.getNomenclatureName();
                    if (firstEmptyProductName == null || firstEmptyProductName.trim().isEmpty()) {
                        firstEmptyProductName = "Неизвестный товар";
                    }
                }
                
                Log.w("MoveListViewModel", "Найден товар с пустым УИДСтрокиТовары: " + 
                      product.getNomenclatureName() + " (nomenclatureUuid: " + product.getNomenclatureUuid() + ")");
            }
        }
        
        if (emptyProductLineIdCount > 0) {
            String errorMessage;
            if (emptyProductLineIdCount == 1) {
                errorMessage = "В перемещении найден товар с пустым полем \"УИДСтрокиТовары\":\n\n" +
                              firstEmptyProductName + "\n\n" +
                              "Данное перемещение нельзя обработать.";
            } else {
                errorMessage = "В перемещении найдено " + emptyProductLineIdCount + 
                              " товаров с пустым полем \"УИДСтрокиТовары\".\n\n" +
                              "Первый из них: " + firstEmptyProductName + "\n\n" +
                              "Данное перемещение нельзя обработать.";
            }
            
            Log.w("MoveListViewModel", "checkForEmptyProductLineIds: найдено " + emptyProductLineIdCount + 
                  " товаров с пустыми УИДСтрокиТовары из " + products.size() + " товаров");
            
            return errorMessage;
        }
        
        Log.d("MoveListViewModel", "checkForEmptyProductLineIds: все " + products.size() + 
              " товаров имеют корректные УИДСтрокиТовары");
        return null;
    }
    
    /**
     * Проверяет перемещение на наличие товаров
     * @param invoice документ для проверки
     * @return сообщение об ошибке если перемещение пустое, иначе null
     */
    private String checkForEmptyMovement(Invoice invoice) {
        if (invoice == null) {
            Log.d("MoveListViewModel", "checkForEmptyMovement: документ null");
            return "Перемещение не содержит данных и не может быть обработано.";
        }
        
        List<Product> products = invoice.getProducts();
        if (products == null || products.isEmpty()) {
            Log.w("MoveListViewModel", "checkForEmptyMovement: перемещение не содержит товаров");
            return "Перемещение не содержит товаров и не может быть обработано.";
        }
        
        Log.d("MoveListViewModel", "checkForEmptyMovement: перемещение содержит " + products.size() + " товаров");
        return null;
    }
    
    /**
     * Сохраняет текущие фильтры как фильтры по умолчанию для текущего пользователя
     */
    public void saveCurrentFiltersAsDefault() {
        User currentUser = getUserUseCase.execute();
        if (currentUser == null || currentUser.getUserGuid() == null || currentUser.getUserGuid().trim().isEmpty()) {
            _showDefaultFiltersMessageEvent.setValue(new SingleEvent<>("Ошибка: пользователь не авторизован"));
            Log.e("MoveListViewModel", "Не удается сохранить фильтры по умолчанию: пользователь не авторизован");
            return;
        }
        
        String userGuid = currentUser.getUserGuid();
        String userName = currentUser.getFullName();
        
        try {
            // Создаем объект с текущими фильтрами
            DefaultFiltersData filtersData = new DefaultFiltersData(userGuid, userName);
            
            // Получаем текущие значения единых фильтров
            filtersData.setSender(senderFilter.getValue() != null ? senderFilter.getValue() : "");
            filtersData.setMovementNumber(movementNumberFilter.getValue() != null ? movementNumberFilter.getValue() : "");
            filtersData.setNomenculature(nomenculatureFilter.getValue() != null ? nomenculatureFilter.getValue() : "");
            filtersData.setSeries(seriesFilter.getValue() != null ? seriesFilter.getValue() : "");
            filtersData.setRecipient(recipientFilter.getValue() != null ? recipientFilter.getValue() : "");
            filtersData.setAssembler(assemblerFilter.getValue() != null ? assemblerFilter.getValue() : "");
            filtersData.setPriority(priorityFilter.getValue() != null ? priorityFilter.getValue() : "");
            filtersData.setReceiver(receiverFilter.getValue() != null ? receiverFilter.getValue() : "");
            filtersData.setCpsChecked(cpsChecked.getValue() != null ? cpsChecked.getValue() : true);
            filtersData.setAvailabilityChecked(availabilityChecked.getValue() != null ? availabilityChecked.getValue() : true);
            
            // Сохраняем фильтры
            boolean success = defaultFiltersManager.saveDefaultFilters(userGuid, userName, filtersData);
            
            if (success) {
                _showDefaultFiltersMessageEvent.setValue(new SingleEvent<>("Фильтры по умолчанию успешно сохранены для пользователя: " + userName));
                Log.d("MoveListViewModel", "Фильтры по умолчанию сохранены для пользователя: " + userName + " (GUID: " + userGuid + ")");
            } else {
                _showDefaultFiltersMessageEvent.setValue(new SingleEvent<>("Ошибка при сохранении фильтров по умолчанию"));
                Log.e("MoveListViewModel", "Ошибка при сохранении фильтров по умолчанию для пользователя: " + userName);
            }
            
        } catch (Exception e) {
            _showDefaultFiltersMessageEvent.setValue(new SingleEvent<>("Ошибка при сохранении фильтров: " + e.getMessage()));
            Log.e("MoveListViewModel", "Исключение при сохранении фильтров по умолчанию", e);
        }
    }
    
    /**
     * Загружает фильтры по умолчанию для текущего пользователя
     */
    public void loadDefaultFiltersForCurrentUser() {
        User currentUser = getUserUseCase.execute();
        if (currentUser == null || currentUser.getUserGuid() == null || currentUser.getUserGuid().trim().isEmpty()) {
            Log.d("MoveListViewModel", "Не удается загрузить фильтры по умолчанию: пользователь не авторизован");
            return;
        }
        
        String userGuid = currentUser.getUserGuid();
        
        try {
            DefaultFiltersData filtersData = defaultFiltersManager.loadDefaultFiltersForUser(userGuid);
            
            if (filtersData != null) {
                Log.d("MoveListViewModel", "Загружены фильтры по умолчанию для пользователя: " + filtersData.getUserName());
                
                // Применяем единые фильтры
                savedStateHandle.set(KEY_SENDER, filtersData.getSender());
                savedStateHandle.set(KEY_MOVEMENT_NUMBER, filtersData.getMovementNumber());
                savedStateHandle.set(KEY_NOMENCULATURE, filtersData.getNomenculature());
                savedStateHandle.set(KEY_SERIES, filtersData.getSeries());
                savedStateHandle.set(KEY_RECIPIENT, filtersData.getRecipient());
                savedStateHandle.set(KEY_ASSEMBLER, filtersData.getAssembler());
                savedStateHandle.set(KEY_PRIORITY, filtersData.getPriority());
                savedStateHandle.set(KEY_RECEIVER, filtersData.getReceiver());
                savedStateHandle.set(KEY_CPS_CHECKED, filtersData.isCpsChecked());
                savedStateHandle.set(KEY_AVAILABILITY_CHECKED, filtersData.isAvailabilityChecked());
                
                // Пересчитываем фильтры
                triggerFilterRecalculation();
                
                Log.d("MoveListViewModel", "Фильтры по умолчанию успешно применены для пользователя: " + filtersData.getUserName());
            } else {
                Log.d("MoveListViewModel", "Фильтры по умолчанию не найдены для пользователя с GUID: " + userGuid);
            }
            
        } catch (Exception e) {
            Log.e("MoveListViewModel", "Ошибка при загрузке фильтров по умолчанию для пользователя: " + userGuid, e);
        }
    }

} 