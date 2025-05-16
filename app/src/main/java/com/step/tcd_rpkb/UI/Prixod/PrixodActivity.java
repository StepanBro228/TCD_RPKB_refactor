package com.step.tcd_rpkb.UI.Prixod;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.UI.Prixod.viewmodel.PrixodViewModel;
import com.step.tcd_rpkb.UI.Prixod.viewmodel.SortCriteria; // Импортируем enum

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PrixodActivity extends BaseFullscreenActivity
        implements InputInfoAdapter.AdapterButtonListener,
                   InputInfoAdapter.OnProductDataChangedListener { // Изменяем интерфейс

    // Флаг для отслеживания касаний пользователя
    public static boolean isUserTouchingScreen = false;
    
    /**
     * Утилитный метод для отключения клавиатуры в EditText с вводом чисел.
     * Может быть вызван из любого места приложения.
     *
     * @param editText Поле ввода, для которого нужно отключить клавиатуру
     */
    public static void disableKeyboardForNumericField(EditText editText) {
        if (editText == null) return;
        
        try {
            // Метод доступен с API 21 (Android 5.0)
            editText.setShowSoftInputOnFocus(false);
        } catch (Exception e) {
            // Игнорируем ошибку для ранних версий Android
            // Можно использовать reflection для более старых версий, но это редкий случай
        }
    }
    
    // RecyclerView для отображения списка товаров
    private RecyclerView rv_info;
    // Адаптер для заполнения RecyclerView данными товаров
    private InputInfoAdapter adapter;
    // Приемник для обработки QR-кодов - БУДЕТ ИЗМЕНЕН/УДАЛЕН
    // private QRcodeReceiver qrcodeReceiver; 
    private BroadcastReceiver barcodeDataReceiver; // Новый ресивер для данных сканера

    // DrawerLayout для бокового меню
    private DrawerLayout drawerLayout;
    private NavigationView sort_bar;
    // Флаг для определения размера экрана
    private boolean isSmallScreen = false;
    // Индикатор фильтрации
    private View filterIndicator;
    // FloatingSortView вместо header элементов
    private FloatingSortView floatingSortView;
    // TextView для отображения ФИО пользователя и роли
    private TextView userFullNameTextView;
    private TextView userRoleTextView;

    // Текущий загруженный документ перемещения - теперь во ViewModel
    // private Invoice currentInvoice;
    private PrixodViewModel prixodViewModel;
    
    // Переменные для поиска
    private boolean isSearchActive = false;
    private String lastSearchQuery = "";

    // BroadcastReceiver для получения UUID от сканера - БУДЕТ УДАЛЕН
    /*
    private final BroadcastReceiver uuidReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Извлекаем UUID из полученного интента
            String uuid = intent.getStringExtra("UUID");
            if (uuid != null) {
                Log.d("Scanner", "Получен UUID: " + uuid);
                // Выполняем поиск товара по UUID и устанавливаем фокус
                // focusOnProduct(uuid); // Логика перенесена в ViewModel
            }
        }
    };
    */

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prixod);
        
        // Инициализация ViewModel
        prixodViewModel = new ViewModelProvider(this).get(PrixodViewModel.class);
        
        // Проверяем, есть ли сохраненные данные
        String moveUuid = getIntent().getStringExtra("moveUuid");
        String productsJson = getIntent().getStringExtra("productsData");
        boolean preserveEditedData = getIntent().getBooleanExtra("preserveEditedData", false);

        // Запрос начальных данных через ViewModel
        prixodViewModel.loadInitialData(moveUuid, productsJson, preserveEditedData);
        
        // Регистрируем глобальный слушатель фокуса для отключения клавиатуры в числовых полях
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
            new android.view.ViewTreeObserver.OnGlobalFocusChangeListener() {
                @Override
                public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                    if (newFocus instanceof EditText) {
                        EditText editText = (EditText) newFocus;
                        // Проверяем, является ли это числовым полем по типу ввода
                        int inputType = editText.getInputType();
                        if ((inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0) {
                            // Это числовое поле, отключаем клавиатуру
                            disableKeyboardForNumericField(editText);
                        }
                        
                        // Если фокус был на текстовом поле, а теперь на числовом, скрываем клавиатуру
                        if (oldFocus instanceof EditText) {
                            int oldInputType = ((EditText) oldFocus).getInputType();
                            boolean oldIsText = (oldInputType & android.text.InputType.TYPE_CLASS_TEXT) != 0;
                            boolean newIsNumber = (inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0;
                            
                            if (oldIsText && newIsNumber) {
                                // Переход с текстового поля на числовое - скрываем клавиатуру
                                hideKeyboard(oldFocus);
                            }
                        }
                    }
                }
            }
        );
        
        // Инициализируем DrawerLayout для бокового меню
        drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton menuButton = findViewById(R.id.nav_menu_button);
        sort_bar = findViewById(R.id.navigation_view);
        
        // Инициализируем RecyclerView
        rv_info = findViewById(R.id.rv_info);
        if (rv_info != null) {
            rv_info.setLayoutManager(new LinearLayoutManager(this));
        } else {
            Log.e("Prixod", "Ошибка: не удалось найти RecyclerView (rv_info)");
            Toast.makeText(this, "Ошибка инициализации интерфейса", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // ВАЖНО: Загружаем данные о перемещении и товарах ПОСЛЕ инициализации RecyclerView
        // loadMoveData(moveUuid); // Заменено на вызов ViewModel
        
        // Инициализируем QRcodeReceiver - УДАЛЯЕМ ИНИЦИАЛИЗАЦИЮ СТАРОГО QRcodeReceiver
        // qrcodeReceiver = new QRcodeReceiver(); 
        
        // Инициализируем новый BroadcastReceiver для данных сканера
        initializeBarcodeDataReceiver();
        
//        // Инициализируем новые элементы фильтрации
//        EditText filterName = findViewById(R.id.filter_name);
//        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
//        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
//        CheckBox filterUntouched = findViewById(R.id.filter_untouched);
//        ImageButton clearName = findViewById(R.id.clear_name);
//        ImageButton clearAmount = findViewById(R.id.clear_amount);
//        Button btnResetAllFilters = findViewById(R.id.btn_reset_all_filters);
        
        // Инициализируем индикатор фильтрации
        filterIndicator = findViewById(R.id.filter_indicator);

        // Инициализируем FloatingSortView
        floatingSortView = findViewById(R.id.floating_sort_view);
        
        // Настраиваем FloatingSortView ПОСЛЕ инициализации адаптера и ViewModel
        setupFloatingSortView();
        
        // Устанавливаем обработчик нажатия для кнопки меню с анимацией
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Запускаем анимацию кнопки
                animateMenuButton(view);
                
                // Открываем боковое меню (с левой стороны) с небольшой задержкой
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }, 300); // Задержка открытия меню для завершения анимации
            }
        });

        // Инициализируем ссылки на элементы в NavigationView
        View navHeaderView = sort_bar.findViewById(R.id.nav_header);
        userFullNameTextView = navHeaderView.findViewById(R.id.user_full_name);
        userRoleTextView = navHeaderView.findViewById(R.id.user_role);
        
        // Получаем карточку пользователя и запускаем крутую анимацию
        // View userCard = navHeaderView.findViewById(R.id.user_name_card);
        // UserManager userManager = UserManager.getInstance(); // Логика пользователя теперь в ViewModel
        // userManager.updateUserInfoUI(userFullNameTextView, userRoleTextView, userAvatarImageView);
        
        // После задержки добавляем эффект мерцания для аватара
        // new Handler().postDelayed(() -> {
        //     userManager.addSparkleEffect(userAvatarImageView);
        // }, 2000);
        
        // Настраиваем слушатели для фильтров
        setupFilterListeners();
        
        // Настраиваем обработчик нажатия на кнопку сброса фильтров
        // btnResetAllFilters.setOnClickListener(new View.OnClickListener() { // Этот блок был дублирован и закомментирован ранее, теперь удаляем его полностью
        //     @Override
        //     public void onClick(View v) {
        //         // Сбрасываем все фильтры и сортировку
        //         resetAllFilters();
        //         
        //         // Закрываем меню
        //         drawerLayout.closeDrawer(GravityCompat.START);
        //         
        //         // Показываем сообщение пользователю
        //         Toast.makeText(Prixod.this, "Все фильтры сброшены", Toast.LENGTH_SHORT).show();
        //     }
        // });
        
        // Добавляем глобальный обработчик касаний для скрытия клавиатуры при нажатии вне полей ввода
        findViewById(R.id.main).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Скрываем клавиатуру при касании основного контента
                    View currentFocus = getCurrentFocus();
                    if (currentFocus != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        currentFocus.clearFocus();
                    }
                    return true;
                }
                return false;
            }
        });
        
        // Настраиваем слушатель для обработки нажатий на фильтры и предотвращения скрытия клавиатуры
        setupNestedScrollViewTouchListener();

        // Добавляем слушатель для DrawerLayout
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Затемняем основное содержимое при открытии бокового меню
                View mainContent = findViewById(R.id.main);
                mainContent.setAlpha(1 - slideOffset * 0.3f); // 30% затемнение при полном открытии
                
                // Анимация карточки пользователя при открытии
                NavigationView navigationView = findViewById(R.id.navigation_view);
                View navHeaderView = navigationView.findViewById(R.id.nav_header);
                View userCard = navHeaderView.findViewById(R.id.user_name_card);
                
                if (userCard != null) {
                    // Эффект появления при открытии
                    userCard.setTranslationX(-100 + slideOffset * 100);
                    userCard.setAlpha(slideOffset);
                    
                    // Небольшое масштабирование
                    float scale = 0.8f + (0.2f * slideOffset);
                    userCard.setScaleX(scale);
                    userCard.setScaleY(scale);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // При полном открытии меню
                // Запускаем анимацию "подпрыгивания" для карточки пользователя
                NavigationView navigationView = findViewById(R.id.navigation_view);
                View navHeaderView = navigationView.findViewById(R.id.nav_header);
                View userCard = navHeaderView.findViewById(R.id.user_name_card);
                
                if (userCard != null) {
                    ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(
                        userCard, "translationY", 0f, -10f, 0f);
                    bounceAnimator.setDuration(500);
                    bounceAnimator.setInterpolator(new android.view.animation.OvershootInterpolator());
                    bounceAnimator.start();
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Меню закрылось - сбрасываем любой фокус с полей ввода
                clearFocusFromEditTexts();
                
                // Скрываем клавиатуру
                hideKeyboard(drawerLayout);
                
                // Прокручиваем до верха, если список отфильтрован, используя данные из ViewModel
                Boolean filtersActive = prixodViewModel.isAnyFilterActiveLiveData.getValue();
                if (filtersActive != null && filtersActive && rv_info != null) {
                    rv_info.smoothScrollToPosition(0);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // При изменении состояния меню
            }
        });
        
        // Улучшенная анимация нажатия для кнопок
        setupButtonAnimations();

        // Подписываемся на LiveData из ViewModel
        observeViewModel();
    }
    
    /**
     * Настраивает анимации для кнопок
     */
    private void setupButtonAnimations() {
        try {
            // Находим кнопки по ID
            final MaterialButton sendButton = findViewById(R.id.btn_send_info);
            final MaterialButton backButton = findViewById(R.id.btn_back_to_move_list);
            
            // Создаем слушатель для обработки касания кнопок с кардинально новой анимацией
            View.OnTouchListener buttonTouchListener = new View.OnTouchListener() {
                private ValueAnimator pulseAnimator;
                private boolean isAnimating = false;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // Останавливаем предыдущую анимацию, если она выполняется
                        if (isAnimating && pulseAnimator != null && pulseAnimator.isRunning()) {
                            pulseAnimator.cancel();
                        }
                        
                        // 1. Мгновенное сжатие при нажатии
                        v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .translationY(dpToPx(2))
                            .setDuration(80)
                            .setInterpolator(new AccelerateInterpolator())
                            .start();
                        
                        // 2. Создаем эффект пульсации, которая будет продолжаться пока кнопка нажата
                        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
                        pulseAnimator.setDuration(700);
                        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
                        
                        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float value = (float) animation.getAnimatedValue();
                                // Синусоидальная пульсация для плавности
                                float scale = 0.95f + 0.01f * (float)Math.sin(value * Math.PI * 2);
                                v.setScaleX(scale);
                                v.setScaleY(scale);
                                
                                // Небольшое смещение по вертикали для эффекта "дыхания"
                                float translateY = dpToPx(2) - dpToPx(0.3f) * (float)Math.sin(value * Math.PI * 2);
                                v.setTranslationY(translateY);
                            }
                        });
                        
                        pulseAnimator.start();
                        isAnimating = true;
                        
                        // Добавляем вибрацию для тактильной обратной связи
                        v.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY, 
                            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        );
                        
                    } else if (event.getAction() == MotionEvent.ACTION_UP || 
                               event.getAction() == MotionEvent.ACTION_CANCEL) {
                        
                        // Останавливаем пульсацию
                        if (pulseAnimator != null && pulseAnimator.isRunning()) {
                            pulseAnimator.cancel();
                        }
                        
                        isAnimating = false;
                        
                        // Многофазная анимация отпускания с эффектами
                        AnimatorSet releaseAnimSet = new AnimatorSet();
                        
                        // Фаза 1: Быстрый отскок вверх
                        ObjectAnimator bounce1 = ObjectAnimator.ofFloat(v, "translationY", v.getTranslationY(), -dpToPx(1));
                        bounce1.setDuration(100);
                        bounce1.setInterpolator(new AccelerateInterpolator(1.5f));
                        
                        // Фаза 2: Возврат с переувеличением
                        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1.03f);
                        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1.03f);
                        
                        // Фаза 3: Финальное установление нормального размера
                        ObjectAnimator scaleXNormal = ObjectAnimator.ofFloat(v, "scaleX", 1.03f, 1.0f);
                        ObjectAnimator scaleYNormal = ObjectAnimator.ofFloat(v, "scaleY", 1.03f, 1.0f);
                        ObjectAnimator translationYNormal = ObjectAnimator.ofFloat(v, "translationY", -dpToPx(1), 0f);
                        
                        // Объединяем фазы 2 и 3 в одну последовательность
                        AnimatorSet upAndNormalize = new AnimatorSet();
                        upAndNormalize.playTogether(scaleXUp, scaleYUp);
                        upAndNormalize.setDuration(150);
                        upAndNormalize.setInterpolator(new OvershootInterpolator(2.0f));
                        
                        AnimatorSet normalizeFinal = new AnimatorSet();
                        normalizeFinal.playTogether(scaleXNormal, scaleYNormal, translationYNormal);
                        normalizeFinal.setDuration(200);
                        normalizeFinal.setInterpolator(new DecelerateInterpolator(1.2f));
                        
                        // Объединяем все фазы в последовательность
                        releaseAnimSet.playSequentially(bounce1, upAndNormalize, normalizeFinal);
                        releaseAnimSet.start();
                    }
                    
                    // Возвращаем false, чтобы обработчик клика тоже сработал
                    return false;
                }
            };
            
            // Применяем обработчик касаний к кнопкам
            if (sendButton != null) {
                // Убедимся, что фон применяется сразу
                sendButton.setBackgroundResource(R.drawable.primary_button_background);
                sendButton.setOnTouchListener(buttonTouchListener);
                // Для уверенности добавим elevation программно
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    sendButton.setElevation(dpToPx(2));
                    sendButton.setStateListAnimator(null); // Отключаем стандартный аниматор
                }
                Log.d("Prixod", "Кардинально новые анимации применены к кнопке отправки");
            }
            
            if (backButton != null) {
                // Убедимся, что фон применяется сразу
                backButton.setBackgroundResource(R.drawable.secondary_button_background);
                backButton.setOnTouchListener(buttonTouchListener);
                // Для уверенности добавим elevation программно
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    backButton.setElevation(dpToPx(2));
                    backButton.setStateListAnimator(null); // Отключаем стандартный аниматор
                }
                Log.d("Prixod", "Кардинально новые анимации применены к кнопке возврата");
            }
            
        } catch (Exception e) {
            Log.e("Prixod", "Ошибка при настройке анимаций кнопок: " + e.getMessage());
        }
    }
    
    /**
     * Конвертирует dp в пиксели
     */
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Регистрируем BroadcastReceiver для QR-кодов - УДАЛЯЕМ РЕГИСТРАЦИЮ uuidReceiver
        // IntentFilter intentFilter = new IntentFilter("com.example.scannerapp.UUID_RECEIVED");
        // registerReceiver(uuidReceiver, intentFilter);
        
        // Регистрируем приемник для получения данных сканирования
        IntentFilter scannerFilter = new IntentFilter("com.example.scannerapp.ACTION_BARCODE_DATA");
        
        // Добавляем дополнительные фильтры для возможных Intent'ов от сканера
        scannerFilter.addAction("android.intent.action.SCAN");
        scannerFilter.addAction("com.google.zxing.client.android.SCAN");
        scannerFilter.addAction("com.symbol.datawedge.api.ACTION");
        // Добавляем поддержку старого фильтра
        scannerFilter.addAction("com.scanner.broadcast");
        
        // Регистрируем новый barcodeDataReceiver вместо старого qrcodeReceiver
        registerReceiver(barcodeDataReceiver, scannerFilter);
        
        Log.d("Prixod", "Зарегистрировали BroadcastReceiver для данных сканера с фильтрами: " +
                         scannerFilter.getAction(0) + ", " + 
                         (scannerFilter.countActions() > 1 ? scannerFilter.getAction(1) : "нет") + ", " +
                         (scannerFilter.countActions() > 2 ? scannerFilter.getAction(2) : "нет") + ", " +
                         (scannerFilter.countActions() > 3 ? scannerFilter.getAction(3) : "нет") + ", " +
                         (scannerFilter.countActions() > 4 ? scannerFilter.getAction(4) : "нет"));
    }

    /**
     * Проверяет размер экрана и устанавливает флаг isSmallScreen
     */
    private void checkScreenSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        
        // Вычисляем размер экрана в дюймах
        float widthInches = displayMetrics.widthPixels / displayMetrics.xdpi;
        float heightInches = displayMetrics.heightPixels / displayMetrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
        
        // Если диагональ меньше или равна 4.5 дюймам, считаем экран маленьким
        isSmallScreen = diagonalInches <= 4.5;
        Log.d("Prixod", "Диагональ экрана: " + diagonalInches + " дюймов, маленький экран: " + isSmallScreen);
    }

    /**
     * Переопределяем метод нажатия кнопки "Назад" для предотвращения
     * случайного выхода из экрана приема.
     */
    @Override
    public void onBackPressed() {
        // Если боковая панель открыта, закрываем её
        super.onBackPressed();
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        // Проверяем ошибки перед выходом через системную кнопку "Назад"
        Set<String> validationErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Обнаружены ошибки ввода. Исправьте их перед выходом.", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            prixodViewModel.requestFocusOnError(firstErrorUuid);
            // Не вызываем super.onBackPressed() и не выходим, пока есть ошибки
            return;
        }
        
        // Если ошибок нет, показываем стандартное предупреждение о кнопках
        // (или можно разрешить выход, если нет ошибок)
        // Пока оставим как было - предупреждение, что нужно использовать кнопки в UI
        Toast.makeText(this, "Для возврата используйте кнопки 'Вернуться в меню' или 'Отправить'", Toast.LENGTH_LONG).show();
        
        // НЕ вызываем super.onBackPressed(), чтобы предотвратить возврат назад
        // Если бы мы хотели разрешить выход при отсутствии ошибок, то здесь бы вызвали super.onBackPressed();
    }

    /**
     * Освобождаем ресурсы: отменяем регистрацию BroadcastReceiver-ов,
     * чтобы избежать утечек памяти при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Безопасная отписка приемников с проверкой исключений
        try {
            // unregisterReceiver(uuidReceiver); // uuidReceiver удален
        } catch (IllegalArgumentException e) {
            // Приемник не был зарегистрирован
            // Log.e("Prixod", "Ошибка при отписке uuidReceiver: " + e.getMessage());
        }
        
        try {
            // unregisterReceiver(qrcodeReceiver); // qrcodeReceiver заменен на barcodeDataReceiver
            if (barcodeDataReceiver != null) {
                unregisterReceiver(barcodeDataReceiver);
            }
        } catch (IllegalArgumentException e) {
            // Приемник не был зарегистрирован
            Log.e("Prixod", "Ошибка при отписке barcodeDataReceiver: " + e.getMessage());
        }
    }

    /**
     * Метод для поиска товара по его UUID.
     * Прокручивает RecyclerView до нужного элемента и устанавливает на нем фокус.
     * ЭТОТ МЕТОД БУДЕТ УДАЛЕН, ТАК КАК ЛОГИКА ПЕРЕНЕСЕНА В VIEWMODEL
     * @param uuid Идентификатор товара, полученный от сканера.
     */
    /*
    private void focusOnProduct(String uuid) {
        Log.d("Prixod", "Поиск товара по UUID: " + uuid);
        
        // Получаем позицию товара в списке по UUID
        int position = adapter.getPositionBySeriasUUID(uuid);
        Log.d("Prixod", "Результат поиска по СерияГУИД - позиция: " + position);
        
        if (position != -1) {
            // Если товар найден, прокручиваем список до нужного элемента и запрашиваем фокус
            rv_info.scrollToPosition(position);
            adapter.requestFocusAt(position);
            Toast.makeText(this, "Товар найден по СерияГУИД", Toast.LENGTH_SHORT).show();
        } else {
            // Попробуем найти по НоменклатураГУИД (для совместимости со старым форматом)
            position = adapter.getPositionByUUID(uuid);
            Log.d("Prixod", "Результат поиска по НоменклатураГУИД - позиция: " + position);
            
            if (position != -1) {
                // Если товар найден по НоменклатураГУИД
                rv_info.scrollToPosition(position);
                adapter.requestFocusAt(position);
                Toast.makeText(this, "Товар найден по НоменклатураГУИД", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Если товар не найден (возможно, он отфильтрован), сбрасываем фильтры
            List<Predicate<Product>> currentFilters = adapter.getFilters();
            boolean hasFilters = !currentFilters.isEmpty();
            
            // Сбрасываем только фильтры, сохраняя сортировку
            adapter.clearFilters();
            
            // Повторяем поиск товара после сброса фильтров
            position = adapter.getPositionBySeriasUUID(uuid);
            Log.d("Prixod", "Результат поиска после сброса фильтров (СерияГУИД) - позиция: " + position);
            
            if (position != -1) {
                // Если товар найден после сброса фильтров
                rv_info.scrollToPosition(position);
                adapter.requestFocusAt(position);
                
                if (hasFilters) {
                    Toast.makeText(this, "Фильтрация сброшена для отображения товара", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Последняя попытка - поиск по НоменклатураГУИД после сброса фильтров
                position = adapter.getPositionByUUID(uuid);
                Log.d("Prixod", "Результат поиска после сброса фильтров (НоменклатураГУИД) - позиция: " + position);
                
                if (position != -1) {
                    rv_info.scrollToPosition(position);
                    adapter.requestFocusAt(position);
                    Toast.makeText(this, "Товар найден по НоменклатураГУИД после сброса фильтров", Toast.LENGTH_SHORT).show();
                } else {
                    // Если товар не найден даже после сброса фильтров
                    Toast.makeText(this, "Товар с таким UUID не найден", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    */

    /**
     * Метод копирования файла из assets во временную директорию приложения.
     * Используется для получения файла JSON, который потом парсится..
     */
    private File copyAssetToFile(String assetFileName) throws IOException {
        File outFile = new File(getCacheDir(), assetFileName);
        try (InputStream is = getAssets().open(assetFileName);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        return outFile;
    }

    // Метод для снятия фокуса при нажатии вне EditText
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Обработка событий касания экрана
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isUserTouchingScreen = true;
            
            // Если касание не на боковом меню и оно открыто, закрываем его и очищаем фокус
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                View menuView = findViewById(R.id.navigation_view);
                if (menuView != null && !isTouchOnView(event, menuView)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    clearFocusFromEditTexts();
                    hideKeyboard(drawerLayout);
                    return true; // Прерываем обработку события
                }
            }
            
            // Проверяем, было ли касание по текущему EditText с фокусом
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                // Проверим, попало ли касание на это поле
                if (!isTouchOnView(event, focusedView)) {
                    // Если касание не на поле ввода, снимаем фокус и скрываем клавиатуру
                    focusedView.clearFocus();
                    hideKeyboard(focusedView);
                }
            }
            
            // Задержка для сброса флага касания
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isUserTouchingScreen = false;
                }
            }, 1000); // 1 секунда для блокировки анимаций, связанных с касанием
        }
        
        return super.dispatchTouchEvent(event);
    }


    // Метод для скрытия клавиатуры и снятия фокуса
    private void hideKeyboard(View view) {
        if (view != null) {
            // Сохраняем ссылку на текущий фокус перед скрытием клавиатуры
            View currentFocus = getCurrentFocus();
            
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            
            // Если был фокус в поле searchname_editText, восстанавливаем его
            if (currentFocus != null && (currentFocus.getId() == R.id.searchname_editText)) {
                // Восстанавливаем фокус с небольшой задержкой
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        currentFocus.requestFocus();
                        if (currentFocus instanceof EditText) {
                            ((EditText) currentFocus).setSelection(((EditText) currentFocus).getText().length());
                        }
                    }
                }, 50);
            }
        }
    }
    
    /**
     * Скрывает клавиатуру, используя корневое представление
     */
    private void hideKeyboard() {
        View rootView = getWindow().getDecorView().getRootView();
        if (rootView != null) {
            hideKeyboard(rootView);
        }
    }

    /**
     * Настраивает плавающий компонент сортировки
     */
    private void setupFloatingSortView() {
        if (floatingSortView == null) return;
        
        floatingSortView.setSortChangeListener(new FloatingSortView.SortChangeListener() {
            @Override
            public void onSortChanged(SortCriteria criteria) {
                prixodViewModel.setSortCriteria(criteria);
            }

            @Override
            public void onClearSort() {
                prixodViewModel.clearSort();
            }
        });
    }

    /**
     * Настраивает слушатели для элементов фильтрации
     */
    private void setupFilterListeners() {
        EditText filterNameEditText = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
        ImageButton clearNameButton = findViewById(R.id.clear_name);
        ImageButton clearAmountButton = findViewById(R.id.clear_amount);
        Button btnResetAllFiltersButton = findViewById(R.id.btn_reset_all_filters);

        // Фильтр по имени
        if (filterNameEditText != null) {
            setupTextWatcher(filterNameEditText, new TextChangeListener() {
            @Override
                public void onTextChanged(String newText) {
                    // if (adapter != null) adapter.applyNameFilter(newText); // Старая логика
                    // updateFilterIndicator(); // Старая логика
                    prixodViewModel.updateNameFilter(newText);
                }
            });
            setupEditTextFocusChangeListener(filterNameEditText);

            if (clearNameButton != null) {
                clearNameButton.setOnClickListener(v -> {
                    filterNameEditText.setText(""); // Очищаем поле, TextWatcher вызовет prixodViewModel.updateNameFilter("")
                    // prixodViewModel.updateNameFilter(""); // Можно и напрямую, но TextWatcher должен сработать
                });
            }
        }

        // Фильтр по количеству (минимальное значение)
        if (amountFilterEditText1 != null) {
            setupTextWatcher(amountFilterEditText1, new TextChangeListener() {
            @Override
                public void onTextChanged(String newText) {
                    applyAmountFilterFromUI();
                }
            });
            setupEditTextFocusChangeListener(amountFilterEditText1);
        }

        // Фильтр по количеству (максимальное значение)
        if (amountFilterEditText2 != null) {
            setupTextWatcher(amountFilterEditText2, new TextChangeListener() {
            @Override
                public void onTextChanged(String newText) {
                    applyAmountFilterFromUI();
                }
            });
            setupEditTextFocusChangeListener(amountFilterEditText2);
        }
        
        if (clearAmountButton != null && amountFilterEditText1 != null && amountFilterEditText2 != null) {
            clearAmountButton.setOnClickListener(v -> {
                boolean triggerUpdate = !amountFilterEditText1.getText().toString().isEmpty() || !amountFilterEditText2.getText().toString().isEmpty();
                amountFilterEditText1.setText("");
                amountFilterEditText2.setText(""); // TextWatcher'ы вызовут applyAmountFilterFromUI, который передаст null, null в ViewModel
                // if(triggerUpdate) applyAmountFilterFromUI(); // Можно вызвать принудительно, если надо
            });
        }


        // Фильтр "Только нетронутые"
        if (filterUntouchedCheckBox != null) {
            filterUntouchedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // if (adapter != null) adapter.applyUntouchedFilter(isChecked); // Старая логика
                // updateFilterIndicator(); // Старая логика
                prixodViewModel.updateUntouchedFilter(isChecked);
                hideKeyboard(); // Скрываем клавиатуру при изменении чекбокса
            });
        }

        // Кнопка "Сбросить все фильтры"
        if (btnResetAllFiltersButton != null) {
            btnResetAllFiltersButton.setOnClickListener(v -> {
                // resetAllFilters(); // Старая логика
                prixodViewModel.resetAllFilters(); // ViewModel обновит LiveData, что приведет к обновлению UI
                hideKeyboard();
            });
        }
        // updateFilterIndicator(); // Старая логика, теперь через LiveData
        // updateAmountHints(0,0); // Старая логика, теперь через LiveData или ViewModel
    }

    private void applyAmountFilterFromUI() {
        EditText amountFilterEditText1 = findViewById(R.id.filter_name); // Ошибка была здесь, должно быть amount_filter_editText1
        EditText amountFilterEditText1_correct = findViewById(R.id.amount_filter_editText1); // Правильная ссылка
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);

        if (amountFilterEditText1_correct == null || amountFilterEditText2 == null) return;

        String minAmountStr = amountFilterEditText1_correct.getText().toString();
        String maxAmountStr = amountFilterEditText2.getText().toString();

        Integer minAmount = null;
        Integer maxAmount = null;

        try {
            if (!minAmountStr.isEmpty()) {
                minAmount = Integer.parseInt(minAmountStr);
                }
            } catch (NumberFormatException e) {
            // amountFilterEditText1_correct.setError("Неверное число"); // Можно добавить обработку ошибки
        }

        try {
            if (!maxAmountStr.isEmpty()) {
                maxAmount = Integer.parseInt(maxAmountStr);
                }
            } catch (NumberFormatException e) {
            // amountFilterEditText2.setError("Неверное число");
        }

        // Валидация: если min > max, можно показать ошибку или поменять их местами
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            // Toast.makeText(this, "Мин. значение не может быть больше макс.", Toast.LENGTH_SHORT).show();
            // amountFilterEditText1_correct.setError("Мин > Макс");
            // amountFilterEditText2.setError("Мин > Макс");
            // Пока просто передаем как есть, ViewModel может содержать более сложную логику
        }
        
        prixodViewModel.updateAmountFilter(minAmount, maxAmount);
        // if (adapter != null) adapter.applyAmountFilter(currentMin, currentMax); // Старая логика
        // updateFilterIndicator(); // Старая логика
    }

    /**
     * Настраивает обработчик изменения текста для поля ввода
     */
    private void setupTextWatcher(EditText editText, TextChangeListener listener) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Ничего не делаем
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Ничего не делаем
            }

            @Override
            public void afterTextChanged(Editable s) {
                listener.onTextChanged(s.toString());
            }
        });
    }

    /**
     * Интерфейс для обработки изменения текста
     */
    private interface TextChangeListener {
        void onTextChanged(String newText);
    }

    /**
     * Настраивает обработчик изменения фокуса для полей ввода
     */
    private void setupEditTextFocusChangeListener(EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // При получении фокуса прокручиваем к полю ввода
                scrollToView(editText);
            }
        });
    }

    /**
     * Настраивает слушатель касаний для NestedScrollView, чтобы обрабатывать
     * клики вне полей ввода и скрывать клавиатуру
     */
    private void setupNestedScrollViewTouchListener() {
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocus = getCurrentFocus();
                    
                    // Проверяем, не касание ли в поле ввода
                    if (currentFocus instanceof EditText && !isTouchOnView(event, currentFocus)) {
                        // Скрываем клавиатуру
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        currentFocus.clearFocus();
                    }
                }
                return false; // Позволяем событию продолжить распространение
            });
        }
    }

    /**
     * Проверяет, находится ли касание в пределах указанного View
     */
    private boolean isTouchOnView(MotionEvent event, View view) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        
        float touchX = event.getRawX();
        float touchY = event.getRawY();
        
        return (touchX >= viewLocation[0] && touchX <= viewLocation[0] + view.getWidth() &&
                touchY >= viewLocation[1] && touchY <= viewLocation[1] + view.getHeight());
    }

    /**
     * Прокручивает к указанному View для обеспечения его видимости
     */
    private void scrollToView(View view) {
        if (view == null) return;
        
        // Находим ScrollView в NavigationView
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            // Получаем координаты представления
            int[] location = new int[2];
            view.getLocationInWindow(location);
            
            // Получаем размеры экрана
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            // Оцениваем высоту клавиатуры (примерно 40% высоты экрана)
            int keyboardHeight = (int) (screenHeight * 0.4);
            
            // Вычисляем смещение, чтобы прокрутить представление так,
            // чтобы оно было видно над оценочной высотой клавиатуры
            int viewBottom = location[1] + view.getHeight();
            int visibleAreaBottom = screenHeight - keyboardHeight;
            
            // Проверяем, нужна ли прокрутка
            if (viewBottom > visibleAreaBottom) {
                // Вычисляем необходимое смещение прокрутки с дополнительным отступом для комфорта
                int scrollOffset = viewBottom - visibleAreaBottom + 50; // 50px дополнительный отступ
                
                // Получаем текущую позицию прокрутки
                int currentScroll = scrollView.getScrollY();
                
                // Прокручиваем с анимацией до новой позиции
                scrollView.smoothScrollTo(0, currentScroll + scrollOffset);
            }
        }
    }

    /**
     * Обновляет индикатор активности фильтров

    private void updateFilterIndicator() {
        if (adapter != null) {
            boolean anyFilterActive = adapter.isAnyFilterActive();
            filterIndicator.setVisibility(anyFilterActive ? View.VISIBLE : View.INVISIBLE);
        }
    }
     */
    /**
     * Обновляет подсказки для полей фильтрации по количеству
     * 
     * @param minAmount минимальное количество
     * @param maxAmount максимальное количество
     */
    public void updateAmountHints(int minAmount, int maxAmount) {
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        
        if (amountFilterEditText1 != null && amountFilterEditText2 != null) {
            // Устанавливаем подсказки для полей ввода
            amountFilterEditText1.setHint(String.valueOf(minAmount));
            amountFilterEditText2.setHint(String.valueOf(maxAmount));
        }
    }

    /**
     * Сбрасывает все фильтры и сортировку
     */
    private void resetAllFilters() { // Старый метод, будет заменен вызовом ViewModel
    
        prixodViewModel.resetAllFilters();
    }

    /**
     * Настраивает слушатель для обработки нажатия клавиши Enter в полях EditText
     * @param editText поле ввода, для которого настраивается слушатель
     */
    private void setupEnterKeyListener(EditText editText) {
        // Используем OnKeyListener вместо OnEditorActionListener для лучшего контроля
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Обрабатываем только нажатие клавиши Enter
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Принудительно скрываем клавиатуру
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    
                    // Очищаем фокус с текущего элемента
                    v.clearFocus();
                    
                    // Закрываем NavigationView, если открыт
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    
                    // Возвращаем true, чтобы предотвратить дальнейшую обработку нажатия Enter
                    return true;
                }
                return false;
            }
        });
        
        // Дополнительно блокируем стандартное поведение IME_ACTION
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_GO) {
                
                // Принудительно скрываем клавиатуру
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                
                // Очищаем фокус
                v.clearFocus();
                
                // Закрываем NavigationView, если открыт
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                
                // Возвращаем true, чтобы предотвратить дальнейшую обработку
                return true;
            }
            return false;
        });
    }
    
    /**
     * Настраивает наблюдателей для LiveData из PrixodViewModel.
     */
    private void observeViewModel() {
        prixodViewModel.productsLiveData.observe(this, products -> {
            Set<String> currentErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
            if (adapter == null) {
                // Инициализация адаптера, если еще не сделано
                adapter = new InputInfoAdapter(PrixodActivity.this, products, this, this);
                rv_info.setAdapter(adapter);
                // Добавляем ItemDecoration только один раз
                if (rv_info.getItemDecorationCount() == 0) {
                    int spaceHeight = getResources().getDimensionPixelSize(R.dimen.item_space_height);
                    int sideSpace = getResources().getDimensionPixelSize(R.dimen.item_side_space);
                    rv_info.addItemDecoration(new ItemSpaceDecoration(spaceHeight, sideSpace));
                }
                adapter.updateData(products, currentErrors); // Начальное обновление с ошибками
            } else {
                adapter.updateData(products, currentErrors); // Передаем ошибки при обновлении
            }
            // Дополнительные действия при обновлении списка, если нужны
            // Например, прокрутка к определенной позиции или обновление сводной информации
        });

        prixodViewModel.isLoadingLiveData.observe(this, isLoading -> {
            if (isLoading) {
                showLoadingDialog("Загрузка данных...");
            } else {
                dismissLoadingDialog();
            }
        });

        prixodViewModel.errorLiveData.observe(this, event -> {
            String errorMessage = event.getContentIfNotHandled();
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                // Можно добавить более сложную логику обработки ошибок, например, диалоговое окно
            }
        });

        prixodViewModel.userFullNameLiveData.observe(this, fullName -> {
            if (userFullNameTextView != null) {
                userFullNameTextView.setText(fullName != null ? fullName : "Пользователь");
            }
        });

        prixodViewModel.userRoleLiveData.observe(this, role -> {
            if (userRoleTextView != null) {
                userRoleTextView.setText(role != null ? role : "Роль не определена");
            }
        });
        
        // Подписка на изменения критерия сортировки
        prixodViewModel.sortCriteriaLiveData.observe(this, criteria -> {
            if (floatingSortView != null) {
                Boolean isAscending = prixodViewModel.isSortAscendingLiveData.getValue();
                floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
            }
        });

        // Подписка на изменения направления сортировки
        // Нужна, если направление может измениться независимо от критерия (хотя в текущей логике ViewModel это не так)
        // или для инициализации при первом запуске, если LiveData не сработает сразу для criteria.
        prixodViewModel.isSortAscendingLiveData.observe(this, isAscending -> {
            if (floatingSortView != null) {
                SortCriteria criteria = prixodViewModel.sortCriteriaLiveData.getValue();
                if (criteria != null) { // Убедимся, что критерий уже установлен
                    floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
                }
            }
        });

        prixodViewModel.focusProductPositionLiveData.observe(this, event -> {
            Integer position = event.getContentIfNotHandled();
            if (position != null && position != -1 && rv_info != null) {
                rv_info.scrollToPosition(position);
                // Задержка для того, чтобы RecyclerView успел прокрутиться и ViewHolder был доступен
                new Handler().postDelayed(() -> {
                    RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(position);
                    if (viewHolder instanceof InputInfoAdapter.InputinfoViewHolder) {
                        InputInfoAdapter.InputinfoViewHolder prixodViewHolder = (InputInfoAdapter.InputinfoViewHolder) viewHolder;
                        EditText targetEditText = prixodViewHolder.list_carried; // Получаем ссылку на EditText
                        if (targetEditText != null) {
                            targetEditText.requestFocus();
                            // Опционально: показать клавиатуру (если это числовое поле, клавиатура скрыта)
                            // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            // imm.showSoftInput(targetEditText, InputMethodManager.SHOW_IMPLICIT);
                            if (targetEditText.getText().length() > 0) {
                               targetEditText.setSelection(targetEditText.getText().length());
                            }
                        }
                    }
                }, 100); // Небольшая задержка
                Toast.makeText(this, "Товар найден и сфокусирован.", Toast.LENGTH_SHORT).show();
            }
        });

        // Подписка на событие "товар не найден для фокуса"
        prixodViewModel.productNotFoundForFocusEvent.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        // Новые подписки для фильтров и сортировки
        prixodViewModel.nameFilterLiveData.observe(this, name -> {
            EditText filterNameEditText = findViewById(R.id.filter_name);
            if (filterNameEditText != null && !filterNameEditText.getText().toString().equals(name)) {
                filterNameEditText.setText(name); // Обновляем UI, если значение изменилось из ViewModel (например, при сбросе)
            }
        });

        prixodViewModel.minAmountFilterLiveData.observe(this, min -> {
            EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
            if (amountFilterEditText1 != null) {
                String currentText = amountFilterEditText1.getText().toString();
                String newText = (min == null || min == 0) ? "" : String.valueOf(min);
                if (!currentText.equals(newText)) {
                    amountFilterEditText1.setText(newText);
                }
            }
        });

        prixodViewModel.maxAmountFilterLiveData.observe(this, max -> {
            EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
            if (amountFilterEditText2 != null) {
                String currentText = amountFilterEditText2.getText().toString();
                String newText = (max == null || max == Integer.MAX_VALUE) ? "" : String.valueOf(max);
                 if (!currentText.equals(newText)) {
                    amountFilterEditText2.setText(newText);
                }
            }
        });

        prixodViewModel.untouchedFilterLiveData.observe(this, active -> {
            CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
            if (filterUntouchedCheckBox != null && filterUntouchedCheckBox.isChecked() != active) {
                filterUntouchedCheckBox.setChecked(active);
            }
        });

        prixodViewModel.isAnyFilterActiveLiveData.observe(this, isActive -> {
            if (filterIndicator != null) {
                filterIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
            }
        });

        prixodViewModel.validationErrorUuidsLiveData.observe(this, errorUuids -> {
            if (adapter != null) {
                // Передаем обновленный список ошибок в адаптер, чтобы он перерисовался
                // ViewModel сама обновит productsLiveData, если это необходимо из-за изменения ошибок,
                // поэтому здесь достаточно просто передать новые ошибки для текущего списка продуктов.
                // Однако, если логика ViewModel не предполагает автоматического обновления productsLiveData
                // только из-за изменения errorUuids, то может потребоваться явный вызов notifyDataSetChanged
                // или обновление списка продуктов.
                // В текущей реализации InputInfoAdapter.updateData(products, errorUuids) вызовет notifyDataSetChanged.
                adapter.updateData(prixodViewModel.productsLiveData.getValue(), errorUuids);
            }
            if (errorUuids != null && !errorUuids.isEmpty()) {
                Log.w("PrixodActivity", "Validation errors present for UUIDs: " + errorUuids.toString());
                // Здесь можно, например, дизейблить кнопку отправки или показывать общее предупреждение
                // MaterialButton sendButton = findViewById(R.id.inputinformation_list_button); // Находим кнопку отправки в разметке адаптера/футера
                // if (sendButton != null) sendButton.setEnabled(false);
            } else {
                Log.d("PrixodActivity", "No validation errors.");
                // MaterialButton sendButton = findViewById(R.id.inputinformation_list_button); // Находим кнопку отправки в разметке адаптера/футера
                // if (sendButton != null) sendButton.setEnabled(true);
            }
        });

        prixodViewModel.forceResetFiltersMessageLiveData.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Загружает данные перемещения на основе переданного UUID.
     * Этот метод обрабатывает данные из JSON файла и отображает их в RecyclerView.
     *
     * @param moveUuid Уникальный идентификатор перемещения
     */
    private void loadMoveData(String moveUuid) {
        // ЭТА ЛОГИКА ДОЛЖНА ПЕРЕЕХАТЬ В РЕПОЗИТОРИЙ/DATAPROVIDER
        // ... existing code ...
    }
    
    // Индикатор загрузки
    private com.step.tcd_rpkb.utils.LoadingDialog loadingDialog;
    
    /**
     * Показывает диалог загрузки
     */
    public void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(this);
        }
        loadingDialog.show(message);
    }
    
    /**
     * Скрывает диалог загрузки
     */
    public void dismissLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    /**
     * Очищает фокус со всех полей ввода
     */
    private void clearFocusFromEditTexts() {
        // Снимаем фокус со всего окна
        getWindow().getDecorView().clearFocus();
        
        // Найдем все видимые EditText через RecyclerView
        if (rv_info != null) {
            // Снимаем фокус с самого RecyclerView
            rv_info.clearFocus();
            
            for (int i = 0; i < rv_info.getChildCount(); i++) {
                View child = rv_info.getChildAt(i);
                if (child != null) {
                    // Очищаем фокус с любых EditText внутри view
                    clearFocusRecursively(child);
                }
            }
        }
        
        // Также очищаем фокус с полей фильтрации
        EditText filterName = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        
        if (filterName != null) {
            filterName.clearFocus();
        }
        
        if (amountFilterEditText1 != null) {
            amountFilterEditText1.clearFocus();
        }
        
        if (amountFilterEditText2 != null) {
            amountFilterEditText2.clearFocus();
        }
    }
    
    /**
     * Временно отключает возможность получения фокуса для всех элементов в ViewGroup
     */
    private void disableFocusRecursively(ViewGroup viewGroup) {
        viewGroup.setFocusable(false);
        viewGroup.setFocusableInTouchMode(false);
        
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            child.setFocusable(false);
            child.setFocusableInTouchMode(false);
            
            if (child instanceof ViewGroup) {
                disableFocusRecursively((ViewGroup) child);
            }
        }
    }
    
    /**
     * Создает модную анимацию для кнопки меню
     * @param view кнопка меню
     */
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

    /**
     * Рекурсивно очищает фокус с любых EditText в иерархии views
     */
    private void clearFocusRecursively(View view) {
        if (view instanceof EditText) {
            view.clearFocus();
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearFocusRecursively(group.getChildAt(i));
            }
        }
    }

    /**
     * Фильтрует продукты по строке поиска
     * 
     * @param query Строка поиска

    private void filterProductsBySearchQuery(String query) {
        if (adapter == null) return;
        
        lastSearchQuery = query;
        
        if (query != null && !query.isEmpty()) {
            isSearchActive = true;
            adapter.filterByName(query);
        } else {
            isSearchActive = false;
            // Очищаем фильтр по имени, оставляя другие фильтры активными
            adapter.removeFilter(InputInfoAdapter.NameFilter.class);
        }
        
        // Обновляем UI
        updateFilterIndicator();
    }
     */
    /**
     * Инициализирует BroadcastReceiver для обработки данных сканера.
     */
    private void initializeBarcodeDataReceiver() {
        barcodeDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("PrixodActivity", "Получен Broadcast от сканера: " + action);

                String barcodeData = null;
                if ("com.example.scannerapp.ACTION_BARCODE_DATA".equals(action)) {
                    barcodeData = intent.getStringExtra("data");
                } else if ("android.intent.action.SCAN".equals(action) ||
                           "com.google.zxing.client.android.SCAN".equals(action)) {
                    barcodeData = intent.getStringExtra("SCAN_RESULT");
                } else if ("com.symbol.datawedge.api.ACTION".equals(action)) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        if (extras.containsKey("com.symbol.datawedge.data_string")) {
                            barcodeData = extras.getString("com.symbol.datawedge.data_string");
                        } else if (extras.containsKey("data")) { // Некоторые сканеры могут использовать просто "data"
                            barcodeData = extras.getString("data");
                        }
                    }
                } else if ("com.scanner.broadcast".equals(action)) {
                    barcodeData = intent.getStringExtra("data");
                }

                if (barcodeData != null && !barcodeData.isEmpty()) {
                    Log.d("PrixodActivity", "Сканированные данные: " + barcodeData);
                    prixodViewModel.processBarcodeData(barcodeData);
                } else {
                    Log.w("PrixodActivity", "Данные сканирования не получены или пусты для action: " + action);
                    // Логируем все extras для отладки, если данные не пришли ожидаемым образом
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        for (String key : extras.keySet()) {
                            Log.d("PrixodActivityScannerExtras", "Extra: " + key + " = " + extras.get(key));
                        }
                    }
                }
            }
        };
    }

    // --- Реализация AdapterButtonListener --- //
    @Override
    public void onSendDataClicked() {
        Set<String> validationErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Найдены ошибки ввода! Проверьте выделенные поля.", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            prixodViewModel.requestFocusOnError(firstErrorUuid);
            return; // Не отправляем данные, если есть ошибки
        }

        // Если ошибок нет, продолжаем
        showLoadingDialog("Подготовка данных...");

        // Получаем ГОТОВУЮ JSON-строку из ViewModel
        String productsJsonToSave = prixodViewModel.getProductsToSaveAsJson();

        // Небольшая задержка для UX, чтобы пользователь увидел диалог
        new Handler().postDelayed(() -> {
            // Gson gson = new Gson(); // Больше не нужно здесь
            // String json = gson.toJson(productsToSave); // Больше не нужно здесь

            // Сбрасываем состояние ViewModel перед выходом (фильтры, сортировку)
            prixodViewModel.resetAllFiltersAndSort(); // Или другой подходящий метод

            dismissLoadingDialog();
            Intent intent = new Intent();
            intent.putExtra("productData", productsJsonToSave); // Используем готовую JSON-строку
            // Флаг restoreFilters может быть больше не нужен в явном виде,
            // так как состояние фильтров/сортировки управляется ViewModel и должно сохраняться/сбрасываться там.
            // Но если он используется в вызывающей активности для каких-то специфических нужд, его можно оставить.
            intent.putExtra("restoreFilters", true); 
            setResult(PrixodActivity.RESULT_OK, intent);
            finish();
        }, 300); // Короткая задержка
    }

    @Override
    public void onGoBackClicked() {
        // Проверяем ошибки перед выходом
        Set<String> validationErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Перед выходом исправьте ошибки ввода!", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            prixodViewModel.requestFocusOnError(firstErrorUuid);
            return; // Не выходим, если есть ошибки
        }

        // Если ошибок нет, продолжаем выход
        showLoadingDialog("Возврат к списку...");
        // Сбрасываем состояние ViewModel перед выходом
        prixodViewModel.resetAllFiltersAndSort(); // Или другой подходящий метод
        
        // Небольшая задержка для UX
        new Handler().postDelayed(() -> {
            dismissLoadingDialog();
            Intent intent = new Intent();
            // Также вопрос про restoreFilters
            intent.putExtra("restoreFilters", true);
            setResult(PrixodActivity.RESULT_CANCELED, intent);
            finish();
        }, 200);
    }

    // --- Реализация OnProductDataChangedListener --- //
    @Override
    public void onProductDataConfirmed(String nomenclatureUuid, int newTakenValue, int positionInAdapter, boolean isValid, boolean byEnterKey) {
        Log.d("PrixodActivity", "Data confirmed for UUID: " + nomenclatureUuid + ", new value: " + newTakenValue + ", position: " + positionInAdapter + ", isValid: " + isValid + ", byEnter: " + byEnterKey);
        prixodViewModel.handleProductDataConfirmation(nomenclatureUuid, newTakenValue, isValid, byEnterKey, positionInAdapter); 
        // positionInAdapter может понадобиться ViewModel для определения следующего элемента для фокуса
        
        // Если в будущем потребуется обновлять сводку или другие элементы UI немедленно
        // на основе этого изменения, это можно будет сделать здесь, 
        // подписавшись на соответствующий LiveData из ViewModel, который изменится после 
        // вызова updateProductTakenValue.
        // Например: prixodViewModel.getSummaryData().observe(...) или prixodViewModel.forceRefreshProductsDisplay() (если необходимо)
        // Также, если isValid == false, можно инициировать дополнительную логику в UI, например, показать Toast
        // if (!isValid) {
        //     Toast.makeText(this, "Введено некорректное количество для товара!", Toast.LENGTH_SHORT).show();
        // }
    }
}
