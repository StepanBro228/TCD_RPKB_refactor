package com.step.tcd_rpkb.UI.Prixod;

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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.utils.AvatarUtils;
import com.step.tcd_rpkb.utils.UserViewAnimations;

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

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PrixodActivity extends BaseFullscreenActivity
        implements InputInfoAdapter.AdapterButtonListener,
                   InputInfoAdapter.OnProductDataChangedListener { // Изменяем интерфейс

    // Флаг для отслеживания касаний пользователя
    public static boolean isUserTouchingScreen = false;
    private Handler focusHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingFocusRunnable = null;
    
    public static void disableKeyboardForNumericField(EditText editText) {
        if (editText == null) return;
        try {
            editText.setShowSoftInputOnFocus(false);
        } catch (Exception e) {
            Log.w("PrixodActivity", "Error disabling soft input on focus: " + e.getMessage());
        }
    }
    @Inject 
    GetUserUseCase getUserUseCase;
    
    private RecyclerView rv_info;
    private InputInfoAdapter adapter;
    private BroadcastReceiver barcodeDataReceiver; 

    private DrawerLayout drawerLayout;
    private NavigationView sort_bar;
    private boolean isSmallScreen = false;
    private View filterIndicator;
    private FloatingSortView floatingSortView;
    private TextView userFullNameTextView;
    private TextView userRoleTextView;
    private ImageView userAvatarImageView;

    private PrixodViewModel prixodViewModel;
    
    private boolean isSearchActive = false;
    private String lastSearchQuery = "";

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
        setContentView(R.layout.activity_prixod);
        
        prixodViewModel = new ViewModelProvider(this).get(PrixodViewModel.class);
        
        String moveUuid = getIntent().getStringExtra("moveUuid");
        String productsJson = getIntent().getStringExtra("productsData");
        boolean preserveEditedData = getIntent().getBooleanExtra("preserveEditedData", false);
        prixodViewModel.loadInitialData(moveUuid, productsJson, preserveEditedData);
        
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
            (oldFocus, newFocus) -> {
                    if (newFocus instanceof EditText) {
                        EditText editText = (EditText) newFocus;
                        int inputType = editText.getInputType();
                        if ((inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0) {
                            disableKeyboardForNumericField(editText);
                        }
                        if (oldFocus instanceof EditText) {
                            int oldInputType = ((EditText) oldFocus).getInputType();
                            boolean oldIsText = (oldInputType & android.text.InputType.TYPE_CLASS_TEXT) != 0;
                            boolean newIsNumber = (inputType & android.text.InputType.TYPE_CLASS_NUMBER) != 0;
                            if (oldIsText && newIsNumber) {
                                hideKeyboard(oldFocus);
                        }
                    }
                }
            }
        );
        
        drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton menuButton = findViewById(R.id.nav_menu_button);
        sort_bar = findViewById(R.id.navigation_view);
        
        if (sort_bar != null) {
            sort_bar.setFocusable(true);
            sort_bar.setFocusableInTouchMode(true);
        }
        
        if (menuButton != null) {
            menuButton.setOnClickListener(null); 
            menuButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusView = getCurrentFocus();
                    if (currentFocusView instanceof EditText) {
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
        }
        
        setupFilterListeners();
        
        findViewById(R.id.main).setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocusView = getCurrentFocus();
                if (currentFocusView instanceof EditText && !isTouchOnView(event, currentFocusView)) { 
                    hideKeyboard(currentFocusView);
                    currentFocusView.clearFocus(); 
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
                hideKeyboard(drawerLayout);
                clearFocusFromNonRecyclerViewEditTexts(); 
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
                Boolean filtersActive = prixodViewModel.isAnyFilterActiveLiveData.getValue();
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
        if (barcodeDataReceiver != null) {
            registerReceiver(barcodeDataReceiver, scannerFilter, Context.RECEIVER_EXPORTED); // For Android 12+
        } else {
            Log.w("PrixodActivity", "barcodeDataReceiver is null in onResume, not registering.");
    }
    }

    @Override
    public void onBackPressed() {
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
            return;
        }
        super.onBackPressed(); 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (barcodeDataReceiver != null) unregisterReceiver(barcodeDataReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("PrixodActivity", "Ошибка при отписке barcodeDataReceiver: " + e.getMessage());
        }
        if (pendingFocusRunnable != null) {
            focusHandler.removeCallbacks(pendingFocusRunnable);
            pendingFocusRunnable = null;
            }
        }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isUserTouchingScreen = true;
            View currentFocusView = getCurrentFocus();
            FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
            
            // Если NavigationView открыт и касание вне его, закрываем и очищаем фокус
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                View menuView = findViewById(R.id.navigation_view);
                if (menuView != null && !isTouchOnView(event, menuView)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // onDrawerClosed позаботится о фокусе и клавиатуре
                    return true; 
                }
            } else {
                // Если NavigationView закрыт, проверяем касание на EditText
                if (currentFocusView instanceof EditText && !isTouchOnView(event, currentFocusView)) {
                    hideKeyboard(currentFocusView);
                    currentFocusView.clearFocus();
                }
            }
            // Обработка касания кнопки меню была перенесена в ее собственный OnTouchListener
            new Handler(Looper.getMainLooper()).postDelayed(() -> isUserTouchingScreen = false, 300); // Уменьшил задержку
        }
        return super.dispatchTouchEvent(event);
    }

    public void scrollToViewIfNeeded(View viewToShow, RecyclerView recyclerView) {
        if (viewToShow == null || recyclerView == null || !viewToShow.isAttachedToWindow() || !recyclerView.isAttachedToWindow()) return;
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
            if (keyboardHeight > dpToPx(50)) { // Уменьшил порог для активной клавиатуры
                visibleContainerBottom = Math.min(recyclerGlobalVisibleRect.bottom, r.bottom - (int)dpToPx(10)); 
                        }
            final int PADDING = (int) dpToPx(8); // Уменьшил отступ
            int scrollBy = 0;
            if (viewTop < visibleContainerTop + PADDING) {
                scrollBy = viewTop - (visibleContainerTop + PADDING); 
            } else if (viewBottom > visibleContainerBottom - PADDING) {
                scrollBy = viewBottom - (visibleContainerBottom - PADDING);
            }
            if (scrollBy != 0) {
                recyclerView.smoothScrollBy(0, scrollBy);
            }
        } catch (Exception e) {
            Log.e("PrixodActivity", "Error in scrollToViewIfNeeded: " + e.getMessage());
        }
    }

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

    private void setupFilterListeners() {
        EditText filterNameEditText = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
        ImageButton clearNameButton = findViewById(R.id.clear_name);
        ImageButton clearAmountButton = findViewById(R.id.clear_amount);
        Button btnResetAllFiltersButton = findViewById(R.id.btn_reset_all_filters);

        if (filterNameEditText != null) {
            setupTextWatcher(filterNameEditText, newText -> prixodViewModel.updateNameFilter(newText));
            setupEditTextFocusChangeListenerForFilters(filterNameEditText);
            setupEnterKeyListenerForFilters(filterNameEditText);
            if (clearNameButton != null) clearNameButton.setOnClickListener(v -> filterNameEditText.setText(""));
        }
        if (amountFilterEditText1 != null) {
            setupTextWatcher(amountFilterEditText1, newText -> applyAmountFilterFromUI());
            setupEditTextFocusChangeListenerForFilters(amountFilterEditText1);
            setupEnterKeyListenerForFilters(amountFilterEditText1);
        }
        if (amountFilterEditText2 != null) {
            setupTextWatcher(amountFilterEditText2, newText -> applyAmountFilterFromUI());
            setupEditTextFocusChangeListenerForFilters(amountFilterEditText2);
            setupEnterKeyListenerForFilters(amountFilterEditText2);
        }
        if (clearAmountButton != null && amountFilterEditText1 != null && amountFilterEditText2 != null) {
            clearAmountButton.setOnClickListener(v -> {
                amountFilterEditText1.setText("");
                amountFilterEditText2.setText(""); 
            });
        }
        if (filterUntouchedCheckBox != null) {
            filterUntouchedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> prixodViewModel.updateUntouchedFilter(isChecked));
        }
        if (btnResetAllFiltersButton != null) {
            btnResetAllFiltersButton.setOnClickListener(v -> prixodViewModel.resetAllFilters());
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
        prixodViewModel.updateAmountFilter(minAmount, maxAmount);
    }

    private void setupTextWatcher(EditText editText, TextChangeListener listener) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { listener.onTextChanged(s.toString()); }
        });
    }

    private interface TextChangeListener { void onTextChanged(String newText); }

    private void setupEditTextFocusChangeListenerForFilters(EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToViewInsideNavigation(editText);
            }
        });
    }

    private void setupNestedScrollViewTouchListener() {
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocusView = getCurrentFocus();
                    if (currentFocusView instanceof EditText && isViewInsideViewGroup(sort_bar, currentFocusView) && !isTouchOnView(event, currentFocusView)) {
                        hideKeyboard(currentFocusView);
                        currentFocusView.clearFocus(); 
                    }
                }
                return false; 
            });
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

    private void scrollToViewInsideNavigation(View view) {
        if (view == null) return;
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.filter_scroll_view);
        if (scrollView != null) {
             // Прокрутка к элементу, чтобы он был виден.
             // requestChildFocus может быть более надежным для ScrollView.
            scrollView.post(() -> scrollView.requestChildFocus(view, view));
    }
    }

    private void setupEnterKeyListenerForFilters(EditText editText) {
        editText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                try {
                    hideKeyboard(v);
                    v.clearFocus();
                    // Закрываем NavigationView, если он открыт (onDrawerClosed позаботится о фокусе на nav_button)
                    if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        // Если NavigationView закрыт, возможно, стоит передать фокус nav_button вручную, 
                        // если это желаемое поведение.
                        // FloatingActionButton navMenuButton = findViewById(R.id.nav_menu_button);
                        // if (navMenuButton != null) navMenuButton.requestFocus();
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
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN )) {
                try {
                    hideKeyboard(v);
                v.clearFocus();
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
        prixodViewModel.productsLiveData.observe(this, products -> {
            Set<String> currentErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
            if (adapter == null) {
                adapter = new InputInfoAdapter(PrixodActivity.this, products, this, this);
                rv_info.setAdapter(adapter);
                if (rv_info.getItemDecorationCount() == 0) {
                    int spaceHeight = getResources().getDimensionPixelSize(R.dimen.item_space_height);
                    int sideSpace = getResources().getDimensionPixelSize(R.dimen.item_side_space);
                    rv_info.addItemDecoration(new ItemSpaceDecoration(spaceHeight, sideSpace));
                }
            }
            // Всегда вызываем updateData, чтобы адаптер знал об изменениях, включая ошибки
            adapter.updateData(products, currentErrors); 
        });

        prixodViewModel.isLoadingLiveData.observe(this, isLoading -> {
            if (isLoading) showLoadingDialog("Загрузка данных...");
            else dismissLoadingDialog();
        });

        prixodViewModel.errorLiveData.observe(this, event -> {
            String errorMessage = event.getContentIfNotHandled();
            if (errorMessage != null) Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });

        prixodViewModel.sortCriteriaLiveData.observe(this, criteria -> {
            if (floatingSortView != null) {
                Boolean isAscending = prixodViewModel.isSortAscendingLiveData.getValue();
                floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
            }
        });

        prixodViewModel.isSortAscendingLiveData.observe(this, isAscending -> {
            if (floatingSortView != null) {
                SortCriteria criteria = prixodViewModel.sortCriteriaLiveData.getValue();
                if (criteria != null) floatingSortView.setViewState(criteria, Boolean.TRUE.equals(isAscending));
            }
        });

        prixodViewModel.focusProductPositionLiveData.observe(this, event -> {
            Integer positionWrapper = event.getContentIfNotHandled();
            if (positionWrapper != null) {
                final int positionToFocus = positionWrapper;
                Log.d("PrixodActivity", "Получен запрос на фокус позиции: " + positionToFocus);

                if (positionToFocus != -1 && rv_info != null && adapter != null && positionToFocus < adapter.getItemCount() - 1) { // -1 из-за кнопки в футере
                    if (pendingFocusRunnable != null) {
                        focusHandler.removeCallbacks(pendingFocusRunnable);
                        Log.d("PrixodActivity", "Отменен предыдущий pendingFocusRunnable.");
                    }

                    pendingFocusRunnable = new Runnable() {
                        private int attemptCount = 0;
                        private final int MAX_ATTEMPTS = 7; 
                        private final int RETRY_DELAY_MS = 150;
                        private final int FOCUS_DELAY_MS = 350;

                        @Override
                        public void run() {
                            attemptCount++;
                            Log.d("PrixodActivity", "pendingFocusRunnable: Попытка " + attemptCount + " для позиции " + positionToFocus);

                            if (rv_info == null || !rv_info.isAttachedToWindow() || adapter == null) {
                                Log.w("PrixodActivity", "pendingFocusRunnable: RecyclerView/Adapter недоступен или не присоединен. Отмена фокуса.");
                                pendingFocusRunnable = null;
                                return;
                            }
                            if (positionToFocus >= adapter.getItemCount() -1) { // Проверка снова, т.к. данные могли измениться
                                Log.w("PrixodActivity", "pendingFocusRunnable: Позиция " + positionToFocus + " стала невалидной. Отмена фокуса.");
                                pendingFocusRunnable = null;
                                return;
                            }

                            if (rv_info.isComputingLayout() || rv_info.isAnimating() || rv_info.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                                if (attemptCount < MAX_ATTEMPTS) {
                                    Log.w("PrixodActivity", "pendingFocusRunnable: RecyclerView занят. Повторная отправка фокуса через " + RETRY_DELAY_MS + "ms.");
                                    focusHandler.postDelayed(this, RETRY_DELAY_MS); 
                                } else {
                                    Log.e("PrixodActivity", "pendingFocusRunnable: Достигнуто макс. попыток. RecyclerView все еще занят. Отмена фокуса.");
                                    pendingFocusRunnable = null;
                                }
                                return;
                            }

                            Log.d("PrixodActivity", "pendingFocusRunnable: RecyclerView свободен. Прокрутка и фокус...");

                            focusHandler.postDelayed(() -> {
                                if (rv_info == null || !rv_info.isAttachedToWindow() || adapter == null) {
                                    Log.w("PrixodActivity", "pendingFocusRunnable (внутренний): RecyclerView/Adapter стал недоступен. Отмена.");
                                    pendingFocusRunnable = null;
                                    return;
                                }
                                 if (positionToFocus >= adapter.getItemCount() -1) {
                                    Log.w("PrixodActivity", "pendingFocusRunnable (внутренний): Позиция " + positionToFocus + " стала невалидной. Отмена.");
                                    pendingFocusRunnable = null;
                                    return;
                                }
                                rv_info.smoothScrollToPosition(positionToFocus);
                                RecyclerView.ViewHolder viewHolder = rv_info.findViewHolderForAdapterPosition(positionToFocus);
                                if (viewHolder instanceof InputInfoAdapter.InputinfoViewHolder) {
                                    InputInfoAdapter.InputinfoViewHolder prixodViewHolder = (InputInfoAdapter.InputinfoViewHolder) viewHolder;
                                    EditText targetEditText = prixodViewHolder.list_carried;
                                    if (targetEditText != null && targetEditText.isAttachedToWindow()) {
                                        targetEditText.requestFocus();
                                        // Курсор устанавливается в onFocusChange адаптера
                                        Log.i("PrixodActivity", "pendingFocusRunnable: Фокус УСПЕШНО УСТАНОВЛЕН на позиции " + positionToFocus);
                                        pendingFocusRunnable = null; 
                                    } else {
                                        Log.w("PrixodActivity", "pendingFocusRunnable (внутренний): EditText null или не присоединен для позиции " + positionToFocus + ". Попытка " + attemptCount);
                                        if (attemptCount < MAX_ATTEMPTS) { 
                                            focusHandler.postDelayed(this, RETRY_DELAY_MS + 50); 
                                        } else {
                                            Log.e("PrixodActivity", "pendingFocusRunnable (внутренний): Макс. попыток для EditText. Отмена.");
                                            pendingFocusRunnable = null;
                                        }
                                    }
                                } else if (viewHolder == null) {
                                    Log.w("PrixodActivity", "pendingFocusRunnable (внутренний): ViewHolder null для позиции " + positionToFocus + ". Попытка " + attemptCount);
                                     if (attemptCount < MAX_ATTEMPTS) { 
                                        focusHandler.postDelayed(this, RETRY_DELAY_MS + 50); 
                                    } else {
                                         Log.e("PrixodActivity", "pendingFocusRunnable (внутренний): Макс. попыток для ViewHolder. Отмена.");
                                        pendingFocusRunnable = null;
                                    }
                                } else {
                                     Log.w("PrixodActivity", "pendingFocusRunnable (внутренний): ViewHolder для " + positionToFocus + " типа "+viewHolder.getClass().getSimpleName() + ". Отмена.");
                                     pendingFocusRunnable = null;
                                }
                            }, FOCUS_DELAY_MS);
                        }
                    };

                    ViewTreeObserver vto = rv_info.getViewTreeObserver();
                    if (vto.isAlive()) {
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (rv_info == null || !rv_info.isAttachedToWindow()) { // Доп. проверка
                                    if (pendingFocusRunnable != null) focusHandler.removeCallbacks(pendingFocusRunnable);
                                    pendingFocusRunnable = null;
                                    try { rv_info.getViewTreeObserver().removeOnGlobalLayoutListener(this); } catch (Exception e) {}
                                    return;
                                }
                                rv_info.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                if (pendingFocusRunnable != null) {
                                    Log.d("PrixodActivity", "OnGlobalLayout сработал для позиции " + positionToFocus + ". Отправка pendingFocusRunnable.");
                                    focusHandler.post(pendingFocusRunnable);
                                } else {
                                     Log.d("PrixodActivity", "OnGlobalLayout сработал, но pendingFocusRunnable is null.");
                                }
                            }
                        });
                    } else {
                        Log.w("PrixodActivity", "ViewTreeObserver не жив для позиции " + positionToFocus + ". Отправка pendingFocusRunnable напрямую.");
                         if (pendingFocusRunnable != null) {
                            focusHandler.post(pendingFocusRunnable);
                        } else {
                             Log.d("PrixodActivity", "ViewTreeObserver не жив, и pendingFocusRunnable is null.");
            }
                    }
                } else if (positionToFocus != -1) {
                     Log.w("PrixodActivity", "Невалидная позиция для фокуса: " + positionToFocus + ", itemCount адаптера: " + (adapter != null ? adapter.getItemCount() : "null adapter"));
                } else {
                    Log.d("PrixodActivity", "Получена невалидная позиция для фокуса (-1). Ничего не делаем.");
                }
            }
        });

        prixodViewModel.productNotFoundForFocusEvent.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });

        prixodViewModel.nameFilterLiveData.observe(this, name -> {
            EditText filterNameEditText = findViewById(R.id.filter_name);
            if (filterNameEditText != null && !filterNameEditText.getText().toString().equals(name)) filterNameEditText.setText(name);
        });

        prixodViewModel.minAmountFilterLiveData.observe(this, min -> {
            EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
            if (amountFilterEditText1 != null) {
                String currentText = amountFilterEditText1.getText().toString();
                String newText = (min == null || min == 0) ? "" : String.valueOf(min);
                if (!currentText.equals(newText)) amountFilterEditText1.setText(newText);
            }
        });

        prixodViewModel.maxAmountFilterLiveData.observe(this, max -> {
            EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
            if (amountFilterEditText2 != null) {
                String currentText = amountFilterEditText2.getText().toString();
                String newText = (max == null || max == Integer.MAX_VALUE) ? "" : String.valueOf(max);
                 if (!currentText.equals(newText)) amountFilterEditText2.setText(newText);
            }
        });

        prixodViewModel.untouchedFilterLiveData.observe(this, active -> {
            CheckBox filterUntouchedCheckBox = findViewById(R.id.filter_untouched);
            if (filterUntouchedCheckBox != null && filterUntouchedCheckBox.isChecked() != active) filterUntouchedCheckBox.setChecked(active);
        });

        prixodViewModel.isAnyFilterActiveLiveData.observe(this, isActive -> {
            if (filterIndicator != null) filterIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);
        });

        prixodViewModel.validationErrorUuidsLiveData.observe(this, errorUuids -> {
            if (adapter != null) {
                // Обновляем данные в адаптере, включая ошибки.
                // Это вызовет notifyDataSetChanged() внутри адаптера, если необходимо.
                adapter.updateData(prixodViewModel.productsLiveData.getValue(), errorUuids);
            }
            if (errorUuids != null && !errorUuids.isEmpty()) Log.w("PrixodActivity", "Ошибки валидации для UUIDs: " + errorUuids.toString());
            else Log.d("PrixodActivity", "Ошибок валидации нет.");
        });

        prixodViewModel.forceResetFiltersMessageLiveData.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
     * Очищает фокус только с EditText, не находящихся в RecyclerView (например, поля фильтров).
     * Фокус с EditText в RecyclerView управляется через адаптер и логику потери фокуса.
     */
    private void clearFocusFromNonRecyclerViewEditTexts() {
        View currentFocusView = getCurrentFocus();
        if (currentFocusView instanceof EditText && !isViewInsideViewGroup(rv_info, currentFocusView)) {
            currentFocusView.clearFocus();
                }
        // Дополнительно проходим по известным полям фильтров
        EditText filterName = findViewById(R.id.filter_name);
        EditText amountFilterEditText1 = findViewById(R.id.amount_filter_editText1);
        EditText amountFilterEditText2 = findViewById(R.id.amount_filter_editText2);
        if (filterName != null && filterName.hasFocus()) filterName.clearFocus();
        if (amountFilterEditText1 != null && amountFilterEditText1.hasFocus()) amountFilterEditText1.clearFocus();
        if (amountFilterEditText2 != null && amountFilterEditText2.hasFocus()) amountFilterEditText2.clearFocus();
        
        // После очистки фокуса с полей, можно передать фокус на корневой элемент, чтобы убрать его откуда-либо
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
                if ("com.example.scannerapp.ACTION_BARCODE_DATA".equals(action)) barcodeData = intent.getStringExtra("data");
                else if ("android.intent.action.SCAN".equals(action) || "com.google.zxing.client.android.SCAN".equals(action)) barcodeData = intent.getStringExtra("SCAN_RESULT");
                else if ("com.symbol.datawedge.api.ACTION".equals(action)) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        if (extras.containsKey("com.symbol.datawedge.data_string")) barcodeData = extras.getString("com.symbol.datawedge.data_string");
                        else if (extras.containsKey("data")) barcodeData = extras.getString("data");
                    }
                } else if ("com.scanner.broadcast".equals(action)) barcodeData = intent.getStringExtra("data");

                if (barcodeData != null && !barcodeData.isEmpty()) {
                    Log.d("PrixodActivity", "Сканированные данные: " + barcodeData);
                    prixodViewModel.processBarcodeData(barcodeData);
                } else {
                    Log.w("PrixodActivity", "Данные сканирования не получены или пусты для action: " + action);
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        for (String key : extras.keySet()) Log.d("PrixodActivityScannerExtras", "Extra: " + key + " = " + extras.get(key));
                    }
                }
            }
        };
    }

    @Override
    public void onSendDataClicked() {
        hideKeyboard(getCurrentFocus()); // Скрываем клавиатуру перед действием
        Set<String> validationErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Найдены ошибки ввода! Проверьте выделенные поля.", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            prixodViewModel.requestFocusOnError(firstErrorUuid);
            return; 
        }
        showLoadingDialog("Подготовка данных...");
        String productsJsonToSave = prixodViewModel.getProductsToSaveAsJson();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            prixodViewModel.resetAllFiltersAndSort(); 
            dismissLoadingDialog();
            Intent intent = new Intent();
            intent.putExtra("productData", productsJsonToSave); 
            intent.putExtra("restoreFilters", true); 
            setResult(PrixodActivity.RESULT_OK, intent);
            finish();
        }, 300); 
    }

    @Override
    public void onGoBackClicked() {
        hideKeyboard(getCurrentFocus()); // Скрываем клавиатуру перед действием
        Set<String> validationErrors = prixodViewModel.validationErrorUuidsLiveData.getValue();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            Toast.makeText(this, "Перед выходом исправьте ошибки ввода!", Toast.LENGTH_LONG).show();
            String firstErrorUuid = validationErrors.iterator().next();
            prixodViewModel.requestFocusOnError(firstErrorUuid);
            return; 
        }
        showLoadingDialog("Возврат к списку...");
        prixodViewModel.resetAllFiltersAndSort(); 
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dismissLoadingDialog();
            Intent intent = new Intent();
            intent.putExtra("restoreFilters", true);
            setResult(PrixodActivity.RESULT_CANCELED, intent);
            finish();
        }, 200);
    }

    @Override
    public void onProductDataConfirmed(String nomenclatureUuid, int newTakenValue, int positionInAdapter, boolean isValid, boolean byEnterKey) {
        Log.d("PrixodActivity", "Данные подтверждены для UUID: " + nomenclatureUuid + ", новое значение: " + newTakenValue + ", позиция: " + positionInAdapter + ", валидно: " + isValid + ", через Enter: " + byEnterKey);
        prixodViewModel.handleProductDataConfirmation(nomenclatureUuid, newTakenValue, isValid, byEnterKey, positionInAdapter); 
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Обработка Enter в EditText фильтров (в NavigationView) уже есть в setupEnterKeyListenerForFilters
        // Обработка Enter в EditText RecyclerView теперь полностью в InputInfoAdapter
        // Обработка Back button для закрытия NavigationView - в onBackPressed()
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
        } else {
            Log.w("PrixodActivity", "Не удалось скрыть клавиатуру: view или window token is null");
        }
    }
}
