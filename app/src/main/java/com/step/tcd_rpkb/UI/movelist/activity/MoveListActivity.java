package com.step.tcd_rpkb.UI.movelist.activity;

import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_FORMIROVAN;
import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_KOMPLEKTUETSA;
import static com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel.STATUS_PODGOTOVLEN;

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
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar; // << ДОБАВЛЕН ИМПОРТ
import com.google.gson.Gson; // Добавляем импорт Gson
import com.step.tcd_rpkb.UI.Prixod.activity.ProductsActivity;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.User; // Добавляем импорт User
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.movelist.fragments.MoveListFragment;
import com.step.tcd_rpkb.UI.movelist.viewmodel.MoveListViewModel; // Добавляем импорт
import com.step.tcd_rpkb.UI.movelist.adapters.ViewPagerAdapter;
import com.step.tcd_rpkb.utils.AvatarUtils; // Добавляем импорт AvatarUtils
import com.step.tcd_rpkb.utils.UserViewAnimations; // Добавляем импорт UserViewAnimations
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase; // Добавляем импорт GetUserUseCase

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject; // Импорт Inject

import dagger.hilt.android.AndroidEntryPoint;
import com.step.tcd_rpkb.utils.SingleEvent; // Добавляем импорт для SingleEvent

@AndroidEntryPoint
public class MoveListActivity extends com.step.tcd_rpkb.base.BaseFullscreenActivity {
    private MoveListViewModel moveListViewModel;

    @Inject
    GetUserUseCase getUserUseCase;
    
    private Gson gson = new Gson(); // Для сериализации MoveItem
    
    private MoveListFragment formirovanFragment;
    private MoveListFragment komplektuetsaFragment;
    private MoveListFragment podgotovlenFragment;


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
    private EditText filterNomenculature;
    private EditText filterSeries;
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
    private ImageButton clearNomeculature;
    private ImageButton clearSeries;
    private  TextView btnApplyFilters;
    private TextView btnResetFilters;
    private TextView btnSetDefaultFilters;
    
    // TextView для отображения ФИО пользователя и роли
    private TextView userFullNameTextView;
    private TextView userRoleTextView;
    private ImageView userAvatarImageView;
    //Переключатели вверху страницы
    private TextView segmentFormirovano;
    private TextView segmentKomplektuetsa;
    private TextView segmentPodgotovlen;
    

    
    // Для фильтрации
    private final Handler filterHandler = new Handler(Looper.getMainLooper());
    private static final long FILTER_DELAY_MS = 300;
    private Runnable filterRunnable;



    //константы для типов перемещения элементов
    private static final int MOVE_TO_KOMPLEKTUETSA = 1;
    private static final int MOVE_TO_FORMIROVAN = 2;
    private static final int MOVE_TO_WORK = 3;
    private static final int FINISH_WORK = 4;


    // Компоненты панели множественного выбора
    private CardView actionButtonsPanel;
    private TextView selectedItemsCount;
    private Button btnMoveToWork;
    private Button btnMoveFromWork;
    private Button btnFinishWork;
    private Button btnCancelSelection;
    
    // Флаг, определяющий режим выбора элементов
    private boolean isSelectionMode = false;
    private FloatingActionButton refreshButton;
    // Для фоновых операций
    private com.step.tcd_rpkb.utils.LoadingDialog loadingDialog; // кастомный диалог

    

    private boolean formirovanDataReadyForChart = false;
    private boolean komplektuetsaDataReadyForChart = false;
    private boolean podgotovlenDataReadyForChart = false;


    private boolean isMultiMoveActive = false;
    private List<String> multiMoveSuccessGuids = null;
    private String multiMoveTargetState = null;
    
    // Флаг для блокировки автоматической прокрутки к верху во время навигации к элементу
    private boolean isNavigatingToSpecificItem = false;

    /**
     * Возвращает текущую позицию вкладки
     * @return индекс текущей вкладки (0 - "Сформирован", 1 - "Комплектуется", 2 - "Подготовлен")
     */
    public int getCurrentTabPosition() {
        Integer tabPosition = moveListViewModel.currentTabPosition.getValue();
        return tabPosition != null ? tabPosition : 0;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movelist);
        
        // Инициализация ViewModel через Hilt
        moveListViewModel = new ViewModelProvider(this).get(MoveListViewModel.class);
        

        
        loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(this);
        
        // Проверяем пришли ли мы из Prixod с флагом restoreFilters
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
            

            int keyboardHeight = screenHeight - r.bottom;
            boolean isKeyboardShowing = keyboardHeight > screenHeight * 0.25;
            
            if (isKeyboardShowing) {

                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {

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
                

                User currentUser = getUserUseCase.execute();

                if (currentUser != null && userFullNameTextView != null && userRoleTextView != null && userAvatarImageView != null) {
                    userFullNameTextView.setText(currentUser.getFullName());
                    userRoleTextView.setText(currentUser.getRole());
                    String initials = AvatarUtils.getInitials(currentUser.getFullName());
                    userAvatarImageView.setImageDrawable(AvatarUtils.createTextAvatar(this, initials));
                
                //анимация для карточки пользователя

                    UserViewAnimations.playFancyAnimation(userCard, userAvatarImageView, userFullNameTextView, userRoleTextView);
                
                // эффект мерцания для аватара

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (userAvatarImageView != null) {
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
        segmentPodgotovlen = findViewById(R.id.segment_podgotovlen);
        
        // Явно устанавливаем состояние выбора для первого сегмента
        segmentFormirovano.setSelected(true);
        segmentKomplektuetsa.setSelected(false);
        segmentPodgotovlen.setSelected(false);
        
        // Предотвращаем получение фокуса сегментами
        preventSegmentsFocus();
        
        // Настройка тулбара
        setupSegmentedControl();
        
        // Инициализация фрагментов
        formirovanFragment = MoveListFragment.newInstance(STATUS_FORMIROVAN);
        komplektuetsaFragment = MoveListFragment.newInstance(STATUS_KOMPLEKTUETSA);
        podgotovlenFragment = MoveListFragment.newInstance(STATUS_PODGOTOVLEN);
        
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
        

        if (filterMovementNumber != null) {
            filterMovementNumber.setFocusable(true);
            filterMovementNumber.setFocusableInTouchMode(true);
            filterMovementNumber.setClickable(true);
            filterMovementNumber.setShowSoftInputOnFocus(false); // Убедимся, что флаг установлен
            
            // Добавляем возможность запрашивать фокус по клику
            filterMovementNumber.setOnClickListener(v -> {
                v.requestFocus();
                hideKeyboard();
            });
        }
        


        
        // Настраивает панель множественного выбора
        setupSelectionPanel();
        
        // Инициализируем кнопку обновления (изначально скрыта)
        setupRefreshButton();
        
        // Подписываемся на LiveData из ViewModel
        observeViewModel();
        
        // Загружаем фильтры по умолчанию для текущего пользователя
        moveListViewModel.loadDefaultFiltersForCurrentUser();
        
            // Загружаем данные из JSON и обновляем фрагменты

        moveListViewModel.loadMoveData();



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
                            loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(MoveListActivity.this);
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

            }
        });

        // Наблюдение за оригинальными списками

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
                    updatePriorityBarChart(new ArrayList<>()); //
                }
            }

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
                     updatePriorityBarChart(new ArrayList<>());
                }
            }

        });
        
        moveListViewModel.originalPodgotovlenList.observe(this, list -> {
            Log.d("MoveList_menu", "Original Podgotovlen list updated in ViewModel, size: " + (list != null ? list.size() : 0));
            podgotovlenDataReadyForChart = true; // Данные загружены
            // Попытка обновить диаграмму здесь, если это текущая вкладка
            if (getCurrentTabPosition() == 2) {
                List<MoveItem> currentFilteredList = moveListViewModel.filteredPodgotovlenList.getValue();
                if (currentFilteredList != null) {
                    Log.d("MoveList_menu", "Original Podgotovlen loaded, forcing chart update for tab 2. Size: " + currentFilteredList.size());
                    updatePriorityBarChart(new ArrayList<>(currentFilteredList));
                } else {
                    updatePriorityBarChart(new ArrayList<>()); //
                }
            }

        });
        
        // Наблюдение за отфильтрованными списками для обновления диаграммы приоритетов
        moveListViewModel.filteredFormirovanList.observe(this, list -> {
            Log.d("MoveList_menu", "[filteredFormirovanList] Triggered. Current Tab: " + getCurrentTabPosition() + ". List size: " + (list != null ? list.size() : "null"));
            
            // Прокручиваем к началу списка ТОЛЬКО если не выполняется навигация к конкретному элементу
            if (getCurrentTabPosition() == 0 && !isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredFormirovanList] Инициируем прокрутку к началу для вкладки 0");
                scrollToTopOfCurrentListWithDelay();
            } else if (getCurrentTabPosition() == 0 && isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredFormirovanList] Пропускаем прокрутку к началу - выполняется навигация к элементу");
            }
            
            // Обновляем диаграмму, только если основные данные уже были загружены для этой вкладки
            if (getCurrentTabPosition() == 0 && formirovanDataReadyForChart && list != null) {
                updatePriorityBarChart(new ArrayList<>(list)); // Передаем копию

            } else if (getCurrentTabPosition() == 0 && formirovanDataReadyForChart && list == null) {

                updatePriorityBarChart(new ArrayList<>()); // Пустая диаграмма, если список стал null после загрузки
            }
        });

        moveListViewModel.filteredKomplektuetsaList.observe(this, list -> {
            Log.d("MoveList_menu", "[filteredKomplektuetsaList] Triggered. Current Tab: " + getCurrentTabPosition() + ". List size: " + (list != null ? list.size() : "null"));
            
            // Прокручиваем к началу списка ТОЛЬКО если не выполняется навигация к конкретному элементу
            if (getCurrentTabPosition() == 1 && !isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredKomplektuetsaList] Инициируем прокрутку к началу для вкладки 1");
                scrollToTopOfCurrentListWithDelay();
            } else if (getCurrentTabPosition() == 1 && isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredKomplektuetsaList] Пропускаем прокрутку к началу - выполняется навигация к элементу");
            }
            
            // Обновляем диаграмму, только если основные данные уже были загружены для этой вкладки
            if (getCurrentTabPosition() == 1 && komplektuetsaDataReadyForChart && list != null) {
                updatePriorityBarChart(new ArrayList<>(list));
            } else if (getCurrentTabPosition() == 1 && komplektuetsaDataReadyForChart && list == null) {
                 updatePriorityBarChart(new ArrayList<>());
            }
        });
        
        moveListViewModel.filteredPodgotovlenList.observe(this, list -> {
            Log.d("MoveList_menu", "[filteredPodgotovlenList] Triggered. Current Tab: " + getCurrentTabPosition() + ". List size: " + (list != null ? list.size() : "null"));
            
            // Прокручиваем к началу списка ТОЛЬКО если не выполняется навигация к конкретному элементу
            if (getCurrentTabPosition() == 2 && !isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredPodgotovlenList] Инициируем прокрутку к началу для вкладки 2");
                scrollToTopOfCurrentListWithDelay();
            } else if (getCurrentTabPosition() == 2 && isNavigatingToSpecificItem) {
                Log.d("MoveList_menu", "[filteredPodgotovlenList] Пропускаем прокрутку к началу - выполняется навигация к элементу");
            }
            
            // Обновляем диаграмму, только если основные данные уже были загружены для этой вкладки
            if (getCurrentTabPosition() == 2 && podgotovlenDataReadyForChart && list != null) {
                updatePriorityBarChart(new ArrayList<>(list));
            } else if (getCurrentTabPosition() == 2 && podgotovlenDataReadyForChart && list == null) {
                updatePriorityBarChart(new ArrayList<>());
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

        // Наблюдение за событием загрузки кэшированных данных
        moveListViewModel.loadCachedDataEvent.observe(this, (SingleEvent<MoveListViewModel.CachedMovementData> event) -> {
            if (event != null) {
                MoveListViewModel.CachedMovementData cachedData = event.getContentIfNotHandled();
                if (cachedData != null) {
                    Log.d("MoveList_menu", "Получено событие loadCachedDataEvent для moveUuid: " + cachedData.moveUuid + 
                          ", размер данных: " + (cachedData.productsJson != null ? cachedData.productsJson.length() : 0) +
                          ", preserveEditedData: " + cachedData.preserveEditedData);
                    
                    // Запускаем PrixodActivity с кэшированными данными
                    android.content.Intent intent = new android.content.Intent(MoveListActivity.this, ProductsActivity.class);
                    intent.putExtra("moveUuid", cachedData.moveUuid);
                    intent.putExtra("productsData", cachedData.productsJson);
                    intent.putExtra("preserveEditedData", cachedData.preserveEditedData);
                    
                    // Получаем полный объект MoveItem и передаем его как JSON
                    MoveItem moveItem = moveListViewModel.getMoveItemByUuid(cachedData.moveUuid);
                    if (moveItem != null) {
                        String moveItemJson = gson.toJson(moveItem);
                        intent.putExtra("moveItemJson", moveItemJson);
                        Log.d("MoveList_menu", "Передаем полный MoveItem для перемещения: " + cachedData.moveUuid);
                    } else {
                        Log.w("MoveList_menu", "MoveItem не найден для перемещения: " + cachedData.moveUuid);
                    }
                    
                    Log.d("MoveList_menu", "Запуск PrixodActivity с кэшированными данными для перемещения: " + cachedData.moveUuid);
                    startActivityForResult(intent, 1001);
                }
            }
        });

        // Наблюдение за событием навигации в PrixodActivity
        moveListViewModel.navigateToPrixodEvent.observe(this, (SingleEvent<String> event) -> {
            if (event != null) {
                String movementId = event.getContentIfNotHandled(); // Получаем ID, если событие не обработано
                if (movementId != null) {
                    Log.d("MoveList_menu", "Получено событие navigateToPrixodEvent для movementId: " + movementId);
                    
                    // Запускаем PrixodActivity БЕЗ кэшированных данных (обычная загрузка с сервера)
                    android.content.Intent intent = new android.content.Intent(MoveListActivity.this, ProductsActivity.class);
                    intent.putExtra("moveUuid", movementId);
                    
                    // Получаем полный объект MoveItem и передаем его как JSON
                    MoveItem moveItem = moveListViewModel.getMoveItemByUuid(movementId);
                    if (moveItem != null) {
                        String moveItemJson = gson.toJson(moveItem);
                        intent.putExtra("moveItemJson", moveItemJson);
                        Log.d("MoveList_menu", "Передаем полный MoveItem для перемещения: " + movementId);
                    } else {
                        Log.w("MoveList_menu", "MoveItem не найден для перемещения: " + movementId);
                    }
                    
                    Log.d("MoveList_menu", "Запуск PrixodActivity для нового перемещения: " + movementId + " (обычная загрузка с сервера)");
                    startActivityForResult(intent, 1001);
                }
            }
        });
        
        moveListViewModel.showToastEvent.observe(this, (SingleEvent<String> event) -> {
            if (event != null) {
                String message = event.getContentIfNotHandled();
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(MoveListActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Наблюдатель для ошибки с пустыми УИДСтрокиТовары
        moveListViewModel.showEmptyProductLineIdErrorEvent.observe(this, (SingleEvent<String> event) -> {
            if (event != null) {
                String errorMessage = event.getContentIfNotHandled();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    showEmptyProductLineIdErrorDialog(errorMessage);
                }
            }
        });
        
        // Наблюдатель для ошибки с пустым перемещением
        moveListViewModel.showEmptyMovementErrorEvent.observe(this, (SingleEvent<String> event) -> {
            if (event != null) {
                String errorMessage = event.getContentIfNotHandled();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    showEmptyMovementErrorDialog(errorMessage);
                }
            }
        });
        
        moveListViewModel.isProcessingItemClick.observe(this, isProcessing -> {
            if (isProcessing != null) {
                if (isProcessing) {
                    loadingDialog.show("Проверка документа...");
        } else {
                loadingDialog.dismiss();
            }
        }
        });
        


        // Наблюдение за событием показа большого диалога ошибки
        moveListViewModel.showErrorDialogEvent.observe(this, event -> {
            if (event != null) {
                com.step.tcd_rpkb.UI.movelist.viewmodel.ErrorDialogData errorData = event.getContentIfNotHandled();
                if (errorData != null) {
                    String error = errorData.getMessage();
                    String title = errorData.getTitle();
                    
                    AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(error)
                        .setCancelable(false)
                        .setPositiveButton("ОК", (d, which) -> {
                            d.dismiss();
                            // Проверяем, является ли это финальной ошибкой массовой обработки
                            if (error.contains("Массовая обработка завершена:")) {
                                // Это финальная ошибка - завершаем массовую обработку без продолжения
                                isMultiMoveActive = false;
                            } else if (isMultiMoveActive) {
                                // Это промежуточная ошибка - продолжаем обработку
                                moveListViewModel.continueMultiMove();
                            }
                        })
                        .show();
                }
            }
        });

        // Наблюдение за событием успешной смены статуса
        moveListViewModel.showSuccessStatusChangeEvent.observe(this, event -> {
            if (event != null) {
                MoveListViewModel.SuccessStatusChangeEvent successEvent = event.getContentIfNotHandled();
                if (successEvent != null) {
                    if (isMultiMoveActive) {
                        if (multiMoveSuccessGuids != null) multiMoveSuccessGuids.add(successEvent.moveGuid);
                        
                        // Проверяем, является ли это финальным элементом массовой обработки
                        if (successEvent.message != null && successEvent.message.contains("Массовая обработка завершена:")) {
                            // Это финальное сообщение - показываем диалог с переходом и завершаем массовую обработку
                            Log.d("MoveList_menu", "Получено финальное сообщение массовой обработки: " + successEvent.message);
                            isMultiMoveActive = false;
                            showSuccessDialog(successEvent);
                        } else {
                            // Это промежуточное сообщение - показываем диалог без перехода
                            Log.d("MoveList_menu", "Получено промежуточное сообщение массовой обработки: " + successEvent.message);
                            showSuccessDialogMulti(successEvent);
                        }
                    } else {
                        showSuccessDialog(successEvent);
                    }
                }
            }
        });

        // Наблюдение за событиями работы с фильтрами по умолчанию
        moveListViewModel.showDefaultFiltersMessageEvent.observe(this, event -> {
            if (event != null) {
                String message = event.getContentIfNotHandled();
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.d("MoveList_menu", "Показано сообщение о фильтрах по умолчанию: " + message);
                }
            }
        });

    }



    private void observeFilterViewModel() {

        moveListViewModel.currentTabPosition.observe(this, position -> {
            if (position != null) {
                updateFiltersUi(); // Обновить UI при смене вкладки
                updateSegmentSelection(position); // Обновить выделение сегмента

            }
        });

        // Единые фильтры для всех вкладок
        moveListViewModel.senderFilter.observe(this, value -> {
            if (filterSender != null) {
                String currentValue = filterSender.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterSender.setText(newValue);
                    if (filterSender.hasFocus()) {
                        filterSender.setSelection(filterSender.getText().length());
                    }
                }
            }
        });
        
        moveListViewModel.movementNumberFilter.observe(this, value -> {
            if (filterMovementNumber != null) {
                String currentValue = filterMovementNumber.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterMovementNumber.setText(newValue);
                    if (filterMovementNumber.hasFocus()) {
                        filterMovementNumber.setSelection(filterMovementNumber.getText().length());
                    }
                }
            }
        });

        moveListViewModel.nomenculatureFilter.observe(this, value -> {
            if (filterNomenculature != null) {
                String currentValue = filterNomenculature.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterNomenculature.setText(newValue);
                    if (filterNomenculature.hasFocus()) {
                        filterNomenculature.setSelection(filterNomenculature.getText().length());
                    }
                }
            }
        });

        moveListViewModel.seriesFilter.observe(this, value -> {
            if (filterSeries != null) {
                String currentValue = filterSeries.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterSeries.setText(newValue);
                    if (filterSeries.hasFocus()) {
                        filterSeries.setSelection(filterSeries.getText().length());
                    }
                }
            }
        });
        
        moveListViewModel.recipientFilter.observe(this, value -> {
            if (filterRecipient != null) {
                String currentValue = filterRecipient.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterRecipient.setText(newValue);
                    if (filterRecipient.hasFocus()) {
                        filterRecipient.setSelection(filterRecipient.getText().length());
                    }
                }
            }
        });
        
        moveListViewModel.assemblerFilter.observe(this, value -> {
            if (filterAssembler != null) {
                String currentValue = filterAssembler.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterAssembler.setText(newValue);
                    if (filterAssembler.hasFocus()) {
                        filterAssembler.setSelection(filterAssembler.getText().length());
                    }
                }
            }
        });
        
        moveListViewModel.receiverFilter.observe(this, value -> {
            if (filterReceiver != null) {
                String currentValue = filterReceiver.getText().toString();
                String newValue = (value == null) ? "" : value;
                if (!currentValue.equals(newValue)) {
                    filterReceiver.setText(newValue);
                    if (filterReceiver.hasFocus()) {
                        filterReceiver.setSelection(filterReceiver.getText().length());
                    }
                }
            }
        });
        
        moveListViewModel.priorityFilter.observe(this, value -> {
            setSpinnerSelection(filterPriority, value);
        });
        
        moveListViewModel.cpsChecked.observe(this, value -> { 
            if (value != null && filterCps != null && filterCps.isChecked() != value) {
                filterCps.setChecked(value); 
            }
        });
        
        moveListViewModel.availabilityChecked.observe(this, value -> { 
            if (value != null && filterAvailability != null && filterAvailability.isChecked() != value) {
                filterAvailability.setChecked(value); 
            }
        });
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
        if (spinner.getSelectedItemPosition() != 0) { // Если значение не найдено, выбираем "Все"
             spinner.setSelection(0);
        }
    }
    
    /**
     * Устанавливает фильтр приоритета и обновляет Spinner
     */
    private void setPriorityFilterAndUpdateSpinner(String priority) {

        moveListViewModel.setPriorityFilter(priority);
        

        if (filterPriority != null) {
            setSpinnerSelection(filterPriority, priority);
        }
        
        // Прокручиваем к началу списка после применения фильтра приоритета
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            scrollToTopOfCurrentListWithDelay();
        }, 200);
    }

    /**
     * Предотвращает появление клавиатуры при закрытии NavigationView
     */
    private void preventKeyboardShowOnDrawerClose() {
        // Получаем текущий фокус перед действиями
        View currentFocus = getCurrentFocus();
        boolean wasOnMovementNumber = (currentFocus == filterMovementNumber);
        boolean wasOnSeriesNumber = (currentFocus == filterSeries);
        
        // Забираем фокус у EditText, кроме filter_movement_number
        if (filterSender != null ) filterSender.clearFocus();
        if (filterNomenculature != null) filterNomenculature.clearFocus();
        if (filterRecipient != null) filterRecipient.clearFocus();
        if (filterAssembler != null) filterAssembler.clearFocus();
        if (filterReceiver != null) filterReceiver.clearFocus();
        

        if (!wasOnMovementNumber || !wasOnSeriesNumber) {
            View rootView = findViewById(R.id.main);
            if (rootView != null) {
                rootView.requestFocus();
            }
        }
        
        // Принудительно скрываем клавиатуру
        hideKeyboard();
        

        if (wasOnMovementNumber && filterMovementNumber != null) {

            new Handler().postDelayed(() -> {
                filterMovementNumber.requestFocus();
                disableKeyboardForNumericField(filterMovementNumber);
            }, 100);
        }
        if (wasOnSeriesNumber && filterSeries != null){
            filterSeries.requestFocus();
            disableKeyboardForNumericField(filterSeries);
        }
        
        // Восстанавливаем позицию прокрутки NavigationView наверх
        NestedScrollView scrollView = navigationView.findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, 0), 100);
        }
        

    }

    /**
     * Обновляет UI фильтров на основе текущего состояния фильтров
     */
    private void updateFiltersUi() {
        // Обновляем UI элементы в соответствии с текущими значениями единых фильтров из ViewModel
        Log.d("MoveList_menu", "updateFiltersUi для единых фильтров");

        filterSender.setText(moveListViewModel.senderFilter.getValue());
        filterMovementNumber.setText(moveListViewModel.movementNumberFilter.getValue());
        filterNomenculature.setText(moveListViewModel.nomenculatureFilter.getValue());
        filterSeries.setText(moveListViewModel.seriesFilter.getValue());
        filterRecipient.setText(moveListViewModel.recipientFilter.getValue());
        filterAssembler.setText(moveListViewModel.assemblerFilter.getValue());
        filterReceiver.setText(moveListViewModel.receiverFilter.getValue());
        setSpinnerSelection(filterPriority, moveListViewModel.priorityFilter.getValue());
        
        Boolean cpsChecked = moveListViewModel.cpsChecked.getValue();
            if (cpsChecked != null) filterCps.setChecked(cpsChecked);
        
        Boolean availChecked = moveListViewModel.availabilityChecked.getValue();
            if (availChecked != null) filterAvailability.setChecked(availChecked);
        

    }
    
    /**
     * Сбрасывает все фильтры для текущей вкладки
     */
    private void resetFilters() {
        // Сбрасываем единые фильтры
        moveListViewModel.setSenderFilter("");
        moveListViewModel.setMovementNumberFilter("");
        moveListViewModel.setNomenculatureFilter("");
        moveListViewModel.setSeriesFilter("");
        moveListViewModel.setRecipientFilter("");
        moveListViewModel.setAssemblerFilter("");
        moveListViewModel.setPriorityFilter("");
        moveListViewModel.setReceiverFilter("");
        moveListViewModel.setCpsChecked(true);
        moveListViewModel.setAvailabilityChecked(true);

        hideKeyboard();
        clearFocus();
        drawerLayout.closeDrawer(GravityCompat.START);
        
        // Прокручиваем к началу списка после сброса фильтров
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            scrollToTopOfCurrentListWithDelay();
        }, 300);

        Log.d("MoveList_menu", "Единые фильтры сброшены");
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
        
        // Обработчик для сегмента "Подготовлен"
        segmentPodgotovlen.setOnClickListener(v -> {
            if (getCurrentTabPosition() != 2) {
                viewPager.setCurrentItem(2, true);
            }
        });
    }
    
    /**
     * Анимирует переключение между сегментами навигационных кнопок
     */
    private void animateSegmentSelection(TextView selected, TextView unselected) {
        if (selected == null || unselected == null) return;
        

        selected.setSelected(true);
        selected.setAlpha(0.7f);
        unselected.setSelected(false);
        unselected.setAlpha(1.0f);
        

        selected.setPivotX(selected.getWidth() / 2f);
        selected.setPivotY(selected.getHeight() / 2f);
        

        selected.setScaleX(0.95f);
        selected.setScaleY(0.95f);
        
        // Запускаем анимацию в фоновом потоке с небольшой задержкой

        new Handler().postDelayed(() -> {

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(selected, "scaleX", 0.95f, 1.05f, 1.0f);
            scaleX.setDuration(350);
            scaleX.setInterpolator(new OvershootInterpolator(1.5f));
            
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(selected, "scaleY", 0.95f, 1.05f, 1.0f);
            scaleY.setDuration(350);
            scaleY.setInterpolator(new OvershootInterpolator(1.5f));
            
            // Анимируем прозрачность
            ObjectAnimator alpha = ObjectAnimator.ofFloat(selected, "alpha", 0.7f, 1.0f);
            alpha.setDuration(350);
            

            scaleX.start();
            scaleY.start();
            alpha.start();
            
            // Немного меняем цвет фона невыбранного сегмента
            unselected.animate()
                .alpha(0.5f)
                .setDuration(200)
                .start();
            

            new Handler().postDelayed(() -> {
                unselected.animate()
                    .alpha(0.7f)
                    .setDuration(150)
                    .start();
            }, 100);
        }, 16);
    }

    /**
     * Предотвращает получение фокуса сегментами переключения
     */
    private void preventSegmentsFocus() {
        // Устанавливаем focusable в false для предотвращения получения фокуса
        segmentFormirovano.setFocusable(false);
        segmentKomplektuetsa.setFocusable(false);
        segmentPodgotovlen.setFocusable(false);
        

        segmentFormirovano.setFocusableInTouchMode(false);
        segmentKomplektuetsa.setFocusableInTouchMode(false);
        segmentPodgotovlen.setFocusableInTouchMode(false);
    }
    
    /**
     * Настраивает ViewPager2 и адаптер для переключения между фрагментами
     */
    private void setupViewPager() {
        // Создаем адаптер и добавляем фрагменты
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPagerAdapter.addFragment(formirovanFragment);
        viewPagerAdapter.addFragment(komplektuetsaFragment);
        viewPagerAdapter.addFragment(podgotovlenFragment);
        
        // Улучшаем производительность ViewPager2 с помощью кэширования страниц
        viewPager.setOffscreenPageLimit(3);
        
        // Устанавливаем адаптер для ViewPager2
        viewPager.setAdapter(viewPagerAdapter);
        

        
        // Обработчик переключения страниц
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                // Очищаем выбранные элементы на предыдущей вкладке
                clearSelectionInCurrentFragment();

                

                moveListViewModel.setCurrentTabPosition(position);

                
                // Используем Handler только для отложенного обновления UI и применения фильтров
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {

                        
                        // обновление статистики для нового таба
                        if (position == 0) {
                            List<MoveItem> list = moveListViewModel.filteredFormirovanList.getValue();
                            if (list != null) updatePriorityBarChart(list);
                        } else if (position == 1) {
                            List<MoveItem> list = moveListViewModel.filteredKomplektuetsaList.getValue();
                            if (list != null) updatePriorityBarChart(list);
                        } else {
                            List<MoveItem> list = moveListViewModel.filteredPodgotovlenList.getValue();
                            if (list != null) updatePriorityBarChart(list);
                        }

                    } catch (Exception e) {
                        Log.e("MoveList_menu", "Ошибка при обновлении данных после смены вкладки: " + e.getMessage(), e);
                    }
                });
                
                // Анимация переключения сегментов сразу без задержки
                if (position == 0) {
                    animateSegmentSelection(segmentFormirovano, segmentKomplektuetsa);
                } else if (position == 1) {
                    animateSegmentSelection(segmentKomplektuetsa, segmentFormirovano);
                } else {
                    animateSegmentSelection(segmentPodgotovlen, segmentFormirovano);
                }
                
            }
        });
    }
    


    /**
     * Обновляет визуальное отображение выбранного сегмента в соответствии с текущей позицией вкладки
     */
    private void updateSegmentSelection(int position) {
        if (segmentFormirovano != null && segmentKomplektuetsa != null && segmentPodgotovlen != null) {
            if (position == 0) {
                segmentFormirovano.setSelected(true);
                segmentKomplektuetsa.setSelected(false);
                segmentPodgotovlen.setSelected(false);
            } else if (position == 1) {
                segmentFormirovano.setSelected(false);
                segmentKomplektuetsa.setSelected(true);
                segmentPodgotovlen.setSelected(false);
            } else {
                segmentFormirovano.setSelected(false);
                segmentKomplektuetsa.setSelected(false);
                segmentPodgotovlen.setSelected(true);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MoveList_menu", "onResume called");

        // Установка глобального слушателя при возобновлении активности
        setupGlobalFocusChangeListener();
    }


    private void applyFilters(){
        moveListViewModel.setSenderFilter(String.valueOf(filterSender.getText()));
        moveListViewModel.setMovementNumberFilter(String.valueOf(filterMovementNumber.getText()));
        moveListViewModel.setNomenculatureFilter(String.valueOf(filterNomenculature.getText()));
        moveListViewModel.setSeriesFilter(String.valueOf(filterSeries.getText()));
        moveListViewModel.setRecipientFilter(String.valueOf(filterRecipient.getText()));
        moveListViewModel.setAssemblerFilter(String.valueOf(filterAssembler.getText()));
        String selectedPriority = filterPriority.getSelectedItem().toString();
        if ("Все".equals(selectedPriority)) {
            selectedPriority = ""; // пустая строка для фильтра "Все"
        }
        moveListViewModel.setPriorityFilter(selectedPriority);
        moveListViewModel.setReceiverFilter(String.valueOf(filterReceiver.getText()));
        moveListViewModel.setCpsChecked(filterCps.isChecked());
        moveListViewModel.setAvailabilityChecked((filterAvailability.isChecked()));
        moveListViewModel.applyFilters();
    }

    /**
     * Настраивает слушатель для обработки нажатия клавиши Enter в полях EditText
     * @param editText поле ввода, для которого настраивается слушатель
     */
    private void setupEnterKeyListener(EditText editText) {
        if (editText == null) return;

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                hideKeyboard();
                v.clearFocus();
                applyFilters();

                if (drawerLayout != null) {
                    drawerLayout.requestFocus();
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

                hideKeyboard();
                v.clearFocus();
                applyFilters();
                if (drawerLayout != null) {
                    drawerLayout.requestFocus();
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true; 
            }
            return false;
        });
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
            editText.setShowSoftInputOnFocus(false);
        } catch (Exception e) {

        }
    }

    /**
     * Добавляем глобальный слушатель изменения фокуса для обработки перехода между полями
     */
    private void setupGlobalFocusChangeListener() {
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
            (oldFocus, newFocus) -> {
                if (newFocus == filterMovementNumber || newFocus == filterSeries) {
                    // Если фокус перешёл на поле "Номер перемещения", скрываем клавиатуру
                    // но НЕ убираем фокус с поля
                    if(filterMovementNumber != null) {
                        filterMovementNumber.setShowSoftInputOnFocus(false);
                    }
                    if(filterSeries != null) {
                        filterSeries.setShowSoftInputOnFocus(false);
                    }

                    hideKeyboard();
                } else if (oldFocus == filterMovementNumber && newFocus instanceof EditText) {

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
            } else {
                statsContainer.removeAllViews(); // Очищаем, если там что-то другое было
                chartView = getLayoutInflater().inflate(R.layout.priority_bar_chart_movelist, statsContainer, false);
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
            
            // Используем те же цвета, что и в RecyclerView
            urgentSection.setBackgroundColor(Color.parseColor("#8B0000")); // Неотложный (бордовый/темно-красный)
            highSection.setBackgroundColor(Color.parseColor("#FF6347")); // Высокий (красный ближе к оранжевому)
            mediumSection.setBackgroundColor(Color.YELLOW); // Средний (желтый)
            lowSection.setBackgroundColor(Color.GREEN); // Низкий/Без приоритета (зеленый)
            
            // Добавляем обработчики нажатий для фильтрации по приоритету
            urgentSection.setOnClickListener(v -> setPriorityFilterAndUpdateSpinner(MoveListViewModel.PRIORITY_URGENT));
            highSection.setOnClickListener(v -> setPriorityFilterAndUpdateSpinner(MoveListViewModel.PRIORITY_HIGH));
            mediumSection.setOnClickListener(v -> setPriorityFilterAndUpdateSpinner(MoveListViewModel.PRIORITY_MEDIUM));
            lowSection.setOnClickListener(v -> setPriorityFilterAndUpdateSpinner(MoveListViewModel.PRIORITY_LOW));

            totalText.setOnClickListener(v -> setPriorityFilterAndUpdateSpinner(""));
            
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
            
            // Объединяем низкий приоритет и без приоритета в один сегмент (зеленый)
            int combinedLowCount = lowCount + noPriorityCount;
            lowText.setText(String.valueOf(combinedLowCount));
            
            // Устанавливаем общее количество
            totalText.setText(String.valueOf(totalCount));
            

            urgentText.setVisibility(View.VISIBLE);
            highText.setVisibility(View.VISIBLE);
            mediumText.setVisibility(View.VISIBLE);
            lowText.setVisibility(View.VISIBLE);

            // Минимальная ширина для пустых секций
            final float MIN_EMPTY_WEIGHT = 6.0f;
            // Минимальная ширина для непустых секций
            final float MIN_FILLED_WEIGHT = 15.0f;
            

            int totalItems = urgentCount + highCount + mediumCount + combinedLowCount;
            

            float totalMinWidth = 0;
            if (urgentCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (highCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (mediumCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            
            if (combinedLowCount == 0) totalMinWidth += MIN_EMPTY_WEIGHT;
            else totalMinWidth += MIN_FILLED_WEIGHT;
            

            float remainingSpace = 100.0f - totalMinWidth;
            

            float urgentWeight, highWeight, mediumWeight, lowWeight;
            
            if (totalItems == 0) {
                // Если нет элементов, распределяем равномерно
                urgentWeight = MIN_EMPTY_WEIGHT;
                highWeight = MIN_EMPTY_WEIGHT;
                mediumWeight = MIN_EMPTY_WEIGHT;
                lowWeight = MIN_EMPTY_WEIGHT;
            } else {
                // Расчет весов с учетом пропорций
                urgentWeight = urgentCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * urgentCount / totalItems);
                
                highWeight = highCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * highCount / totalItems);
                
                mediumWeight = mediumCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * mediumCount / totalItems);
                
                lowWeight = combinedLowCount == 0 ? MIN_EMPTY_WEIGHT : 
                    MIN_FILLED_WEIGHT + (remainingSpace * combinedLowCount / totalItems);
            }
            
            // Устанавливаем веса
            setFrameLayoutWeight(urgentSection, urgentWeight);
            setFrameLayoutWeight(highSection, highWeight);
            setFrameLayoutWeight(mediumSection, mediumWeight);
            setFrameLayoutWeight(lowSection, lowWeight);

        } catch (Exception e) {
            Log.e("MoveList_menu", "Ошибка при обновлении диаграммы приоритетов: " + e.getMessage());
        }
    }
    

    
    /**
     * Устанавливает вес для FrameLayout в LinearLayout
     */
    private void setFrameLayoutWeight(FrameLayout layout, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        if (params.weight != weight) {
        params.weight = weight;
        layout.setLayoutParams(params);
        }
    }


    @Override
    protected void onDestroy() {
        
        // Отменяем подсветки фильтров
        clearFilterIndicator();
        
        
        
        // Освобождаем ссылки
        formirovanFragment = null;
        komplektuetsaFragment = null;
        podgotovlenFragment = null;
        
        super.onDestroy();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
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
        btnFinishWork = findViewById(R.id.btnFinishWork);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        
        // Изначально скрываем панель
        if (actionButtonsPanel != null) actionButtonsPanel.setVisibility(View.GONE);
        
        // Настраиваем обработчики нажатий
        if (btnMoveToWork != null) {
            btnMoveToWork.setOnClickListener(v -> {
                // Определяем, на какой вкладке мы находимся
                int currentTab = getCurrentTabPosition();
                if (currentTab == 0) { // Вкладка "Сформирован"
                    moveSelectedItemsBetweenStates(MOVE_TO_KOMPLEKTUETSA);
                } else if (currentTab == 2) { // Вкладка "Подготовлен"
                    moveSelectedItemsBetweenStates(MOVE_TO_WORK);
                }
            });
        }
        if (btnMoveFromWork != null) btnMoveFromWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(MOVE_TO_FORMIROVAN));
        if (btnFinishWork != null) btnFinishWork.setOnClickListener(v -> moveSelectedItemsBetweenStates(FINISH_WORK));
        
        // Обработчик нажатия на кнопку "Отмена"
        if (btnCancelSelection != null) {
        btnCancelSelection.setOnClickListener(v -> {
                clearSelectionInCurrentFragment();


                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isSelectionMode) {
                    setSelectionMode(false);
                    }
                }, 200);
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
        return getCurrentTabPosition() == 0 ? formirovanFragment : getCurrentTabPosition() == 1 ? komplektuetsaFragment : podgotovlenFragment;
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
        
        // Находим контейнеры для строк кнопок
        LinearLayout actionButtonsSecondRow = findViewById(R.id.actionButtonsSecondRow);
        
        if (selectedCount > 0) {
            if (!isSelectionMode) {
                setSelectionMode(true);
            }
            
            if (selectedItemsCount != null) selectedItemsCount.setText(String.format("Выбрано: %d", selectedCount));
            
            if (getCurrentTabPosition() == 0) { // Вкладка "Сформирован"
                if (btnMoveToWork != null) btnMoveToWork.setVisibility(View.VISIBLE);
                if (btnMoveFromWork != null) btnMoveFromWork.setVisibility(View.GONE);
                if (btnFinishWork != null) btnFinishWork.setVisibility(View.GONE);
                if (actionButtonsSecondRow != null) actionButtonsSecondRow.setVisibility(View.VISIBLE);
                btnCancelSelection.setVisibility(View.VISIBLE);


            } else if (getCurrentTabPosition() == 1) { // Вкладка "Комплектуется"
                if (btnMoveToWork != null) btnMoveToWork.setVisibility(View.GONE);
                if (btnMoveFromWork != null) btnMoveFromWork.setVisibility(View.VISIBLE);
                if (btnFinishWork != null) btnFinishWork.setVisibility(View.VISIBLE);
                if (actionButtonsSecondRow != null) actionButtonsSecondRow.setVisibility(View.VISIBLE);
                btnCancelSelection.setVisibility(View.VISIBLE);
            } else { // Вкладка "Подготовлен"
                if (btnMoveToWork != null) btnMoveToWork.setVisibility(View.VISIBLE);
                if (btnMoveFromWork != null) btnMoveFromWork.setVisibility(View.GONE);
                if (btnFinishWork != null) btnFinishWork.setVisibility(View.GONE);
                if (actionButtonsSecondRow != null) actionButtonsSecondRow.setVisibility(View.VISIBLE);
                btnCancelSelection.setVisibility(View.VISIBLE);
            }
            
            if (actionButtonsPanel.getVisibility() != View.VISIBLE) {
                actionButtonsPanel.setVisibility(View.VISIBLE);
                actionButtonsPanel.setAlpha(0f);
                actionButtonsPanel.animate().alpha(1f).setDuration(200).start();
            }
        } else {
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
        } else if (moveType == MOVE_TO_WORK) {
            currentStatus = STATUS_PODGOTOVLEN;
            targetStatus = STATUS_KOMPLEKTUETSA;
        } else if (moveType == FINISH_WORK) {
            currentStatus = STATUS_KOMPLEKTUETSA;
            targetStatus = STATUS_PODGOTOVLEN;
        } else {
            Log.e("MoveList_menu", "Неизвестный тип перемещения: " + moveType);
            return;
        }
        // --- МАССОВЫЙ РЕЖИМ ---
        if (selectedItems.size() > 1) {
            isMultiMoveActive = true;
            multiMoveSuccessGuids = new ArrayList<>();
            multiMoveTargetState = targetStatus;
        } else {
            isMultiMoveActive = false;
            multiMoveSuccessGuids = null;
            multiMoveTargetState = null;
        }
        moveListViewModel.moveItems(new ArrayList<>(selectedItems), currentStatus, targetStatus);
        clearSelectionInCurrentFragment();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isSelectionMode) {
                setSelectionMode(false);
            }
        }, 200);
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
            if (navMenuButton != null) navMenuButton.hide();
            
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
            if (segmentPodgotovlen != null) {
                segmentPodgotovlen.setClickable(false);
                segmentPodgotovlen.setEnabled(false);
            }
        } else {
            // Возвращаем все функции UI
            viewPager.setUserInputEnabled(true);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (navMenuButton != null) {
                navMenuButton.show();
                navMenuButton.setClickable(true);
                navMenuButton.setEnabled(true);

                if (navMenuButton.isShown()) {
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
            if (segmentPodgotovlen != null) {
                segmentPodgotovlen.setClickable(true);
                segmentPodgotovlen.setEnabled(true);
            }
        }
    }

    /**
     * Настраивает кнопку обновления данных
     */
    private void setupRefreshButton() {

        refreshButton = findViewById(R.id.refresh_button);

        if (refreshButton == null) {
            Log.e("MoveList_menu", "refreshButton не найдена в setupRefreshButton");
            return;
        }

        refreshButton.setVisibility(View.GONE);
        

        refreshButton.setOnClickListener(v -> {
            Log.d("MoveList_menu", "Refresh button clicked.");
            moveListViewModel.applyPendingUpdates();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d("MoveList_menu", "Получен результат активности: requestCode=" + requestCode + 
              ", resultCode=" + resultCode);
              
        // Проверяем, что это результат из Prixod
        if (requestCode == 1001) {
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
        filterNomenculature = findViewById(R.id.filter_nomenculature);
        filterSeries = findViewById(R.id.filter_series);
        filterReceiver = findViewById(R.id.filter_receiver);
        filterPriority = findViewById(R.id.filter_priority);
        filterCps = findViewById(R.id.filter_cps);
        filterAvailability = findViewById(R.id.filter_availability);
        clearSender = findViewById(R.id.clear_sender);
        clearMovementNumber = findViewById(R.id.clear_movement_number);
        clearNomeculature = findViewById(R.id.clear_nomenculature);
        clearSeries = findViewById(R.id.clear_series);
        clearRecipient = findViewById(R.id.clear_recipient);
        clearAssembler = findViewById(R.id.clear_assembler);
        clearPriority = findViewById(R.id.clear_priority);
        clearReceiver = findViewById(R.id.clear_receiver);

        btnApplyFilters = findViewById(R.id.btn_apply_filters);
        btnResetFilters = findViewById(R.id.btn_reset_filters);
        btnSetDefaultFilters = findViewById(R.id.btn_set_default_filters);

        // Инициализация адаптера для Spinner приоритетов
        if (filterPriority != null) {
            List<String> priorityOptions = new ArrayList<>();
            priorityOptions.add("Все");
            priorityOptions.add(MoveListViewModel.PRIORITY_URGENT); // "Неотложный"
            priorityOptions.add(MoveListViewModel.PRIORITY_HIGH);   // "Высокий"
            priorityOptions.add(MoveListViewModel.PRIORITY_MEDIUM); // "Средний"
            priorityOptions.add(MoveListViewModel.PRIORITY_LOW);    // "Низкий"

            ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, priorityOptions);
            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterPriority.setAdapter(priorityAdapter);
        }


        setupFilterListeners();
    }

    private void setupFilterListeners() {
        setupEnterKeyListener(filterSender);
        setupEnterKeyListener(filterMovementNumber);
        setupEnterKeyListener(filterNomenculature);
        setupEnterKeyListener(filterSeries);
        setupEnterKeyListener(filterRecipient);
        setupEnterKeyListener(filterAssembler);
        setupEnterKeyListener(filterReceiver);



        // Слушатели для кнопок очистки
        clearSender.setOnClickListener(v -> filterSender.setText(""));
        clearMovementNumber.setOnClickListener(v -> filterMovementNumber.setText(""));
        clearNomeculature.setOnClickListener(v -> filterNomenculature.setText(""));
        clearSeries.setOnClickListener(v -> filterSeries.setText(""));
        clearRecipient.setOnClickListener(v -> filterRecipient.setText(""));
        clearAssembler.setOnClickListener(v -> filterAssembler.setText(""));
        clearPriority.setOnClickListener(v -> filterPriority.setSelection(0));
        clearReceiver.setOnClickListener(v -> filterReceiver.setText(""));

        btnApplyFilters.setOnClickListener(v -> {
            applyFilters();
            hideKeyboard();
            if (drawerLayout != null) {
                drawerLayout.requestFocus();
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });
        btnResetFilters.setOnClickListener(v -> resetFilters());
        btnSetDefaultFilters.setOnClickListener(v -> {
            // Сохраняем текущие фильтры как фильтры по умолчанию
            moveListViewModel.saveCurrentFiltersAsDefault();
        });
    }

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

            view.post(() -> {
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect);

            Rect windowRect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(windowRect);

            int screenHeight = getWindow().getDecorView().getRootView().getHeight();
            int keyboardHeight = screenHeight - windowRect.bottom;
            

            if (keyboardHeight < 100 && viewRect.top >= windowRect.top && viewRect.bottom <= windowRect.bottom) {
                    return;
                }


            int targetViewBottomY = windowRect.bottom - dpToPx(16);

            // Текущая позиция нижнего края поля ввода на экране
            int currentViewBottomY = viewRect.bottom;

            if (currentViewBottomY > targetViewBottomY) {
                // Поле ввода перекрывается клавиатурой или находится слишком низко
                int scrollAmount = currentViewBottomY - targetViewBottomY;
                scrollView.smoothScrollBy(0, scrollAmount);
                Log.d("MoveList_menu", "scrollToView: Scrolling by " + scrollAmount);
        } else {
            }
        });
    }

    /**
     * Конвертирует dp в пиксели. Вспомогательный метод.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
        }

    private void setupNavigationView() {
        if (navigationView == null || drawerLayout == null || navMenuButton == null) {
            Log.e("MoveList_menu", "setupNavigationView: NavigationView, DrawerLayout or NavMenuButton is null");
            return;
        }

        navMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {

                        currentFocus.clearFocus();
                        hideKeyboard(currentFocus);
                    }

                    // Запускаем анимацию кнопки
                    animateMenuButton(v);

                    // Открываем боковое меню
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (drawerLayout != null) {
                                drawerLayout.openDrawer(GravityCompat.START);
                            }
                        }
                    }, 300);
                } catch (Exception e) {
                    Log.e("PrixodActivity", "Ошибка при обработке нажатия на menuButton: " + e.getMessage());
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {

            Log.d("MoveList_menu", "Selected navigation item: " + item.getTitle());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


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
                if (navigationView != null) {
                    navigationView.setFocusable(true);
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
                                    hideKeyboard();
                                    applyFilters();
                                    drawerLayout.closeDrawer(GravityCompat.START);
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                }
            }
        });
    }

    private void animateMenuButton(View view) {
        // Создаем набор аниматоров для комплексной анимации
        AnimatorSet animatorSet = new AnimatorSet();


        final android.graphics.drawable.Drawable originalBackground = view.getBackground();


        ObjectAnimator scaleXPulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 0.8f, 1.1f, 1f);
        ObjectAnimator scaleYPulse = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 0.8f, 1.1f, 1f);


        ObjectAnimator rotationY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f);


        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 15f, -10f, 0f);


        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            float originalElevation = view.getElevation();
            ObjectAnimator elevation = ObjectAnimator.ofFloat(view, "elevation", originalElevation, originalElevation + 15f, originalElevation);
            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alpha, elevation);


            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            view.setClipToOutline(true);
        } else {

            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alpha);
        }


        animatorSet.setDuration(500);


        animatorSet.setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f));


        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
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


        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;


            int finalRadius = Math.max(view.getWidth(), view.getHeight());


            android.animation.Animator rippleAnim = android.view.ViewAnimationUtils.createCircularReveal(
                    view, centerX, centerY, 0, finalRadius);
            rippleAnim.setDuration(400);
            rippleAnim.start();


            try {
                final int originalColor = 0xFF3F51B5; // Индиго
                final int highlightColor = 0xFF4CAF50; // Зеленый


                android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
                gradientDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                gradientDrawable.setColor(highlightColor);
                view.setBackground(gradientDrawable);


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

    /**
     * Проверяет, находится ли касание в пределах указанного View.
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
            if (!(parent instanceof View)) {
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
                    hideKeyboard(drawerLayout);
                    return true;
                }
            }

            // 2. Обработка, если фокус на EditText внутри NavigationView (меню может быть открыто или закрыто)
                View currentFocusedView = getCurrentFocus();
            if (currentFocusedView instanceof EditText && navigationView != null && isViewInsideViewGroup(navigationView, currentFocusedView)) {
                if (!isTouchOnView(event, currentFocusedView)) {
                    // Касание вне текущего сфокусированного EditText в NavigationView
                    hideKeyboard(currentFocusedView);
                    currentFocusedView.clearFocus();

                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    public void hideKeyboard(View fromView) {
        if (fromView == null) {
            hideKeyboard();
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (fromView.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(fromView.getWindowToken(), 0);
        }
    }

    /**
     * Переключается на целевую вкладку и прокручивает до элемента с указанным GUID
     * @param targetState целевой статус (вкладка)
     * @param moveGuid GUID элемента для поиска
     */
    private void navigateToTargetTabAndScroll(String targetState, String moveGuid) {
        Log.d("MoveList_menu", "navigateToTargetTabAndScroll вызван для статуса: " + targetState + ", GUID: " + moveGuid);
        
        // Устанавливаем флаг блокировки автоматической прокрутки к верху
        isNavigatingToSpecificItem = true;
        
        // Определяем номер целевой вкладки
        int targetTabPosition = -1;
        if (MoveListViewModel.STATUS_FORMIROVAN.equals(targetState)) {
            targetTabPosition = 0;
        } else if (MoveListViewModel.STATUS_KOMPLEKTUETSA.equals(targetState)) {
            targetTabPosition = 1;
        } else if (MoveListViewModel.STATUS_PODGOTOVLEN.equals(targetState)) {
            targetTabPosition = 2;
        }

        if (targetTabPosition == -1) {
            Log.e("MoveList_menu", "Неизвестный целевой статус: " + targetState);
            isNavigatingToSpecificItem = false; // Сбрасываем флаг при ошибке
            return;
        }

        Log.d("MoveList_menu", "Целевая позиция вкладки: " + targetTabPosition + ", текущая позиция: " + getCurrentTabPosition());

        // Переключаемся на целевую вкладку
        if (viewPager != null && getCurrentTabPosition() != targetTabPosition) {
            Log.d("MoveList_menu", "Переключаемся на вкладку " + targetTabPosition);
            viewPager.setCurrentItem(targetTabPosition, true);
        } else {
            Log.d("MoveList_menu", "Уже на нужной вкладке " + targetTabPosition);
        }


        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.d("MoveList_menu", "Начинаем прокрутку к элементу с GUID: " + moveGuid);
            scrollToItemByGuid(targetState, moveGuid);
            

            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                isNavigatingToSpecificItem = false;
                Log.d("MoveList_menu", "Флаг блокировки прокрутки к верху сброшен");
            }, 1000);
        }, 300);
    }

        /**
     * Прокручивает список до элемента с указанным GUID
     * @param targetState целевой статус для определения правильного фрагмента
     * @param moveGuid GUID элемента для поиска
     */
    private void scrollToItemByGuid(String targetState, String moveGuid) {
        // Находим позицию элемента в отфильтрованном списке
        int itemPosition = moveListViewModel.findItemPositionByGuid(moveGuid, targetState);
        
        if (itemPosition == -1) {
            Log.w("MoveList_menu", "Элемент с GUID " + moveGuid + " не найден в списке " + targetState);
            return;
        }

        // Получаем текущий фрагмент
        MoveListFragment currentFragment = getCurrentFragment();
        if (currentFragment != null && currentFragment.getRecyclerView() != null) {
            RecyclerView recyclerView = currentFragment.getRecyclerView();
            androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
            
            if (layoutManager != null) {

                int recyclerHeight = recyclerView.getHeight();
                

                int itemHeight = 200;
                if (recyclerView.getChildCount() > 0) {
                    View firstChild = recyclerView.getChildAt(0);
                    if (firstChild != null) {
                        itemHeight = firstChild.getHeight();
                    }
                }
                
                int offset = (recyclerHeight - itemHeight) / 2;
                

                layoutManager.scrollToPositionWithOffset(itemPosition, offset);
                
                Log.d("MoveList_menu", "Прокрутка к элементу на позиции " + itemPosition + " в статусе " + targetState + " с центрированием");
                

                new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    highlightItem(recyclerView, itemPosition, moveGuid);
                }, 300);
            } else {

                recyclerView.smoothScrollToPosition(itemPosition);
                Log.d("MoveList_menu", "Fallback прокрутка к элементу на позиции " + itemPosition);
            }
        } else {
            Log.e("MoveList_menu", "Не удалось получить RecyclerView для прокрутки");
        }
    }

    /**
     * Визуально выделяет элемент в RecyclerView
     * @param recyclerView RecyclerView с элементами
     * @param position позиция элемента для выделения
     * @param moveGuid GUID элемента для дополнительной проверки
     */
    private void highlightItem(RecyclerView recyclerView, int position, String moveGuid) {
        if (recyclerView == null) return;
        
        // Ждем, пока RecyclerView завершит layout после прокрутки
        recyclerView.post(() -> {
            androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder = 
                recyclerView.findViewHolderForAdapterPosition(position);
            
            if (viewHolder != null && viewHolder.itemView != null) {
                View itemView = viewHolder.itemView;
                
                // Сохраняем оригинальный фон
                android.graphics.drawable.Drawable originalBackground = itemView.getBackground();
                int originalElevation = (int) itemView.getElevation();
                
                // Создаем анимацию выделения
                animateHighlight(itemView, originalBackground, originalElevation);
                
                Log.d("MoveList_menu", "Элемент с GUID " + moveGuid + " визуально выделен");
            } else {
                Log.w("MoveList_menu", "ViewHolder не найден для позиции " + position + ", повторная попытка через 100мс");
                new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    highlightItem(recyclerView, position, moveGuid);
                }, 100);
            }
        });
    }

    /**
     * Анимация выделения элемента
     * @param itemView View элемента для анимации
     * @param originalBackground оригинальный фон элемента
     * @param originalElevation оригинальная высота элемента
     */
    private void animateHighlight(View itemView, android.graphics.drawable.Drawable originalBackground, int originalElevation) {
        // Создаем выделяющий фон (светло-желтый с прозрачностью)
        android.graphics.drawable.GradientDrawable highlightBackground = new android.graphics.drawable.GradientDrawable();
        highlightBackground.setColor(0x80FFEB3B); // Светло-желтый
        highlightBackground.setCornerRadius(12f); // Скругленные углы
        
        // Анимация появления выделения
        itemView.setBackground(highlightBackground);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            itemView.setElevation(originalElevation + 8f); // Поднимаем элемент
        }
        

        animatePulse(itemView, 0, 1, () -> {

            itemView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .withEndAction(() -> {
                    itemView.setBackground(originalBackground);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        itemView.setElevation(originalElevation);
                    }
                })
                .start();
        });
    }

    /**
     * Создает импульсную анимацию для выделения элемента
     * @param itemView View для анимации
     * @param currentPulse текущий импульс (для рекурсии)
     * @param totalPulses общее количество импульсов
     * @param onComplete callback по завершении всех импульсов
     */
    private void animatePulse(View itemView, int currentPulse, int totalPulses, Runnable onComplete) {
        if (currentPulse >= totalPulses) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        // Анимация увеличения
        itemView.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(300)
            .withEndAction(() -> {
                // Анимация уменьшения
                itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            animatePulse(itemView, currentPulse + 1, totalPulses, onComplete);
                        }, 200);
                    })
                    .start();
            })
            .start();
    }

    /**
     * Показывает диалог успешной смены статуса с большой центральной кнопкой "ОК"
     * @param successEvent событие успешной смены статуса
     */
    private void showSuccessDialog(MoveListViewModel.SuccessStatusChangeEvent successEvent) {
        Log.d("MoveList_menu", "showSuccessDialog вызван с сообщением: " + successEvent.message);
        Log.d("MoveList_menu", "Целевой статус: " + successEvent.targetState + ", GUID: " + successEvent.moveGuid);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Успешно")
               .setMessage(successEvent.message)
               .setCancelable(false)
               .setPositiveButton("ОК", (dialog, which) -> {
                   dialog.dismiss();
                   Log.d("MoveList_menu", "Кнопка ОК нажата, начинаем переход к целевой вкладке");
                   navigateToTargetTabAndScroll(successEvent.targetState, successEvent.moveGuid);
               });

        AlertDialog dialog = builder.create();
        dialog.show();

        new Handler(android.os.Looper.getMainLooper()).post(() -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (okButton != null) {
                Log.d("MoveList_menu", "Настройка кнопки ОК в диалоге успеха");
                

                okButton.setTextSize(18);
                okButton.setPadding(60, 30, 60, 30);
                okButton.setMinWidth(200);
                okButton.setMinHeight(80);
                okButton.setGravity(1);
                

                
                Log.d("MoveList_menu", "Кнопка ОК настроена успешно");
            } else {
                Log.e("MoveList_menu", "Не удалось получить кнопку ОК в диалоге успеха");
            }
        });
    }

    // Диалог успеха для массовой обработки (без перехода)
    private void showSuccessDialogMulti(MoveListViewModel.SuccessStatusChangeEvent successEvent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Успешно")
               .setMessage(successEvent.message)
               .setCancelable(false)
               .setPositiveButton("ОК", (dialog, which) -> {
                   dialog.dismiss();
                   moveListViewModel.continueMultiMove();
               });
        AlertDialog dialog = builder.create();
        dialog.show();
        new Handler(android.os.Looper.getMainLooper()).post(() -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (okButton != null) {
                okButton.setTextSize(18);
                okButton.setPadding(60, 30, 60, 30);
                okButton.setMinWidth(200);
                okButton.setMinHeight(80);
                okButton.setGravity(1);
            }
        });
    }

    /**
     * Показывает диалог с ошибкой о пустых УИДСтрокиТовары
     * @param errorMessage сообщение об ошибке
     */
    private void showEmptyProductLineIdErrorDialog(String errorMessage) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Ошибка в данных перемещения")
            .setMessage(errorMessage)
            .setPositiveButton("ОК", (dialog, which) -> {

                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
        
        Log.w("MoveList_menu", "Показан диалог ошибки УИДСтрокиТовары: " + errorMessage);
    }
    
    /**
     * Показывает диалог с ошибкой о пустом перемещении
     * @param errorMessage сообщение об ошибке
     */
    private void showEmptyMovementErrorDialog(String errorMessage) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Пустое перемещение")
            .setMessage(errorMessage)
            .setPositiveButton("ОК", (dialog, which) -> {

                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
        
        Log.w("MoveList_menu", "Показан диалог ошибки пустого перемещения: " + errorMessage);
    }

    /**
     * Прокручивает текущий активный список к началу с задержкой
     * Используется после применения фильтров чтобы пользователь видел результаты с самого верха
     */
    private void scrollToTopOfCurrentListWithDelay() {
        Log.d("MoveList_menu", "scrollToTopOfCurrentListWithDelay() вызван для вкладки: " + getCurrentTabPosition());
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d("MoveList_menu", "Выполняем отложенную прокрутку к началу списка");
            scrollToTopOfCurrentList();
        });
    }

    /**
     * Прокручивает текущий активный список к началу
     * Используется после применения фильтров чтобы пользователь видел результаты с самого верха
     */
    private void scrollToTopOfCurrentList() {
        MoveListFragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            try {
                RecyclerView recyclerView = currentFragment.getRecyclerView();
                if (recyclerView != null && recyclerView.getAdapter() != null) {
                    int itemCount = recyclerView.getAdapter().getItemCount();
                    Log.d("MoveList_menu", "Попытка прокрутки к началу списка. Элементов в адаптере: " + itemCount);
                    
                    if (itemCount >= 0) {
                        recyclerView.scrollToPosition(0);
                        Log.d("MoveList_menu", "Прокрутка к началу списка выполнена");
                    } else {
                        Log.w("MoveList_menu", "Некорректное количество элементов в адаптере: " + itemCount);
                    }
                } else {
                    Log.w("MoveList_menu", "RecyclerView или адаптер null при попытке прокрутки. RecyclerView: " + 
                          (recyclerView != null ? "не null" : "null") + 
                          ", Adapter: " + (recyclerView != null && recyclerView.getAdapter() != null ? "не null" : "null"));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        retryScrollToTop(currentFragment);
                    }, 50);
                }
            } catch (Exception e) {
                Log.e("MoveList_menu", "Ошибка при прокрутке к началу списка: " + e.getMessage());
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    retryScrollToTop(currentFragment);
                }, 100);
            }
        } else {
            Log.w("MoveList_menu", "CurrentFragment null при попытке прокрутки. Текущая вкладка: " + getCurrentTabPosition());
        }
    }

    /**
     * Повторная попытка прокрутки с дополнительными проверками
     */
    private void retryScrollToTop(MoveListFragment fragment) {
        if (fragment != null) {
            try {
                RecyclerView recyclerView = fragment.getRecyclerView();
                if (recyclerView != null && recyclerView.getAdapter() != null) {
                    recyclerView.scrollToPosition(0); // Используем обычную прокрутку для fallback
                    Log.d("MoveList_menu", "Повторная прокрутка к началу списка выполнена");
                } else {
                    Log.e("MoveList_menu", "Повторная попытка прокрутки также не удалась - RecyclerView или адаптер все еще null");
                }
            } catch (Exception fallbackError) {
                Log.e("MoveList_menu", "Повторная прокрутка также не удалась: " + fallbackError.getMessage());
            }
        }
    }

}