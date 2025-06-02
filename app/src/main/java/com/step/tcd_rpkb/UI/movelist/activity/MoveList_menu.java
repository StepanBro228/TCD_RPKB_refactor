package com.step.tcd_rpkb.UI.movelist.activity;

import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_FORMIROVAN;
import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_KOMPLEKTUETSA;

import android.animation.AnimatorSet;
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
import android.view.ViewParent;
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
import com.google.android.material.snackbar.Snackbar; // << ДОБАВЛЕН ИМПОРТ
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
    // private Button btnUndoMove; // << УДАЛЕНО
    
    // Флаг, определяющий режим выбора элементов
    private boolean isSelectionMode = false;
    

    private FloatingActionButton refreshButton;
    // Для фоновых операций


    private com.step.tcd_rpkb.utils.LoadingDialog loadingDialog; // Возвращаем кастомный диалог
    // private AlertDialog alertDialog; // Убираем поле для AlertDialog
    
    // Счетчик активных загрузок для корректного управления диалогом загрузки
    private int activeLoadingCount = 0;
    private final Object loadingLock = new Object();

    // Флаги для управления первым обновлением диаграммы
    private boolean formirovanDataReadyForChart = false;
    private boolean komplektuetsaDataReadyForChart = false;

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
                    // focusedView.postDelayed(() -> scrollToView(focusedView), 300);
                    scrollToView(focusedView); // Вызываем напрямую
                }
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
        
        // Инициализируем слушатель для NestedScrollView в NavigationView
        setupFilterScrollViewTouchListener(); // Добавляем вызов нового метода
        
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
            filterMovementNumber.setShowSoftInputOnFocus(false); // Убедимся, что флаг установлен
            
            // Добавляем возможность запрашивать фокус по клику
            filterMovementNumber.setOnClickListener(v -> {
                v.requestFocus();
                // setShowSoftInputOnFocus(false) должен предотвратить появление клавиатуры.
                // В качестве дополнительной меры явно скрываем клавиатуру.
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (v.getWindowToken() != null) { // Проверяем токен перед использованием
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            });
        }
        
        // Показываем подсказку о новой функциональности выбора элементов
        showMoveItemHintIfNeeded();
        
        // Настраивает панель множественного выбора
        setupSelectionPanel();
        
        // Инициализируем кнопку обновления (изначально скрыта)
        setupRefreshButton();
        
        // Подписываемся на LiveData из ViewModel
        observeViewModel();
            // Загружаем данные из JSON и обновляем фрагменты
        // Убираем задержку, чтобы данные загружались как можно скорее
            moveListViewModel.loadMoveData(); // Загружаем данные через ViewModel



    }

    private void observeViewModel() {
        // Наблюдение за состоянием загрузки
        moveListViewModel.isLoading.observe(this, isLoading -> {
            Log.d("MoveList_menu", "isLoading LiveData changed: " + isLoading);
            if (isLoading) {
                Log.d("MoveList_menu", "isLoading is TRUE. Posting runnable to show dialog.");
                getWindow().getDecorView().post(() -> {
                    Log.d("MoveList_menu", "Runnable in post() for show is executing.");
                    // Показываем, только если isLoading ВСЕ ЕЩЕ true к моменту выполнения Runnable
                    if (moveListViewModel.isLoading.getValue() == Boolean.TRUE) {
                        if (loadingDialog == null) {
                            loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(MoveList_menu.this);
                            Log.d("MoveList_menu", "Custom LoadingDialog created via post.");
                        }
                        if (loadingDialog != null && !loadingDialog.isShowing()) {
                loadingDialog.show();
                            Log.d("MoveList_menu", "Custom LoadingDialog.show() called via post.");
                } else {
                            Log.d("MoveList_menu", "Custom LoadingDialog already showing or null (checked via post).");
                        }
                    } else {
                        Log.d("MoveList_menu", "Runnable for show executed, but isLoading is now false. Dialog not shown.");
                    }
                });
            } else {
                Log.d("MoveList_menu", "isLoading is FALSE. Posting runnable to hide dialog.");
                // Скрытие тоже можно обернуть в post для консистентности
                getWindow().getDecorView().post(() -> {
                    Log.d("MoveList_menu", "Runnable in post() for dismiss is executing.");
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        Log.d("MoveList_menu", "Dismissing custom LoadingDialog via post.");
                loadingDialog.dismiss();
                    } else {
                        Log.d("MoveList_menu", "Custom LoadingDialog was not showing or null when dismiss (via post) was called.");
                    }
                });
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
            formirovanDataReadyForChart = true; // Данные загружены
            // Попытка обновить диаграмму здесь, если это текущая вкладка
            if (getCurrentTabPosition() == 0) {
                List<MoveItem> currentFilteredList = moveListViewModel.filteredFormirovanList.getValue();
                if (currentFilteredList != null) {
                    Log.d("MoveList_menu", "Original Formirovan loaded, forcing chart update for tab 0. Size: " + currentFilteredList.size());
                    updatePriorityBarChart(new ArrayList<>(currentFilteredList));
                } else {
                    updatePriorityBarChart(new ArrayList<>()); // или не обновлять, если список null
                }
            }
            updateFragmentsAfterMoving(); 
        });

        moveListViewModel.originalKomplektuetsaList.observe(this, list -> {
            Log.d("MoveList_menu", "Original Komplektuetsa list updated in ViewModel, size: " + (list != null ? list.size() : 0));
            komplektuetsaDataReadyForChart = true; // Данные загружены
            // Попытка обновить диаграмму здесь, если это текущая вкладка
            if (getCurrentTabPosition() == 1) {
                List<MoveItem> currentFilteredList = moveListViewModel.filteredKomplektuetsaList.getValue();
                if (currentFilteredList != null) {
                    Log.d("MoveList_menu", "Original Komplektuetsa loaded, forcing chart update for tab 1. Size: " + currentFilteredList.size());
                    updatePriorityBarChart(new ArrayList<>(currentFilteredList));
                } else {
                     updatePriorityBarChart(new ArrayList<>()); // или не обновлять, если список null
                }
            }
            updateFragmentsAfterMoving(); 
        });
        
        // Наблюдение за отфильтрованными списками для обновления диаграммы приоритетов
        moveListViewModel.filteredFormirovanList.observe(this, list -> {
            // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredFormirovanList] Triggered. Current Tab: " + getCurrentTabPosition() + ". List size: " + (list != null ? list.size() : "null"));
            // Обновляем диаграмму, только если основные данные уже были загружены для этой вкладки
            if (getCurrentTabPosition() == 0 && formirovanDataReadyForChart && list != null) {
                // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredFormirovanList] Updating priority chart for Formirovan. Size: " + list.size());
                updatePriorityBarChart(new ArrayList<>(list)); // Передаем копию
            } else if (getCurrentTabPosition() == 0 && formirovanDataReadyForChart && list == null) {
                // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredFormirovanList] Updating priority chart for Formirovan with EMPTY list (source was null).");
                updatePriorityBarChart(new ArrayList<>()); // Пустая диаграмма, если список стал null после загрузки
            // } else {
                 // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredFormirovanList] SKIPPING chart update. Tab: " + getCurrentTabPosition() + " formirovanDataReady: " + formirovanDataReadyForChart + " list is null: " + (list == null));
            }
        });

        moveListViewModel.filteredKomplektuetsaList.observe(this, list -> {
            // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredKomplektuetsaList] Triggered. Current Tab: " + getCurrentTabPosition() + ". List size: " + (list != null ? list.size() : "null"));
            // Обновляем диаграмму, только если основные данные уже были загружены для этой вкладки
            if (getCurrentTabPosition() == 1 && komplektuetsaDataReadyForChart && list != null) {
                // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredKomplektuetsaList] Updating priority chart for Komplektuetsa. Size: " + list.size());
                updatePriorityBarChart(new ArrayList<>(list)); // Передаем копию
            } else if (getCurrentTabPosition() == 1 && komplektuetsaDataReadyForChart && list == null) {
                // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredKomplektuetsaList] Updating priority chart for Komplektuetsa with EMPTY list (source was null).");
                 updatePriorityBarChart(new ArrayList<>()); // Пустая диаграмма, если список стал null после загрузки
            // } else {
                // Log.d("DEBUG_UPDATE", "[MoveList_menu.observer.filteredKomplektuetsaList] SKIPPING chart update. Tab: " + getCurrentTabPosition() + " komplektuetsaDataReady: " + komplektuetsaDataReadyForChart + " list is null: " + (list == null));
            }
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
        
        // Наблюдатель для Snackbar с возможностью отмены
        moveListViewModel.showUndoSnackbarEvent.observe(this, (SingleEvent<MoveListViewModel.SnackbarEvent> event) -> {
            if (event != null) {
                MoveListViewModel.SnackbarEvent snackbarEvent = event.getContentIfNotHandled();
                if (snackbarEvent != null) {
                    View rootView = findViewById(android.R.id.content); // Или ваш корневой элемент макета, например R.id.drawer_layout
                    if (rootView == null) rootView = getWindow().getDecorView().findViewById(android.R.id.content);
                    if (rootView == null) rootView = drawerLayout; // Fallback to drawerLayout if others are null
                    
                    if (rootView != null) {
                        Snackbar snackbar = Snackbar.make(rootView, snackbarEvent.message, Snackbar.LENGTH_LONG);
                        if (snackbarEvent.showUndoAction) {
                            snackbar.setAction("Отменить", v -> {
                                moveListViewModel.undoMove();
                            });
                            // Можно установить цвет кнопки "Отменить", если требуется
                            // snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.your_undo_action_color));
                        }
                        snackbar.show();
                    } else {
                        // Fallback to Toast if no suitable view found for Snackbar
                        Toast.makeText(MoveList_menu.this, snackbarEvent.message, Toast.LENGTH_LONG).show();
                    }
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
        
        // Наблюдатель для возможности отмены
        // moveListViewModel.undoAvailable.observe(this, isUndoAvailable -> {
        //     if (btnUndoMove != null) {
        //         btnUndoMove.setVisibility(isUndoAvailable != null && isUndoAvailable ? View.VISIBLE : View.GONE);
        //     }
        // });
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
        moveListViewModel.formirovanSenderFilter.observe(this, value -> {
            if (filterSender != null && getCurrentTabPosition() == 0) {
                String currentValue = filterSender.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterSender.setText(newValue);
                }
            }
        });
        moveListViewModel.formirovanMovementNumberFilter.observe(this, value -> {
            if (filterMovementNumber != null && getCurrentTabPosition() == 0) {
                String currentValue = filterMovementNumber.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterMovementNumber.setText(newValue);
                }
            }
        });
        moveListViewModel.formirovanRecipientFilter.observe(this, value -> {
            if (filterRecipient != null && getCurrentTabPosition() == 0) {
                String currentValue = filterRecipient.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterRecipient.setText(newValue);
                }
            }
        });
        moveListViewModel.formirovanAssemblerFilter.observe(this, value -> {
            if (filterAssembler != null && getCurrentTabPosition() == 0) {
                String currentValue = filterAssembler.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterAssembler.setText(newValue);
                }
            }
        });
        moveListViewModel.formirovanReceiverFilter.observe(this, value -> {
            if (filterReceiver != null && getCurrentTabPosition() == 0) {
                String currentValue = filterReceiver.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterReceiver.setText(newValue);
                }
            }
        });
        moveListViewModel.formirovanPriorityFilter.observe(this, value -> {
            if (getCurrentTabPosition() == 0) setSpinnerSelection(filterPriority, value);
        });
        moveListViewModel.formirovanCpsChecked.observe(this, value -> { if (getCurrentTabPosition() == 0 && value != null && filterCps != null && filterCps.isChecked() != value) filterCps.setChecked(value); });
        moveListViewModel.formirovanAvailabilityChecked.observe(this, value -> { if (getCurrentTabPosition() == 0 && value != null && filterAvailability != null && filterAvailability.isChecked() != value) filterAvailability.setChecked(value); });

        // Для вкладки "Комплектуется"
        moveListViewModel.komplektuetsaSenderFilter.observe(this, value -> {
            if (filterSender != null && getCurrentTabPosition() == 1) {
                String currentValue = filterSender.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterSender.setText(newValue);
                }
            }
        });
        moveListViewModel.komplektuetsaMovementNumberFilter.observe(this, value -> {
            if (filterMovementNumber != null && getCurrentTabPosition() == 1) {
                String currentValue = filterMovementNumber.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterMovementNumber.setText(newValue);
                }
            }
        });
        moveListViewModel.komplektuetsaRecipientFilter.observe(this, value -> {
            if (filterRecipient != null && getCurrentTabPosition() == 1) {
                String currentValue = filterRecipient.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterRecipient.setText(newValue);
                }
            }
        });
        moveListViewModel.komplektuetsaAssemblerFilter.observe(this, value -> {
            if (filterAssembler != null && getCurrentTabPosition() == 1) {
                String currentValue = filterAssembler.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterAssembler.setText(newValue);
                }
            }
        });
        moveListViewModel.komplektuetsaReceiverFilter.observe(this, value -> {
            if (filterReceiver != null && getCurrentTabPosition() == 1) {
                String currentValue = filterReceiver.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterReceiver.setText(newValue);
                }
            }
        });
        moveListViewModel.komplektuetsaPriorityFilter.observe(this, value -> {
            if (getCurrentTabPosition() == 1) setSpinnerSelection(filterPriority, value);
        });
        moveListViewModel.komplektuetsaCpsChecked.observe(this, value -> { if (getCurrentTabPosition() == 1 && value != null && filterCps != null && filterCps.isChecked() != value) filterCps.setChecked(value); });
        moveListViewModel.komplektuetsaAvailabilityChecked.observe(this, value -> { if (getCurrentTabPosition() == 1 && value != null && filterAvailability != null && filterAvailability.isChecked() != value) filterAvailability.setChecked(value); });
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

        // Убираем тестовый AlertDialog из onResume
        // if (alertDialog == null) { ... }
        // if (!alertDialog.isShowing()) { ... }
        
        // Закрываем диалог загрузки на всякий случай, 
        // если он остался открытым из-за какой-то ошибки
        // loadingDialog.dismiss(); // ВРЕМЕННО КОММЕНТИРУЕМ
        

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
        if (editText == null) return;

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && v.getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                v.clearFocus(); 

                if (drawerLayout != null) {
                    drawerLayout.requestFocus(); // Немедленно передаем фокус DrawerLayout
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                
                return true; 
            }
            return false; 
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_NEXT || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && v.getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                v.clearFocus(); 

                if (drawerLayout != null) {
                    drawerLayout.requestFocus(); // Немедленно передаем фокус DrawerLayout
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true; 
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
                    if(filterMovementNumber != null) {
                        filterMovementNumber.setShowSoftInputOnFocus(false); // Перестраховка
                    }
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (newFocus.getWindowToken() != null) { // Проверяем токен
                    imm.hideSoftInputFromWindow(newFocus.getWindowToken(), 0);
                    }
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
            
            LinearLayout statsContainer = findViewById(R.id.priority_stats_layout);
            if (statsContainer == null) {
                Log.e("MoveList_menu", "updatePriorityBarChart: контейнер не найден");
                return;
            }
            
            View chartView;
            // Проверяем, есть ли уже chartView в контейнере
            if (statsContainer.getChildCount() > 0 && statsContainer.getChildAt(0).getId() == R.id.priority_chart_root) {
                chartView = statsContainer.getChildAt(0);
                // Log.d("MoveList_menu", "updatePriorityBarChart: chartView найден, обновляем существующий.");
            } else {
                // Log.d("MoveList_menu", "updatePriorityBarChart: chartView не найден, создаем новый.");
                statsContainer.removeAllViews(); // Очищаем, если там что-то другое было
                chartView = getLayoutInflater().inflate(R.layout.priority_bar_chart, statsContainer, false);
                chartView.setId(R.id.priority_chart_root); // Устанавливаем ID для корневого элемента диаграммы
                statsContainer.addView(chartView);
            }
            
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
            
            // Добавляем диаграмму в контейнер - это уже сделано выше, если chartView создавался
            // statsContainer.addView(chartView); 
            
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
        if (params.weight != weight) { // Обновляем только если вес изменился
        params.weight = weight;
        layout.setLayoutParams(params);
        }
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
                // List<MoveItem> currentListToStat; // Удалено - обновление диаграммы теперь через отдельные наблюдатели
                int currentTab = getCurrentTabPosition(); // Используем актуальный getter
                String statusToLog = (currentTab == 0) ? STATUS_FORMIROVAN : STATUS_KOMPLEKTUETSA;

                // Удалены строки ниже, так как диаграмма обновляется наблюдателями filteredFormirovanList и filteredKomplektuetsaList
                // if (currentTab == 0) {
                //     currentListToStat = moveListViewModel.filteredFormirovanList.getValue();
                // } else {
                //     currentListToStat = moveListViewModel.filteredKomplektuetsaList.getValue();
                // }

                // if (currentListToStat != null) {
                //      updatePriorityBarChart(new ArrayList<>(currentListToStat)); // Передаем копию
                //      Log.d("MoveList_menu", "updateFragmentsAfterMoving: Статистика обновлена для вкладки " + statusToLog);
                // } else {
                //     Log.d("MoveList_menu", "updateFragmentsAfterMoving: Отфильтрованный список для статистики null для вкладки " + statusToLog);
                //     updatePriorityBarChart(new ArrayList<>()); 
                // }
                Log.d("MoveList_menu", "updateFragmentsAfterMoving: Обновление диаграммы приоритетов теперь обрабатывается наблюдателями filteredXXXList для вкладки " + statusToLog);
            });
        } catch (Exception e) {
            Log.e("MoveList_menu", "Ошибка при обновлении фрагментов: " + e.getMessage());
            Toast.makeText(this, "Произошла ошибка при обновлении списков", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Принудительно закрываем диалог загрузки
        // loadingDialog.dismiss(); // ВРЕМЕННО КОММЕНТИРУЕМ
        
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
        // loadingDialog.dismiss(); // ВРЕМЕННО КОММЕНТИРУЕМ
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
        if (actionButtonsPanel != null) actionButtonsPanel.setVisibility(View.GONE);
        
        // Настраиваем обработчики нажатий
        if (btnMoveToWork != null) btnMoveToWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(MOVE_TO_KOMPLEKTUETSA));
        if (btnMoveFromWork != null) btnMoveFromWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(MOVE_TO_FORMIROVAN));
        
        // Обработчик нажатия на кнопку "Отмена"
        if (btnCancelSelection != null) {
        btnCancelSelection.setOnClickListener(v -> {
                clearSelectionInCurrentFragment(); // Это запустит анимацию скрытия панели в updateSelectionPanel

                // Выходим из режима выбора и показываем кнопку меню ПОСЛЕ того, как анимация панели отработает
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isSelectionMode) { // Проверяем, действительно ли нужно выходить из режима
                    setSelectionMode(false);
                    }
                    // Фокус уже устанавливается внутри setSelectionMode(false)
                }, 200); // Задержка чуть больше анимации панели (150мс)
        });
        }
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
        if (actionButtonsPanel == null || selectedItemsCount == null) {
            Log.e("MoveList_menu", "Панель действий не инициализирована");
            return;
        }
        
        if (selectedCount > 0) {
            if (!isSelectionMode) {
                setSelectionMode(true); // Включаем режим выбора (это скроет navMenuButton)
            }
            
            if (selectedItemsCount != null) selectedItemsCount.setText(String.format("Выбрано: %d", selectedCount));
            
            if (getCurrentTabPosition() == 0) { 
                if (btnMoveToWork != null) btnMoveToWork.setVisibility(View.VISIBLE);
                if (btnMoveFromWork != null) btnMoveFromWork.setVisibility(View.GONE);
            } else { 
                if (btnMoveToWork != null) btnMoveToWork.setVisibility(View.GONE);
                if (btnMoveFromWork != null) btnMoveFromWork.setVisibility(View.VISIBLE);
            }
            
            if (actionButtonsPanel.getVisibility() != View.VISIBLE) {
                actionButtonsPanel.setVisibility(View.VISIBLE);
                actionButtonsPanel.setAlpha(0f);
                actionButtonsPanel.animate().alpha(1f).setDuration(200).start();
            }
        } else { // selectedCount == 0
            if (actionButtonsPanel.getVisibility() == View.VISIBLE) {
                actionButtonsPanel.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        if (actionButtonsPanel != null) {
                        actionButtonsPanel.setVisibility(View.GONE);
                        }
                        // Если количество выбранных элементов стало 0, и мы БЫЛИ в режиме выбора,
                        // то выходим из режима выбора.
                        if (isSelectionMode) {
                        setSelectionMode(false);
                        }
                    })
                    .start();
            } else {
                // Если панель УЖЕ невидима (например, при инициализации или после предыдущего скрытия),
                // но selectedCount == 0 и мы все еще в режиме выбора (маловероятно, но возможно),
                // также выходим из режима выбора.
                if (isSelectionMode) {
                setSelectionMode(false);
                }
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

        String currentStatus;
        String targetStatus;
            
            if (moveType == MOVE_TO_KOMPLEKTUETSA) {
            currentStatus = STATUS_FORMIROVAN; 
            targetStatus = STATUS_KOMPLEKTUETSA;
            } else if (moveType == MOVE_TO_FORMIROVAN) {
            currentStatus = STATUS_KOMPLEKTUETSA;
            targetStatus = STATUS_FORMIROVAN;
        } else {
            Log.e("MoveList_menu", "Неизвестный тип перемещения: " + moveType);
            return;
        }
        
        moveListViewModel.moveItems(new ArrayList<>(selectedItems), currentStatus, targetStatus);
        clearSelectionInCurrentFragment(); // Это запустит анимацию скрытия панели

        // Выходим из режима выбора и показываем кнопку меню ПОСЛЕ того, как анимация панели отработает
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isSelectionMode) { // Проверяем, действительно ли нужно выходить из режима
        setSelectionMode(false);
            }
            // Фокус уже устанавливается внутри setSelectionMode(false)
        }, 200); // Задержка чуть больше анимации панели (150мс)
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
            viewPager.setUserInputEnabled(false);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            if (navMenuButton != null) navMenuButton.hide(); // Скрываем кнопку меню
            
            MoveListFragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.getAdapter() != null) {
                currentFragment.getAdapter().setContainerClickEnabled(false);
            }
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
            viewPager.setUserInputEnabled(true);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            // Кнопка navMenuButton будет показана из updateSelectionPanel после скрытия панели действий
            if (navMenuButton != null) {
                navMenuButton.show(); // Показываем кнопку меню
                navMenuButton.setClickable(true);
                navMenuButton.setEnabled(true);
                 // Переводим фокус на кнопку меню, когда выходим из режима выбора
                if (navMenuButton.isShown()) { // Убедимся, что она действительно видима
                    navMenuButton.requestFocus();
                }
            }

            MoveListFragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.getAdapter() != null) {
                currentFragment.getAdapter().setContainerClickEnabled(true);
            }
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
        if (filterMovementNumber != null) {
            // Устанавливаем флаг как можно раньше после получения View
            filterMovementNumber.setShowSoftInputOnFocus(false);
        }
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

        // Инициализация адаптера для Spinner приоритетов
        if (filterPriority != null) {
            List<String> priorityOptions = new ArrayList<>();
            priorityOptions.add("Все"); // Должен быть первым для корректной работы resetFilters и setSpinnerSelection
            priorityOptions.add(MoveListViewModel.PRIORITY_URGENT); // "Неотложный"
            priorityOptions.add(MoveListViewModel.PRIORITY_HIGH);   // "Высокий"
            priorityOptions.add(MoveListViewModel.PRIORITY_MEDIUM); // "Средний"
            priorityOptions.add(MoveListViewModel.PRIORITY_LOW);    // "Низкий"

            ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, priorityOptions);
            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterPriority.setAdapter(priorityAdapter);
        }

        // Устанавливаем TextWatcher'ы и слушатели
        setupFilterListeners();
    }

    private void setupFilterListeners() {
        // TextWatchers для EditText
        addTextWatcher(filterSender, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanSenderFilter(text);
            else moveListViewModel.setKomplektuetsaSenderFilter(text);
        });
        setupEnterKeyListener(filterSender); // Добавляем слушатель Enter

        addTextWatcher(filterMovementNumber, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanMovementNumberFilter(text);
            else moveListViewModel.setKomplektuetsaMovementNumberFilter(text);
        });
        setupEnterKeyListener(filterMovementNumber); // Добавляем слушатель Enter

        addTextWatcher(filterRecipient, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanRecipientFilter(text);
            else moveListViewModel.setKomplektuetsaRecipientFilter(text);
        });
        setupEnterKeyListener(filterRecipient); // Добавляем слушатель Enter

        addTextWatcher(filterAssembler, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanAssemblerFilter(text);
            else moveListViewModel.setKomplektuetsaAssemblerFilter(text);
        });
        setupEnterKeyListener(filterAssembler); // Добавляем слушатель Enter

        addTextWatcher(filterReceiver, text -> {
            if (getCurrentTabPosition() == 0) moveListViewModel.setFormirovanReceiverFilter(text);
            else moveListViewModel.setKomplektuetsaReceiverFilter(text);
        });
        setupEnterKeyListener(filterReceiver); // Добавляем слушатель Enter

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
        if (scrollView == null) {
            Log.w("MoveList_menu", "scrollToView: NestedScrollView with ID R.id.filter_scroll_view not found in navigationView.");
            return;
        }

        // Используем post, чтобы дождаться, пока view будет полностью размещен и клавиатура (если есть) повлияет на размеры окна
            view.post(() -> {
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect); // Глобальные координаты поля ввода

            Rect windowRect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(windowRect); // Видимая область окна Activity

            int screenHeight = getWindow().getDecorView().getRootView().getHeight();
            int keyboardHeight = screenHeight - windowRect.bottom; // Предполагаемая высота клавиатуры
            
            // Если клавиатура не видна (или ее высота мала), и поле уже видимо, не скроллим
            // Это предотвратит ненужные скачки, если клавиатура уже скрыта или поле полностью видно.
            if (keyboardHeight < 100 && viewRect.top >= windowRect.top && viewRect.bottom <= windowRect.bottom) {
                 // Log.d("MoveList_menu", "scrollToView: Keyboard not significant or view already visible. No scroll needed.");
                    return;
                }

            // Целевая позиция Y для нижнего края поля ввода (с небольшим отступом)
            int targetViewBottomY = windowRect.bottom - dpToPx(16); // 16dp отступ от верха клавиатуры/низа видимой области

            // Текущая позиция нижнего края поля ввода на экране
            int currentViewBottomY = viewRect.bottom;

            if (currentViewBottomY > targetViewBottomY) {
                // Поле ввода перекрывается клавиатурой или находится слишком низко
                int scrollAmount = currentViewBottomY - targetViewBottomY;
                scrollView.smoothScrollBy(0, scrollAmount);
                Log.d("MoveList_menu", "scrollToView: Scrolling by " + scrollAmount);
        } else {
                // Поле ввода уже выше целевой позиции или полностью видно над клавиатурой
                // Можно добавить логику для прокрутки вниз, если поле слишком высоко, но обычно это не требуется.
                // Log.d("MoveList_menu", "scrollToView: View is already above target or fully visible.");
            }
        });
    }

    /**
     * Конвертирует dp в пиксели. Вспомогательный метод.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
        }

    // --- КОНЕЦ ДОБАВЛЕННЫХ МЕТОДОВ ---

    // +++ МЕТОДЫ ДЛЯ НАСТРОЙКИ UI (NavigationView и TouchListener) +++
    private void setupNavigationView() {
        if (navigationView == null || drawerLayout == null || navMenuButton == null) {
            Log.e("MoveList_menu", "setupNavigationView: NavigationView, DrawerLayout or NavMenuButton is null");
            return;
        }

        navMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Дополнительно проверяем и очищаем фокус
                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        // Не должно происходить, т.к. уже очистили в onTouch,
                        // но на всякий случай повторяем
                        currentFocus.clearFocus();
                        hideKeyboard(currentFocus);
                    }

                    // Запускаем анимацию кнопки (из исходной логики)
                    animateMenuButton(v);

                    // Открываем боковое меню с небольшой задержкой
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (drawerLayout != null) {
                                drawerLayout.openDrawer(GravityCompat.START);
                            }
                        }
                    }, 300); // Задержка открытия меню для завершения анимации
                } catch (Exception e) {
                    // Если что-то пошло не так, просто открываем шторку
                    Log.e("PrixodActivity", "Ошибка при обработке нажатия на menuButton: " + e.getMessage());
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            // TODO: Добавить обработку выбора пунктов меню
            Log.d("MoveList_menu", "Selected navigation item: " + item.getTitle());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Добавляем слушатель на DrawerLayout для вызова preventKeyboardShowOnDrawerClose
        // при закрытии шторки жестом или кнопкой "назад"
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                preventKeyboardShowOnDrawerClose();
                clearAllFocusInNavigationEditTexts();
                hideKeyboard();
                
                if (navMenuButton != null) {
                    navMenuButton.setFocusable(true);
                    navMenuButton.setFocusableInTouchMode(true);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (navMenuButton != null && navMenuButton.isAttachedToWindow()) { 
                           navMenuButton.requestFocus();
                        }
                    }, 100); 
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (filterMovementNumber != null && filterMovementNumber.hasFocus()) {
                    hideKeyboard(filterMovementNumber);
                }
                // Когда NavigationView открывается, даем ему возможность перехватывать нажатия клавиш
                // Это нужно для обработки Enter, когда фокус не на EditText
                if (navigationView != null) {
                    navigationView.setFocusable(true); // Убедимся, что он может получить фокус
                    navigationView.setFocusableInTouchMode(true);
                    navigationView.requestFocus(); // Передаем фокус, чтобы он мог слушать клавиши
                    navigationView.setOnKeyListener((v, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                View currentFocus = getCurrentFocus();
                                boolean focusIsOnEditTextInNav = false;
                                if (currentFocus instanceof EditText && isViewInsideViewGroup(navigationView, currentFocus)) {
                                    focusIsOnEditTextInNav = true;
                                }

                                if (!focusIsOnEditTextInNav) {
                                    drawerLayout.closeDrawer(GravityCompat.START);
                                    return true; // Событие обработано
                                }
                            }
                        }
                        return false; // Передаем событие дальше, если не обработано
                    });
                }
            }
        });
    }

    private void animateMenuButton(View view) {
        // Создаем набор аниматоров для комплексной анимации
        AnimatorSet animatorSet = new AnimatorSet();

        // Сохраняем оригинальный цвет фона
        final android.graphics.drawable.Drawable originalBackground = view.getBackground();

        // Анимация пульсации (увеличение/уменьшение)
        ObjectAnimator scaleXPulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 0.8f, 1.1f, 1f);
        ObjectAnimator scaleYPulse = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 0.8f, 1.1f, 1f);

        // Анимация вращения на 360 градусов с учетом Z-оси (3D эффект)
        ObjectAnimator rotationY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f);

        // Анимация смещения с эффектом пружины
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 15f, -10f, 0f);

        // Анимация изменения прозрачности
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f);

        // Анимация тени (elevation) для эффекта "всплытия" кнопки
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            float originalElevation = view.getElevation();
            ObjectAnimator elevation = ObjectAnimator.ofFloat(view, "elevation", originalElevation, originalElevation + 15f, originalElevation);
            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alpha, elevation);

            // Создаем эффект ripple программно
            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            view.setClipToOutline(true);
        } else {
            // Для старых устройств используем базовую анимацию
            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alpha);
        }

        // Настраиваем параметры анимации
        animatorSet.setDuration(500); // Оптимальная длительность

        // Используем пользовательский интерполятор для более плавной анимации
        animatorSet.setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f));

        // Добавляем слушатель завершения анимации
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Возвращаем исходные значения после анимации
                view.setRotationY(0f);
                view.setTranslationY(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setAlpha(1f);

                // Возвращаем оригинальный фон
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    view.setBackground(originalBackground);
                }
            }
        });

        // Запускаем анимацию
        animatorSet.start();

        // Добавляем тактильную обратную связь (вибрацию)
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

        // Создаем эффект круговой волны при нажатии (ripple)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Получаем координаты центра кнопки
            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;

            // Создаем маску для ripple эффекта
            int finalRadius = Math.max(view.getWidth(), view.getHeight());

            // Создаем анимацию ripple
            android.animation.Animator rippleAnim = android.view.ViewAnimationUtils.createCircularReveal(
                    view, centerX, centerY, 0, finalRadius);
            rippleAnim.setDuration(400);
            rippleAnim.start();

            // Дополнительно меняем фон на короткое время
            try {
                final int originalColor = 0xFF3F51B5; // Индиго
                final int highlightColor = 0xFF4CAF50; // Зеленый

                // Создаем временный цветной фон
                android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
                gradientDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                gradientDrawable.setColor(highlightColor);
                view.setBackground(gradientDrawable);

                // Возвращаем оригинальный цвет через небольшую задержку
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        android.graphics.drawable.GradientDrawable originalDrawable = new android.graphics.drawable.GradientDrawable();
                        originalDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        originalDrawable.setColor(originalColor);
                        view.setBackground(originalDrawable);
                    }
                }, 200);
            } catch (Exception e) {
                Log.e("MenuAnimation", "Ошибка при изменении цвета фона: " + e.getMessage());
            }
        }
    }
    // Метод setupTouchListener удален, так как его логика перенесена в dispatchTouchEvent
    // private void setupTouchListener() { ... }

    // --- КОНЕЦ МЕТОДОВ ДЛЯ НАСТРОЙКИ UI ---

    // +++ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ФОКУСОМ И КЛАВИАТУРОЙ +++

    /**
     * Проверяет, находится ли касание в пределах указанного View.
     * Копировано из PrixodActivity.
     */
    private boolean isTouchOnView(MotionEvent event, View view) {
        if (view == null || event == null) {
            return false;
        }
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);

        float touchX = event.getRawX();
        float touchY = event.getRawY();

        return (touchX >= viewLocation[0] && touchX <= viewLocation[0] + view.getWidth() &&
                touchY >= viewLocation[1] && touchY <= viewLocation[1] + view.getHeight());
    }

    /**
     * Проверяет, является ли view дочерним элементом group (прямым или косвенным).
     */
    private boolean isViewInsideViewGroup(ViewGroup group, View view) {
        if (group == null || view == null) {
            return false;
        }
        if (group == view) {
            return true;
        }
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == group) {
                return true;
            }
            if (!(parent instanceof View)) { // Дошли до Window или чего-то не View
                break;
            }
            parent = parent.getParent();
        }
        return false;
    }


    /**
     * Рекурсивно снимает фокус с EditText внутри ViewGroup.
     */
    private void clearFocusRecursivelyFromViewGroup(ViewGroup group) {
        if (group == null) {
            return; 
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof EditText) {
                child.clearFocus();
            } else if (child instanceof ViewGroup) {
                clearFocusRecursivelyFromViewGroup((ViewGroup) child);
            }
        }
    }

    /**
     * Снимает фокус со всех EditText внутри NavigationView.
     */
    private void clearAllFocusInNavigationEditTexts() {
        if (navigationView != null) {
            clearFocusRecursivelyFromViewGroup(navigationView);
        }
    }
    
    /**
     * Настраивает слушатель касаний для NestedScrollView в NavigationView,
     * чтобы обрабатывать клики вне полей ввода и скрывать клавиатуру.
     */
    private void setupFilterScrollViewTouchListener() {
        if (navigationView == null) return;
        NestedScrollView scrollView = navigationView.findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusedView = getCurrentFocus();
                    // Проверяем, является ли текущий фокус EditText и находится ли он внутри NavigationView
                    if (currentFocusedView instanceof EditText && isViewInsideViewGroup(navigationView, currentFocusedView)) {
                        // Если касание было НЕ на этом EditText
                        if (!isTouchOnView(event, currentFocusedView)) {
                            hideKeyboard(currentFocusedView); // Скрываем клавиатуру, используя токен текущего фокуса
                            currentFocusedView.clearFocus();
                            // Не потребляем событие (return false), чтобы ScrollView мог его обработать (например, для скроллинга)
                        }
                    }
                }
                return false; // Позволяем событию продолжить распространение для скроллинга
            });
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 1. Обработка, если NavigationView (боковое меню) открыто
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (navigationView != null && !isTouchOnView(event, navigationView)) {
                    // Касание вне NavigationView при открытом меню
                    drawerLayout.closeDrawer(GravityCompat.START);
                    clearAllFocusInNavigationEditTexts(); // Очищаем фокус в NavigationView
                    hideKeyboard(drawerLayout); // Используем hideKeyboard(drawerLayout) для большей надежности
                    return true; // Потребляем событие, чтобы предотвратить другие действия
                }
                // Если касание внутри NavigationView, позволяем событию обрабатываться дальше (например, setupFilterScrollViewTouchListener)
            }

            // 2. Обработка, если фокус на EditText внутри NavigationView (меню может быть открыто или закрыто)
                View currentFocusedView = getCurrentFocus();
            if (currentFocusedView instanceof EditText && navigationView != null && isViewInsideViewGroup(navigationView, currentFocusedView)) {
                if (!isTouchOnView(event, currentFocusedView)) {
                    // Касание вне текущего сфокусированного EditText в NavigationView
                    hideKeyboard(currentFocusedView);
                    currentFocusedView.clearFocus();
                    // Не потребляем событие здесь полностью (super.dispatchTouchEvent все равно вызовется),
                    // чтобы позволить другим элементам среагировать, если это необходимо,
                    // но основная работа по снятию фокуса и скрытию клавиатуры сделана.
                }
            }
            // 3. Если просто тапнули по основной части экрана (R.id.main) и клавиатура была открыта для какого-то поля (не обязательно в NavigationView)
            // Это покрывается общим случаем выше, если currentFocusedView - это EditText.
            // Если нужно специфичное поведение для R.id.main, его можно добавить, но лучше обобщить.
            // View mainArea = findViewById(R.id.main); // Предположим, это ваш главный контейнер контента
            // if (mainArea != null && isTouchOnView(event, mainArea) && !(currentFocusedView instanceof EditText && isTouchOnView(event, currentFocusedView))) {
            //    if (currentFocusedView instanceof EditText) { // Если фокус был на каком-то EditText
            //        hideKeyboard(currentFocusedView);
            //        currentFocusedView.clearFocus();
            //    } else { // Если фокус не был на EditText, но клавиатура могла быть активна
            //        hideKeyboard();
            //    }
            // }


        }
        return super.dispatchTouchEvent(event);
    }
    
    // --- КОНЕЦ ВСПОМОГАТЕЛЬНЫХ МЕТОДОВ ---


    /**
     * Обновляет визуальное отображение выбранного сегмента в соответствии с текущей позицией вкладки
     */
    private void hideKeyboard(View fromView) {
        if (fromView == null) {
            hideKeyboard(); // Fallback to default hideKeyboard if fromView is null
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (fromView.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(fromView.getWindowToken(), 0);
        }
    }
}