package com.step.tcd_rpkb.UI.Prixod.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.step.tcd_rpkb.UI.Prixod.viewmodel.ProductsViewModel;
import com.step.tcd_rpkb.UI.Serias.activity.SeriesSelectionActivity;
import com.step.tcd_rpkb.UI.Prixod.adapter.ProductsAdapter;
import com.step.tcd_rpkb.UI.Prixod.adapter.ItemSpaceDecoration;
import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.UI.Prixod.viewmodel.SortCriteria;
import com.step.tcd_rpkb.domain.model.Invoice;
import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.utils.AvatarUtils;
import com.step.tcd_rpkb.utils.FocusManager;
import com.step.tcd_rpkb.utils.UserViewAnimations;

import java.util.List;
import java.util.Set;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import android.view.inputmethod.EditorInfo;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import android.graphics.Rect;

@AndroidEntryPoint
public class ProductsActivity extends BaseFullscreenActivity
        implements ProductsAdapter.OnProductDataChangedListener { // Изменяем интерфейс

    // Константа для startActivityForResult
    private static final int REQUEST_SELECT_SERIES = 101;
                   
    // Флаг для отслеживания касаний пользователя
    public static boolean isUserTouchingScreen = false;
    private Handler focusHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingFocusRunnable = null;
    
    /**
     * Отключает клавиатуру для числовых полей, сохраняя видимость курсора
     * Метод написан по образцу MoveList_menu.java
     */
    private static void disableKeyboardForNumericField(EditText editText) {
        if (editText == null) return;
        
        try {
            // Сохраняем числовой тип ввода
            int inputType = android.text.InputType.TYPE_CLASS_NUMBER | 
                           android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
            
            // Отключаем подсказки при вводе
            inputType |= android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            
            editText.setInputType(inputType);
            
            // Обеспечиваем фокусировку
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            
            // Отключаем отображение клавиатуры при получении фокуса
            editText.setShowSoftInputOnFocus(false);
            
            // Обеспечиваем видимость курсора
            editText.setCursorVisible(true);
            
            // Отключаем возможность вызова контекстного меню долгим нажатием
            editText.setLongClickable(false);
            
            // Скрываем клавиатуру, если она показана
            InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e("PrixodActivity", "Error disabling keyboard for numeric field: " + e.getMessage());
        }
    }
    
    /**
     * Настраивает глобальный слушатель изменения фокуса для правильной обработки клавиатуры
     */
    private void setupGlobalFocusChangeListener() {
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
            (oldFocus, newFocus) -> {
                if (newFocus instanceof EditText) {
                    EditText editText = (EditText) newFocus;
                    
                    // Проверяем, находится ли EditText в NavigationView
                    boolean isInNavigationView = isViewInsideViewGroup(sort_bar, editText);
                    
                    // Получаем тип ввода
                    int inputType = editText.getInputType();
                    boolean isNumeric = (inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0;
                    
                    if (isNumeric) {
                        // Для числовых полей отключаем клавиатуру и сохраняем курсор
                        disableKeyboardForNumericField(editText);
                        
                        // Устанавливаем позицию курсора в конец текста, если он не пустой
                        if (editText.getText().length() > 0) {
                            editText.setSelection(editText.getText().length());
                        }
                    } else {
                        // Для текстовых полей показываем клавиатуру
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                    
                    // Если предыдущий фокус был на текстовом поле, а новый на числовом, скрываем клавиатуру

                    if (oldFocus instanceof EditText) {
                        int oldInputType = ((EditText) oldFocus).getInputType();
                        boolean oldIsText = (oldInputType & android.text.InputType.TYPE_CLASS_TEXT) != 0;
                        boolean newIsNumeric = (inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0;
                        
                        if (oldIsText && newIsNumeric) {
                            hideKeyboard(oldFocus);
                        }
                    }
                }
            }
        );
    }

    @Inject
    GetUserUseCase getUserUseCase;


    private RecyclerView rv_info;
    private ProductsAdapter adapter;
    private BroadcastReceiver barcodeDataReceiver; 

    private DrawerLayout drawerLayout;
    private NavigationView sort_bar;
    private View filterIndicator;
    private FloatingSortView floatingSortView;
    private TextView userFullNameTextView;
    private TextView userRoleTextView;
    private ImageView userAvatarImageView;
    
    //  переменные для панели выбора контейнера
    private View selectionActionPanel;
    private Button btnSelectSeries;
    private Button btnCancelItemSelection;
    private boolean isSelectionMode = false;
    private boolean isAnimatingSelectionPanel = false;

    //  переменные для блока информации о перемещении
    private View moveInfoBlockView;
    private TextView moveDisplayTextView;
    private TextView positionsCountView; 
    private TextView itemsCountView;
    //  переменные для прогресс-бара
    private ProgressBar progressBarFilled;
    private TextView progressRatioTextView;
    
    //  переменные для фиксированных кнопок
    private View fixedButtonsPanel;
    private MaterialButton btnSendInfo;
    private MaterialButton btnBackToMoveList;

    private ProductsViewModel productsViewModel;
    
    private Gson gson = new Gson(); // Для десериализации MoveItem


    private boolean isViewInsideViewGroup(ViewGroup group, View view) {
        if (group == null || view == null) return false;
        if (group == view) return true;
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == group) return true;
            if (!(parent instanceof View)) break;
            parent = parent.getParent();
        }
        return false;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        

        isAnimatingSelectionPanel = false;
        


        // Очищаем старые файлы (старше 7 дней)
        long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 дней в миллисекундах

        
        // Также очищаем старые файлы с данными продуктов
        com.step.tcd_rpkb.utils.ProductsDataManager productsDataManager = 
            new com.step.tcd_rpkb.utils.ProductsDataManager(this);
        int cleanedProductsFiles = productsDataManager.cleanupOldProductsFiles(maxAge);
        Log.d("PrixodActivity", "Очищено старых файлов с данными продуктов: " + cleanedProductsFiles);
        
        productsViewModel = new ViewModelProvider(this).get(ProductsViewModel.class);
        
        // Извлекаем данные из Intent и временного файла
        String moveUuid = getIntent().getStringExtra("moveUuid");
        
        // Получаем полный объект MoveItem, переданный из MoveList_menu
        String moveItemJson = getIntent().getStringExtra("moveItemJson");
        MoveItem moveItem = null;
        if (moveItemJson != null && !moveItemJson.isEmpty()) {
            try {
                moveItem = gson.fromJson(moveItemJson, MoveItem.class);
                Log.d("PrixodActivity", "Получен MoveItem из Intent: " + 
                      "номер=" + (moveItem != null ? moveItem.getNumber() : "null") + 
                      ", дата=" + (moveItem != null ? moveItem.getDate() : "null") + 
                      ", статус=" + (moveItem != null ? moveItem.getSigningStatus() : "null"));
            } catch (Exception e) {
                Log.e("PrixodActivity", "Ошибка парсинга MoveItem из JSON: " + e.getMessage(), e);
            }
        }
        
        // Загружаем сохраненные данные только для статуса "Комплектуется"
        String productsJson = null;

        
        // Если в файле данных нет ИЛИ статус не "Комплектуется", пытаемся загрузить из Intent
        if (productsJson == null) {
            productsJson = getIntent().getStringExtra("productsData");
            // Если данные не найдены по ключу "productsData", проверяем ключ "productData"
            if (productsJson == null) {
                productsJson = getIntent().getStringExtra("productData");
            }
            Log.d("PrixodActivity", "Загружены данные из Intent: " + 
                  (productsJson != null ? "найдено " + productsJson.length() + " символов" : "данных нет"));
        }
        
        boolean preserveEditedData = getIntent().getBooleanExtra("preserveEditedData", false);
        
        Log.d("PrixodActivity", "onCreate: moveUuid=" + moveUuid + 
                               ", productsJson=" + (productsJson != null ? "присутствует (" + productsJson.length() + " символов)" : "отсутствует") + 
                               ", preserveEditedData=" + preserveEditedData + 
                               ", moveItem=" + (moveItem != null ? "получен" : "null"));
        

        
        // Передаем все данные в ViewModel для определения режима чтения и отображения информации
        productsViewModel.loadInitialData(moveUuid, productsJson, preserveEditedData, moveItem);
        

        setupGlobalFocusChangeListener();
        
        drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton menuButton = findViewById(R.id.nav_menu_button);
        sort_bar = findViewById(R.id.navigation_view);
        
        if (sort_bar != null) {
            sort_bar.setFocusable(true);
            sort_bar.setFocusableInTouchMode(true);
        }
        

        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {

            View decorView = getWindow().getDecorView();
            Rect r = new Rect();
            decorView.getWindowVisibleDisplayFrame(r);
            int screenHeight = decorView.getHeight();
            

            int keyboardHeight = screenHeight - r.bottom;
            boolean isKeyboardShowing = keyboardHeight > screenHeight * 0.25;
            
            if (isKeyboardShowing) {
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText && isViewInsideViewGroup(sort_bar, focusedView)) {
                    scrollToViewInsideNavigationWithKeyboard(focusedView);
                }
            }
        });
        
        if (menuButton != null) {
            menuButton.setOnClickListener(null); 
            menuButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusView = getCurrentFocus();
                    if (currentFocusView instanceof EditText) {
                        // Очищаем фокус и скрываем клавиатуру
                        currentFocusView.clearFocus();
                        hideKeyboard(currentFocusView);
                        View rootView = findViewById(android.R.id.content);
                        if (rootView != null) rootView.requestFocus();
                    }
                    v.requestFocus();
                }
                return false;
            });
            menuButton.setOnClickListener(v -> {
                try {
                    View currentFocusView = getCurrentFocus();
                    if (currentFocusView instanceof EditText) {
                        // Очищаем фокус и скрываем клавиатуру
                        currentFocusView.clearFocus();
                        hideKeyboard(currentFocusView);
                    }
                    animateMenuButton(v);
                    v.postDelayed(() -> {
                        if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
                    }, 300);
                } catch (Exception e) {
                    Log.e("PrixodActivity", "Ошибка при обработке нажатия на menuButton: " + e.getMessage());
                    if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
                }
            });
            menuButton.setClickable(true);
            menuButton.setFocusable(true);
            menuButton.setFocusableInTouchMode(true);
        }
        
        rv_info = findViewById(R.id.rv_info);
        if (rv_info != null) {
            rv_info.setLayoutManager(new LinearLayoutManager(this));
            rv_info.setItemAnimator(null); // Отключаем анимации по умолчанию для RecyclerView
        } else {
            Log.e("Prixod", "Ошибка: не удалось найти RecyclerView (rv_info)");
            Toast.makeText(this, "Ошибка инициализации интерфейса", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeBarcodeDataReceiver();
        filterIndicator = findViewById(R.id.filter_indicator);
        floatingSortView = findViewById(R.id.floating_sort_view);
        setupFloatingSortView();
        
        // Инициализируем панель выбора контейнера
        selectionActionPanel = findViewById(R.id.selectionActionPanel);
        if (selectionActionPanel != null) {
            btnSelectSeries = selectionActionPanel.findViewById(R.id.btnSelectSeries);
            btnCancelItemSelection = selectionActionPanel.findViewById(R.id.btnCancelItemSelection);
            
            // Настраиваем обработчики нажатий
            if (btnSelectSeries != null) {
                btnSelectSeries.setOnClickListener(v -> {
                    if (adapter != null) {
                        Product selectedProduct = adapter.getSelectedProduct();
                        if (selectedProduct != null) {
                            // Рассчитываем общее количество в документе для данной номенклатуры
                            double totalQuantityInDocument = calculateTotalQuantityForNomenclature(selectedProduct.getNomenclatureUuid());

                            if (moveUuid != null && !moveUuid.isEmpty()) {
                                // Получаем все продукты из ViewModel
                                List<Product> allProducts = productsViewModel.getAllProducts();
                                

                                
                                // Сохраняем продукты по moveUuid
                                boolean saved = productsDataManager.saveProductsData(moveUuid, allProducts);
                                
                                if (!saved) {
                                    android.widget.Toast.makeText(ProductsActivity.this, 
                                        "Ошибка при сохранении данных продуктов", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                Log.d("PrixodActivity", "Сохранены данные продуктов для перемещения: " + 
                                      moveUuid + ", количество продуктов: " + allProducts.size());
                            } else {
                                Log.e("PrixodActivity", "moveUuid отсутствует, невозможно сохранить данные продуктов");
                                android.widget.Toast.makeText(ProductsActivity.this, 
                                    "Ошибка: отсутствует идентификатор перемещения", android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Запускаем Activity для подбора серии
                            Intent intent = new Intent(ProductsActivity.this, SeriesSelectionActivity.class);
                            // Передаем в Activity данные о выбранном продукте
                            intent.putExtra("nomenclatureUuid", selectedProduct.getNomenclatureUuid());
                            intent.putExtra("nomenclatureName", selectedProduct.getNomenclatureName());
                            intent.putExtra("productLineId", selectedProduct.getProductLineId());
                            intent.putExtra("moveUuid", moveUuid);
                            intent.putExtra("totalQuantity", totalQuantityInDocument);
                            intent.putExtra("wareHouse", getIntent().getStringExtra("wareHouse"));
                            Log.d("ProductsActivity" ,  "Передаем склад отправителя: " + getIntent().getStringExtra("wareHouse"));
                            startActivityForResult(intent, REQUEST_SELECT_SERIES);
                        }
                    }
                });
            }
            
            if (btnCancelItemSelection != null) {
                btnCancelItemSelection.setOnClickListener(v -> {
                    if (adapter != null) {
                        adapter.clearSelection();
                    }

                });
            }
        }
        
        View navHeaderView = sort_bar.findViewById(R.id.nav_header);
        if (navHeaderView != null) {
            View userCard = navHeaderView.findViewById(R.id.user_name_card);
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
                    UserViewAnimations.playFancyAnimation(userCard, userAvatarImageView, userFullNameTextView, userRoleTextView);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (userAvatarImageView != null && userAvatarImageView.isAttachedToWindow()) { 
                            UserViewAnimations.addSparkleEffect(userAvatarImageView);
                        }
                    }, 2000);
                }
            }
            
            // Добавляем блок с информацией о перемещении между шапкой пользователя и фильтрами
            addMoveInfoBlockToNavView();
        }
        
        setupFilterListeners();
        
        // Инициализируем фиксированные кнопки
        initializeFixedButtons();
        
        // Добавляем обработчик нажатия на основной контент для гарантированного снятия фокуса
        findViewById(R.id.main).setOnClickListener(v -> {
            View currentFocusView = getCurrentFocus();
            if (currentFocusView instanceof EditText) {
                // Проверяем, не является ли this EditText частью RecyclerView
                if (!isViewInsideViewGroup(rv_info, currentFocusView)) {
                    currentFocusView.clearFocus();
                    hideKeyboard(currentFocusView);
                }
            }
            resetFocusAndTouchFlag();
        });
        
        findViewById(R.id.main).setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocusView = getCurrentFocus();
                if (currentFocusView instanceof EditText && !isTouchOnView(event, currentFocusView)) { 
                    hideKeyboard(currentFocusView);
                    currentFocusView.clearFocus(); 
                    resetFocusAndTouchFlag();
                    }
                }
                return false;
        });
        
        setupNestedScrollViewTouchListener();
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                View mainContent = findViewById(R.id.main);
                if(mainContent != null) mainContent.setAlpha(1 - slideOffset * 0.3f);
                NavigationView navigationView = findViewById(R.id.navigation_view);
                View navHeaderViewLocal = navigationView.findViewById(R.id.nav_header);
                View userCard = navHeaderViewLocal.findViewById(R.id.user_name_card);
                if (userCard != null) {
                    userCard.setTranslationX(-100 + slideOffset * 100);
                    userCard.setAlpha(slideOffset);
                    float scale = 0.8f + (0.2f * slideOffset);
                    userCard.setScaleX(scale);
                    userCard.setScaleY(scale);
                }
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                NavigationView navigationView = findViewById(R.id.navigation_view);
                View navHeaderViewLocal = navigationView.findViewById(R.id.nav_header);
                View userCard = navHeaderViewLocal.findViewById(R.id.user_name_card);
                if (userCard != null) {
                    ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(userCard, "translationY", 0f, -10f, 0f);
                    bounceAnimator.setDuration(500);
                    bounceAnimator.setInterpolator(new android.view.animation.OvershootInterpolator());
                    bounceAnimator.start();
                }
                if (sort_bar != null) {
                    sort_bar.setFocusable(true); 
                    sort_bar.setFocusableInTouchMode(true);
                    sort_bar.requestFocus();
            }
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                // При закрытии NavigationView скрываем клавиатуру и очищаем все фокусы
                hideKeyboard(drawerLayout);
                clearFocusFromNonRecyclerViewEditTexts();
                clearAllFocusInNavigationEditTexts();
                
                FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
                if (navMenuButton != null) {
                    navMenuButton.setClickable(true);
                    navMenuButton.setEnabled(true);
                    navMenuButton.setFocusable(true);
                    navMenuButton.setFocusableInTouchMode(true);
                    View rootView = getWindow().getDecorView().getRootView();
                    if (rootView != null) rootView.requestFocus();
                    navMenuButton.requestFocus();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (navMenuButton != null && navMenuButton.isAttachedToWindow()) {
                            View currentFocusView = getCurrentFocus();
                            if (currentFocusView != navMenuButton) navMenuButton.requestFocus();
                        }
                    }, 50);
                }
                Boolean filtersActive = productsViewModel.isAnyFilterActiveLiveData.getValue();
                if (filtersActive != null && filtersActive && rv_info != null) {
                    rv_info.smoothScrollToPosition(0);
                }
            }
            @Override
            public void onDrawerStateChanged(int newState) {}
        });
        
        setupButtonAnimations();
        observeViewModel();
    }
    
    private void setupButtonAnimations() {
        try {
            final MaterialButton sendButton = findViewById(R.id.btn_send_info);
            final MaterialButton backButton = findViewById(R.id.btn_back_to_move_list);
            View.OnTouchListener buttonTouchListener = new View.OnTouchListener() {
                private ValueAnimator pulseAnimator;
                private boolean isAnimating = false;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (isAnimating && pulseAnimator != null && pulseAnimator.isRunning()) pulseAnimator.cancel();
                        v.animate().scaleX(0.95f).scaleY(0.95f).translationY(dpToPx(2)).setDuration(80).setInterpolator(new AccelerateInterpolator()).start();
                        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
                        pulseAnimator.setDuration(700);
                        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
                        pulseAnimator.addUpdateListener(animation -> {
                                float value = (float) animation.getAnimatedValue();
                                float scale = 0.95f + 0.01f * (float)Math.sin(value * Math.PI * 2);
                                v.setScaleX(scale);
                                v.setScaleY(scale);
                                float translateY = dpToPx(2) - dpToPx(0.3f) * (float)Math.sin(value * Math.PI * 2);
                                v.setTranslationY(translateY);
                        });
                        pulseAnimator.start();
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        if (pulseAnimator != null && pulseAnimator.isRunning()) pulseAnimator.cancel();
                        isAnimating = false;
                        AnimatorSet releaseAnimSet = new AnimatorSet();
                        ObjectAnimator bounce1 = ObjectAnimator.ofFloat(v, "translationY", v.getTranslationY(), -dpToPx(1));
                        bounce1.setDuration(100);
                        bounce1.setInterpolator(new AccelerateInterpolator(1.5f));
                        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1.03f);
                        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1.03f);
                        ObjectAnimator scaleXNormal = ObjectAnimator.ofFloat(v, "scaleX", 1.03f, 1.0f);
                        ObjectAnimator scaleYNormal = ObjectAnimator.ofFloat(v, "scaleY", 1.03f, 1.0f);
                        ObjectAnimator translationYNormal = ObjectAnimator.ofFloat(v, "translationY", -dpToPx(1), 0f);
                        AnimatorSet upAndNormalize = new AnimatorSet();
                        upAndNormalize.playTogether(scaleXUp, scaleYUp);
                        upAndNormalize.setDuration(150);
                        upAndNormalize.setInterpolator(new OvershootInterpolator(2.0f));
                        AnimatorSet normalizeFinal = new AnimatorSet();
                        normalizeFinal.playTogether(scaleXNormal, scaleYNormal, translationYNormal);
                        normalizeFinal.setDuration(200);
                        normalizeFinal.setInterpolator(new DecelerateInterpolator(1.2f));
                        releaseAnimSet.playSequentially(bounce1, upAndNormalize, normalizeFinal);
                        releaseAnimSet.start();
                    }
                    return false;
                }
            };
            if (sendButton != null) {
                sendButton.setBackgroundResource(R.drawable.primary_button_background);
                sendButton.setOnTouchListener(buttonTouchListener);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    sendButton.setElevation(dpToPx(2));
                    sendButton.setStateListAnimator(null);
                }
            }
            if (backButton != null) {
                backButton.setBackgroundResource(R.drawable.secondary_button_background);
                backButton.setOnTouchListener(buttonTouchListener);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    backButton.setElevation(dpToPx(2));
                    backButton.setStateListAnimator(null);
                }
            }
        } catch (Exception e) {
            Log.e("PrixodActivity", "Ошибка при настройке анимаций кнопок: " + e.getMessage());
        }
    }
    
    /**
     * Улучшенная версия метода для прокрутки к элементу в NavigationView при показе клавиатуры
     * @param view View, к которому нужно прокрутить содержимое
     */
    private void scrollToViewInsideNavigationWithKeyboard(View view) {
        if (view == null || sort_bar == null) return;
        
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView == null) {
            Log.e("PrixodActivity", "scrollToViewInsideNavigationWithKeyboard: scrollView не найден");
            return;
        }


        view.post(() -> {
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect); // Глобальные координаты поля ввода
            
            Rect windowRect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(windowRect);
            
            int screenHeight = getWindow().getDecorView().getRootView().getHeight();
            int keyboardHeight = screenHeight - windowRect.bottom;
            

            if (keyboardHeight < 100 && viewRect.top >= windowRect.top && viewRect.bottom <= windowRect.bottom) {
                Log.d("PrixodActivity", "scrollToViewInsideNavigationWithKeyboard: Клавиатура не видна или поле уже в зоне видимости");
                return;
            }
            

            int targetViewBottomY = windowRect.bottom - Math.round(dpToPx(16));
            

            int currentViewBottomY = viewRect.bottom;
            
            if (currentViewBottomY > targetViewBottomY) {
                // Поле ввода перекрывается клавиатурой или находится слишком низко
                int scrollAmount = currentViewBottomY - targetViewBottomY;
                scrollView.smoothScrollBy(0, scrollAmount);
                Log.d("PrixodActivity", "scrollToViewInsideNavigationWithKeyboard: Прокрутка на " + scrollAmount + " пикселей");
            } else {
                Log.d("PrixodActivity", "scrollToViewInsideNavigationWithKeyboard: Поле уже видно над клавиатурой");
            }
        });
    }

    /**
     * Улучшенный метод конвертации dp в пиксели
     * @param dp значение в dp
     * @return значение в пикселях
     */
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter scannerFilter = new IntentFilter("com.example.scannerapp.ACTION_BARCODE_DATA");
        scannerFilter.addAction("android.intent.action.SCAN");
        scannerFilter.addAction("com.google.zxing.client.android.SCAN");
        scannerFilter.addAction("com.symbol.datawedge.api.ACTION");
        scannerFilter.addAction("com.scanner.broadcast");
        // Добавляем поддержку ТСД Атол Smart.lite
        scannerFilter.addAction("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST");
        if (barcodeDataReceiver != null) {
            // совместимости с Android 7+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                registerReceiver(barcodeDataReceiver, scannerFilter, Context.RECEIVER_EXPORTED);
            } else {
                // Для Android 7-12
                registerReceiver(barcodeDataReceiver, scannerFilter);
            }
            Log.i("PrixodActivity", "BroadcastReceiver зарегистрирован для сканера");
            Log.d("PrixodActivity", "Поддерживаемые Actions сканера:");
            Log.d("PrixodActivity", "  - com.example.scannerapp.ACTION_BARCODE_DATA");
            Log.d("PrixodActivity", "  - android.intent.action.SCAN");
            Log.d("PrixodActivity", "  - com.google.zxing.client.android.SCAN");
            Log.d("PrixodActivity", "  - com.symbol.datawedge.api.ACTION");
            Log.d("PrixodActivity", "  - com.scanner.broadcast");
            Log.d("PrixodActivity", "  - com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST (Атол Smart.lite)");
        } else {
            Log.w("PrixodActivity", "barcodeDataReceiver is null in onResume, not registering.");
        }
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // Если выполняется анимация, сначала завершаем ее
        if (isAnimatingSelectionPanel) {
            resetSelectionPanelAnimation();
            return;
        }
        
        if (isSelectionMode) {
            // Если активен режим выбора, выходим из него
            setSelectionMode(false);
            return;
        }
        
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return; 
        }
        
        if (floatingSortView != null && floatingSortView.isPanelExpanded()) {
            floatingSortView.toggleSortPanel();
            return; 
        }
        
        View currentFocusView = getCurrentFocus();
        if (currentFocusView instanceof EditText) {

            hideKeyboard(currentFocusView);
            currentFocusView.clearFocus(); 
            resetFocusAndTouchFlag();
            return;
        }
        

        isUserTouchingScreen = false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (barcodeDataReceiver != null) unregisterReceiver(barcodeDataReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("PrixodActivity", "Ошибка при отписке barcodeDataReceiver: " + e.getMessage());
        }

        String moveUuid = getIntent().getStringExtra("moveUuid");
        if (moveUuid != null && !moveUuid.isEmpty() ) {
            // Сохраняем данные перед закрытием, если есть изменения
            if (productsViewModel != null) {
                boolean hasUnsavedChanges = Boolean.FALSE.equals(productsViewModel.isSyncedWith1C.getValue());
                if (hasUnsavedChanges) {
                    Log.d("PrixodActivity", "Сохраняем несинхронизированные данные для " + moveUuid + " при закрытии активности");
                } else {
                    Log.d("PrixodActivity", "Данные синхронизированы для " + moveUuid + ", файл сохраняется для повторного использования");
                }
            }
        }
        
        // Отменяем любые ожидающие запросы фокуса
        FocusManager.cancelPendingFocusRequests();
        

        if (pendingFocusRunnable != null) {
            focusHandler.removeCallbacks(pendingFocusRunnable);
            pendingFocusRunnable = null;
            }

        if (adapter != null) {
            adapter.cancelAllAnimations();
        }

        adapter = null;
        productsViewModel = null;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View currentFocusView = getCurrentFocus();
            
            // Если в данный момент выполняется анимация тряски, то касание игнорируем
            if (adapter != null && adapter.isAnimatingView()) {
                Log.d("PrixodActivity", "Игнорируем касание во время анимации");
                return super.dispatchTouchEvent(event);
            }

            if (isUserTouchingScreen) {
                Log.d("PrixodActivity", "isUserTouchingScreen = true, но все равно проверяем снятие фокуса");
            }
            
            // Если NavigationView открыт и касание вне его, закрываем и очищаем фокус
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                View menuView = findViewById(R.id.navigation_view);
                if (menuView != null && !isTouchOnView(event, menuView)) {
                    drawerLayout.closeDrawer(GravityCompat.START);

                    return true; 
                }
            } 
            
            // Проверяем, нужно ли снять фокус с EditText
            if (currentFocusView instanceof EditText) {

                View touchedView = findViewAtPosition(event.getRawX(), event.getRawY());
                

                boolean isTouchOnOtherEditText = 
                    touchedView != null && 
                    touchedView instanceof EditText && 
                    touchedView != currentFocusView;
                

                if (!isTouchOnView(event, currentFocusView) && !isTouchOnOtherEditText) {
                    Log.d("PrixodActivity", "Снимаем фокус с EditText при касании вне поля");
                    hideKeyboard(currentFocusView);
                    currentFocusView.clearFocus();
                    
                    // Запрашиваем фокус на корневой элемент
                    View rootView = findViewById(android.R.id.content);
                    if (rootView != null) rootView.requestFocus();
                    

                    isUserTouchingScreen = false;
                }
            }
            
            // Устанавливаем флаг касания
            isUserTouchingScreen = true;
            

            new Handler(Looper.getMainLooper()).postDelayed(() -> isUserTouchingScreen = false, 200);
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Находит View в указанной позиции экрана
     * @param x координата X касания
     * @param y координата Y касания
     * @return View по указанным координатам или null если не найдена
     */
    private View findViewAtPosition(float x, float y) {
        if (rv_info == null) return null;
        
        // Сначала проверяем, попадает ли точка в область RecyclerView
        int[] recyclerLocation = new int[2];
        rv_info.getLocationOnScreen(recyclerLocation);
        android.graphics.Rect recyclerRect = new android.graphics.Rect(
            recyclerLocation[0], 
            recyclerLocation[1], 
            recyclerLocation[0] + rv_info.getWidth(), 
            recyclerLocation[1] + rv_info.getHeight()
        );
        
        if (!recyclerRect.contains((int)x, (int)y)) {
            return null;
        }
        
        // Если точка в RecyclerView, ищем конкретный ViewHolder
        for (int i = 0; i < rv_info.getChildCount(); i++) {
            View child = rv_info.getChildAt(i);
            if (child == null) continue;
            
            int[] childLocation = new int[2];
            child.getLocationOnScreen(childLocation);
            android.graphics.Rect childRect = new android.graphics.Rect(
                childLocation[0],
                childLocation[1],
                childLocation[0] + child.getWidth(),
                childLocation[1] + child.getHeight()
            );
            
            if (childRect.contains((int)x, (int)y)) {
                // Нашли ViewHolder, теперь ищем EditText внутри него
                if (child instanceof ViewGroup) {
                    return findEditTextInViewGroup((ViewGroup)child, x, y);
                }
                return child;
            }
        }
        
        return null;
    }
    
    /**
     * Рекурсивно ищет EditText внутри ViewGroup по координатам
     * @param viewGroup группа для поиска
     * @param x координата X касания
     * @param y координата Y касания
     * @return найденный EditText или null
     */
    private View findEditTextInViewGroup(ViewGroup viewGroup, float x, float y) {
        if (viewGroup == null) return null;
        
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child == null) continue;
            
            int[] childLocation = new int[2];
            child.getLocationOnScreen(childLocation);
            android.graphics.Rect childRect = new android.graphics.Rect(
                childLocation[0],
                childLocation[1],
                childLocation[0] + child.getWidth(),
                childLocation[1] + child.getHeight()
            );
            
            if (childRect.contains((int)x, (int)y)) {
                if (child instanceof EditText) {
                    return child;
                } else if (child instanceof ViewGroup) {
                    View found = findEditTextInViewGroup((ViewGroup)child, x, y);
                    if (found != null) return found;
                }
            }
        }
        
        return null;
    }

    private void setupFloatingSortView() {
        if (floatingSortView == null) return;
        floatingSortView.setSortChangeListener(new FloatingSortView.SortChangeListener() {
            @Override
            public void onSortChanged(SortCriteria criteria) {
                productsViewModel.setSortCriteria(criteria);
            }
            @Override
            public void onClearSort() {
                productsViewModel.clearSort();
            }
        });
    }

    private void setupFilterListeners() {
        EditText filterNameEditText = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
        ImageButton clearNameButton = findViewById(R.id.clear_name);
        ImageButton clearAmountButton = findViewById(R.id.clear_amount);
        Button btnResetAllFiltersButton = findViewById(R.id.btn_reset_all_filters);

        if (filterNameEditText != null) {
            setupTextWatcher(filterNameEditText, newText -> productsViewModel.updateNameFilter(newText));
            setupEnterKeyListenerForFilters(filterNameEditText);
            if (clearNameButton != null) clearNameButton.setOnClickListener(v -> filterNameEditText.setText(""));
        }
        if (amountFilterEditText1 != null) {
            setupTextWatcher(amountFilterEditText1, newText -> applyAmountFilterFromUI());
            setupEnterKeyListenerForFilters(amountFilterEditText1);
        }
        if (amountFilterEditText2 != null) {
            setupTextWatcher(amountFilterEditText2, newText -> applyAmountFilterFromUI());
            setupEnterKeyListenerForFilters(amountFilterEditText2);
        }
        if (clearAmountButton != null && amountFilterEditText1 != null && amountFilterEditText2 != null) {
            clearAmountButton.setOnClickListener(v -> {
                amountFilterEditText1.setText("");
                amountFilterEditText2.setText(""); 
            });
        }
        if (filterUntouchedCheckBox != null) {
            filterUntouchedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> productsViewModel.updateUntouchedFilter(isChecked));
        }
        if (btnResetAllFiltersButton != null) {
            btnResetAllFiltersButton.setOnClickListener(v -> productsViewModel.resetAllFilters());
        }
    }
    
    /**
     * Инициализирует фиксированные кнопки внизу экрана
     */
    private void initializeFixedButtons() {
        fixedButtonsPanel = findViewById(R.id.fixed_buttons_panel);
        btnSendInfo = findViewById(R.id.btn_send_info);
        btnBackToMoveList = findViewById(R.id.btn_back_to_move_list);
        
        if (btnSendInfo != null) {
            btnSendInfo.setOnClickListener(v -> onSendDataClicked());
        }
        
        if (btnBackToMoveList != null) {
            btnBackToMoveList.setOnClickListener(v -> onGoBackClicked());
        }
        
        Log.d("PrixodActivity", "Фиксированные кнопки инициализированы");
    }
    
    /**
     * Управляет видимостью фиксированных кнопок в зависимости от статуса перемещения
     * @param moveStatus статус перемещения
     */
    private void updateFixedButtonsVisibility(String moveStatus) {
        if (btnSendInfo != null) {
            // Кнопка "Отпр. в 'Подготовленно'" видна только для статуса "Комплектуется"
            if ("Комплектуется".equals(moveStatus)) {
                btnSendInfo.setVisibility(View.VISIBLE);
                Log.d("PrixodActivity", "Кнопка 'Отпр. в \"Подготовленно\"' показана для статуса: " + moveStatus);
            } else {
                btnSendInfo.setVisibility(View.GONE);
                Log.d("PrixodActivity", "Кнопка 'Отпр. в \"Подготовленно\"' скрыта для статуса: " + moveStatus);
            }
        }
        
        // Кнопка "Вернуться в меню" всегда видна
        if (btnBackToMoveList != null) {
            btnBackToMoveList.setVisibility(View.VISIBLE);
        }
    }

    private void applyAmountFilterFromUI() {
        EditText amountFilterEditText1_correct = findViewById(R.id.amount_filter_editText1); 
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        if (amountFilterEditText1_correct == null || amountFilterEditText2 == null) return;
        String minAmountStr = amountFilterEditText1_correct.getText().toString();
        String maxAmountStr = amountFilterEditText2.getText().toString();
        Integer minAmount = null, maxAmount = null;
        try {
            if (!minAmountStr.isEmpty()) minAmount = Integer.parseInt(minAmountStr);
        } catch (NumberFormatException e) { Log.w("PrixodActivity", "Invalid min amount: " + minAmountStr); }
        try {
            if (!maxAmountStr.isEmpty()) maxAmount = Integer.parseInt(maxAmountStr);
        } catch (NumberFormatException e) { Log.w("PrixodActivity", "Invalid max amount: " + maxAmountStr); }
        productsViewModel.updateAmountFilter(minAmount, maxAmount);
    }

    private void setupTextWatcher(EditText editText, TextChangeListener listener) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { listener.onTextChanged(s.toString()); }
        });
    }

    private interface TextChangeListener { void onTextChanged(String newText); }



    private void setupNestedScrollViewTouchListener() {
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusView = getCurrentFocus();
                    if (currentFocusView instanceof EditText && isViewInsideViewGroup(sort_bar, currentFocusView) && !isTouchOnView(event, currentFocusView)) {
                        // Скрываем клавиатуру и очищаем фокус
                        hideKeyboard(currentFocusView);
                        currentFocusView.clearFocus();
                        
                        // Запрашиваем фокус на родительский ScrollView
                        scrollView.requestFocus();
                    }
                }
                return false; 
            });
        }
        
        // Добавляем обработчик касания для самой NavigationView
        if (sort_bar != null) {
            // Находим корневой контейнер в NavigationView
            ViewGroup rootContainer = null;
            for (int i = 0; i < sort_bar.getChildCount(); i++) {
                View child = sort_bar.getChildAt(i);
                if (child instanceof ViewGroup && !(child instanceof androidx.core.widget.NestedScrollView)) {
                    rootContainer = (ViewGroup) child;
                    break;
                }
            }
            
            if (rootContainer != null) {
                rootContainer.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        View currentFocusView = getCurrentFocus();
                        if (currentFocusView instanceof EditText && isViewInsideViewGroup(sort_bar, currentFocusView) && !isTouchOnView(event, currentFocusView)) {
                            // Скрываем клавиатуру и очищаем фокус
                            hideKeyboard(currentFocusView);
                            currentFocusView.clearFocus();
                            
                            // Запрашиваем фокус на NavigationView
                            sort_bar.requestFocus();
                        }
                    }
                    return false;
                });
            }
        }
    }

    private boolean isTouchOnView(MotionEvent event, View view) {
        if (view == null || !view.isAttachedToWindow()) return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        float touchX = event.getRawX();
        float touchY = event.getRawY();
        return (touchX >= viewLocation[0] && touchX <= viewLocation[0] + view.getWidth() &&
                touchY >= viewLocation[1] && touchY <= viewLocation[1] + view.getHeight());
    }


    private void setupEnterKeyListenerForFilters(EditText editText) {
        // Обработчик нажатия клавиши Enter
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                try {

                    hideKeyboard(v);
                    v.clearFocus();
                    
                    // Запрашиваем фокус на корневой элемент
                    View rootView = findViewById(android.R.id.content);
                    if (rootView != null) rootView.requestFocus();
                    
                    // Закрываем NavigationView, если он открыт
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e("PrixodActivity", "Ошибка при обработке Enter в фильтре: " + e.getMessage(), e);
                }
                return true;
            }
            return false;
        });
        

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                try {
                    // Скрываем клавиатуру
                    hideKeyboard(v);
                    v.clearFocus();
                    
                    // Запрашиваем фокус на корневой элемент
                    View rootView = findViewById(android.R.id.content);
                    if (rootView != null) rootView.requestFocus();
                    
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e("PrixodActivity", "Ошибка при обработке Editor Action в фильтре: " + e.getMessage(), e);
                }
                return true;
            }
            return false;
        });
    }
    
    private void observeViewModel() {
        productsViewModel.productsLiveData.observe(this, products -> {
            Set<String> currentErrors = productsViewModel.validationErrorUuidsLiveData.getValue();
            if (adapter == null) {
                adapter = new ProductsAdapter(ProductsActivity.this, products, this);
                
                // Устанавливаем статус перемещения в адаптер
                String currentMoveStatus = productsViewModel.getCurrentMoveStatus();
                if (currentMoveStatus == null) {
                    // Если статус еще не загружен в ViewModel, берем из Intent
                    currentMoveStatus = getIntent().getStringExtra("signingStatus");
                }
                adapter.setMoveStatus(currentMoveStatus);
                updateFixedButtonsVisibility(currentMoveStatus);
                Log.d("PrixodActivity", "Установлен статус перемещения в адаптер: " + currentMoveStatus);

                adapter.setItemSelectionListener(new ProductsAdapter.OnItemSelectionListener() {
                    @Override
                    public void onItemSelected(int position, Product product) {
                        handleItemSelection(position, product);
                    }

                    @Override
                    public void onItemDeselected() {
                        handleItemDeselection();
                    }
                });
                
                rv_info.setAdapter(adapter);
                if (rv_info.getItemDecorationCount() == 0) {
                    int spaceHeight = getResources().getDimensionPixelSize(R.dimen.item_space_height);
                    int sideSpace = getResources().getDimensionPixelSize(R.dimen.item_side_space);
                    rv_info.addItemDecoration(new ItemSpaceDecoration(spaceHeight, sideSpace));
                }
            }
            adapter.updateData(products, currentErrors); 
        });

        productsViewModel.isLoadingLiveData.observe(this, isLoading -> {
            if (isLoading) showLoadingDialog("Загрузка данных...");
            else dismissLoadingDialog();
        });

        productsViewModel.errorLiveData.observe(this, event -> {
            String errorMessage = event.getContentIfNotHandled();
            if (errorMessage != null) Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });

        productsViewModel.sortCriteriaLiveData.observe(this, criteria -> {
            if (floatingSortView != null) {
                Boolean isAscending = productsViewModel.isSortAscendingLiveData.getValue();
                floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
            }
        });

        productsViewModel.isSortAscendingLiveData.observe(this, isAscending -> {
            if (floatingSortView != null) {
                SortCriteria criteria = productsViewModel.sortCriteriaLiveData.getValue();
                if (criteria != null) floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
            }
        });

        // Наблюдатель для обычного фокуса
        productsViewModel.focusProductPositionLiveData.observe(this, event -> {
            Integer positionWrapper = event.getContentIfNotHandled();
            if (positionWrapper != null) {
                final int positionToFocus = positionWrapper;
                Log.d("PrixodActivity", "Получен запрос на фокус позиции: " + positionToFocus);

                // Проверяем валидность позиции
                if (positionToFocus != -1 && rv_info != null && adapter != null && positionToFocus < adapter.getItemCount()) {
                    // Отменяем предыдущий запрос фокуса, если он был
                    if (pendingFocusRunnable != null) {
                        focusHandler.removeCallbacks(pendingFocusRunnable);
                        pendingFocusRunnable = null;
                    }
                    
                    // Отменяем любые текущие анимации в адаптере
                    adapter.cancelAllAnimations();
                    
                    // Отменяем ожидающие запросы фокуса в FocusManager
                    FocusManager.cancelPendingFocusRequests();
                    
                    // Добавляем флаг для предотвращения обработки события dispatchTouchEvent
                    ProductsActivity.isUserTouchingScreen = true;
                    
                    // Обработчик для установки фокуса
                    pendingFocusRunnable = () -> {
                        try {
                            if (rv_info != null && adapter != null && positionToFocus >= 0) {

                                RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(positionToFocus);
                                if (viewHolder instanceof ProductsAdapter.InputinfoViewHolder) {
                                    EditText editText = ((ProductsAdapter.InputinfoViewHolder) viewHolder).list_carried;
                                    

                                    View parentContainer = findParentItemView(editText);
                                    

                                    View viewToScroll = (parentContainer != null) ? parentContainer : editText;
                                    

                                    scrollToViewIfNeeded(viewToScroll, rv_info);
                                    Log.d("PrixodActivity", "Прокрутка к элементу для позиции " + positionToFocus);
                                    

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (editText != null && editText.isAttachedToWindow()) {
                                            // Проверяем режим "только чтение"
                                            boolean isReadOnly = productsViewModel.isCurrentlyReadOnly();
                                            
                                            if (isReadOnly) {
                                                // В режиме "только чтение" только прокручиваем к элементу без фокуса
                                                Log.d("PrixodActivity", "Режим 'только чтение': только прокрутка к позиции " + positionToFocus + 
                                                      " без установки фокуса");
                                                // Сбрасываем флаг сразу, так как фокус не устанавливаем
                                                isUserTouchingScreen = false;
                                            } else {
                                                // В обычном режиме устанавливаем фокус как обычно

                                                isUserTouchingScreen = true;
                                                
                                                // Запрашиваем фокус и устанавливаем курсор в конец
                                                editText.requestFocus();
                                                editText.setSelection(editText.getText().length());
                                                

                                                Runnable resetTouchingFlagRunnable = new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (adapter != null && adapter.isAnimatingView()) {

                                                            new Handler(Looper.getMainLooper()).postDelayed(this, 100);
                                                        } else {

                                                            isUserTouchingScreen = false;
                                                            Log.d("PrixodActivity", "Флаг isUserTouchingScreen сброшен после установки фокуса");
                                                        }
                                                    }
                                                };
                                                

                                                new Handler(Looper.getMainLooper()).postDelayed(resetTouchingFlagRunnable, 300);

                                                Log.d("PrixodActivity", "Фокус установлен на EditText в позиции " + positionToFocus);
                                            }
                                        } else {

                                            isUserTouchingScreen = false;
                                            Log.d("PrixodActivity", "EditText недоступен, флаг isUserTouchingScreen сброшен");
                                        }
                                    }, 150);
                                } else {
                                    Log.w("PrixodActivity", "ViewHolder не найден или не является InputinfoViewHolder");
                                    
                                    // Прокручиваем к позиции без анимации, чтобы элемент стал видимым на экране
                                    rv_info.scrollToPosition(positionToFocus);
                                    

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                        isUserTouchingScreen = false;

                                        if (pendingFocusRunnable != null) {
                                            focusHandler.post(pendingFocusRunnable);
                                        }
                                    }, 100);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("PrixodActivity", "Ошибка при установке фокуса: " + e.getMessage());

                            isUserTouchingScreen = false;
                        }
                    };
                    

                    focusHandler.postDelayed(pendingFocusRunnable, 50);
                } else {
                    Log.w("PrixodActivity", "Невалидная позиция для фокуса: " + positionToFocus);
                }
            }
        });

        productsViewModel.productNotFoundForFocusEvent.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) showProductNotFoundDialog(message);
        });

        productsViewModel.nameFilterLiveData.observe(this, name -> {
            EditText filterNameEditText = findViewById(R.id.filter_name);
            if (filterNameEditText != null && !filterNameEditText.getText().toString().equals(name)) filterNameEditText.setText(name);
        });

        productsViewModel.minAmountFilterLiveData.observe(this, min -> {
            EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
            if (amountFilterEditText1 != null) {
                String currentText = amountFilterEditText1.getText().toString();
                String newText = (min == null || min == 0) ? "" : String.valueOf(min);
                if (!currentText.equals(newText)) amountFilterEditText1.setText(newText);
            }
        });

        productsViewModel.maxAmountFilterLiveData.observe(this, max -> {
            EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
            if (amountFilterEditText2 != null) {
                String currentText = amountFilterEditText2.getText().toString();
                String newText = (max == null || max == Integer.MAX_VALUE) ? "" : String.valueOf(max);
                 if (!currentText.equals(newText)) amountFilterEditText2.setText(newText);
            }
        });

        productsViewModel.untouchedFilterLiveData.observe(this, active -> {
            CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
            if (filterUntouchedCheckBox != null && filterUntouchedCheckBox.isChecked() != active) filterUntouchedCheckBox.setChecked(active);
        });

        productsViewModel.isAnyFilterActiveLiveData.observe(this, isActive -> {
            if (filterIndicator != null) filterIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
        });

        productsViewModel.validationErrorUuidsLiveData.observe(this, errorUuids -> {
            if (adapter != null) {

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {

                adapter.updateData(productsViewModel.productsLiveData.getValue(), errorUuids);
                    } catch (Exception e) {
                        Log.e("PrixodActivity", "Ошибка при обновлении данных адаптера: " + e.getMessage());
                    }
                });
            }
            if (errorUuids != null && !errorUuids.isEmpty()) Log.w("PrixodActivity", "Ошибки валидации для UUIDs: " + errorUuids.toString());
            else Log.d("PrixodActivity", "Ошибок валидации нет.");
        });

        productsViewModel.forceResetFiltersMessageLiveData.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        
        // Наблюдатель для анимации тряски поля ввода
        productsViewModel.shakeViewEvent.observe(this, event -> {
            String productLineId = event.getContentIfNotHandled();
            if (productLineId != null && adapter != null && rv_info != null) {

                if (pendingFocusRunnable != null) {
                    focusHandler.removeCallbacks(pendingFocusRunnable);
                    pendingFocusRunnable = null;
                }
                
                // Отменяем любые текущие анимации в адаптере
                adapter.cancelAllAnimations();
                

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // Поиск позиции в адаптере
                        List<Product> products = productsViewModel.productsLiveData.getValue();
                        if (products != null) {
                            // Находим позицию продукта
                            int foundPosition = -1;
                            for (int i = 0; i < products.size(); i++) {
                                Product product = products.get(i);
                                if (product != null && java.util.Objects.equals(productLineId, product.getProductLineId())) {
                                    foundPosition = i;
                                    break;
                                }
                            }
                            
                            // Если нашли позицию
                            if (foundPosition != -1) {
                                final int finalFoundPosition = foundPosition;
                                Log.d("PrixodActivity", "shakeViewEvent: найден продукт с productLineId " + 
                                      productLineId + " на позиции " + finalFoundPosition);
                                      

                                rv_info.scrollToPosition(finalFoundPosition);
                                

                                isUserTouchingScreen = true;
                                

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {

                                        RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(finalFoundPosition);
                                        if (viewHolder instanceof ProductsAdapter.InputinfoViewHolder) {
                                            EditText editText = ((ProductsAdapter.InputinfoViewHolder) viewHolder).list_carried;
                                            

                                            View parentContainer = findParentItemView(editText);
                                            

                                            View viewToScroll = (parentContainer != null) ? parentContainer : editText;
                                            

                                            scrollToViewIfNeeded(viewToScroll, rv_info);
                                            

                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                if (editText != null && editText.isAttachedToWindow()) {

                                                    editText.requestFocus();
                                                    editText.setSelection(editText.getText().length());
                                                    

                                                    adapter.shakeView(editText, true);
                                                    
                                                    Log.d("PrixodActivity", "Выполнена анимация тряски и установлен фокус для позиции " + finalFoundPosition);
                                                    

                                                    Runnable resetTouchingFlagAfterShake = new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (adapter != null && adapter.isAnimatingView()) {

                                                                new Handler(Looper.getMainLooper()).postDelayed(this, 100);
                                                            } else {

                                                                isUserTouchingScreen = false;
                                                                Log.d("PrixodActivity", "Флаг isUserTouchingScreen сброшен после анимации тряски");
                                                            }
                                                        }
                                                    };
                                                    

                                                    new Handler(Looper.getMainLooper()).postDelayed(resetTouchingFlagAfterShake, 500);
                                                } else {

                                                    isUserTouchingScreen = false;
                                                    Log.d("PrixodActivity", "EditText недоступен при анимации, флаг isUserTouchingScreen сброшен");
                                                }
                                            }, 200);
                                        } else {
                                            Log.w("PrixodActivity", "ViewHolder не найден или не является InputinfoViewHolder");
                                            isUserTouchingScreen = false;
                                        }
                                    } catch (Exception e) {
                                        Log.e("PrixodActivity", "Ошибка при анимации тряски: " + e.getMessage());
                                        isUserTouchingScreen = false;
                                    }
                                }, 100);
                            } else {
                                Log.w("PrixodActivity", "Не удалось найти продукт с productLineId " + productLineId);
                                isUserTouchingScreen = false;
                            }
                        } else {
                            Log.w("PrixodActivity", "Список продуктов пуст или null");
                            isUserTouchingScreen = false;
                        }
                    } catch (Exception e) {
                        Log.e("PrixodActivity", "Ошибка при обработке shakeViewEvent: " + e.getMessage());
                        isUserTouchingScreen = false;
                    }
                }, 50);
            }
        });
        
        // Наблюдатель для фокуса с автоматическим выбором (после сканирования QR-кода)
        productsViewModel.focusAndSelectProductPositionLiveData.observe(this, event -> {
            Integer positionWrapper = event.getContentIfNotHandled();
            if (positionWrapper != null) {
                final int positionToFocus = positionWrapper;
                Log.d("PrixodActivity", "Получен запрос на фокус с выбором позиции: " + positionToFocus);

                // Проверяем валидность позиции
                if (positionToFocus != -1 && rv_info != null && adapter != null && positionToFocus < adapter.getItemCount()) {
                    // Отменяем предыдущий запрос фокуса, если он был
                    if (pendingFocusRunnable != null) {
                        focusHandler.removeCallbacks(pendingFocusRunnable);
                        pendingFocusRunnable = null;
                    }
                    

                    adapter.cancelAllAnimations();
                    

                    FocusManager.cancelPendingFocusRequests();
                    

                    ProductsActivity.isUserTouchingScreen = true;
                    

                    pendingFocusRunnable = () -> {
                        try {
                            if (rv_info != null && adapter != null && positionToFocus >= 0) {

                                RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(positionToFocus);
                                if (viewHolder instanceof ProductsAdapter.InputinfoViewHolder) {
                                    EditText editText = ((ProductsAdapter.InputinfoViewHolder) viewHolder).list_carried;
                                    

                                    View parentContainer = findParentItemView(editText);
                                    

                                    View viewToScroll = (parentContainer != null) ? parentContainer : editText;
                                    

                                    scrollToViewIfNeeded(viewToScroll, rv_info);
                                    Log.d("PrixodActivity", "Прокрутка к элементу для позиции " + positionToFocus);
                                    

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (editText != null && editText.isAttachedToWindow()) {

                                            isUserTouchingScreen = true;
                                            

                                            editText.requestFocus();
                                            editText.setSelection(editText.getText().length());
                                            
                                            // Автоматически выбираем элемент для режима "Подбор Серии"
                                            List<Product> currentProducts = productsViewModel.productsLiveData.getValue();
                                            if (currentProducts != null && positionToFocus < currentProducts.size()) {
                                                Product selectedProduct = currentProducts.get(positionToFocus);
                                                if (selectedProduct != null && adapter != null) {

                                                    adapter.selectItem(positionToFocus);
                                                    
                                                    // Включаем режим выбора
                                                    if (!isSelectionMode) {
                                                        setSelectionMode(true);
                                                    }
                                                    
                                                    Log.d("PrixodActivity", "Автоматически выбран элемент после сканирования QR-кода: " + 
                                                          selectedProduct.getNomenclatureName());
                                                }
                                            }
                                            

                                            Runnable resetTouchingFlagRunnable = new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (adapter != null && adapter.isAnimatingView()) {

                                                        new Handler(Looper.getMainLooper()).postDelayed(this, 100);
                                                    } else {

                                                        isUserTouchingScreen = false;
                                                        Log.d("PrixodActivity", "Флаг isUserTouchingScreen сброшен после установки фокуса с выбором");
                                                    }
                                                }
                                            };
                                            

                                            new Handler(Looper.getMainLooper()).postDelayed(resetTouchingFlagRunnable, 300);

                                            Log.d("PrixodActivity", "Фокус установлен и элемент выбран в позиции " + positionToFocus);
                                        } else {

                                            isUserTouchingScreen = false;
                                            Log.d("PrixodActivity", "EditText недоступен, флаг isUserTouchingScreen сброшен");
                                        }
                                    }, 150);
                                } else {
                                    Log.w("PrixodActivity", "ViewHolder не найден или не является InputinfoViewHolder");
                                    
                                    // Прокручиваем к позиции без анимации, чтобы элемент стал видимым на экране
                                    rv_info.scrollToPosition(positionToFocus);
                                    

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                        isUserTouchingScreen = false;
                                        

                                        if (pendingFocusRunnable != null) {
                                            focusHandler.post(pendingFocusRunnable);
                                        }
                                    }, 100);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("PrixodActivity", "Ошибка при установке фокуса с выбором: " + e.getMessage());

                            isUserTouchingScreen = false;
                        }
                    };
                    

                    focusHandler.postDelayed(pendingFocusRunnable, 50);
                } else {
                    Log.w("PrixodActivity", "Невалидная позиция для фокуса с выбором: " + positionToFocus);
                }
            }
        });

        // Наблюдатель для сообщений об ошибках валидации
        productsViewModel.validationErrorMessageEvent.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("PrixodActivity", "Ошибка при показе Toast сообщения: " + e.getMessage());
                    }
                });
            }
        });
        
        // Наблюдатель для события возврата в меню с сохранением данных (обычный возврат)
        productsViewModel.returnToMenuWithDataEvent.observe(this, event -> {
            String productsJson = event.getContentIfNotHandled();
            if (productsJson != null) {
                returnToMenuWithSavedData(productsJson);
            }
        });
        
        // Наблюдатель для события возврата в меню с изменением статуса на "Подготовлен"
        productsViewModel.returnToMenuWithStatusChangeEvent.observe(this, event -> {
            String productsJson = event.getContentIfNotHandled();
            if (productsJson != null) {
                returnToMenuWithStatusChange(productsJson);
            }
        });
        
        // Наблюдатель для показа диалога о сохраненных изменениях при выходе
        productsViewModel.showDataSavedDialogEvent.observe(this, event -> {
            Boolean showDialog = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(showDialog)) {
                showDataSavedDialog();
            }
        });
        
        // Наблюдатель для уведомления об успешном использовании данных
        productsViewModel.dataSuccessfullyLoadedEvent.observe(this, event -> {
            Boolean dataLoaded = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(dataLoaded)) {

                Intent notificationIntent = new Intent();
                notificationIntent.putExtra("clearReturnData", true);
                setResult(RESULT_FIRST_USER, notificationIntent);
                Log.d("PrixodActivity", "Отправлено уведомление об успешном использовании данных");
            }
        });
        
        // Наблюдатель для режима "только чтение"
        productsViewModel.isReadOnlyMode.observe(this, isReadOnly -> {
            if (Boolean.TRUE.equals(isReadOnly)) {
                Log.d("PrixodActivity", "Режим 'только чтение' ВКЛЮЧЕН");

                new Handler(Looper.getMainLooper()).post(() -> {
                    setReadOnlyMode(true);
                    

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d("PrixodActivity", "Повторная установка режима 'только чтение' с задержкой");
                        setReadOnlyMode(true);
                    }, 500);
                });
            } else {
                Log.d("PrixodActivity", "Режим 'только чтение' ВЫКЛЮЧЕН");
                new Handler(Looper.getMainLooper()).post(() -> {
                    setReadOnlyMode(false);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d("PrixodActivity", "Повторная установка режима 'НЕ только чтение' с задержкой");
                        setReadOnlyMode(false);
                    }, 500);
                });
            }
        });



        // Наблюдатель для диалога множественного выбора товаров
        productsViewModel.multipleProductsFoundEvent.observe(this, event -> {
            ProductsViewModel.MultipleProductsData data = event.getContentIfNotHandled();
            if (data != null) {
                showMultipleProductsFoundDialog(data);
            }
        });
        
        // Наблюдатель для диалога подтверждения статистики перед отправкой в "Подготовлено"
        productsViewModel.showStatisticsConfirmationEvent.observe(this, event -> {
            ProductsViewModel.ProductsStatistics statistics = event.getContentIfNotHandled();
            if (statistics != null) {
                showStatisticsConfirmationDialog(statistics);
            }
        });
        
        // Добавляем наблюдатель для принудительного обновления адаптера после отметки всех товаров
        productsViewModel.productsMarkedAsCompletedEvent.observe(this, event -> {
            Boolean success = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(success) && adapter != null) {
                Log.d("PrixodActivity", "Принудительное обновление адаптера после отметки товаров как скомплектованные");
                adapter.notifyDataSetChanged();
            }
        });

        // Наблюдатель для прокрутки к позиции без установки фокуса
        productsViewModel.scrollToPositionEvent.observe(this, event -> {
            Integer position = event.getContentIfNotHandled();
            if (position != null && rv_info != null && adapter != null && position < adapter.getItemCount() - 1) {
                Log.d("PrixodActivity", "Получен запрос на прокрутку к позиции: " + position);
                
                // Прокручиваем к позиции без анимации сначала
                rv_info.scrollToPosition(position);
                

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (rv_info != null && position < adapter.getItemCount() - 1) {

                        RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(position);
                        if (viewHolder instanceof ProductsAdapter.InputinfoViewHolder) {

                            View containerView = viewHolder.itemView;
                            scrollToViewIfNeeded(containerView, rv_info);
                            Log.d("PrixodActivity", "Выполнена прокрутка к контейнеру на позиции " + position);
                        } else {
                            rv_info.smoothScrollToPosition(position);
                            Log.d("PrixodActivity", "ViewHolder не найден, выполнена плавная прокрутка к позиции " + position);
                        }
                    }
                }, 150);
            }
        });
        
        // Наблюдатель за статусом перемещения для обновления видимости кнопок в адаптере
        productsViewModel.moveItemLiveData.observe(this, moveItem -> {
            if (adapter != null && moveItem != null) {
                String moveStatus = moveItem.getSigningStatus();
                adapter.setMoveStatus(moveStatus);
                updateFixedButtonsVisibility(moveStatus);
                Log.d("PrixodActivity", "Обновлен статус перемещения в адаптере: " + moveStatus);
            }
        });
    }

    private com.step.tcd_rpkb.utils.LoadingDialog loadingDialog;
    public void showLoadingDialog(String message) {
        if (isFinishing() || isDestroyed()) return;
        if (loadingDialog == null) loadingDialog = new com.step.tcd_rpkb.utils.LoadingDialog(this);
        if (!loadingDialog.isShowing()) loadingDialog.show(message);
    }
    public void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
             try {
            loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e("PrixodActivity", "Error dismissing loading dialog: " + e.getMessage());
            }
        }
    }

    /**
     * Очищает фокус только с EditText, не находящихся в RecyclerView 
     */
    private void clearFocusFromNonRecyclerViewEditTexts() {
        View currentFocusView = getCurrentFocus();
        if (currentFocusView instanceof EditText && !isViewInsideViewGroup(rv_info, currentFocusView)) {

            currentFocusView.clearFocus();
        }
        

        EditText filterName = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        
        if (amountFilterEditText1 != null && amountFilterEditText1.hasFocus()) {
            amountFilterEditText1.clearFocus();
        }
        if (amountFilterEditText2 != null && amountFilterEditText2.hasFocus()) {
            amountFilterEditText2.clearFocus();
        }
        if (filterName != null && filterName.hasFocus()) {
            filterName.clearFocus();
        }
        

        View rootView = getWindow().getDecorView().getRootView();
        if (rootView != null) {
            rootView.requestFocus();
        }
    }

    private void animateMenuButton(View view) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleXPulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 0.8f, 1.1f, 1f);
        ObjectAnimator scaleYPulse = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 0.8f, 1.1f, 1f);
        ObjectAnimator rotationY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 15f, -10f, 0f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f); // Изменил имя переменной, чтобы не конфликтовать
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            float originalElevation = view.getElevation();
            ObjectAnimator elevation = ObjectAnimator.ofFloat(view, "elevation", originalElevation, originalElevation + 15f, originalElevation);
            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alphaAnim, elevation);
            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    if (view.getWidth() == 0 || view.getHeight() == 0) return; // Защита от крэша, если размеры еще не определены
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            view.setClipToOutline(true);
        } else {
            animatorSet.playTogether(scaleXPulse, scaleYPulse, rotationY, translateY, alphaAnim);
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
            }
        });
        animatorSet.start();
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
             if (view.getWidth() == 0 || view.getHeight() == 0) return; // Защита от крэша
            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;
            int finalRadius = Math.max(view.getWidth(), view.getHeight());
            try {
                android.animation.Animator rippleAnim = android.view.ViewAnimationUtils.createCircularReveal(view, centerX, centerY, 0, finalRadius);
            rippleAnim.setDuration(400);
            rippleAnim.start();
            } catch (IllegalStateException e) {
                 Log.e("PrixodActivity", "Error creating circular reveal animation: " + e.getMessage());
    }
        }
    }

    private void initializeBarcodeDataReceiver() {
        barcodeDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("PrixodActivity", "Получен Broadcast от сканера: " + action);
                String barcodeData = null;
                if ("com.example.scannerapp.ACTION_BARCODE_DATA".equals(action)) {
                    barcodeData = intent.getStringExtra("data");
                } else if ("android.intent.action.SCAN".equals(action) || "com.google.zxing.client.android.SCAN".equals(action)) {
                    barcodeData = intent.getStringExtra("SCAN_RESULT");
                } else if ("com.symbol.datawedge.api.ACTION".equals(action)) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        if (extras.containsKey("com.symbol.datawedge.data_string")) {
                            barcodeData = extras.getString("com.symbol.datawedge.data_string");
                        } else if (extras.containsKey("data")) {
                            barcodeData = extras.getString("data");
                        }
                    }
                } else if ("com.scanner.broadcast".equals(action)) {
                    barcodeData = intent.getStringExtra("data");
                } else if ("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST".equals(action)) {
                    // Обработка данных от ТСД Атол Smart.lite
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        if (extras.containsKey("EXTRA_BARCODE_DECODING_DATA")) {
                            barcodeData = extras.getString("EXTRA_BARCODE_DECODING_DATA");
                            Log.d("PrixodActivity", "Получены данные от Атол Smart.lite через EXTRA_BARCODE_DECODING_DATA");
                        } 

                        else if (extras.containsKey("barcode_data")) {
                            barcodeData = extras.getString("barcode_data");
                            Log.d("PrixodActivity", "Получены данные от Атол Smart.lite через barcode_data");
                        } else if (extras.containsKey("data")) {
                            barcodeData = extras.getString("data");
                            Log.d("PrixodActivity", "Получены данные от Атол Smart.lite через data");
                        }
                        
                        // Логируем
                        Log.d("PrixodActivity", "Все доступные ключи в broadcast от Атол Smart.lite:");
                        for (String key : extras.keySet()) {
                            Object value = extras.get(key);
                            Log.d("PrixodActivity", "  " + key + " = " + value);
                        }
                    }
                }

                if (barcodeData != null && !barcodeData.isEmpty()) {
                    barcodeData = barcodeData.trim(); //
                    Log.i("PrixodActivity", "Успешно получены данные сканирования: " + barcodeData + " (источник: " + action + ")");
                    productsViewModel.processBarcodeData(barcodeData);
                } else {
                    Log.w("PrixodActivity", "Данные сканирования не получены или пусты для action: " + action);
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Log.d("PrixodActivity", "Доступные ключи в Intent:");
                        for (String key : extras.keySet()) {
                            Object value = extras.get(key);
                            Log.d("PrixodActivityScannerExtras", "Extra: " + key + " = " + value + " (тип: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                        }
                    } else {
                        Log.d("PrixodActivity", "Intent.getExtras() вернул null");
                    }
                }
            }
        };
    }

    public void onSendDataClicked() {

        resetFocusAndTouchFlag();
        
        hideKeyboard(getCurrentFocus());
        Set<String> validationErrors = productsViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Найдены ошибки ввода! Проверьте выделенные поля.", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            productsViewModel.requestFocusOnError(firstErrorUuid);
            return; 
        }
        

        productsViewModel.handleSendTo1C();
    }

    public void onGoBackClicked() {

        resetFocusAndTouchFlag();
        
        hideKeyboard(getCurrentFocus());
        Set<String> validationErrors = productsViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Перед выходом исправьте ошибки ввода или удалите неверные значения!", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            productsViewModel.requestFocusOnError(firstErrorUuid);
            return; 
        }
        

        productsViewModel.handleBackToMenu();
    }

    @Override
    public void onProductDataConfirmed(String productLineId, double newTakenValue, int positionInAdapter, boolean isValid, boolean byEnterKey) {
        Log.d("PrixodActivity", "Данные подтверждены для productLineId: " + productLineId + 
                ", новое значение: " + newTakenValue + 
                ", позиция: " + positionInAdapter + 
                ", валидно: " + isValid + 
                ", через Enter: " + byEnterKey);
                

        if (byEnterKey) {
            FocusManager.cancelPendingFocusRequests();
            

            if (adapter != null) {
                adapter.cancelAllAnimations();
            }
            

            if (pendingFocusRunnable != null) {
                focusHandler.removeCallbacks(pendingFocusRunnable);
                pendingFocusRunnable = null;
            }
        }
        

        productsViewModel.handleProductDataConfirmation(
            productLineId, 
            newTakenValue, 
            isValid, 
            byEnterKey, 
            positionInAdapter
        ); 
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private void hideKeyboard(View fromView) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        View viewToUse = fromView;
        if (viewToUse == null) {
            viewToUse = getCurrentFocus();
        }
        if (viewToUse == null) {
            viewToUse = getWindow().getDecorView().getRootView();
        }
        
        if (viewToUse != null && viewToUse.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(viewToUse.getWindowToken(), 0);
        }
    }

    public void scrollToViewIfNeeded(View viewToShow, RecyclerView recyclerView) {
        if (viewToShow == null || recyclerView == null || !viewToShow.isAttachedToWindow() || !recyclerView.isAttachedToWindow()) return;
        

        if (viewToShow instanceof EditText) {
            EditText editText = (EditText) viewToShow;
            int inputType = editText.getInputType();
            if ((inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0) {
                editText.setCursorVisible(true);
            }
        }
        
        try {
            int[] viewLocation = new int[2];
            viewToShow.getLocationInWindow(viewLocation);
            int viewTop = viewLocation[1];
            int viewBottom = viewTop + viewToShow.getHeight();
            
            android.graphics.Rect recyclerGlobalVisibleRect = new android.graphics.Rect();
            recyclerView.getGlobalVisibleRect(recyclerGlobalVisibleRect);
            android.graphics.Rect r = new android.graphics.Rect();
            View rootView = getWindow().getDecorView();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeightMetrics = getResources().getDisplayMetrics().heightPixels; 
            int visibleScreenHeight = r.bottom - r.top; 
            int keyboardHeight = screenHeightMetrics - visibleScreenHeight; 
            int visibleContainerTop = recyclerGlobalVisibleRect.top;
            int visibleContainerBottom = recyclerGlobalVisibleRect.bottom;
            if (keyboardHeight > dpToPx(50)) { // Клавиатура активна
                visibleContainerBottom = Math.min(recyclerGlobalVisibleRect.bottom, r.bottom - (int)dpToPx(10)); 
            }
            
            // Увеличиваем верхний и нижний отступы
            final int TOP_PADDING = (int) dpToPx(40);
            final int BOTTOM_PADDING = (int) dpToPx(80);
            

            View parentContainer = findParentItemView(viewToShow);
            if (parentContainer != null) {

                int[] containerLocation = new int[2];
                parentContainer.getLocationInWindow(containerLocation);
                int containerTop = containerLocation[1];
                int containerBottom = containerTop + parentContainer.getHeight();

                int scrollBy = 0;
                if (containerTop < visibleContainerTop + TOP_PADDING) {
                    scrollBy = containerTop - (visibleContainerTop + TOP_PADDING);
                } else if (containerBottom > visibleContainerBottom - BOTTOM_PADDING) {
                    scrollBy = containerBottom - (visibleContainerBottom - BOTTOM_PADDING);
                    scrollBy += dpToPx(60);
                }
                
                if (scrollBy != 0) {
                    final int finalScrollBy = scrollBy;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        recyclerView.smoothScrollBy(0, finalScrollBy);
                    }, 150);
                }
            } else {

                int scrollBy = 0;
                if (viewTop < visibleContainerTop + TOP_PADDING) {
                    scrollBy = viewTop - (visibleContainerTop + TOP_PADDING); 
                } else if (viewBottom > visibleContainerBottom - BOTTOM_PADDING) {
                    scrollBy = viewBottom - (visibleContainerBottom - BOTTOM_PADDING);

                    if (viewToShow instanceof EditText) {
                        scrollBy += dpToPx(60);
                    }
                }
                
                if (scrollBy != 0) {
                    final int finalScrollBy = scrollBy;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        recyclerView.smoothScrollBy(0, finalScrollBy);
                    }, 150);
                }
            }
        } catch (Exception e) {
            Log.e("PrixodActivity", "Error in scrollToViewIfNeeded: " + e.getMessage());
        }
        

        if (viewToShow instanceof EditText) {
            EditText editText = (EditText) viewToShow;
            int inputType = editText.getInputType();
            if ((inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0) {

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (editText.isAttachedToWindow() && editText.hasFocus()) {
                        editText.setCursorVisible(true);
                        if (!editText.getText().toString().isEmpty()) {
                            editText.setSelection(editText.getText().length());
                        }
                    }
                }, 200);
            }
        }
    }
    
    /**
     * Находит родительский ItemView для View внутри RecyclerView
     * @param view View, для которой ищем родительский ItemView
     * @return родительский ItemView или null, если не найден
     */
    private View findParentItemView(View view) {
        if (view == null) return null;
        
        ViewParent parent = view.getParent();
        while (parent != null && parent instanceof View) {
            View parentView = (View) parent;

            if (parentView.getId() == R.id.item_container) {
                return parentView;
            }

            try {
                int id = parentView.getId();
                if (id != View.NO_ID) {
                    String resourceName = getResources().getResourceEntryName(id);
                    if ("item_container".equals(resourceName)) {
                        return parentView;
                    }
                }
            } catch (Exception e) {
            }
            

            ViewParent grandParent = parentView.getParent();
            if (grandParent instanceof RecyclerView) {
                return parentView;
            }

            Object tag = parentView.getTag();
            if (tag instanceof String && "item_view_root".equals(tag)) {
                return parentView;
            }

            parent = parentView.getParent();
        }

        return null;
    }

    /**
     * Вспомогательный метод для гарантированного сброса флага isUserTouchingScreen 
     * и снятия фокуса с текущего View при необходимости
     */
    private void resetFocusAndTouchFlag() {
        // Сбрасываем флаг
        isUserTouchingScreen = false;
        

        View currentFocusView = getCurrentFocus();
        if (currentFocusView instanceof EditText && !isViewInsideViewGroup(rv_info, currentFocusView)) {
            currentFocusView.clearFocus();
            hideKeyboard(currentFocusView);
            Log.d("PrixodActivity", "resetFocusAndTouchFlag: фокус снят с EditText");
        }
        

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.requestFocus();
        }
        
        Log.d("PrixodActivity", "resetFocusAndTouchFlag: флаг isUserTouchingScreen сброшен");
    }

    /**
     * Рекурсивно сбрасывает фокус со всех EditText в ViewGroup
     */
    private void clearFocusRecursivelyFromViewGroup(ViewGroup group) {
        if (group == null) return;
        
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                if (editText.hasFocus()) {
                    editText.clearFocus();
                }
            } else if (child instanceof ViewGroup) {
                clearFocusRecursivelyFromViewGroup((ViewGroup) child);
            }
        }
    }

    /**
     * Сбрасывает фокус со всех EditText в NavigationView
     */
    private void clearAllFocusInNavigationEditTexts() {
        if (sort_bar == null) return;

        clearFocusRecursivelyFromViewGroup(sort_bar);

        View rootView = getWindow().getDecorView().getRootView();
        if (rootView != null) {
            rootView.requestFocus();
        }
    }
    
    /**
     * Добавляет блок с информацией о перемещении в NavigationView
     */
    private void addMoveInfoBlockToNavView() {
        if (sort_bar == null) return;
        

        LinearLayout navigationContentLayout = null;
        androidx.core.widget.NestedScrollView scrollView = sort_bar.findViewById(R.id.filter_scroll_view);
        
        if (scrollView != null && scrollView.getChildCount() > 0 && scrollView.getChildAt(0) instanceof LinearLayout) {
            navigationContentLayout = (LinearLayout) scrollView.getChildAt(0);
        }
        
        if (navigationContentLayout == null) {
            Log.e("PrixodActivity", "Не удалось найти контейнер для добавления информации о перемещении");
            return;
        }
        

        LayoutInflater inflater = LayoutInflater.from(this);
        moveInfoBlockView = inflater.inflate(R.layout.info_block_products_nav_view, navigationContentLayout, false);
        

        moveDisplayTextView = moveInfoBlockView.findViewById(R.id.tv_move_display_text);
        positionsCountView = moveInfoBlockView.findViewById(R.id.tv_positions_count);
        itemsCountView = moveInfoBlockView.findViewById(R.id.tv_items_count);

        progressBarFilled = moveInfoBlockView.findViewById(R.id.progress_bar_filled);
        progressRatioTextView = moveInfoBlockView.findViewById(R.id.tv_progress_ratio);
        

        navigationContentLayout.addView(moveInfoBlockView, 1);
        

        productsViewModel.currentInvoiceLiveData.observe(this, invoice -> {
            if (invoice != null) {
                updateMoveInfoBlock(invoice);
                updateProgressBar(invoice);
            }
        });
        

        productsViewModel.moveItemLiveData.observe(this, moveItem -> {
            Invoice invoice = productsViewModel.currentInvoiceLiveData.getValue();
            if (invoice != null) {
                updateMoveInfoBlock(invoice);
            }
        });

        productsViewModel.originalProductListUpdatedEvent.observe(this, event -> {
            Invoice invoice = productsViewModel.currentInvoiceLiveData.getValue();
            if (invoice != null) {
                updateProgressBar(invoice);
            }
        });
    }
    
    /**
     * Обновляет информацию о перемещении в блоке NavigationView
     */
    private void updateMoveInfoBlock(Invoice invoice) {
        if (moveDisplayTextView == null || positionsCountView == null || itemsCountView == null) {
            Log.e("PrixodActivity", "Компоненты для отображения информации о перемещении не инициализированы");
            return;
        }

        // Получаем дополнительную информацию из MoveItem
        MoveItem moveItem = productsViewModel.moveItemLiveData.getValue();
        
        // Логируем данные
        Log.d("PrixodActivity", "updateMoveInfoBlock: moveItem = " + (moveItem != null ? "доступен" : "null"));
        if (moveItem != null) {
            Log.d("PrixodActivity", "updateMoveInfoBlock: moveItem.getNumber() = " + moveItem.getNumber());
            Log.d("PrixodActivity", "updateMoveInfoBlock: moveItem.getDate() = " + moveItem.getDate());
        }
        Log.d("PrixodActivity", "updateMoveInfoBlock: prixodViewModel.getMoveNumber() = " + productsViewModel.getMoveNumber());
        Log.d("PrixodActivity", "updateMoveInfoBlock: prixodViewModel.getFormattedMoveDate() = " + productsViewModel.getFormattedMoveDate());
        
        // Формируем заголовок перемещения
        String displayText;
        
        if (moveItem != null) {

            // Формируем заголовок из номера и даты
            String moveNumber = !TextUtils.isEmpty(moveItem.getNumber()) ?
                moveItem.getNumber() : productsViewModel.getMoveNumber();

            String moveDate = productsViewModel.getFormattedMoveDate();

            if (!TextUtils.isEmpty(moveNumber) && !TextUtils.isEmpty(moveDate)) {
                displayText = "Перемещение " + moveNumber + " от " + moveDate;
            } else if (!TextUtils.isEmpty(moveNumber)) {
                displayText = "Перемещение " + moveNumber;
            } else {
                displayText = "Перемещение";
            }

        } else {

            String moveNumber = productsViewModel.getMoveNumber();
            String moveDate = productsViewModel.getFormattedMoveDate();
            
            if (!TextUtils.isEmpty(moveNumber) && !TextUtils.isEmpty(moveDate)) {
                displayText = "Перемещение " + moveNumber + " от " + moveDate;
            } else if (!TextUtils.isEmpty(moveNumber)) {
                displayText = "Перемещение " + moveNumber;
            } else {
                displayText = "Перемещение";
            }
        }
        
        // Логируем
        Log.d("PrixodActivity", "updateMoveInfoBlock: финальный displayText = '" + displayText + "'");
        
        // Применяем стиль к заголовку
        SpannableString spannableTitleText = new SpannableString(displayText);
        spannableTitleText.setSpan(
            new StyleSpan(Typeface.BOLD),
            0, displayText.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        moveDisplayTextView.setText(spannableTitleText);
        
        // Количество позиций
        int positionsCount = invoice.getProducts().size();
        
        // Формируем строку с количеством позиций, выделяя число
        String positionsText = "Позиций: " + positionsCount;
        SpannableString spannablePositionsText = new SpannableString(positionsText);
        int posValStartIndex = "Позиций: ".length();
        spannablePositionsText.setSpan(
            new StyleSpan(Typeface.BOLD),
            posValStartIndex, positionsText.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannablePositionsText.setSpan(
            new ForegroundColorSpan(Color.rgb(0, 120, 215)),
            posValStartIndex, positionsText.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        positionsCountView.setText(spannablePositionsText);
        
        // Количество товаров
        int totalItemsCount = 0;
        for (Product product : invoice.getProducts()) {
            totalItemsCount += product.getQuantity();
        }
        
        // Формируем строку с количеством товаров, выделяя число
        String itemsText = "Товаров: " + totalItemsCount;
        SpannableString spannableItemsText = new SpannableString(itemsText);
        int itemsValStartIndex = "Товаров: ".length();
        spannableItemsText.setSpan(
            new StyleSpan(Typeface.BOLD),
            itemsValStartIndex, itemsText.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannableItemsText.setSpan(
            new ForegroundColorSpan(Color.rgb(0, 120, 215)),
            itemsValStartIndex, itemsText.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        itemsCountView.setText(spannableItemsText);
    }
    
    /**
     * Обновляет прогресс-бар заполнения позиций
     * 
     * @param invoice Объект Invoice с полным списком продуктов
     */
    private void updateProgressBar(Invoice invoice) {
        if (progressBarFilled == null || progressRatioTextView == null) {
            Log.e("PrixodActivity", "Компоненты прогресс-бара не инициализированы");
            return;
        }
        
        // Получаем оригинальный список продуктов
        List<Product> originalProducts = productsViewModel.getOriginalProductList();
        if (originalProducts == null || originalProducts.isEmpty()) {

            originalProducts = invoice.getProducts();
            if (originalProducts == null || originalProducts.isEmpty()) {
                // Если и в Invoice продуктов нет, отображаем нулевой прогресс
                progressBarFilled.setProgress(0);
                progressRatioTextView.setText("0/0 (0%)");
                return;
            }
        }
        
        // Подсчитываем количество товаров
        int totalItems = 0;
        int takenItems = 0;
        
        for (Product product : originalProducts) {
            totalItems += product.getQuantity();
            takenItems += product.getTaken();
        }
        
        // Рассчитываем процент заполнения
        int progressPercentage = totalItems > 0 ? (takenItems * 100) / totalItems : 0;
        
        // Обновляем прогресс-бар
        progressBarFilled.setProgress(progressPercentage);
        
        // Формируем текст с процентом
        String progressText = String.format("%d/%d (%d%%)", takenItems, totalItems, progressPercentage);
        

        SpannableString spannableProgress = new SpannableString(progressText);
        

        spannableProgress.setSpan(
            new StyleSpan(Typeface.BOLD),
            0, progressText.indexOf(" ("),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        

        int openParenIndex = progressText.indexOf("(");
        int closeParenIndex = progressText.indexOf(")") + 1;
        if (openParenIndex > 0 && closeParenIndex > openParenIndex) {
            spannableProgress.setSpan(
                new ForegroundColorSpan(
                    progressPercentage < 30 ? Color.rgb(220, 53, 69) :   // Красный для <30
                    progressPercentage < 70 ? Color.rgb(255, 193, 7) :   // Желтый для 30-70
                    Color.rgb(40, 167, 69)                               // Зеленый для >70
                ),
                openParenIndex, closeParenIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        // Устанавливаем текст
        progressRatioTextView.setText(spannableProgress);
        

        int progressColor = 
            progressPercentage < 30 ? Color.rgb(220, 53, 69) :   // Красный для <30
            progressPercentage < 70 ? Color.rgb(255, 193, 7) :   // Желтый для 30-70
            Color.rgb(40, 167, 69);                              // Зеленый для >70
            
        // Применяем цвет к прогресс-бару
        progressBarFilled.getProgressDrawable().setColorFilter(
            progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    /**
     * Устанавливает режим выбора элемента и управляет видимостью и доступностью UI элементов
     * @param enabled true для включения режима выбора, false для выключения
     */
    private void setSelectionMode(boolean enabled) {
        if (isAnimatingSelectionPanel) {
            Log.d("PrixodActivity", "Анимация панели уже выполняется, игнорируем запрос на изменение режима");
            return;
        }

        if (isSelectionMode == enabled) {
            return;
        }
        
        Log.d("PrixodActivity", "Установка режима выбора: " + enabled);
        
        if (enabled) {
            isSelectionMode = true;
            
            // Блокируем навигацию
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            
            // Скрываем кнопку меню
            FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
            if (navMenuButton != null) {
                navMenuButton.setVisibility(View.GONE);
            }
            
            // Блокируем сортировку
            if (floatingSortView != null) {
                floatingSortView.setVisibility(View.GONE);
            }
            
            // Скрываем фиксированные кнопки
            if (fixedButtonsPanel != null) {
                fixedButtonsPanel.setVisibility(View.GONE);
            }
            
            // Показываем панель действий с анимацией
            showSelectionPanel(true);
        } else {

            if (adapter != null && adapter.getSelectedPosition() != RecyclerView.NO_POSITION) {
                adapter.clearSelectionSilently(); // Используем тихую очистку без вызова колбэка
            }
            isSelectionMode = false;

            hideSelectionPanelAndShowOtherElements();
        }
    }
    
    /**
     * Скрывает панель выбора и показывает остальные элементы UI (без анимации)
     */
    private void hideSelectionPanelAndShowOtherElements() {
        if (selectionActionPanel == null) return;
        
        // скрываем панель выбора
        selectionActionPanel.setVisibility(View.GONE);
        selectionActionPanel.setAlpha(0f);
        
        // показываем остальные элементы
        animateElementsAppearance();
    }
    
    /**
     * Показывает UI элементы после скрытия панели выбора (без анимации для избежания мигания)
     */
    private void animateElementsAppearance() {
        FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
        
        // показываем элементы без анимации
        if (navMenuButton != null) {
            navMenuButton.setAlpha(1f);
            navMenuButton.setVisibility(View.VISIBLE);
        }
        
        if (floatingSortView != null) {
            floatingSortView.setAlpha(1f);
            floatingSortView.setVisibility(View.VISIBLE);
        }
        
        if (fixedButtonsPanel != null) {
            fixedButtonsPanel.setAlpha(1f);
            fixedButtonsPanel.setVisibility(View.VISIBLE);
        }
        
        // Все операции завершены
        isAnimatingSelectionPanel = false;
        
        // Разблокируем навигацию
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        
        Log.d("PrixodActivity", "Выключение режима выбора завершено");
    }


    
    /**
     * Показывает панель действий выбора мгновенно (без анимации)
     * @param show true для показа
     */
    private void showSelectionPanel(boolean show) {
        if (selectionActionPanel == null || !show) return;

        selectionActionPanel.setAlpha(1f);
        selectionActionPanel.setVisibility(View.VISIBLE);
        
        Log.d("PrixodActivity", "Панель выбора показана мгновенно");
    }
    
    /**
     * Обрабатывает выбор элемента в адаптере
     * @param position позиция выбранного элемента
     * @param product выбранный продукт
     */
    private void handleItemSelection(int position, Product product) {
        if (!isSelectionMode) {
            setSelectionMode(true);
        }
        
        Log.d("PrixodActivity", "Выбран элемент на позиции " + position + 
                ": " + (product != null ? product.getNomenclatureName() : "null"));
    }
    
    /**
     * Обрабатывает отмену выбора элемента в адаптере
     */
    private void handleItemDeselection() {
        if (isSelectionMode) {
            Log.d("PrixodActivity", "Выбор элемента отменен, выключаем режим выбора");
            setSelectionMode(false);
        } else {
            Log.d("PrixodActivity", "Выбор элемента отменен, но режим выбора уже неактивен");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        

        resetSelectionPanelAnimation();
    }

    /**
     * Сбрасывает анимацию панели выбора и устанавливает правильное состояние видимости
     * в зависимости от текущего режима выбора
     */
    private void resetSelectionPanelAnimation() {
        if (selectionActionPanel == null) return;
        

        selectionActionPanel.animate().cancel();
        

        FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
        if (navMenuButton != null) {
            navMenuButton.animate().cancel();
        }
        
        if (floatingSortView != null) {
            floatingSortView.animate().cancel();
        }
        
        if (fixedButtonsPanel != null) {
            fixedButtonsPanel.animate().cancel();
        }
        
        // Сбрасываем флаг анимации
        isAnimatingSelectionPanel = false;

        if (isSelectionMode) {
            selectionActionPanel.setAlpha(1.0f);
            selectionActionPanel.setVisibility(View.VISIBLE);
            
            // Скрываем остальные элементы UI
            if (navMenuButton != null) {
                navMenuButton.setAlpha(1.0f);
                navMenuButton.setVisibility(View.GONE);
            }
            if (floatingSortView != null) {
                floatingSortView.setAlpha(1.0f);
                floatingSortView.setVisibility(View.GONE);
            }
            if (fixedButtonsPanel != null) {
                fixedButtonsPanel.setAlpha(1.0f);
                fixedButtonsPanel.setVisibility(View.GONE);
            }
        } else {
            selectionActionPanel.setVisibility(View.GONE);
            selectionActionPanel.setAlpha(0.0f); // Готовим для следующего появления
            
            // Показываем остальные элементы UI
            if (navMenuButton != null) {
                navMenuButton.setAlpha(1.0f);
                navMenuButton.setVisibility(View.VISIBLE);
            }
            if (floatingSortView != null) {
                floatingSortView.setAlpha(1.0f);
                floatingSortView.setVisibility(View.VISIBLE);
            }
            if (fixedButtonsPanel != null) {
                fixedButtonsPanel.setAlpha(1.0f);
                fixedButtonsPanel.setVisibility(View.VISIBLE);
            }
        }
        
        Log.d("PrixodActivity", "Сброшена анимация панели выбора, текущий режим: " + 
              (isSelectionMode ? "выбор активен" : "выбор неактивен"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SELECT_SERIES) {
            // Получаем номенклатуру из выбранного продукта для очистки временных файлов
            String nomenclatureUuidToCleanup = null;
            if (adapter != null) {
                Product selectedProduct = adapter.getSelectedProduct();
                if (selectedProduct != null) {
                    nomenclatureUuidToCleanup = selectedProduct.getNomenclatureUuid();
                }
            }
            


            if (adapter != null) {
                adapter.clearSelection();
            }
            
            // Обрабатываем результат замены серии
            if (resultCode == RESULT_OK && data != null) {
                boolean success = data.getBooleanExtra("success", false);
                String action = data.getStringExtra("action");
                
                if (success && "series_changed".equals(action)) {
                    // Получаем информацию о замене серии
                    int updatedProductsCount = data.getIntExtra("updatedProductsCount", 0);
                    int newProductsCount = data.getIntExtra("newProductsCount", 0);
                    String[] changedProductLineIds = data.getStringArrayExtra("changedProductLineIds");
                    
                    Log.d("PrixodActivity", "Получен результат замены серии. Обновлено: " + updatedProductsCount + 
                                          ", новых: " + newProductsCount + 
                                          ", изменено ID: " + (changedProductLineIds != null ? changedProductLineIds.length : 0));
                    
                    // Десериализуем списки продуктов из JSON
                    try {
                        String updatedProductsJson = data.getStringExtra("updatedProductsJson");
                        String newProductsJson = data.getStringExtra("newProductsJson");
                        
                        if (updatedProductsJson != null && newProductsJson != null) {
                            com.google.gson.Gson gson = new GsonBuilder()
                                    .disableHtmlEscaping()
                                    .create();
                            
                            // Десериализуем обновленные продукты
                            java.lang.reflect.Type updatedProductsType = new com.google.gson.reflect.TypeToken<java.util.List<com.step.tcd_rpkb.domain.model.Product>>(){}.getType();
                            java.util.List<com.step.tcd_rpkb.domain.model.Product> updatedProducts = gson.fromJson(updatedProductsJson, updatedProductsType);
                            
                            // Десериализуем новые продукты
                            java.lang.reflect.Type newProductsType = new com.google.gson.reflect.TypeToken<java.util.List<com.step.tcd_rpkb.domain.model.Product>>(){}.getType();
                            java.util.List<com.step.tcd_rpkb.domain.model.Product> newProducts = gson.fromJson(newProductsJson, newProductsType);
                            
                            // Применяем изменения к ProductsViewModel
                            boolean applied = productsViewModel.applySeriesChangeResult(
                                updatedProducts != null ? updatedProducts : new java.util.ArrayList<>(),
                                newProducts != null ? newProducts : new java.util.ArrayList<>(),
                                changedProductLineIds != null ? java.util.Arrays.asList(changedProductLineIds) : new java.util.ArrayList<>()
                            );
                            
                            if (applied) {
                                Toast.makeText(this, "Замена серии выполнена успешно. Обновлено: " + updatedProductsCount + 
                                                     ", создано новых: " + newProductsCount, Toast.LENGTH_LONG).show();
                                Log.d("PrixodActivity", "Изменения замены серии успешно применены к ProductsViewModel");
                                

                                if (adapter != null) {
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        List<Product> currentProducts = productsViewModel.productsLiveData.getValue();
                                        Set<String> currentErrors = productsViewModel.validationErrorUuidsLiveData.getValue();
                                        if (currentProducts != null) {
                                            Log.d("PrixodActivity", "Принудительное обновление адаптера после замены серий");
                                            adapter.updateData(currentProducts, currentErrors, true); // Принудительное обновление
                                        }
                                    }, 100);
                                }
                            } else {
                                Toast.makeText(this, "Ошибка применения изменений замены серии", Toast.LENGTH_SHORT).show();
                                Log.e("PrixodActivity", "Не удалось применить изменения замены серии к ProductsViewModel");
                            }
                        } else {
                            Log.w("PrixodActivity", "JSON данные продуктов отсутствуют, обновляем данные из кеша");
                            // Если JSON данные отсутствуют, обновляем данные из кеша
                            productsViewModel.refreshProductsFromCache();
                            Toast.makeText(this, "Данные обновлены из кеша", Toast.LENGTH_SHORT).show();
                        }

                        productsViewModel.refreshProductsFromCache();
                        

                        if (adapter != null) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                List<Product> currentProducts = productsViewModel.productsLiveData.getValue();
                                Set<String> currentErrors = productsViewModel.validationErrorUuidsLiveData.getValue();
                                if (currentProducts != null) {
                                    Log.d("PrixodActivity", "Дополнительное принудительное обновление адаптера после refreshProductsFromCache");
                                    adapter.updateData(currentProducts, currentErrors, true); // Принудительное обновление
                                }
                            }, 200);
                        }
                    } catch (Exception e) {
                        Log.e("PrixodActivity", "Ошибка десериализации результата замены серии: " + e.getMessage(), e);

                        productsViewModel.refreshProductsFromCache();
                        Toast.makeText(this, "Замена серии выполнена, данные обновлены из кеша", Toast.LENGTH_SHORT).show();
                    }
                    
                } else if (success) {

                    productsViewModel.refreshProductsFromCache();
                    Toast.makeText(this, "Операция успешно выполнена", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.d("PrixodActivity", "Операция выбора серий отменена пользователем");
            }
        }
    }

    // метод для расчета общего количества в документе для номенклатуры
    private double calculateTotalQuantityForNomenclature(String nomenclatureUuid) {
        if (nomenclatureUuid == null || productsViewModel == null) {
            return 0.0;
        }
        
        // Получаем оригинальный список продуктов
        List<Product> allProducts = productsViewModel.getOriginalProductList();
        if (allProducts == null || allProducts.isEmpty()) {
            return 0.0;
        }
        
        // Суммируем quantity всех продуктов с таким же nomenclatureUuid
        double totalQuantity = 0.0;
        for (Product product : allProducts) {
            if (product != null && nomenclatureUuid.equals(product.getNomenclatureUuid())) {
                totalQuantity += product.getQuantity();
            }
        }
        
        Log.d("PrixodActivity", "Общее количество в документе для номенклатуры " + nomenclatureUuid + ": " + totalQuantity);
        return totalQuantity;
    }

    
    /**
     * Возвращается в меню с сохранением данных
     * @param productsJson JSON с данными для сохранения
     */
    private void returnToMenuWithSavedData(String productsJson) {
        showLoadingDialog("Возврат к списку...");
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dismissLoadingDialog();
            Intent intent = new Intent();
            
            // Получаем moveUuid из текущего Intent
            String moveUuid = getIntent().getStringExtra("moveUuid");

            if (moveUuid != null) {
                intent.putExtra("moveUuid", moveUuid);
                Log.d("PrixodActivity", "Передаем moveUuid в результат: " + moveUuid);
            }

            intent.putExtra("changeStatusToPodgotovlen", false);
            intent.putExtra("restoreFilters", true);
            intent.putExtra("preserveEditedData", false);
            
            Log.d("PrixodActivity", "Возвращаем RESULT_OK БЕЗ смены статуса (обычный возврат)");
            
            setResult(ProductsActivity.RESULT_OK, intent);
            
            finish();
        }, 200);
    }
    
    /**
     * Возвращается в меню с изменением статуса на "Подготовлен"
     */
    private void returnToMenuWithStatusChange(String productsJson) {
        showLoadingDialog("Смена статуса...");
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dismissLoadingDialog();
            Intent intent = new Intent();
            
            // Получаем moveUuid из текущего Intent
            String moveUuid = getIntent().getStringExtra("moveUuid");

            if (moveUuid != null) {
                intent.putExtra("moveUuid", moveUuid);
                Log.d("PrixodActivity", "Передаем moveUuid в результат: " + moveUuid);
            }
            

            intent.putExtra("changeStatusToPodgotovlen", true);
            intent.putExtra("restoreFilters", true);
            intent.putExtra("preserveEditedData", false);
            
            Log.d("PrixodActivity", "Возвращаем RESULT_OK С ФЛАГОМ смены статуса на 'Подготовлен'");
            
            setResult(ProductsActivity.RESULT_OK, intent);
            
            finish();
        }, 200);
    }

    /**
     * Устанавливает режим "только чтение" для всего UI
     * @param readOnly true для включения режима "только чтение"
     */
    private void setReadOnlyMode(boolean readOnly) {
        Log.d("PrixodActivity", "Устанавливаем режим 'только чтение': " + readOnly);
        
        // Блокируем/разблокируем кнопку "Подбор Серии"
        if (btnSelectSeries != null) {
            btnSelectSeries.setEnabled(!readOnly);
            btnSelectSeries.setAlpha(readOnly ? 0.5f : 1.0f);
            Log.d("PrixodActivity", "Кнопка 'Подбор Серии' " + (readOnly ? "ЗАБЛОКИРОВАНА" : "РАЗБЛОКИРОВАНА"));
        }
        
        // Передаем режим в адаптер для блокировки EditText и выбора контейнеров
        if (adapter != null) {
            adapter.setReadOnlyMode(readOnly);
            adapter.setSelectionEnabled(!readOnly); // Блокируем выбор контейнеров в режиме "только чтение"
            Log.d("PrixodActivity", "Режим 'только чтение' передан в адаптер: " + readOnly);
            Log.d("PrixodActivity", "Выбор контейнеров " + (readOnly ? "ЗАБЛОКИРОВАН" : "РАЗБЛОКИРОВАН"));
            

            if (rv_info != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (rv_info.getAdapter() == adapter) {
                        adapter.notifyDataSetChanged();
                        Log.d("PrixodActivity", "Принудительно обновлены все элементы для применения режима 'только чтение'");
                    }
                }, 100);
            }
        }
        
        // Принудительно выключаем режим выбора элементов в режиме "только чтение"
        if (readOnly && isSelectionMode) {
            setSelectionMode(false);
            Log.d("PrixodActivity", "Режим выбора элементов выключен из-за режима 'только чтение'");
        }
        

        if (productsViewModel != null) {
            boolean viewModelReadOnly = productsViewModel.isCurrentlyReadOnly();
            if (viewModelReadOnly != readOnly) {
                Log.d("PrixodActivity", "Обнаружено несоответствие режима 'только чтение' между Activity и ViewModel");

            }
        }
    }



    /**
     * Показывает большое сообщение об ошибке с кнопкой ОК
     * @param message текст сообщения об ошибке
     */
    private void showProductNotFoundDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Продукт не найден")
            .setMessage(message)
            .setPositiveButton("ОК", (dialog, which) -> {
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }

    /**
     * Показывает диалог для выбора действия с несколькими товарами, найденными по одной серии
     * @param data данные о найденных товарах
     */
    private void showMultipleProductsFoundDialog(ProductsViewModel.MultipleProductsData data) {
        List<Product> products = data.getProducts();
        int totalCount = data.getTotalCount();
        String senderStorage = data.getSenderStorage();
        
        // Получаем название номенклатуры
        String nomenclatureName = products.isEmpty() ? "Не указано" : products.get(0).getNomenclatureName();
        if (nomenclatureName == null || nomenclatureName.trim().isEmpty()) {
            nomenclatureName = "Не указано";
        }
        
        // Формируем сообщение для диалога
        String message = String.format(
            "Найдено несколько товаров: \"%s\"\n\n" +
            "Количество товаров: %d\n" +
            "Общее количество: %d\n\n" +
            "Место хранения отправителя: %s\n\n" +
            "Отметить их все как скомплектованные?",
            nomenclatureName, products.size(), totalCount, senderStorage
        );
        

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("Да", (dialog, which) -> {

                String seriesUuid = products.isEmpty() ? null : products.get(0).getSeriesUuid();
                productsViewModel.markAllProductsAsCompleted(products, true, seriesUuid);
                dialog.dismiss();
            })
            .setNegativeButton("Нет", (dialog, which) -> {

                if (!products.isEmpty()) {
                    Product firstProduct = products.get(0);
                    List<Product> currentProducts = productsViewModel.productsLiveData.getValue();
                    if (currentProducts != null) {
                        for (int i = 0; i < currentProducts.size(); i++) {
                            String firstUuid = firstProduct.getNomenclatureUuid();
                            String currentUuid = currentProducts.get(i).getNomenclatureUuid();
                            if (java.util.Objects.equals(firstUuid, currentUuid)) {
                                productsViewModel.requestFocusOnError(firstProduct.getNomenclatureUuid());
                                break;
                            }
                        }
                    }
                }
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }
    

    
    /**
     * Показывает диалог подтверждения со статистикой перед отправкой в "Подготовлено"
     * @param statistics статистика по товарам
     */
    private void showStatisticsConfirmationDialog(ProductsViewModel.ProductsStatistics statistics) {
        // Формируем сообщение со статистикой
        String message = String.format(
            "Общее количество строк товаров: %d\n" +
            "Полностью скомплектовано: %d\n" +
            "Частично скомплектовано: %d\n" +
            "Нескомплектовано: %d\n\n" +
            "Изменить статус перемещения?",
            statistics.getTotalProducts(),
            statistics.getFullyCompleted(),
            statistics.getPartiallyCompleted(),
            statistics.getNotCompleted()
        );
        

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Подтверждение отправки")
            .setMessage(message)
            .setPositiveButton("Да", (dialog, which) -> {
                productsViewModel.confirmSendTo1C();
                dialog.dismiss();
            })
            .setNegativeButton("Нет", (dialog, which) -> {
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }
    
    /**
     * Показывает диалог о том, что изменения сохранены локально при выходе
     */
    private void showDataSavedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Информация")
            .setMessage("Внесенные изменения были сохранены локально. Вы можете вернуться и продолжить работу.")
            .setPositiveButton("ОК", (dialog, which) -> {

                productsViewModel.proceedWithBackToMenu();
                dialog.dismiss();
            })
            .setCancelable(false)
            .show();
    }
}
