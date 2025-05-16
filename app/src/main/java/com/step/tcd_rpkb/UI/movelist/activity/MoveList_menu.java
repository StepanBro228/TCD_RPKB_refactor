package com.step.tcd_rpkb.UI.movelist.activity;

import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_FORMIROVAN;
import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_KOMPLEKTUETSA;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider; // Добавляем импорт

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.step.tcd_rpkb.UI.Prixod.PrixodActivity;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.User; // Добавляем импорт User
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.movelist.fragments.MoveListFragment;
import com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel; // Добавляем импорт
import com.step.tcd_rpkb.ViewPagerAdapter;
import com.step.tcd_rpkb.utils.AvatarUtils; // Добавляем импорт AvatarUtils
import com.step.tcd_rpkb.utils.UserViewAnimations; // Добавляем импорт UserViewAnimations
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase; // Добавляем импорт GetUserUseCase
import com.step.tcd_rpkb.data.repository.UserRepositoryImpl; // Добавляем импорт UserRepositoryImpl
import com.step.tcd_rpkb.domain.repository.UserRepository; // Добавляем импорт UserRepository
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase; // Уже должен быть, но для явности

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject; // Импорт Inject

import dagger.hilt.android.AndroidEntryPoint;
import com.step.tcd_rpkb.UI.movelist.viewmodel.SingleEvent; // Добавляем импорт для SingleEvent, если он еще не виден

@AndroidEntryPoint
public class MoveList_menu extends com.step.tcd_rpkb.base.BaseFullscreenActivity {
    private MoveListViewModel moveListViewModel;

    @Inject // Внедряем GetUserUseCase
    GetUserUseCase getUserUseCase;
    
    private MoveListFragment formirovanFragment;
    private MoveListFragment komplektuetsaFragment;
    // private int currentTabPosition = 0; // Перемещено в ViewModel

    private DrawerLayout drawerLayout;
    private FloatingActionButton navMenuButton;
    private NavigationView navigationView;
    private View filterIndicator;
    
    // ViewPager2 для свайпов между фрагментами
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    
    // Компоненты для фильтрации
    private EditText filterSender;
    private EditText filterMovementNumber;
    private EditText filterRecipient;
    private EditText filterAssembler;
    private Spinner filterPriority;
    private EditText filterReceiver;
    
    private CheckBox filterCps;
    private CheckBox filterAvailability;
    
    private ImageButton clearSender;
    private ImageButton clearMovementNumber;
    private ImageButton clearRecipient;
    private ImageButton clearAssembler;
    private ImageButton clearPriority;
    private ImageButton clearReceiver;
    
    private TextView btnResetFilters;
    
    // TextView для отображения ФИО пользователя и роли
    private TextView userFullNameTextView;
    private TextView userRoleTextView;
    private ImageView userAvatarImageView;
    // Добавляем наши новые переключатели
    private TextView segmentFormirovano;
    private TextView segmentKomplektuetsa;
    

    
    // Для оптимизации фильтрации в реальном времени
    private final Handler filterHandler = new Handler(Looper.getMainLooper()); // Используем Looper.getMainLooper()
    private static final long FILTER_DELAY_MS = 300; // 300 мс задержка
    private Runnable filterRunnable;

    // Для отображения статистики приоритетов
    private TextView priorityStatsTextView;

    // Добавляем константы для типов перемещения элементов
    private static final int MOVE_TO_KOMPLEKTUETSA = 1;
    private static final int MOVE_TO_FORMIROVAN = 2;

    // Для хранения информации о показанной подсказке
    private static final String PREF_MOVE_HINT_SHOWN = "move_hint_shown";

    // Компоненты панели множественного выбора
    private CardView actionButtonsPanel;
    private TextView selectedItemsCount;
    private Button btnMoveToWork;
    private Button btnMoveFromWork;
    private Button btnCancelSelection;
    
    // Флаг, определяющий режим выбора элементов
    private boolean isSelectionMode = false;
    

    private FloatingActionButton refreshButton;
    // Для фоновых операций


    // Индикатор загрузки
    private com.step.tcd_rpkb.utils.LoadingDialog loadingDialog;
    
    // Счетчик активных загрузок для корректного управления диалогом загрузки
    private int activeLoadingCount = 0;
    private final Object loadingLock = new Object();

    /**
     * Возвращает текущую позицию вкладки
     * @return индекс текущей вкладки (0 - "Сформирован", 1 - "Комплектуется")
     */
    public int getCurrentTabPosition() {
        // return currentTabPosition; // Получаем из ViewModel
        Integer tabPosition = moveListViewModel.currentTabPosition.getValue();
        return tabPosition != null ? tabPosition : 0;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movelist_menu);
        
        // Инициализация ViewModel через Hilt
        moveListViewModel = new ViewModelProvider(this).get(MoveListViewModel.class);
        
        loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(this);
        
        // Проверяем, пришли ли мы из Prixod с флагом restoreFilters
        boolean restoreFromIntent = getIntent().getBooleanExtra("restoreFilters", false);
        if (restoreFromIntent) {
            Log.d("MoveList_menu", "onCreate: Обнаружен флаг restoreFilters в Intent. ViewModel управляет состоянием.");
        }
        
        // Регистрируем глобальный обработчик изменения размеров окна (для клавиатуры)
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // Оценка высоты клавиатуры через определение разницы между высотой окна и содержимого
            View decorView = getWindow().getDecorView();
            Rect r = new Rect();
            decorView.getWindowVisibleDisplayFrame(r);
            int screenHeight = decorView.getHeight();
            
            // Если разница больше 25% экрана, считаем что клавиатура показана
            int keyboardHeight = screenHeight - r.bottom;
            boolean isKeyboardShowing = keyboardHeight > screenHeight * 0.25;
            
            if (isKeyboardShowing) {
                // Если клавиатура показана, и есть фокус на поле ввода
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {
                    // Задержка необходима, чтобы анимация открытия клавиатуры завершилась
                    focusedView.postDelayed(() -> scrollToView(focusedView), 300);
                }
            }
        });
        
        // Инициализация глобального обработчика нажатий для контроля клавиатуры
        getWindow().getDecorView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Если нажата клавиша Back и боковое меню открыто
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        // Убираем фокус и клавиатуру перед закрытием
                        clearFocus();
                        hideKeyboard();
                    }
                }
                return false;
            }
        });
        
        // Инициализация основных компонентов UI
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navMenuButton = findViewById(R.id.nav_menu_button);
        filterIndicator = findViewById(R.id.filter_indicator);
        viewPager = findViewById(R.id.viewPager);
        
        // Инициализация компонентов фильтрации
        initFilterComponents();
        
        // Получаем ссылки на элементы в NavigationView
        View headerView = navigationView.findViewById(R.id.nav_header);
        if (headerView != null) {
            View userCard = headerView.findViewById(R.id.user_name_card);
            if (userCard != null) {
                userFullNameTextView = userCard.findViewById(R.id.user_full_name);
                userRoleTextView = userCard.findViewById(R.id.user_role);
                userAvatarImageView = userCard.findViewById(R.id.user_avatar);
                
                // UserRepository userRepository = new UserRepositoryImpl(getApplicationContext());
                // GetUserUseCase getUserUseCaseLocal = new GetUserUseCase(userRepository); // Локальное создание удаляем
                User currentUser = getUserUseCase.execute(); // Используем внедренный getUserUseCase

                if (currentUser != null && userFullNameTextView != null && userRoleTextView != null && userAvatarImageView != null) {
                    userFullNameTextView.setText(currentUser.getFullName());
                    userRoleTextView.setText(currentUser.getRole());
                    String initials = AvatarUtils.getInitials(currentUser.getFullName());
                    userAvatarImageView.setImageDrawable(AvatarUtils.createTextAvatar(this, initials));
                
                // Запускаем крутую анимацию для карточки пользователя

                    UserViewAnimations.playFancyAnimation(userCard, userAvatarImageView, userFullNameTextView, userRoleTextView);
                
                // После задержки добавляем эффект мерцания для аватара

                    new Handler(Looper.getMainLooper()).postDelayed(() -> { // Используем Looper.getMainLooper() для Handler
                        if (userAvatarImageView != null) { // Проверка на случай, если Activity уже уничтожена
                           UserViewAnimations.addSparkleEffect(userAvatarImageView);
                        }
                }, 2000);
                }
                //
            }
        }
        
        // Настройка навигационной шторки
        setupNavigationView();
        
        // Скрытие клавиатуры при нажатии вне поля ввода
        setupTouchListener();
        
        // Добавляем обработчик для основного контейнера, чтобы закрывать боковое меню и клавиатуру
        findViewById(R.id.main).setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
                hideKeyboard();
                clearFocus();
            }
        });
        
        // Инициализация новых сегментов
        segmentFormirovano = findViewById(R.id.segment_formirovano);
        segmentKomplektuetsa = findViewById(R.id.segment_komplektuetsa);
        
        // Явно устанавливаем состояние выбора для первого сегмента
        segmentFormirovano.setSelected(true);
        segmentKomplektuetsa.setSelected(false);
        
        // Предотвращаем получение фокуса сегментами
        preventSegmentsFocus();
        
        // Настройка тулбара
        setupSegmentedControl();
        
        // Инициализация фрагментов
        formirovanFragment = MoveListFragment.newInstance(STATUS_FORMIROVAN);
        komplektuetsaFragment = MoveListFragment.newInstance(STATUS_KOMPLEKTUETSA);
        
        // Настройка ViewPager и табов
        setupViewPager();
        
        // Предотвращение появления клавиатуры при закрытии шторки
        preventKeyboardShowOnDrawerClose();
        
        
        // Подписываемся на LiveData из ViewModel для обновления UI фильтров при их изменении
        observeFilterViewModel();
        

        
        // Настраиваем глобальный слушатель изменения фокуса для отслеживания фокуса между всеми EditText полями
        setupGlobalFocusChangeListener();
        
        Log.d("MoveList_menu", "onCreate: Инициализация завершена, текущая вкладка: " + getCurrentTabPosition());
        
        // Гарантируем, что поле filter_movement_number будет clickable и focusable
        if (filterMovementNumber != null) {
            filterMovementNumber.setFocusable(true);
            filterMovementNumber.setFocusableInTouchMode(true);
            filterMovementNumber.setClickable(true);
            
            // Добавляем возможность запрашивать фокус по клику
            filterMovementNumber.setOnClickListener(v -> {
                v.requestFocus();
                // Отключаем клавиатуру
                disableKeyboardForNumericField(filterMovementNumber);
            });
        }
        
        // Показываем подсказку о новой функциональности выбора элементов
        showMoveItemHintIfNeeded();
        
        // Настраивает панель множественного выбора
        setupSelectionPanel();
        
        // Инициализируем кнопку обновления (изначально скрыта)
        setupRefreshButton();
        
        // Загружаем данные после завершения инициализации UI
        // Добавляем небольшую задержку для корректного отображения индикатора загрузки
        new Handler().postDelayed(() -> {
            // Загружаем данные из JSON и обновляем фрагменты

            moveListViewModel.loadMoveData(); // Загружаем данные через ViewModel
        }, 150); // 150 мс задержка

        // Подписываемся на LiveData из ViewModel
        observeViewModel();
    }

    private void observeViewModel() {
        // Наблюдение за состоянием загрузки
        moveListViewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                loadingDialog.show();
                } else {
                loadingDialog.dismiss();
            }
        });

        // Наблюдение за сообщениями об ошибках
        moveListViewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                // Можно очистить ошибку в ViewModel после отображения, если это необходимо
                // moveListViewModel.clearErrorMessage(); 
            }
        });

        // Наблюдение за списками (оригинальными)
        // Фильтрация и сортировка происходят во ViewModel или через triggerFilterRecalculation
        // Фрагменты будут наблюдать за filteredFormirovanList и filteredKomplektuetsaList
        moveListViewModel.originalFormirovanList.observe(this, list -> {
            Log.d("MoveList_menu", "Original Formirovan list updated in ViewModel, size: " + (list != null ? list.size() : 0));
            // applyFiltersToFragment(); // Устарело, логика фильтрации теперь в ViewModel
            // Обновление UI, если нужно напрямую реагировать на изменение оригинального списка
            // Например, если какая-то общая статистика считается по оригинальному списку
            updateFragmentsAfterMoving(); // Обновляем статистику и UI после загрузки/обновления данных

        });

        moveListViewModel.originalKomplektuetsaList.observe(this, list -> {
            Log.d("MoveList_menu", "Original Komplektuetsa list updated in ViewModel, size: " + (list != null ? list.size() : 0));
            // applyFiltersToFragment(); // Устарело
            updateFragmentsAfterMoving(); // Обновляем статистику и UI после загрузки/обновления данных
        });
        
        // Наблюдение за необходимостью показать/скрыть кнопку обновления
        moveListViewModel.showRefreshButton.observe(this, show -> {
            if (refreshButton != null) {
                refreshButton.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        // Наблюдение за активностью фильтров для обновления индикатора
        moveListViewModel.isAnyFilterActiveLive.observe(this, isActive -> {
            if (filterIndicator != null) {
                filterIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
            }
        });

        // Наблюдение за событием навигации в PrixodActivity
        moveListViewModel.navigateToPrixodEvent.observe(this, (SingleEvent<String> event) -> { // Явно указываем тип event
            if (event != null) {
                String movementId = event.getContentIfNotHandled(); // Получаем ID, если событие не обработано
                if (movementId != null) {
                    Log.d("MoveList_menu", "Получено событие navigateToPrixodEvent для movementId: " + movementId);
                    // persistCurrentFiltersToStaticState(); // Удаляем вызов, ViewModel сохраняет состояние автоматически

                    android.content.Intent intent = new android.content.Intent(MoveList_menu.this, PrixodActivity.class);
                    intent.putExtra("moveUuid", movementId);
                    
                    // Получаем данные из ViewModel для передачи в PrixodActivity
                    String jsonData = moveListViewModel.getProductsJsonForPrixod();
                    boolean preserveData = moveListViewModel.shouldPreserveEditedDataForPrixod();

                    if (jsonData != null) {
                        intent.putExtra("productsData", jsonData);
                        intent.putExtra("preserveEditedData", preserveData);
                        Log.d("MoveList_menu", "Передача сохраненных данных товаров в Prixod из ViewModel");
                    }
                    
                    startActivityForResult(intent, 1001);
                    moveListViewModel.clearPrixodReturnData(); // Очищаем данные в ViewModel после использования
                }
            }
        });
        
        moveListViewModel.showToastEvent.observe(this, (SingleEvent<String> event) -> { // Явно указываем тип event
            if (event != null) {
                String message = event.getContentIfNotHandled();
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(MoveList_menu.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        moveListViewModel.isProcessingItemClick.observe(this, isProcessing -> {
            if (isProcessing != null) {
                if (isProcessing) {
                    loadingDialog.show("Проверка документа..."); // Используем другой текст для этого индикатора
        } else {
                loadingDialog.dismiss();
            }
        }
        });
    }

    private void observeFilterViewModel() {
        // Обновляем UI фильтров при изменении соответствующего LiveData в ViewModel
        moveListViewModel.currentTabPosition.observe(this, position -> {
            if (position != null) {
                updateFiltersUi(); // Обновить UI при смене вкладки
                updateSegmentSelection(position); // Обновить выделение сегмента
                // Дополнительные действия при смене вкладки, если нужны
            }
        });

        // Для вкладки "Сформирован"
        moveListViewModel.formirovanSenderFilter.observe(this, value -> { if (getCurrentTabPosition() == 0) filterSender.setText(value); });
        moveListViewModel.formirovanMovementNumberFilter.observe(this, value -> { if (getCurrentTabPosition() == 0) filterMovementNumber.setText(value); });
        moveListViewModel.formirovanRecipientFilter.observe(this, value -> { if (getCurrentTabPosition() == 0) filterRecipient.setText(value); });
        moveListViewModel.formirovanAssemblerFilter.observe(this, value -> { if (getCurrentTabPosition() == 0) filterAssembler.setText(value); });
        moveListViewModel.formirovanReceiverFilter.observe(this, value -> { if (getCurrentTabPosition() == 0) filterReceiver.setText(value); });
        moveListViewModel.formirovanPriorityFilter.observe(this, value -> {
            if (getCurrentTabPosition() == 0) setSpinnerSelection(filterPriority, value);
        });
        moveListViewModel.formirovanCpsChecked.observe(this, value -> { if (getCurrentTabPosition() == 0 && value != null) filterCps.setChecked(value); });
        moveListViewModel.formirovanAvailabilityChecked.observe(this, value -> { if (getCurrentTabPosition() == 0 && value != null) filterAvailability.setChecked(value); });

        // Для вкладки "Комплектуется"
        moveListViewModel.komplektuetsaSenderFilter.observe(this, value -> { if (getCurrentTabPosition() == 1) filterSender.setText(value); });
        moveListViewModel.komplektuetsaMovementNumberFilter.observe(this, value -> { if (getCurrentTabPosition() == 1) filterMovementNumber.setText(value); });
        moveListViewModel.komplektuetsaRecipientFilter.observe(this, value -> { if (getCurrentTabPosition() == 1) filterRecipient.setText(value); });
        moveListViewModel.komplektuetsaAssemblerFilter.observe(this, value -> { if (getCurrentTabPosition() == 1) filterAssembler.setText(value); });
        moveListViewModel.komplektuetsaReceiverFilter.observe(this, value -> { if (getCurrentTabPosition() == 1) filterReceiver.setText(value); });
        moveListViewModel.komplektuetsaPriorityFilter.observe(this, value -> {
            if (getCurrentTabPosition() == 1) setSpinnerSelection(filterPriority, value);
        });
        moveListViewModel.komplektuetsaCpsChecked.observe(this, value -> { if (getCurrentTabPosition() == 1 && value != null) filterCps.setChecked(value); });
        moveListViewModel.komplektuetsaAvailabilityChecked.observe(this, value -> { if (getCurrentTabPosition() == 1 && value != null) filterAvailability.setChecked(value); });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner == null || spinner.getAdapter() == null) return;
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value != null && value.equals(adapter.getItem(i).toString())) {
                if (spinner.getSelectedItemPosition() != i) {
                    spinner.setSelection(i);
                }
                return;
            }
        }
        if (spinner.getSelectedItemPosition() != 0) { // Если значение не найдено, выбираем "Все" (первый элемент)
             spinner.setSelection(0);
        }
    }
    


    /**
     * Предотвращает появление клавиатуры при закрытии NavigationView
     */
    private void preventKeyboardShowOnDrawerClose() {
        // Получаем текущий фокус перед действиями
        View currentFocus = getCurrentFocus();
        boolean wasOnMovementNumber = (currentFocus == filterMovementNumber);
        
        // Забираем фокус у EditText, кроме filter_movement_number
        if (filterSender != null && currentFocus != filterMovementNumber) filterSender.clearFocus();
        // НЕ удаляем фокус с filterMovementNumber
        if (filterRecipient != null) filterRecipient.clearFocus();
        if (filterAssembler != null) filterAssembler.clearFocus();
        if (filterReceiver != null) filterReceiver.clearFocus();
        
        // Если фокус НЕ был на filter_movement_number, устанавливаем фокус на корневой контейнер
        if (!wasOnMovementNumber) {
            View rootView = findViewById(R.id.main);
            if (rootView != null) {
                rootView.requestFocus();
            }
        }
        
        // Принудительно скрываем клавиатуру
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        } else {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
        
        // Если фокус был на filter_movement_number, повторно запрашиваем фокус
        if (wasOnMovementNumber && filterMovementNumber != null) {
            // Небольшая задержка для надежности
            new Handler().postDelayed(() -> {
                filterMovementNumber.requestFocus();
                disableKeyboardForNumericField(filterMovementNumber);
            }, 100);
        }
        
        // Восстанавливаем позицию прокрутки NavigationView наверх
        NestedScrollView scrollView = navigationView.findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            // Используем плавную прокрутку с небольшой задержкой
            scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, 0), 100);
        }
        
        // При закрытии NavigationView сохраняем текущие фильтры
        // saveCurrentFilters(); // Больше не нужно, значения обновляются в ViewModel по мере ввода
    }

    /**
     * Обновляет UI фильтров на основе текущего состояния фильтров
     */
    private void updateFiltersUi() {
        // Обновляем UI элементы в соответствии с текущими значениями фильтров из ViewModel
        int tabPos = getCurrentTabPosition();
        Log.d("MoveList_menu", "updateFiltersUi for tab: " + tabPos);

        if (tabPos == 0) {
            filterSender.setText(moveListViewModel.formirovanSenderFilter.getValue());
            filterMovementNumber.setText(moveListViewModel.formirovanMovementNumberFilter.getValue());
            filterRecipient.setText(moveListViewModel.formirovanRecipientFilter.getValue());
            filterAssembler.setText(moveListViewModel.formirovanAssemblerFilter.getValue());
            filterReceiver.setText(moveListViewModel.formirovanReceiverFilter.getValue());
            setSpinnerSelection(filterPriority, moveListViewModel.formirovanPriorityFilter.getValue());
            Boolean cpsChecked = moveListViewModel.formirovanCpsChecked.getValue();
            if (cpsChecked != null) filterCps.setChecked(cpsChecked);
            Boolean availChecked = moveListViewModel.formirovanAvailabilityChecked.getValue();
            if (availChecked != null) filterAvailability.setChecked(availChecked);
        } else {
            filterSender.setText(moveListViewModel.komplektuetsaSenderFilter.getValue());
            filterMovementNumber.setText(moveListViewModel.komplektuetsaMovementNumberFilter.getValue());
            filterRecipient.setText(moveListViewModel.komplektuetsaRecipientFilter.getValue());
            filterAssembler.setText(moveListViewModel.komplektuetsaAssemblerFilter.getValue());
            filterReceiver.setText(moveListViewModel.komplektuetsaReceiverFilter.getValue());
            setSpinnerSelection(filterPriority, moveListViewModel.komplektuetsaPriorityFilter.getValue());
            Boolean cpsChecked = moveListViewModel.komplektuetsaCpsChecked.getValue();
            if (cpsChecked != null) filterCps.setChecked(cpsChecked);
            Boolean availChecked = moveListViewModel.komplektuetsaAvailabilityChecked.getValue();
            if (availChecked != null) filterAvailability.setChecked(availChecked);
        }
        // updateFilterIndicator(); // УДАЛЕНО: ViewModel управляет индикатором через LiveData
    }
    
    /**
     * Сбрасывает все фильтры для текущей вкладки
     */
    private void resetFilters() {
        int tabPos = getCurrentTabPosition();
        if (tabPos == 0) {
            moveListViewModel.setFormirovanSenderFilter("");
            moveListViewModel.setFormirovanMovementNumberFilter("");
            moveListViewModel.setFormirovanRecipientFilter("");
            moveListViewModel.setFormirovanAssemblerFilter("");
            moveListViewModel.setFormirovanPriorityFilter("");
            moveListViewModel.setFormirovanReceiverFilter("");
            moveListViewModel.setFormirovanCpsChecked(true);
            moveListViewModel.setFormirovanAvailabilityChecked(true);
        } else {
            moveListViewModel.setKomplektuetsaSenderFilter("");
            moveListViewModel.setKomplektuetsaMovementNumberFilter("");
            moveListViewModel.setKomplektuetsaRecipientFilter("");
            moveListViewModel.setKomplektuetsaAssemblerFilter("");
            moveListViewModel.setKomplektuetsaPriorityFilter("");
            moveListViewModel.setKomplektuetsaReceiverFilter("");
            moveListViewModel.setKomplektuetsaCpsChecked(true);
            moveListViewModel.setKomplektuetsaAvailabilityChecked(true);
        }

        hideKeyboard();
        clearFocus();
        drawerLayout.closeDrawer(GravityCompat.START);
        Log.d("MoveList_menu", "Filters reset for tab: " + tabPos);
    }

    private void setupSegmentedControl() {
        // Обработчик для сегмента "Сформировано"
        segmentFormirovano.setOnClickListener(v -> {
            if (getCurrentTabPosition() != 0) {
                viewPager.setCurrentItem(0, true); // второй параметр - плавная анимация
            }
        });
        
        // Обработчик для сегмента "Комплектуется"
        segmentKomplektuetsa.setOnClickListener(v -> {
            if (getCurrentTabPosition() != 1) {
                viewPager.setCurrentItem(1, true);
            }
        });
    }
    
    /**
     * Анимирует переключение между сегментами навигационных кнопок
     */
    private void animateSegmentSelection(TextView selected, TextView unselected) {
        if (selected == null || unselected == null) return;
        
        // Быстро меняем состояние перед началом анимации для мгновенной визуальной обратной связи
        selected.setSelected(true);
        selected.setAlpha(0.7f);  // Немного прозрачности, которую будем анимировать до 1.0
        unselected.setSelected(false);
        unselected.setAlpha(1.0f);
        
        // Анимируем scale выбранного сегмента с пружинным эффектом
        selected.setPivotX(selected.getWidth() / 2f);
        selected.setPivotY(selected.getHeight() / 2f);
        
        // Сначала устанавливаем начальное значение без анимации
        selected.setScaleX(0.95f);
        selected.setScaleY(0.95f);
        
        // Запускаем анимацию в фоновом потоке с небольшой задержкой
        // для предотвращения блокировки UI потока
        new Handler().postDelayed(() -> {
            // Анимируем scale с пружинным эффектом
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(selected, "scaleX", 0.95f, 1.05f, 1.0f);
            scaleX.setDuration(350);
            scaleX.setInterpolator(new OvershootInterpolator(1.5f));
            
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(selected, "scaleY", 0.95f, 1.05f, 1.0f);
            scaleY.setDuration(350);
            scaleY.setInterpolator(new OvershootInterpolator(1.5f));
            
            // Анимируем прозрачность
            ObjectAnimator alpha = ObjectAnimator.ofFloat(selected, "alpha", 0.7f, 1.0f);
            alpha.setDuration(350);
            
            // Используем облегченный метод запуска анимаций без AnimatorSet для эффективности
            scaleX.start();
            scaleY.start();
            alpha.start();
            
            // Немного меняем цвет фона невыбранного сегмента
            unselected.animate()
                .alpha(0.5f)
                .setDuration(200)
                .start();
            
            // Добавляем небольшую задержку перед анимацией невыбранного сегмента
            new Handler().postDelayed(() -> {
                unselected.animate()
                    .alpha(0.7f)
                    .setDuration(150)
                    .start();
            }, 100);
        }, 16); // 16ms = примерно 1 кадр при 60fps
    }

    /**
     * Предотвращает получение фокуса сегментами переключения
     */
    private void preventSegmentsFocus() {
        // Устанавливаем focusable в false для предотвращения получения фокуса
        segmentFormirovano.setFocusable(false);
        segmentKomplektuetsa.setFocusable(false);
        
        // Дополнительно блокируем получение фокуса с клавиатуры
        segmentFormirovano.setFocusableInTouchMode(false);
        segmentKomplektuetsa.setFocusableInTouchMode(false);
    }
    
    /**
     * Настраивает ViewPager2 и адаптер для переключения между фрагментами
     */
    private void setupViewPager() {
        // Создаем адаптер и добавляем фрагменты
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPagerAdapter.addFragment(formirovanFragment);
        viewPagerAdapter.addFragment(komplektuetsaFragment);
        
        // Улучшаем производительность ViewPager2 с помощью кэширования страниц
        viewPager.setOffscreenPageLimit(2); // Кэшируем оба фрагмента
        
        // Устанавливаем адаптер для ViewPager2
        viewPager.setAdapter(viewPagerAdapter);
        
        // Отключаем свайп для предотвращения случайного переключения
        // Раскомментируйте строку ниже, если хотите отключить свайп между вкладками
        // viewPager.setUserInputEnabled(false);
        
        // Обработчик переключения страниц
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                // Очищаем выбранные элементы на предыдущей вкладке
                clearSelectionInCurrentFragment();
                
                // Сохраняем предыдущую позицию для возможного восстановления фильтров
                final int previousPosition = getCurrentTabPosition();
                
                Log.d("MoveList_menu", "onPageSelected: Переключение с вкладки " + previousPosition + " на " + position);
                

                
                // Теперь меняем текущую позицию в ViewModel
                moveListViewModel.setCurrentTabPosition(position);

                
                // Используем Handler только для отложенного обновления UI и применения фильтров
                new Handler(Looper.getMainLooper()).post(() -> { // Убедимся, что работаем в UI потоке
                    try {
                        
                        // Обновляем индикатор фильтров
                        // updateFilterIndicator(); // УДАЛЕНО: ViewModel управляет индикатором через LiveData
                        
                        
                        // Дополнительно гарантируем обновление статистики для нового таба
                        // Вместо этого, можно запросить текущий отфильтрованный список и обновить диаграмму
                        if (position == 0) {
                            List<MoveItem> list = moveListViewModel.filteredFormirovanList.getValue();
                            if (list != null) updatePriorityBarChart(list);
                        } else {
                            List<MoveItem> list = moveListViewModel.filteredKomplektuetsaList.getValue();
                            if (list != null) updatePriorityBarChart(list);
                        }

                    } catch (Exception e) {
                        Log.e("MoveList_menu", "Ошибка при обновлении данных после смены вкладки: " + e.getMessage(), e);
                    }
                });
                
                // Анимация переключения сегментов сразу без задержки для лучшего UX
                if (position == 0) {
                    animateSegmentSelection(segmentFormirovano, segmentKomplektuetsa);
                } else {
                    animateSegmentSelection(segmentKomplektuetsa, segmentFormirovano);
                }
                
            }
        });
    }
    
    /**
     * Настраивает защиту от случайного получения фокуса для сегментов
     */
    private void setupSegmentTouchProtection(TextView segmentView) {
        if (segmentView == null) return;
        
        // Предотвращаем стандартное поведение получения фокуса
        segmentView.setFocusable(false);
        segmentView.setFocusableInTouchMode(false);
        
        // Добавляем обработчик нажатий, который обеспечит правильную обработку
        segmentView.setOnTouchListener((v, event) -> {
            // При нажатии на сегмент убираем фокус с других элементов
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
                clearFocus();
            }
            
            // Возвращаем false, чтобы стандартный обработчик onClick тоже сработал
            return false;
        });
    }


    

    



    
    /**
     * Обновляет визуальное отображение выбранного сегмента в соответствии с текущей позицией вкладки
     */
    private void updateSegmentSelection(int position) {
        if (segmentFormirovano != null && segmentKomplektuetsa != null) {
            if (position == 0) {
                segmentFormirovano.setSelected(true);
                segmentKomplektuetsa.setSelected(false);
            } else {
                segmentFormirovano.setSelected(false);
                segmentKomplektuetsa.setSelected(true);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MoveList_menu", "onResume called");
        
        // Закрываем диалог загрузки на всякий случай, 
        // если он остался открытым из-за какой-то ошибки
        loadingDialog.dismiss();
        

        // Вместо логики выше, просто обновляем индикатор, т.к. ViewModel сама управляет состоянием фильтров.
        // updateFilterIndicator(); // УДАЛЕНО: ViewModel управляет индикатором через LiveData

        
        // Установка глобального слушателя при возобновлении активности
        setupGlobalFocusChangeListener();
    }

    /**
     * Настраивает слушатель для обработки нажатия клавиши Enter в полях EditText
     * @param editText поле ввода, для которого настраивается слушатель
     */
    private void setupEnterKeyListener(EditText editText) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                
                // По порядку: 1. Очищаем фокус, 2. Скрываем клавиатуру, 3. Закрываем drawer
                clearFocus();
                hideKeyboard();
                
                // Предотвращаем установку фокуса на корневой элемент при закрытии NavigationView
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // Получаем ссылку на корневой элемент
                    View rootView = getWindow().getDecorView().getRootView();
                    
                    // Устанавливаем временный слушатель закрытия NavigationView
                    drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                        @Override
                        public void onDrawerClosed(View drawerView) {
                            // Убираем фокус со всех элементов после закрытия
                            rootView.clearFocus();
                            
                            // Важно: удаляем этот слушатель, чтобы избежать утечек памяти
                            drawerLayout.removeDrawerListener(this);
                        }
                    });
                    
                    // Закрываем NavigationView
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                
                // Устанавливаем focusable=false для всех дочерних элементов в иерархии
                // чтобы предотвратить непреднамеренный захват фокуса
                ViewGroup rootView = (ViewGroup) getWindow().getDecorView().getRootView();
                clearFocusableRecursively(rootView);
                
                return true; // Сообщаем, что событие полностью обработано
            }
            return false;
        });
    }
    
    /**
     * Рекурсивно устанавливает focusable=false для всех элементов в иерархии View
     * чтобы предотвратить непреднамеренный захват фокуса
     */
    private void clearFocusableRecursively(ViewGroup viewGroup) {
        // Пропускаем обработку для NavigationView
        if (viewGroup instanceof NavigationView) {
            return;
        }
        
        // Устанавливаем focusable=false для текущего ViewGroup
        viewGroup.setFocusable(false);
        viewGroup.setFocusableInTouchMode(false);
        
        // Обрабатываем все дочерние элементы
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            // Проверяем, не является ли элемент нашим полем filter_movement_number
            if (child == filterMovementNumber) {
                continue; // Пропускаем это поле, сохраняя его focusable свойства
            }
            
            // Пропускаем элементы, для которых важно иметь фокус
            if (child instanceof EditText || child instanceof Button) {
                continue;
            }
            
            // Устанавливаем focusable=false для текущего дочернего элемента
            child.setFocusable(false);
            child.setFocusableInTouchMode(false);
            
            // Если дочерний элемент - ViewGroup, обрабатываем рекурсивно
            if (child instanceof ViewGroup) {
                clearFocusableRecursively((ViewGroup) child);
            }
        }
    }

    /**
     * Утилитный метод для отключения клавиатуры в EditText с вводом чисел.
     * Аналогично Prixod.disableKeyboardForNumericField
     *
     * @param editText Поле ввода, для которого нужно отключить клавиатуру
     */
    private static void disableKeyboardForNumericField(EditText editText) {
        if (editText == null) return;
        
        try {
            // Метод доступен с API 21 (Android 5.0)
            // Отключаем только показ клавиатуры, но сохраняем возможность получения фокуса
            editText.setShowSoftInputOnFocus(false);
            
            // НЕ меняем focusable и focusableInTouchMode свойства, чтобы сохранить возможность установки фокуса
        } catch (Exception e) {
            // Игнорируем ошибку для ранних версий Android
        }
    }

    /**
     * Добавляем глобальный слушатель изменения фокуса для обработки перехода между полями
     */
    private void setupGlobalFocusChangeListener() {
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
            (oldFocus, newFocus) -> {
                if (newFocus == filterMovementNumber) {
                    // Если фокус перешёл на поле "Номер перемещения", скрываем клавиатуру
                    // но НЕ убираем фокус с поля
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(newFocus.getWindowToken(), 0);
                } else if (oldFocus == filterMovementNumber && newFocus instanceof EditText) {
                    // Если фокус ушёл с поля "Номер перемещения" на другое текстовое поле,
                    // то клавиатура будет показана через обработчик onFocusChange
                }
            }
        );
    }

    /**
     * Создает современную минималистичную диаграмму, показывающую статистику приоритетов
     */
    private void updatePriorityBarChart(List<MoveItem> items) {
        try {
            // Проверка на null
            if (items == null) {
                Log.e("MoveList_menu", "updatePriorityBarChart: список элементов null");
                return;
            }
            
            // Находим и очищаем контейнер
            LinearLayout statsContainer = findViewById(R.id.priority_stats_layout);
            if (statsContainer == null) {
                Log.e("MoveList_menu", "updatePriorityBarChart: контейнер не найден");
                return;
            }
            
            // Очищаем существующие элементы
            statsContainer.removeAllViews();
            
            // Подсчитываем количества по приоритетам
            int urgentCount = 0, highCount = 0, mediumCount = 0, lowCount = 0, noPriorityCount = 0;
            
            for (MoveItem item : items) {
                String priority = item.getPriority();
                if (priority == null || priority.isEmpty()) {
                    noPriorityCount++;
                } else if (MoveListViewModel.PRIORITY_URGENT.equals(priority)) { // Используем константу из ViewModel
                    urgentCount++;
                } else if (MoveListViewModel.PRIORITY_HIGH.equals(priority)) { // Используем константу из ViewModel
                    highCount++;
                } else if (MoveListViewModel.PRIORITY_MEDIUM.equals(priority)) { // Используем константу из ViewModel
                    mediumCount++;
                } else if (MoveListViewModel.PRIORITY_LOW.equals(priority)) { // Используем константу из ViewModel
                    lowCount++;
                }
            }
            
            // Общее количество элементов
            int totalCount = items.size();
            
            // Создаем новую диаграмму
            View chartView = getLayoutInflater().inflate(R.layout.priority_bar_chart, statsContainer, false);
            
            // Находим все секции и тексты с количеством
            FrameLayout urgentSection = chartView.findViewById(R.id.urgent_section);
            FrameLayout highSection = chartView.findViewById(R.id.high_section);
            FrameLayout mediumSection = chartView.findViewById(R.id.medium_section);
            FrameLayout lowSection = chartView.findViewById(R.id.low_section);
            
            TextView urgentText = chartView.findViewById(R.id.urgent_count);
            TextView highText = chartView.findViewById(R.id.high_count);
            TextView mediumText = chartView.findViewById(R.id.medium_count);
            TextView lowText = chartView.findViewById(R.id.low_count);
            TextView totalText = chartView.findViewById(R.id.total_badge);
            
            // Используем те же цвета, что и в RecyclerView (MoveAdapter)
            // Важно: точные цвета из MoveAdapter.setPriorityColor
            urgentSection.setBackgroundColor(Color.parseColor("#8B0000")); // Неотложный (бордовый/темно-красный)
            highSection.setBackgroundColor(Color.parseColor("#FF6347")); // Высокий (красный ближе к оранжевому)
            mediumSection.setBackgroundColor(Color.YELLOW); // Средний (желтый)
            lowSection.setBackgroundColor(Color.GREEN); // Низкий/Без приоритета (зеленый)
            
            // Устанавливаем количество для каждой секции
            urgentText.setText(String.valueOf(urgentCount));
            highText.setText(String.valueOf(highCount));
            mediumText.setText(String.valueOf(mediumCount));
            
            // ВАЖНО: Объединяем низкий приоритет и без приоритета в один сегмент (зеленый)
            // Это должно соответствовать логике фильтрации в методе matchesFilters(),
            // где элементы без приоритета должны включаться при фильтре по низкому приоритету
            int combinedLowCount = lowCount + noPriorityCount;
            lowText.setText(String.valueOf(combinedLowCount));
            
            // Устанавливаем общее количество
            totalText.setText(String.valueOf(totalCount));
            
            // Задаем видимость значения числа в зависимости от наличия элементов
            // Всегда показываем числа, включая нули
            urgentText.setVisibility(View.VISIBLE);
            highText.setVisibility(View.VISIBLE);
            mediumText.setVisibility(View.VISIBLE);
            lowText.setVisibility(View.VISIBLE);
            
            // Новый алгоритм расчета пропорций
            // Минимальная ширина для пустых секций
            final float MIN_EMPTY_WEIGHT = 6.0f;
            // Минимальная ширина для непустых секций
            final float MIN_FILLED_WEIGHT = 15.0f;
            
            // Расчет суммы элементов для шкалы
            int totalItems = urgentCount + highCount + mediumCount + combinedLowCount;
            
            // Вычисляем доступное пространство после учета минимальных размеров
            float totalMinWidth = 0;
            if (urgentCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (highCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (mediumCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (combinedLowCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            // Оставшееся распределяемое пространство (из 100%)
            float remainingSpace = 100.0f - totalMinWidth;
            
            // Вычисляем веса для каждого сектора
            float urgentWeight, highWeight, mediumWeight, lowWeight;
            
            if (totalItems == 0) {
                // Если нет элементов, распределяем равномерно
                urgentWeight = MIN_EMPTY_WEIGHT;
                highWeight = MIN_EMPTY_WEIGHT;
                mediumWeight = MIN_EMPTY_WEIGHT;
                lowWeight = MIN_EMPTY_WEIGHT;
            } else {
                // Расчет весов с учетом пропорций и минимальных значений
                urgentWeight = urgentCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * urgentCount / totalItems);
                
                highWeight = highCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * highCount / totalItems);
                
                mediumWeight = mediumCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * mediumCount / totalItems);
                
                lowWeight = combinedLowCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * combinedLowCount / totalItems);
            }
            
            // Устанавливаем веса через LayoutParams
            setFrameLayoutWeight(urgentSection, urgentWeight);
            setFrameLayoutWeight(highSection, highWeight);
            setFrameLayoutWeight(mediumSection, mediumWeight);
            setFrameLayoutWeight(lowSection, lowWeight);
            
            // Добавляем диаграмму в контейнер
            statsContainer.addView(chartView);
            
        } catch (Exception e) {
            Log.e("MoveList_menu", "Ошибка при обновлении диаграммы приоритетов: " + e.getMessage());
        }
    }
    
    /**
     * Рассчитывает вес для секции диаграммы на основе количества и общего количества
     */
    private float calculateSectionWeight(int count, int totalCount, float minWeight) {
        if (totalCount == 0) return minWeight;
        return count > 0 ? Math.max((float) count / totalCount * 100, minWeight) : minWeight;
    }
    
    /**
     * Устанавливает вес для FrameLayout в LinearLayout
     */
    private void setFrameLayoutWeight(FrameLayout layout, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        params.weight = weight;
        layout.setLayoutParams(params);
    }

    private void updateFragmentsAfterMoving() {
        try {
            // Используем обработчик для выполнения обновления в основном потоке
            new Handler(Looper.getMainLooper()).post(() -> {
                // Фрагменты обновятся автоматически через наблюдение за filteredFormirovanList 
                // и filteredKomplektuetsaList из ViewModel, которые, в свою очередь,
                // должны обновиться после изменения original списков в ViewModel.
                // Поэтому явное обновление данных фрагментов здесь не требуется.

                Log.d("MoveList_menu", "updateFragmentsAfterMoving: Фрагменты должны обновиться через LiveData ViewModel.");

                // Обновляем статистику приоритетов для текущего выбранного фрагмента
                // на основе уже обновленных и отфильтрованных данных из ViewModel.
                List<MoveItem> currentListToStat;
                int currentTab = getCurrentTabPosition(); // Используем актуальный getter
                String statusToLog = (currentTab == 0) ? STATUS_FORMIROVAN : STATUS_KOMPLEKTUETSA;

                if (currentTab == 0) {
                    currentListToStat = moveListViewModel.filteredFormirovanList.getValue();
                } else {
                    currentListToStat = moveListViewModel.filteredKomplektuetsaList.getValue();
                }

                if (currentListToStat != null) {
                     updatePriorityBarChart(new ArrayList<>(currentListToStat)); // Передаем копию
                     Log.d("MoveList_menu", "updateFragmentsAfterMoving: Статистика обновлена для вкладки " + statusToLog);
                } else {
                    Log.d("MoveList_menu", "updateFragmentsAfterMoving: Отфильтрованный список для статистики null для вкладки " + statusToLog);
                    updatePriorityBarChart(new ArrayList<>()); 
                }
            });
        } catch (Exception e) {
            Log.e("MoveList_menu", "Ошибка при обновлении фрагментов: " + e.getMessage());
            Toast.makeText(this, "Произошла ошибка при обновлении списков", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Принудительно закрываем диалог загрузки
        loadingDialog.dismiss();
        
        // Отменяем подсветки фильтров
        clearFilterIndicator();
        
        
        
        // Освобождаем ссылки для предотвращения утечек памяти
        formirovanFragment = null;
        komplektuetsaFragment = null;
        
        super.onDestroy();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        
        // Принудительно закрываем диалог загрузки
        loadingDialog.dismiss();
    }
    
    /**
     * Очищает индикатор фильтров
     */
    private void clearFilterIndicator() {
        if (filterIndicator != null) {
            filterIndicator.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Настраивает панель множественного выбора элементов
     */
    private void setupSelectionPanel() {
        // Инициализируем компоненты
        actionButtonsPanel = findViewById(R.id.actionButtonsPanel);
        selectedItemsCount = findViewById(R.id.selectedItemsCount);
        btnMoveToWork = findViewById(R.id.btnMoveToWork);
        btnMoveFromWork = findViewById(R.id.btnMoveFromWork);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        
        // Изначально скрываем панель
        actionButtonsPanel.setVisibility(View.GONE);
        
        // Настраиваем обработчики нажатий
        btnMoveToWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(MOVE_TO_KOMPLEKTUETSA));
        btnMoveFromWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(MOVE_TO_FORMIROVAN));
        
        // Обработчик нажатия на кнопку "Отмена"
        btnCancelSelection.setOnClickListener(v -> {
            // Снимаем выбор со всех элементов
            clearSelectionInCurrentFragment();
            
            // Скрываем панель выбора
            actionButtonsPanel.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    actionButtonsPanel.setVisibility(View.GONE);
                    // Отключаем режим выбора
                    setSelectionMode(false);
                })
                .start();
        });
    }
    
    /**
     * Очищает выбор в текущем фрагменте
     */
    private void clearSelectionInCurrentFragment() {
        MoveListFragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.clearSelection();
        }
        updateSelectionPanel(0);
    }
    
    /**
     * Возвращает текущий активный фрагмент
     */
    private MoveListFragment getCurrentFragment() {
        return getCurrentTabPosition() == 0 ? formirovanFragment : komplektuetsaFragment;
    }
    
    /**
     * Обновляет панель множественного выбора
     * @param selectedCount количество выбранных элементов
     */
    public void updateSelectionPanel(int selectedCount) {
        // Проверяем наличие компонентов панели
        if (actionButtonsPanel == null || selectedItemsCount == null) {
            Log.e("MoveList_menu", "Панель действий не инициализирована");
            return;
        }
        
        // Если есть выбранные элементы, показываем панель, иначе скрываем
        if (selectedCount > 0) {
            if (!isSelectionMode) {
                // Включаем режим выбора
                setSelectionMode(true);
            }
            
            // Обновляем счетчик выбранных элементов
            selectedItemsCount.setText(String.format("Выбрано: %d", selectedCount));
            
            // Показываем только нужную кнопку в зависимости от текущей вкладки
            if (getCurrentTabPosition() == 0) { // Вкладка "Сформировано"
                btnMoveToWork.setVisibility(View.VISIBLE);
                btnMoveFromWork.setVisibility(View.GONE);
            } else { // Вкладка "Комплектуется"
                btnMoveToWork.setVisibility(View.GONE);
                btnMoveFromWork.setVisibility(View.VISIBLE);
            }
            
            // Показываем панель с анимацией
            if (actionButtonsPanel.getVisibility() != View.VISIBLE) {
                actionButtonsPanel.setVisibility(View.VISIBLE);
                actionButtonsPanel.setAlpha(0f);
                actionButtonsPanel.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            }
        } else {
            // Скрываем панель с анимацией, если она видна
            if (actionButtonsPanel.getVisibility() == View.VISIBLE) {
                actionButtonsPanel.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        actionButtonsPanel.setVisibility(View.GONE);
                        // Отключаем режим выбора
                        setSelectionMode(false);
                    })
                    .start();
            } else {
                // Отключаем режим выбора, если панель не видна
                setSelectionMode(false);
            }
        }
    }
    
    /**
     * Перемещает выбранные элементы между статусами "Сформирован" и "Комплектуется"
     */
    private void moveSelectedItemsBetweenStates(int moveType) {
        MoveListFragment currentFragment = getCurrentFragment();
        if (currentFragment == null) {
            Log.e("MoveList_menu", "moveSelectedItemsBetweenStates: currentFragment is null");
            return;
        }
        
        List<MoveItem> selectedItems = currentFragment.getSelectedItems();
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Нет выбранных элементов", Toast.LENGTH_SHORT).show();
            return;
        }

        // Определяем текущее и целевое состояние
        String currentStatus;
        String targetStatus;
            
            if (moveType == MOVE_TO_KOMPLEKTUETSA) {
            currentStatus = STATUS_FORMIROVAN; // Используем константы из ViewModel
            targetStatus = STATUS_KOMPLEKTUETSA;
            } else if (moveType == MOVE_TO_FORMIROVAN) {
            currentStatus = STATUS_KOMPLEKTUETSA;
            targetStatus = STATUS_FORMIROVAN;
        } else {
            Log.e("MoveList_menu", "Неизвестный тип перемещения: " + moveType);
            return;
        }
        
        // Проверка на перемещение элемента "Нет в наличии" в "Комплектуется"



        // Вызываем метод ViewModel для перемещения элементов
        moveListViewModel.moveItems(new ArrayList<>(selectedItems), currentStatus, targetStatus);

        // Логика ниже (обновление UI, Snackbar) теперь обрабатывается через LiveData наблюдатели,
        // подписанные на moveOperationMessage и filteredList изменения.

        // Очищаем выбор в UI
        clearSelectionInCurrentFragment();
        // Отключаем режим выбора в UI
        setSelectionMode(false);
        

    }

    /**
     * Показывает подсказку о новой функциональности выбора с помощью чекбоксов
     */
    private void showMoveItemHintIfNeeded() {
        // Проверяем, была ли уже показана подсказка
        boolean hintShown = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean(PREF_MOVE_HINT_SHOWN, false);
        
        if (!hintShown) {
            // Создаем и настраиваем диалог
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Новая функция")
                   .setMessage("Теперь вы можете выбрать несколько контейнеров с помощью чекбоксов и выполнить групповое действие с ними.")
                   .setPositiveButton("Понятно", (dialog, which) -> {
                       // Запоминаем, что подсказка показана
                       getSharedPreferences("app_prefs", MODE_PRIVATE)
                           .edit()
                           .putBoolean(PREF_MOVE_HINT_SHOWN, true)
                           .apply();
                   })
                   .setIcon(android.R.drawable.ic_dialog_info)
                   .setCancelable(false) // Нельзя закрыть без нажатия кнопки
                   .show();
        }
    }

    /**
     * Устанавливает режим выбора и блокирует/разблокирует соответствующие элементы UI
     * @param enabled true - включить режим выбора, false - отключить
     */
    private void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        
        if (enabled) {
            // Блокируем навигацию и свайпы
            // 1. Отключаем ViewPager2 для свайпа между фрагментами
            viewPager.setUserInputEnabled(false);
            
            // 2. Блокируем открытие NavigationView
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            
            // 3. Скрываем кнопку открытия NavigationView
            navMenuButton.hide();
            
            // 4. Отключаем обработчики нажатия для контейнеров
            MoveListFragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.getAdapter() != null) {
                currentFragment.getAdapter().setContainerClickEnabled(false);
            }
            
            // 5. Отключаем нажатия на segment_control для предотвращения переключения между фрагментами
            if (segmentFormirovano != null) {
                segmentFormirovano.setClickable(false);
                segmentFormirovano.setEnabled(false);
            }
            if (segmentKomplektuetsa != null) {
                segmentKomplektuetsa.setClickable(false);
                segmentKomplektuetsa.setEnabled(false);
            }
        } else {
            // Возвращаем все функции UI
            // 1. Включаем ViewPager2 для свайпа между фрагментами
            viewPager.setUserInputEnabled(true);
            
            // 2. Разблокируем открытие NavigationView
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            
            // 3. Показываем кнопку открытия NavigationView
            navMenuButton.show();
            
            // 4. Включаем обработчики нажатия для контейнеров
            MoveListFragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.getAdapter() != null) {
                currentFragment.getAdapter().setContainerClickEnabled(true);
            }
            
            // 5. Включаем нажатия на segment_control для возвращения возможности переключения между фрагментами
            if (segmentFormirovano != null) {
                segmentFormirovano.setClickable(true);
                segmentFormirovano.setEnabled(true);
            }
            if (segmentKomplektuetsa != null) {
                segmentKomplektuetsa.setClickable(true);
                segmentKomplektuetsa.setEnabled(true);
            }
        }
    }

    /**
     * Настраивает кнопку обновления данных
     */
    private void setupRefreshButton() {
        // Получаем ссылку на кнопку обновления
        refreshButton = findViewById(R.id.refresh_button);
        
        // Проверяем, что кнопка была найдена
        if (refreshButton == null) {
            Log.e("MoveList_menu", "refreshButton не найдена в setupRefreshButton");
            return;
        }
        
        // Изначально кнопка скрыта
        refreshButton.setVisibility(View.GONE);
        
        // Установка обработчика нажатия
        refreshButton.setOnClickListener(v -> {
            Log.d("MoveList_menu", "Refresh button clicked.");
            
            // Вызываем метод ViewModel для применения обновлений
            moveListViewModel.applyPendingUpdates();

            // Логика обновления UI и скрытия диалога загрузки теперь управляется через LiveData из ViewModel
            // Обновление фрагментов и статистики произойдет через наблюдателей LiveData
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d("MoveList_menu", "Получен результат активности: requestCode=" + requestCode + 
              ", resultCode=" + resultCode);
              
        // Проверяем, что это результат из Prixod
        if (requestCode == 1001) {
            // Отмечаем, что вернулись из Prixod - УДАЛЕНО, ViewModel управляет этим
            // isReturnedFromPrixod = true; 
            
            // Передаем Intent в ViewModel для обработки данных и запуска проверки обновлений
            moveListViewModel.onReturnedFromPrixod(data);
            Log.d("MoveList_menu", "onActivityResult: Вызван moveListViewModel.onReturnedFromPrixod()");

            
            Log.d("MoveList_menu", "onActivityResult: Логика обновления данных теперь инициируется из ViewModel после onReturnedFromPrixod.");
        }
    }

    private void initFilterComponents() {
        // Инициализация полей ввода и других элементов фильтрации
        filterSender = findViewById(R.id.filter_sender);
        filterMovementNumber = findViewById(R.id.filter_movement_number);
        filterRecipient = findViewById(R.id.filter_recipient);
        filterAssembler = findViewById(R.id.filter_assembler);
        filterReceiver = findViewById(R.id.filter_receiver);
        filterPriority = findViewById(R.id.filter_priority);
        filterCps = findViewById(R.id.filter_cps);
        filterAvailability = findViewById(R.id.filter_availability);
        clearSender = findViewById(R.id.clear_sender);
        clearMovementNumber = findViewById(R.id.clear_movement_number);
        clearRecipient = findViewById(R.id.clear_recipient);
        clearAssembler = findViewById(R.id.clear_assembler);
        clearPriority = findViewById(R.id.clear_priority);
        clearReceiver = findViewById(R.id.clear_receiver);

        btnResetFilters = findViewById(R.id.btn_reset_filters);

        // Устанавливаем TextWatcher'ы и слушатели
        setupFilterListeners();
    }

    private void setupFilterListeners() {
        // TextWatchers для EditText
        addTextWatcher(filterSender, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanSenderFilter(text);
            else moveListViewModel.setKomplektuetsaSenderFilter(text);
        });
        addTextWatcher(filterMovementNumber, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanMovementNumberFilter(text);
            else moveListViewModel.setKomplektuetsaMovementNumberFilter(text);
        });
        addTextWatcher(filterRecipient, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanRecipientFilter(text);
            else moveListViewModel.setKomplektuetsaRecipientFilter(text);
        });
        addTextWatcher(filterAssembler, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanAssemblerFilter(text);
            else moveListViewModel.setKomplektuetsaAssemblerFilter(text);
        });
        addTextWatcher(filterReceiver, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanReceiverFilter(text);
            else moveListViewModel.setKomplektuetsaReceiverFilter(text);
        });

        // Слушатель для Spinner
        filterPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPriority = parent.getItemAtPosition(position).toString();
                if ("Все".equals(selectedPriority)) {
                    selectedPriority = ""; // "" представляет "Все" в ViewModel
                }
                if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanPriorityFilter(selectedPriority);
                else moveListViewModel.setKomplektuetsaPriorityFilter(selectedPriority);
    }

    @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Слушатели для CheckBox
        filterCps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanCpsChecked(isChecked);
            else moveListViewModel.setKomplektuetsaCpsChecked(isChecked);
        });
        filterAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanAvailabilityChecked(isChecked);
            else moveListViewModel.setKomplektuetsaAvailabilityChecked(isChecked);
        });
        
        // Слушатели для кнопок очистки
        clearSender.setOnClickListener(v -> filterSender.setText(""));
        clearMovementNumber.setOnClickListener(v -> filterMovementNumber.setText(""));
        clearRecipient.setOnClickListener(v -> filterRecipient.setText(""));
        clearAssembler.setOnClickListener(v -> filterAssembler.setText(""));
        clearPriority.setOnClickListener(v -> filterPriority.setSelection(0));
        clearReceiver.setOnClickListener(v -> filterReceiver.setText(""));





        // Кнопка "Сбросить фильтры"
        btnResetFilters.setOnClickListener(v -> resetFilters());
    }

    /**
     * Вспомогательный метод для добавления TextWatcher к EditText
     */
    private void addTextWatcher(EditText editText, FilterTextWatcher watcher) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
    @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Удаляем предыдущий Runnable, если он есть
                if (filterRunnable != null) {
                    filterHandler.removeCallbacks(filterRunnable);
                }
                // Создаем новый Runnable для отложенного применения фильтра
                filterRunnable = () -> watcher.onTextChanged(s.toString().trim());
                // Запускаем Runnable с задержкой
                filterHandler.postDelayed(filterRunnable, FILTER_DELAY_MS);
            }
        });
    }

    /**
     * Функциональный интерфейс для TextWatcher
     */
    @FunctionalInterface
    private interface FilterTextWatcher {
        void onTextChanged(String text);
    }

    /**
     * Проверяет, применен ли фильтр к данному значению (не пустой и не "Все")
     */
    private boolean isFilterApplied(String filterValue) {
        return filterValue != null && !filterValue.isEmpty();
    }

    // +++ ДОБАВЛЕННЫЕ МЕТОДЫ +++
    private void clearFocus() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.clearFocus();
            // Попытка убрать фокус, передав его родительскому элементу
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.requestFocus();
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            // Если нет текущего фокуса, создаем временный View для получения токена окна
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void scrollToView(View view) {
        if (view == null || navigationView == null) return;
        NestedScrollView scrollView = navigationView.findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            // Используем post для выполнения прокрутки после того, как view будет полностью размещен
            view.post(() -> {
                Rect scrollBounds = new Rect();
                scrollView.getHitRect(scrollBounds);
                if (view.getLocalVisibleRect(scrollBounds)) {
                    // view уже виден, ничего не делаем
                    return;
                }
                // Простой вариант прокрутки к верху view
                scrollView.smoothScrollTo(0, view.getTop());
            });
        } else {
            Log.w("MoveList_menu", "scrollToView: NestedScrollView with ID R.id.filter_scroll_view not found in navigationView.");
        }
    }
    // --- КОНЕЦ ДОБАВЛЕННЫХ МЕТОДОВ ---

    // +++ МЕТОДЫ ДЛЯ НАСТРОЙКИ UI (NavigationView и TouchListener) +++
    private void setupNavigationView() {
        if (navigationView == null || drawerLayout == null || navMenuButton == null) {
            Log.e("MoveList_menu", "setupNavigationView: NavigationView, DrawerLayout or NavMenuButton is null");
            return;
        }

        navMenuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                // Перед открытием скрываем клавиатуру и убираем фокус
                hideKeyboard();
                clearFocus();
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            // TODO: Добавить обработку выбора пунктов меню
            // Пример:
            // if (id == R.id.nav_item1) {
            //    // Действие для nav_item1
            // } else if (id == R.id.nav_item2) {
            //    // Действие для nav_item2
            // }

            Log.d("MoveList_menu", "Selected navigation item: " + item.getTitle());

            // Закрываем шторку после выбора
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Добавляем слушатель на DrawerLayout для вызова preventKeyboardShowOnDrawerClose
        // при закрытии шторки жестом или кнопкой "назад"
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                preventKeyboardShowOnDrawerClose();
            }
        });
    }

    private void setupTouchListener() {
        View mainContent = findViewById(R.id.drawer_layout); // Предполагаем, что у вас есть корневой контейнер с таким ID
                                                                      // или используйте findViewById(android.R.id.content).getChildAt(0)
                                                                      // или ваш конкретный корневой элемент layout/movelist_menu.xml
        if (mainContent == null) {
            Log.e("MoveList_menu", "setupTouchListener: Main content view (R.id.main_content_container) not found.");
            // В качестве запасного варианта, если R.id.main_content_container не найден,
            // можно попробовать установить слушатель на getWindow().getDecorView(), но это может перехватывать слишком много событий.
            // View decorView = getWindow().getDecorView();
            // decorView.setOnTouchListener(...);
            return; 
        }

        mainContent.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Если шторка открыта, закрываем ее и потребляем событие
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    hideKeyboard(); // Также скрываем клавиатуру
                    clearFocus();   // И убираем фокус
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true; // Событие обработано
                }

                // Скрываем клавиатуру, если текущий фокус не на EditText
                // или если просто хотим скрывать клавиатуру при любом касании вне EditText
                View currentFocusedView = getCurrentFocus();
                if (currentFocusedView instanceof EditText) {
                    // Можно добавить более сложную логику, чтобы не скрывать клавиатуру,
                    // если тап был внутри этого же EditText (хотя обычно EditText сам это обрабатывает)
                    // Rect outRect = new Rect();
                    // currentFocusedView.getGlobalVisibleRect(outRect);
                    // if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    //    hideKeyboard();
                    //    clearFocus(); // Возможно, не всегда нужно убирать фокус
                    // }
                } else {
                    // Если фокус не на EditText, или его вообще нет, скрываем клавиатуру
                    hideKeyboard();
                    // clearFocus(); // Убирать фокус здесь может быть излишне, если только клавиатура мешает
                }
            }
            return false; // Возвращаем false, чтобы другие слушатели (например, у ViewPager) тоже могли обработать событие
        });
    }
    // --- КОНЕЦ МЕТОДОВ ДЛЯ НАСТРОЙКИ UI ---
}