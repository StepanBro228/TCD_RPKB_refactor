package com.step.tcd_rpkb.UI.ZnpSelection.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import com.google.gson.GsonBuilder;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.ZnpSelection.adapter.SeriesChangeAdapter;
import com.step.tcd_rpkb.UI.ZnpSelection.viewmodel.ZnpSelectionViewModel;
import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.domain.model.SeriesChangeItem;
import com.step.tcd_rpkb.utils.FocusManager;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import android.app.AlertDialog;
import android.annotation.SuppressLint;

/**
 * Activity для замены серии товаров
 */
@AndroidEntryPoint
public class ZnpSelectionActivity extends BaseFullscreenActivity {
    
    private static final String TAG = "ZnpSelectionActivity";
    
    private ZnpSelectionViewModel viewModel;
    private SeriesChangeAdapter adapter;
    
    // UI компоненты
    private TextView tvNomenclatureName;
    private TextView tvSeriesName;
    private TextView tvWarehouse;
    private TextView tvUnitOfMeasurement;
    private TextView tvFreeBalance;
    private com.google.android.material.textfield.TextInputEditText etChangeQuantity;
    private MaterialButton btnChangeSeries;
    private MaterialButton btnSave;
    private RecyclerView recyclerViewProducts;
    private MaterialButton btnCancel;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private View rootLayout;
    private View focusDummy; // Невидимый элемент для приема фокуса
    
    // Данные о номенклатуре и исходной серии
    private String nomenclatureUuid;
    private String nomenclatureName;
    private String moveUuid;
    private String targetSeriesUuid;
    private String targetSeriesName;
    
    // Свободный остаток для целевой серии
    private double freeBalance = 0.0;
    
    // Флаг для отслеживания были ли применены изменения
    private boolean changesApplied = false;
    
    // Ключ для резервной копии продуктов
    private static final String BACKUP_SUFFIX = "_backup";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_znp_selection);
        
        // Инициализация UI
        initializeViews();
        

        
        // Получение ViewModel
        viewModel = new ViewModelProvider(this).get(ZnpSelectionViewModel.class);
        
        // Получаем данные из Intent
        Intent intent = getIntent();
        if (intent != null) {
            nomenclatureUuid = intent.getStringExtra("nomenclatureUuid");
            nomenclatureName = intent.getStringExtra("nomenclatureName");
            moveUuid = intent.getStringExtra("moveUuid");
            targetSeriesUuid = intent.getStringExtra("targetSeriesUuid");
            targetSeriesName = intent.getStringExtra("targetSeriesName");
            freeBalance = intent.getDoubleExtra("freeBalance", 0.0);
            
            Log.d(TAG, "Получены данные из Intent: nomenclatureUuid=" + nomenclatureUuid +
                      ", nomenclatureName=" + nomenclatureName +
                      ", moveUuid=" + moveUuid +
                      ", targetSeriesUuid=" + targetSeriesUuid +
                      ", targetSeriesName=" + targetSeriesName +
                      ", freeBalance=" + freeBalance);
            
            // Проверка наличия необходимых данных
            if (targetSeriesUuid == null || targetSeriesUuid.isEmpty() || nomenclatureUuid == null || nomenclatureUuid.isEmpty()) {
                Toast.makeText(this, "Ошибка: отсутствует информация о серии или номенклатуре", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Заполнение UI данными
            populateBasicInfo();
            
            // Загрузка продуктов для замены серии
            loadProductsForSeriesChange();
        } else {
            Toast.makeText(this, "Ошибка: отсутствуют данные", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // Подписка на обновления ViewModel
        observeViewModel();
    }
    
    private void initializeViews() {
        // Инициализация основных компонентов
        tvNomenclatureName = findViewById(R.id.tv_nomenclature_name);
        tvSeriesName = findViewById(R.id.tv_series_name);
        tvWarehouse = findViewById(R.id.tv_warehouse);
        tvUnitOfMeasurement = findViewById(R.id.tv_unit_of_measurement);
        tvFreeBalance = findViewById(R.id.tv_free_balance);
        etChangeQuantity = findViewById(R.id.et_change_quantity);
        btnChangeSeries = findViewById(R.id.btn_change_series);
        btnSave = findViewById(R.id.btn_save);
        recyclerViewProducts = findViewById(R.id.recycler_view_products);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        rootLayout = findViewById(android.R.id.content);
        focusDummy = findViewById(R.id.focus_dummy);
        

        rootLayout.setFocusable(true);
        rootLayout.setFocusableInTouchMode(true);
        rootLayout.setClickable(true);
        

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            rootLayout.setDefaultFocusHighlightEnabled(false);
        }
        rootLayout.setBackground(null);
        

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            recyclerViewProducts.setDefaultFocusHighlightEnabled(false);
        }
        

        if (focusDummy != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                focusDummy.setDefaultFocusHighlightEnabled(false);
            }
        }
        
        // Настройка поведения основного EditText
        setupMainEditText();
        
        // Настройка RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SeriesChangeAdapter(new ArrayList<>());
        recyclerViewProducts.setAdapter(adapter);
        
        // Установка слушателя изменения выбора продуктов
        adapter.setOnSelectionChangeListener((position, isSelected) -> {
            viewModel.updateItemSelection(position, isSelected);
        });

        btnChangeSeries.setOnClickListener(v -> onChangeSeriesButtonClicked());
        btnSave.setOnClickListener(v -> onSaveButtonClicked());
        btnCancel.setOnClickListener(v -> onCancelButtonClicked());
        btnBack.setOnClickListener(v -> onBackButtonClicked());
    }

    /**
     * Настройка основного поля ввода количества для замены серии
     */
    private void setupMainEditText() {
        // Отключаем стандартную клавиатуру
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            etChangeQuantity.setShowSoftInputOnFocus(false);
        }
        

        etChangeQuantity.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "Основной EditText onFocusChange: hasFocus=" + hasFocus);
            
            if (hasFocus) {

                FocusManager.cancelPendingFocusRequests();
                
                String value = etChangeQuantity.getText().toString();
                // Если значение 0, очищаем поле
                if (value.equals("0") || value.equals("0.0") || value.equals("0.00")) {
                    etChangeQuantity.setText("");
                } else {
                    // Устанавливаем курсор в конец строки
                    etChangeQuantity.post(() -> {
                        if (etChangeQuantity.hasFocus()) {
                            etChangeQuantity.setSelection(etChangeQuantity.getText().length());
                        }
                    });
                }
            }
        });
        
        // Обработка нажатия на EditText
        etChangeQuantity.setOnClickListener(v -> {
            Log.d(TAG, "Основной EditText onClick");

            FocusManager.requestFocusChange(etChangeQuantity, true, 50);
            
            String value = etChangeQuantity.getText().toString();
            if (value.equals("0") || value.equals("0.0") || value.equals("0.00")) {
                etChangeQuantity.setText("");
            }
        });
        

        etChangeQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                validateChangeQuantity();
            }
        });
        
        // Обработка нажатия Enter
        etChangeQuantity.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterPressed = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == android.view.KeyEvent.ACTION_DOWN);
            
            if (isEnterPressed) {
                Log.d(TAG, "Enter нажат в основном EditText");
                etChangeQuantity.clearFocus();
                if (focusDummy != null) {
                    focusDummy.requestFocus();
                } else {
                    rootLayout.requestFocus();
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * Универсальный метод для снятия фокуса со всех полей ввода
     */
    private void clearAllFocus() {
        Log.d(TAG, "Принудительное снятие фокуса со всех EditText");
        

        FocusManager.cancelPendingFocusRequests();
        

        View currentFocus = getCurrentFocus();
        Log.d(TAG, "Текущий фокус до очистки: " + 
              (currentFocus != null ? currentFocus.getClass().getSimpleName() : "null"));
        

        if (etChangeQuantity != null) {
            etChangeQuantity.clearFocus();
        }
        

        clearRecyclerViewEditTextFocus();
        

        if (currentFocus != null) {
            currentFocus.clearFocus();
        }
        
        // Запрашиваем фокус на невидимый элемент
        if (focusDummy != null) {
            focusDummy.requestFocus();
        } else if (rootLayout != null) {
            rootLayout.requestFocus();
        }
        

        hideKeyboard();
        

        rootLayout.post(() -> {
            View newFocus = getCurrentFocus();
            Log.d(TAG, "Фокус после очистки: " + 
                  (newFocus != null ? newFocus.getClass().getSimpleName() : "null"));

            if (newFocus instanceof EditText) {
                Log.d(TAG, "Фокус все еще на EditText, выполняем дополнительную очистку");
                newFocus.clearFocus();
                if (focusDummy != null) {
                    focusDummy.requestFocus();
                } else {
                    rootLayout.requestFocus();
                }
            }
        });
    }
    
    /**
     * Снятие фокуса со всех EditText в RecyclerView
     */
    private void clearRecyclerViewEditTextFocus() {
        if (recyclerViewProducts != null) {
            for (int i = 0; i < recyclerViewProducts.getChildCount(); i++) {
                View child = recyclerViewProducts.getChildAt(i);
                if (child != null) {
                    clearFocusRecursively(child);
                }
            }
        }
    }
    
    /**
     * Рекурсивно снятие фокуса с EditText
     */
    private void clearFocusRecursively(View view) {
        if (view instanceof EditText && view.hasFocus()) {
            view.clearFocus();
            return;
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                clearFocusRecursively(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * Скрытие программной клавиатуры
     */
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    
    /**
     * Проверка, было ли касание вне EditText
     */
    private boolean isTouchOutsideEditText(MotionEvent event) {
        // Проверяем касание относительно основного поля ввода
        if (isTouchInsideView(etChangeQuantity, event)) {
            return false;
        }
        
        // Проверяем касание относительно всех EditText в RecyclerView
        if (recyclerViewProducts != null) {
            for (int i = 0; i < recyclerViewProducts.getChildCount(); i++) {
                View child = recyclerViewProducts.getChildAt(i);
                if (child != null) {
                    if (isTouchInsideAnyEditText(child, event)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Проверяет, было ли касание внутри указанного View
     */
    private boolean isTouchInsideView(View view, MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        android.graphics.Rect outRect = new android.graphics.Rect();
        view.getGlobalVisibleRect(outRect);
        return outRect.contains((int) event.getRawX(), (int) event.getRawY());
    }
    
    /**
     * Рекурсивно проверяет, было ли касание внутри любого EditText в указанном View
     */
    private boolean isTouchInsideAnyEditText(View view, MotionEvent event) {
        if (view instanceof EditText) {
            return isTouchInsideView(view, event);
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (isTouchInsideAnyEditText(viewGroup.getChildAt(i), event)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void populateBasicInfo() {
        // Заполняем основную информацию из Intent
        tvNomenclatureName.setText(nomenclatureName != null ? nomenclatureName : "Неизвестная номенклатура");
        tvSeriesName.setText(createFormattedText("Серия: " , (targetSeriesName != null ? targetSeriesName : "Неизвестная серия"), true));
        
        // Используем свободный остаток переданный из SeriesSelectionActivity
        tvFreeBalance.setText(String.format("%.2f", freeBalance));
        Log.d(TAG, "Свободный остаток установлен из SeriesSelectionActivity: " + freeBalance);




        tvWarehouse.setText(getIntent().getStringExtra("wareHouse") != null ? getIntent().getStringExtra("wareHouse") : "");


    }
    private SpannableStringBuilder createFormattedText(String prefix, String name, boolean flag) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int start = builder.length();
        builder.append(prefix);
        int middle = builder.length();
        builder.append(name);
        int end = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), middle, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (flag = true){
            int color = ContextCompat.getColor(this, R.color.colorText);
            int colorAlpha = ColorUtils.setAlphaComponent(color, 179);
            builder.setSpan(new ForegroundColorSpan(colorAlpha), start, middle, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new AbsoluteSizeSpan(18, true), middle, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
    
    private void loadProductsForSeriesChange() {
        // Создаем резервную копию продуктов перед загрузкой
        createProductsBackup();
        
        // Загружаем продукты для замены серии
        viewModel.loadProductsForSeriesChange(nomenclatureUuid, moveUuid, targetSeriesUuid, targetSeriesName, freeBalance);


    }
    
    private void observeViewModel() {
        // Наблюдение за списком продуктов для замены серии
        viewModel.getSeriesChangeItems().observe(this, this::updateProductsList);
        
        // Наблюдение за максимальным доступным количеством
        viewModel.getMaxAvailableQuantity().observe(this, maxAvailable -> {
            Log.d(TAG, "Обновление максимального доступного количества: " + maxAvailable);
            updateMaxAvailableQuantityDisplay(maxAvailable);
        });
        
        // Наблюдение за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        
        // Наблюдение за событием успешной замены серии
        viewModel.getSeriesChangeSuccessEvent().observe(this, event -> {
            ZnpSelectionViewModel.SeriesChangeResult result = event.getContentIfNotHandled();
            if (result != null) {
                // Обновляем данные в ViewModel
                viewModel.refreshDataAfterSeriesChange();
                
                // Обновляем tvFreeBalance
                double updatedFreeBalance = viewModel.getCurrentFreeBalance();
                tvFreeBalance.setText(String.format("%.2f", updatedFreeBalance));
                
                // Сбрасываем выбор в адаптере и обновляем данные
                adapter.clearSelection();
                
                // Получаем текущие данные из ViewModel и обновляем список
                List<com.step.tcd_rpkb.domain.model.SeriesChangeItem> currentItems = viewModel.getSeriesChangeItems().getValue();
                if (currentItems != null) {
                    updateProductsList(currentItems);
                }
                
                //  Сбрасываем значение в etChangeQuantity на 0
                etChangeQuantity.setText("0");
                
                //  Показываем Toast
                Toast.makeText(this, "Серия успешно заменена", Toast.LENGTH_SHORT).show();
                
                //  Устанавливаем флаг что изменения были применены
                changesApplied = true;
                

            }
        });
        
        // Наблюдение за состоянием загрузки
        viewModel.getIsLoading().observe(this, this::showLoading);
    }
    
    private void updateProductsList(List<SeriesChangeItem> seriesChangeItems) {
        if (seriesChangeItems != null) {
            Log.d(TAG, "Обновление UI данными продуктов для замены серии");

            Log.d(TAG, "Обновление адаптера с " + seriesChangeItems.size() + " продуктами");
            adapter.updateData(seriesChangeItems);
            
            SeriesChangeItem first = seriesChangeItems.get(0);
            if(first != null){
                tvUnitOfMeasurement.setText(first.getUnitName() != null ? first.getUnitName() : "");
            }

        } else {
            Log.w(TAG, "SeriesChangeItems равен null при обновлении UI");
        }
    }
    
    /**
     * Обновляет отображение максимального доступного количества
     */
    private void updateMaxAvailableQuantityDisplay(double maxAvailable) {

        boolean hasFocus = etChangeQuantity.hasFocus();
        int selStart = etChangeQuantity.getSelectionStart();
        int selEnd = etChangeQuantity.getSelectionEnd();
        

        String formattedValue;
        if (maxAvailable == Math.floor(maxAvailable)) {
            formattedValue = String.format("%.0f", maxAvailable);
        } else {
            formattedValue = String.format("%.2f", maxAvailable);
        }
        
        Log.d(TAG, "Форматированное значение максимального доступного количества: " + formattedValue);
        

        etChangeQuantity.setText(formattedValue);
        

        if (hasFocus) {
            FocusManager.requestFocusChange(etChangeQuantity, false, 50);
            

            etChangeQuantity.post(() -> {
                if (etChangeQuantity.hasFocus() && selStart >= 0 && selEnd >= 0) {
                    int textLength = etChangeQuantity.getText().length();
                    try {
                        etChangeQuantity.setSelection(
                            Math.min(selStart, textLength),
                            Math.min(selEnd, textLength)
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка восстановления позиции курсора: " + e.getMessage());
                    }
                }
            });
        }
        
        // Обновляем состояние кнопок
        btnChangeSeries.setEnabled(maxAvailable > 0);
        btnSave.setEnabled(true);
    }
    

    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewProducts.setVisibility(show ? View.GONE : View.VISIBLE);
        btnChangeSeries.setEnabled(!show);
        btnSave.setEnabled(true);
        btnCancel.setEnabled(!show);
    }
    
    private void onChangeSeriesButtonClicked() {
        //  Проверяем есть ли выбранные продукты
        if (!viewModel.hasSelectedItems()) {
            Toast.makeText(this, "Необходимо выбрать продукты для замены серии", Toast.LENGTH_SHORT).show();
            return;
        }
        
        //  Получаем и валидируем количество для замены
        String changeText = etChangeQuantity.getText().toString().trim();
        if (changeText.isEmpty()) {
            Toast.makeText(this, "Необходимо указать количество для замены серии", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double changeQuantity;
        try {
            changeQuantity = parseDoubleWithCommaSupport(changeText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверный формат числа", Toast.LENGTH_SHORT).show();
            return;
        }
        
        //  Валидируем количество через ViewModel
        if (!viewModel.validateChangeQuantity(changeQuantity)) {
            return; // Ошибка будет показана через ErrorMessage LiveData
        }
        
        //  Выполняем замену серии
        viewModel.performSeriesChange(changeQuantity);
    }

    private void onSaveButtonClicked() {
        // При сохранении удаляем резервную копию
        cleanupProductsBackup();

        ZnpSelectionViewModel.SeriesChangeResult result = viewModel.getLastSeriesChangeResult();
        
        Log.d(TAG, "Сохранение с результатом: обновлено продуктов: " + result.updatedProducts.size() + 
                   ", создано новых: " + result.newProducts.size() + 
                   ", изменено ID: " + result.changedProductLineIds.size());
        
        setResultAndFinish(true, result);
    }
    

    
    private void onCancelButtonClicked() {
        // Проверяем есть ли изменения или выбранные элементы
        if (changesApplied || viewModel.hasSelectedItems()) {
            showConfirmExitWithoutSavingDialog("отмена");
        } else {
            // Если изменений не было, просто удаляем резервную копию
            cleanupProductsBackup();
            viewModel.clearLastSeriesChangeResult();
            setResultAndFinish(false);
        }
    }
    
    private void onBackButtonClicked() {
        onBackPressed();
    }
    
    private void setResultAndFinish(boolean success) {
        setResultAndFinish(success, null);
    }
    
    private void setResultAndFinish(boolean success, ZnpSelectionViewModel.SeriesChangeResult result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("success", success);
        if (success && result != null) {
            // Передаем результат замены серии для SeriesSelectionActivity
            resultIntent.putExtra("action", "series_changed");
            resultIntent.putExtra("updatedProductsCount", result.updatedProducts.size());
            resultIntent.putExtra("newProductsCount", result.newProducts.size());
            resultIntent.putExtra("changedProductLineIds", result.changedProductLineIds.toArray(new String[0]));
            
            // Передаем данные для обновления серий
            resultIntent.putExtra("nomenclatureUuid", nomenclatureUuid);
            resultIntent.putExtra("targetSeriesUuid", targetSeriesUuid);
            
            // Сериализуем продукты в JSON для передачи в ProductsActivity
            try {
                com.google.gson.Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .create();
                String updatedProductsJson = gson.toJson(result.updatedProducts);
                String newProductsJson = gson.toJson(result.newProducts);
                
                resultIntent.putExtra("updatedProductsJson", updatedProductsJson);
                resultIntent.putExtra("newProductsJson", newProductsJson);
                
                Log.d(TAG, "Замена серии завершена успешно. Обновлено продуктов: " + result.updatedProducts.size() + 
                           ", создано новых: " + result.newProducts.size() + 
                           ". JSON данные подготовлены для передачи в ProductsActivity");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сериализации данных продуктов: " + e.getMessage(), e);
            }
            
            Log.d(TAG, "Замена серии завершена успешно. Обновлено продуктов: " + result.updatedProducts.size() + 
                       ", создано новых: " + result.newProducts.size() + 
                       ". Возвращаемся в SeriesSelectionActivity");
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    /**
     * Резервное копирование больше не нужно: данные в Realm не изменяются
     * до момента подтверждения пользователем. ViewModel хранит изменения в памяти.
     */
    private void createProductsBackup() {
        Log.d(TAG, "createProductsBackup: not needed with Realm, skipping");
    }

    private void restoreProductsFromBackup() {
        Log.d(TAG, "restoreProductsFromBackup: not needed with Realm, skipping");
    }

    private void cleanupProductsBackup() {
        Log.d(TAG, "cleanupProductsBackup: not needed with Realm, skipping");
    }
    
    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {

        if (changesApplied || viewModel.hasSelectedItems()) {
            showConfirmExitWithoutSavingDialog("выход");
        } else {
            cleanupProductsBackup();
            viewModel.clearLastSeriesChangeResult();
            setResultAndFinish(false);
        }
    }
    
    /**
     * Перехват всех событий касания на самом низком уровне
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {

            if (isTouchOutsideEditText(ev)) {
                Log.d(TAG, "Касание вне всех EditText, принудительно снимаем фокус");

                clearAllFocus();
                

                if (focusDummy != null) {
                    focusDummy.requestFocus();
                } else {
                    rootLayout.requestFocus();
                }
                boolean result = super.dispatchTouchEvent(ev);
                

                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    Log.d(TAG, "EditText все еще имеет фокус после обработки события, принудительно снимаем");
                    currentFocus.clearFocus();
                    if (focusDummy != null) {
                        focusDummy.requestFocus();
                    } else {
                        rootLayout.requestFocus();
                    }
                }
                
                return result;
            } else {
                Log.d(TAG, "Касание внутри EditText, продолжаем стандартную обработку");
            }
        }

        return super.dispatchTouchEvent(ev);
    }
    
    /**
     * Валидирует значение в поле количества для замены серии
     */
    private void validateChangeQuantity() {
        String text = etChangeQuantity.getText().toString().trim();
        
        if (text.isEmpty()) {
            return;
        }
        
        try {
            double changeValue = parseDoubleWithCommaSupport(text);
            

            Double maxAvailable = viewModel.getMaxAvailableQuantity().getValue();
            if (maxAvailable == null) {
                maxAvailable = 0.0;
            }
            

            int inputDecimalPlaces = getDecimalPlaces(text);
            
            String formattedValue = null;
            String message = null;
            

            if (changeValue < 0) {
                message = "Значение не может быть отрицательным";
                formattedValue = "0";
                if (inputDecimalPlaces > 0) {
                    String format = "%." + inputDecimalPlaces + "f";
                    formattedValue = String.format(format, 0.0);
                }
            }

            else if (changeValue > maxAvailable) {
                message = String.format("Значение %.2f превышает максимально доступное количество %.2f", 
                                      changeValue, maxAvailable);
                

                boolean isMaxValueInteger = (maxAvailable == Math.floor(maxAvailable));
                

                if (inputDecimalPlaces == 0) {

                    formattedValue = String.format("%.0f", Math.floor(maxAvailable));
                } else {

                    if (isMaxValueInteger) {
                        String format = "%." + inputDecimalPlaces + "f";
                        formattedValue = String.format(format, maxAvailable);
                    } else {
                        int maxValueDecimalPlaces = getDecimalPlaces(String.valueOf(maxAvailable));
                        int finalDecimalPlaces = Math.max(inputDecimalPlaces, maxValueDecimalPlaces);
                        String format = "%." + finalDecimalPlaces + "f";
                        formattedValue = String.format(format, maxAvailable);
                    }
                }
            }
            
            // Если нужно заменить значение
            if (formattedValue != null && message != null) {
                Log.d(TAG, "Заменяем невалидное значение '" + text + "' (знаков после запятой: " + inputDecimalPlaces + 
                           ") на '" + formattedValue + "'");
                
                // Показываем сообщение об ошибке
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                
                // Устанавливаем корректное значение
                etChangeQuantity.setText(formattedValue);
                etChangeQuantity.setSelection(etChangeQuantity.getText().length());
            }
        } catch (NumberFormatException e) {
            // Игнорируем некорректный ввод
        }
    }
    

    
    /**
     * Определяет количество знаков после запятой в строковом представлении числа
     */
    private int getDecimalPlaces(String numberString) {
        if (numberString == null || numberString.isEmpty()) {
            return 0;
        }
        
        int dotIndex = numberString.indexOf('.');
        if (dotIndex == -1) {
            return 0;
        }

        String afterDot = numberString.substring(dotIndex + 1);
        while (afterDot.endsWith("0") && afterDot.length() > 0) {
            afterDot = afterDot.substring(0, afterDot.length() - 1);
        }
        
        return afterDot.length();
    }
    
    /**
     * Безопасно парсит строку в double, поддерживая запятую как десятичный разделитель
     * @param text строка для парсинга
     * @return распарсенное число или исключение NumberFormatException если не удалось распарсить
     * @throws NumberFormatException если строка не является валидным числом
     */
    private double parseDoubleWithCommaSupport(String text) throws NumberFormatException {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Пустая строка");
        }

        String normalizedText = text.trim().replace(',', '.');
        return Double.parseDouble(normalizedText);
    }


         @Override
     protected void onDestroy() {
         FocusManager.cancelPendingFocusRequests();
         super.onDestroy();
     }

    /**
     * Показывает диалог подтверждения выхода без сохранения
     */
    private void showConfirmExitWithoutSavingDialog(String action) {
        String message;
        if (changesApplied && viewModel.hasSelectedItems()) {
            message = "У вас есть несохраненные изменения и выбранные продукты. Выйти без сохранения?";
        } else if (changesApplied) {
            message = "У вас есть несохраненные изменения. Выйти без сохранения?";
        } else if (viewModel.hasSelectedItems()) {
            message = "У вас есть выбранные продукты. Выйти без замены серии?";
        } else {
            message = "У вас есть несохраненные данные. Выйти без сохранения?";
        }
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage(message)
                .setPositiveButton("Да", (dialog, which) -> {
                    if (changesApplied) {
                        restoreProductsFromBackup();
                        viewModel.rollbackFreeBalanceChanges();
                        Log.d(TAG, "Изменения отменены при " + action + ", данные восстановлены");
                    } else {
                        cleanupProductsBackup();
                    }
                    viewModel.clearLastSeriesChangeResult();
                    
                    setResultAndFinish(false);
                })
                .setNegativeButton("Нет", null)
                .show();
    }
} 