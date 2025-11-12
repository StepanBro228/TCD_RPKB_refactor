package com.step.tcd_rpkb.UI.Serias.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Rect;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.Serias.adapter.SeriesSelectionAdapter;
import com.step.tcd_rpkb.UI.Serias.viewmodel.SeriesSelectionViewModel;
import com.step.tcd_rpkb.UI.ZnpSelection.activity.ZnpSelectionActivity;
import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import android.annotation.SuppressLint;

/**
 * Activity для подбора серий товара
 */
@AndroidEntryPoint
public class SeriesSelectionActivity extends BaseFullscreenActivity {

    private static final String TAG = "SeriesSelectionActivity";
    private static final int REQUEST_CODE_ZNP_SELECTION = 1001;

    private SeriesSelectionViewModel viewModel;
    private SeriesSelectionAdapter adapter;
    
    // UI компоненты
    private TextView tvNomenclatureName;
    private RecyclerView recyclerView;
    private Button btnSave;
    private Button btnCancel;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    
    // Для отображения статистики
    private TextView tvDocumentQuantityTotal;
    private TextView tvAllocatedQuantityTotal;
    private TextView tvAllocatedPercent;
    
    // ProgressBar для отображения процента распределения
    private ProgressBar progressAllocated;
    
    // Данные, полученные из Intent
    private String nomenclatureUuid;
    private String nomenclatureName;
    private String productLineId;
    private String moveUuid;
    private String wareHouse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_selection);
        
        // Инициализация UI компонентов
        initializeViews();
        
        // Получаем ViewModel
        viewModel = new ViewModelProvider(this).get(SeriesSelectionViewModel.class);
        
        // Получаем данные из Intent
        Intent intent = getIntent();
        if (intent != null) {
            nomenclatureUuid = intent.getStringExtra("nomenclatureUuid");
            nomenclatureName = intent.getStringExtra("nomenclatureName");
            productLineId = intent.getStringExtra("productLineId");
            moveUuid = intent.getStringExtra("moveUuid");
            wareHouse = intent.getStringExtra("wareHouse");
            double totalDocumentQuantity = intent.getDoubleExtra("totalQuantity", 0.0);
            
            // Проверяем наличие необходимых данных
            if (nomenclatureUuid == null || nomenclatureUuid.isEmpty() || moveUuid == null || moveUuid.isEmpty()) {
                Toast.makeText(this, "Ошибка: отсутствуют необходимые данные", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Устанавливаем название номенклатуры в заголовок
            tvNomenclatureName.setText(getString(R.string.series_selection_title, nomenclatureName));
            
            // Загружаем данные о сериях
            loadSeriesData(nomenclatureUuid, moveUuid, productLineId, totalDocumentQuantity);
        } else {
            Toast.makeText(this, "Ошибка: отсутствуют данные", Toast.LENGTH_SHORT).show();
            finish();
        }


        observeViewModel();
    }

    private void initializeViews() {
        // Инициализация основных компонентов
        tvNomenclatureName = findViewById(R.id.tv_nomenclature_name);
        recyclerView = findViewById(R.id.recycler_view_series);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        
        // Инициализация статистики
        tvDocumentQuantityTotal = findViewById(R.id.tv_document_quantity_total);
        tvAllocatedQuantityTotal = findViewById(R.id.tv_allocated_quantity_total);
        tvAllocatedPercent = findViewById(R.id.tv_allocated_percent);
        
        // Инициализация ProgressBar для прогресса распределения
        progressAllocated = findViewById(R.id.progress_allocated);
        
        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SeriesSelectionAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        

        int paddingInDp = 6;
        float density = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * density);
        recyclerView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
        recyclerView.setClipToPadding(false);
        

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int itemSpacingInDp = 2;
                int itemSpacingInPx = (int) (itemSpacingInDp * density);
                
                // Устанавливаем отступы
                outRect.left = itemSpacingInPx;
                outRect.right = itemSpacingInPx;
                outRect.bottom = itemSpacingInPx;

                if (position == 0) {
                    outRect.top = itemSpacingInPx;
                }
            }
        });
        
        // Установка обработчика нажатия на элемент серии
        adapter.setOnSeriesItemClickListener((seriesItem, position) -> {
            openZnpSelectionActivity(seriesItem);
        });
        
        // Настройка кнопок
        btnSave.setOnClickListener(v -> onSaveButtonClicked());
        btnCancel.setOnClickListener(v -> onCancelButtonClicked());
        btnBack.setOnClickListener(v -> onBackButtonClicked());
    }

    private void loadSeriesData(String nomenclatureUuid, String moveUuid, String productLineId, double totalDocumentQuantity) {
        if (nomenclatureUuid == null || moveUuid == null) {
            return;
        }
        showLoading(true);
        viewModel.loadSeriesData(nomenclatureUuid, moveUuid, productLineId, totalDocumentQuantity);
    }

    private void observeViewModel() {
        // Наблюдение за данными серий
        viewModel.getSeriesItems().observe(this, seriesItems -> {
            adapter.updateData(seriesItems);
            showLoading(false);
        });
        
        // Наблюдение за общим количеством в документе
        viewModel.getDocumentQuantity().observe(this, documentQuantity -> {
            tvDocumentQuantityTotal.setText(getString(R.string.document_quantity_total, documentQuantity));
            updateAllocatedPercentage();
        });
        
        // Наблюдение за распределенным количеством
        viewModel.getAllocatedQuantity().observe(this, allocatedQuantity -> {
            tvAllocatedQuantityTotal.setText(getString(R.string.allocated_quantity_total, allocatedQuantity));
            updateAllocatedPercentage();
        });
        
        // Наблюдение за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        
        // Наблюдение за событием успешного сохранения
        viewModel.getSaveSuccessEvent().observe(this, event -> {
            Boolean success = event.getContentIfNotHandled();
            if (success != null && success) {
                setResultAndFinish(true);
            }
        });
        
        // Наблюдение за состоянием загрузки
        viewModel.getIsLoading().observe(this, isLoading -> showLoading(isLoading));
    }
    
    /**
     * Открывает страницу замены серии товаров
     * @param seriesItem элемент серии (целевая серия для замены)
     */
    private void openZnpSelectionActivity(SeriesItem seriesItem) {
        Intent intent = new Intent(this, ZnpSelectionActivity.class);
        
        // Передаем данные о номенклатуре
        intent.putExtra("nomenclatureUuid", nomenclatureUuid);
        intent.putExtra("nomenclatureName", nomenclatureName);
        intent.putExtra("moveUuid", moveUuid);
        intent.putExtra("wareHouse", wareHouse);
        Log.d(TAG, "Открываем ZnpSelectionActivity для серии: " + seriesItem.getSeriesName() +
                " с свободным остатком: " + seriesItem.getFreeBalance());
        
        // Передаем данные о целевой серии
        intent.putExtra("targetSeriesUuid", seriesItem.getSeriesUuid());
        intent.putExtra("targetSeriesName", seriesItem.getSeriesName());
        intent.putExtra("freeBalance", seriesItem.getFreeBalance());
        
        Log.d(TAG, "Передаем склад отправителя: " + wareHouse);
        
        startActivityForResult(intent, REQUEST_CODE_ZNP_SELECTION);
    }
    
    /**
     * Обновляет процент распределения
     */
    private void updateAllocatedPercentage() {
        Double documentQuantity = viewModel.getDocumentQuantity().getValue();
        Double allocatedQuantity = viewModel.getAllocatedQuantity().getValue();
        
        if (documentQuantity == null || documentQuantity <= 0) {
            tvAllocatedPercent.setText("0%");
            progressAllocated.setProgress(0);
            return;
        }
        
        if (allocatedQuantity == null) {
            allocatedQuantity = 0.0;
        }
        
        // Расчет процента распределения
        int percentage = (int) Math.round((allocatedQuantity / documentQuantity) * 100);
        tvAllocatedPercent.setText(percentage + "%");

        progressAllocated.setProgress(percentage);

        int color;
        if (percentage < 30) {
            color = getResources().getColor(R.color.colorError);
            tvAllocatedPercent.setTextColor(color);
        } else if (percentage < 70) {
            color = getResources().getColor(R.color.colorWarning);
            tvAllocatedPercent.setTextColor(color);
        } else {
            color = getResources().getColor(R.color.colorSuccess);
            tvAllocatedPercent.setTextColor(color);
        }
        

        progressAllocated.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
    
    private void onSaveButtonClicked() {
        if (viewModel.getAllocatedQuantity().getValue() == null || 
            viewModel.getAllocatedQuantity().getValue() <= 0) {
            Toast.makeText(this, "Необходимо распределить серии", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Проверяем, что распределенное количество соответствует количеству в документе
        Double documentQuantity = viewModel.getDocumentQuantity().getValue();
        Double allocatedQuantity = viewModel.getAllocatedQuantity().getValue();
        
        if (documentQuantity != null && allocatedQuantity != null) {
            double difference = Math.abs(documentQuantity - allocatedQuantity);
            

            if (difference != 0) {

                new AlertDialog.Builder(this)
                        .setTitle("Предупреждение")
                        .setMessage("Распределенное количество (" + String.format("%.2f", allocatedQuantity) + 
                                ") не соответствует количеству в документе (" + String.format("%.2f", documentQuantity) + 
                                ").\n\nПродолжить сохранение?")
                        .setPositiveButton("Да", (dialog, which) -> {
                            viewModel.saveAllocation(nomenclatureUuid, moveUuid);
                        })
                        .setNegativeButton("Нет", null)
                        .show();
                return;
            }
        }
        

        viewModel.saveAllocation(nomenclatureUuid, moveUuid);
    }
    
    private void onCancelButtonClicked() {

        if (viewModel.hasUnsavedChanges()) {
            showConfirmExitWithoutSavingDialog();
        } else {
            setResultAndFinish(false);
        }
    }
    
    private void onBackButtonClicked() {
        onBackPressed();
    }
    
    private void setResultAndFinish(boolean success) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("success", success);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    /**
     * Обновляет данные серий из кеша после замены серий
     */
    private void refreshSeriesDataFromCache() {
        if (nomenclatureUuid != null && moveUuid != null) {

            Double currentDocumentQuantity = viewModel.getDocumentQuantity().getValue();
            double totalQuantity = currentDocumentQuantity != null ? currentDocumentQuantity : 0.0;
            viewModel.loadSeriesData(nomenclatureUuid, moveUuid, productLineId, totalQuantity);
        }
    }
    
    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // Проверяем есть ли несохраненные изменения
        if (viewModel.hasUnsavedChanges()) {
            showConfirmExitWithoutSavingDialog();
        } else {
            setResultAndFinish(false);
        }
    }
    
    private void showConfirmExitWithoutSavingDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("У вас есть несохраненные изменения. Выйти без сохранения?")
                .setPositiveButton("Да", (dialog, which) -> {
                    // При отмене восстанавливаем оригинальные данные
                    viewModel.restoreOriginalSeriesData();
                    setResultAndFinish(false);
                })
                .setNegativeButton("Нет", null)
                .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_ZNP_SELECTION && resultCode == RESULT_OK) {
            if (data != null) {
                boolean success = data.getBooleanExtra("success", false);
                if (success) {
                    // Получаем данные о замене серии
                    String action = data.getStringExtra("action");
                    int updatedProductsCount = data.getIntExtra("updatedProductsCount", 0);
                    int newProductsCount = data.getIntExtra("newProductsCount", 0);
                    String[] changedProductLineIds = data.getStringArrayExtra("changedProductLineIds");
                    
                    // Получаем JSON данные для передачи в ProductsActivity
                    String updatedProductsJson = data.getStringExtra("updatedProductsJson");
                    String newProductsJson = data.getStringExtra("newProductsJson");
                    
                    Log.d(TAG, "Замена серии завершена. Обновлено продуктов: " + updatedProductsCount +
                               ", создано новых: " + newProductsCount + 
                               ", изменено ID: " + (changedProductLineIds != null ? changedProductLineIds.length : 0));
                    
                    if ("series_changed".equals(action)) {
                        // Обновляем данные серий из кеша
                        refreshSeriesDataFromCache();

                        viewModel.markAsChanged();
                        
                        Toast.makeText(this, "Замена серии успешно выполнена", Toast.LENGTH_SHORT).show();
                        
                        // Передаем данные о замене серии в ProductsActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("success", true);
                        resultIntent.putExtra("action", "series_changed");
                        resultIntent.putExtra("updatedProductsCount", updatedProductsCount);
                        resultIntent.putExtra("newProductsCount", newProductsCount);
                        resultIntent.putExtra("changedProductLineIds", changedProductLineIds);
                        resultIntent.putExtra("updatedProductsJson", updatedProductsJson);
                        resultIntent.putExtra("newProductsJson", newProductsJson);
                        
                        setResult(RESULT_OK, resultIntent);
                        

                    }
                } else {

                    Toast.makeText(this, "Замена серии отменена", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


} 