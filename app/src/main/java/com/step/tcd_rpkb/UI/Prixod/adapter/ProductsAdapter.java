package com.step.tcd_rpkb.UI.Prixod.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.UI.Prixod.activity.ProductsActivity;
import com.step.tcd_rpkb.domain.model.Product;
import com.step.tcd_rpkb.utils.FocusManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.InputinfoViewHolder> {
    private static final String TAG = "InputInfoAdapter";
    
    private List<Product> products;
    private Set<String> currentErrorUuids = new HashSet<>();

    private RecyclerView recyclerView;
    private ProductsActivity productsActivity;
    private ObjectAnimator currentAnimator;
    private boolean isAnimatingView = false;
    
    // переменные для отслеживания выбора
    private int selectedPosition = RecyclerView.NO_POSITION;
    private boolean selectionEnabled = true;
    
    // Режим "только чтение"
    private boolean readOnlyMode = false;
    
    // Статус перемещения для управления видимостью кнопок
    private String moveStatus = null;
    
    public interface OnProductDataChangedListener {
        void onProductDataConfirmed(String productLineId, double value, int positionInAdapter, boolean isValid, boolean byEnterKey);
    }
    private OnProductDataChangedListener dataChangedListener;
    
    // интерфейс для обработки выбора контейнера
    public interface OnItemSelectionListener {
        void onItemSelected(int position, Product product);
        void onItemDeselected();
    }
    private OnItemSelectionListener itemSelectionListener;
    
    // Сеттер для слушателя выбора
    public void setItemSelectionListener(OnItemSelectionListener listener) {
        this.itemSelectionListener = listener;
    }
    

    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    // Возвращает выбранный продукт или null, если ничего не выбрано
    public Product getSelectedProduct() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < products.size()) {
            return products.get(selectedPosition);
        }
        return null;
    }
    
    // Включает или выключает возможность выбора контейнеров
    public void setSelectionEnabled(boolean enabled) {
        if (selectionEnabled != enabled) {
            selectionEnabled = enabled;
            notifyDataSetChanged();
        }
    }
    
    // Очищает выбор контейнера
    public void clearSelection() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(oldSelectedPosition);
            if (itemSelectionListener != null) {
                itemSelectionListener.onItemDeselected();
            }
        }
    }
    
    // Очищает выбор контейнера БЕЗ вызова колбэка (тихая очистка)
    public void clearSelectionSilently() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(oldSelectedPosition);
        }
    }
    
    // Установка выбора на определенную позицию
    public void selectItem(int position) {
        if (position >= 0 && position < products.size() && selectionEnabled) {
            if (selectedPosition != RecyclerView.NO_POSITION) {
                int oldSelectedPosition = selectedPosition;
                selectedPosition = RecyclerView.NO_POSITION;
                notifyItemChanged(oldSelectedPosition);
            }
            
            // Устанавливаем новую выбранную позицию
            selectedPosition = position;
            notifyItemChanged(position);
            

            if (itemSelectionListener != null) {
                itemSelectionListener.onItemSelected(position, products.get(position));
            }
        }
    }
    
    /**
     * Устанавливает режим "только чтение"
     * @param readOnly true для включения режима "только чтение"
     */
    public void setReadOnlyMode(boolean readOnly) {

        this.readOnlyMode = readOnly;
        Log.d(TAG, "Режим 'только чтение' установлен: " + readOnly);
        
        // Обновляем все элементы
        notifyDataSetChanged();
        
        // Принудительно обновляем видимые элементы
        if (recyclerView != null) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View view = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                if (viewHolder instanceof InputinfoViewHolder) {
                    InputinfoViewHolder holder = (InputinfoViewHolder) viewHolder;
                    if (holder.list_carried != null) {
                        holder.list_carried.setEnabled(!readOnly);
                        holder.list_carried.setFocusable(!readOnly);
                        holder.list_carried.setFocusableInTouchMode(!readOnly);
                        holder.list_carried.setAlpha(readOnly ? 0.5f : 1.0f);
                        Log.d(TAG, "Принудительно обновлен EditText для позиции " + viewHolder.getAdapterPosition());
                    }
                }
            }
        }
    }
    

    
    /**
     * Устанавливает статус перемещения для управления видимостью кнопок
     * @param status статус перемещения
     */
    public void setMoveStatus(String status) {
        this.moveStatus = status;
        Log.d(TAG, "Статус перемещения установлен: " + status);
        
        // Обновляем видимость кнопок
        notifyDataSetChanged();
    }

    /**
     * Безопасно парсит строку в double, поддерживая запятую как десятичный разделитель
     * @param text строка для парсинга
     * @return распарсенное число или 0.0 если не удалось распарсить
     */
    private double parseDoubleWithCommaSupport(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            String normalizedText = text.trim().replace(',', '.');
            return Double.parseDouble(normalizedText);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Не удалось распарсить число: " + text, e);
            return 0.0;
        }
    }

    public ProductsAdapter(ProductsActivity productsActivity, List<Product> products, OnProductDataChangedListener dataChangedListener) {
        this.productsActivity = productsActivity;
        this.products = new ArrayList<>(products != null ? products : new ArrayList<>());
        this.dataChangedListener = dataChangedListener;
        
        if (productsActivity != null) {
            this.recyclerView = productsActivity.findViewById(R.id.rv_info);
        }
    }

    
    public void updateData(List<Product> newProducts, Set<String> errorUuids) {
        updateData(newProducts, errorUuids, false);
    }
    
    /**
     * Обновляет данные адаптера
     * @param newProducts новый список продуктов
     * @param errorUuids множество UUID с ошибками валидации
     * @param forceUpdate принудительное полное обновление (для случаев замены серий)
     */
    public void updateData(List<Product> newProducts, Set<String> errorUuids, boolean forceUpdate) {
        Log.d(TAG, "updateData: Получены новые данные: " + 
              (newProducts != null ? newProducts.size() : 0) + " продуктов");
              
        // Логируем несколько первых продуктов с их taken значениями
        if (newProducts != null && !newProducts.isEmpty()) {
            int logCount = Math.min(5, newProducts.size());
            for (int i = 0; i < logCount; i++) {
                Product product = newProducts.get(i);
                Log.d(TAG, "updateData: Продукт [" + i + "] '" + product.getNomenclatureName() + 
                      "' taken=" + product.getTaken());
            }
            if (newProducts.size() > 5) {
                Log.d(TAG, "updateData: ... и еще " + (newProducts.size() - 5) + " продуктов");
            }
        }
        
        // Если новый список null, просто выходим
        if (newProducts == null) {
            Log.d(TAG, "updateData: Получен null список, обновление не выполняется");
            return;
        }
        
        // Создаем список измененных позиций
        Set<Integer> changedPositions = new HashSet<>();
        

        boolean productsChanged = products.size() != newProducts.size();
        
        if (!productsChanged) {
            // Проверяем, изменились ли продукты или их значения taken
            for (int i = 0; i < products.size() && i < newProducts.size(); i++) {
                Product oldProduct = products.get(i);
                Product newProduct = newProducts.get(i);
                
                // Проверяем изменение UUID (изменился состав списка)
                if (!oldProduct.getNomenclatureUuid().equals(newProduct.getNomenclatureUuid())) {
                    productsChanged = true;
                    break;
                }
                
                // Проверку изменений серии для корректного обновления после замены серий
                boolean seriesChanged = !java.util.Objects.equals(oldProduct.getSeriesName(), newProduct.getSeriesName()) ||
                                      !java.util.Objects.equals(oldProduct.getSeriesUuid(), newProduct.getSeriesUuid()) ||
                                      !java.util.Objects.equals(oldProduct.getProductLineId(), newProduct.getProductLineId());
                
                if (seriesChanged) {
                    changedPositions.add(i);
                    Log.d(TAG, "updateData: Обнаружено изменение серии для продукта '" + 
                          newProduct.getNomenclatureName() + "': серия '" + 
                          oldProduct.getSeriesName() + "' -> '" + newProduct.getSeriesName() + "'");
                }
                
                // Проверяем изменение значения taken
                if (oldProduct.getTaken() != newProduct.getTaken()) {
                    changedPositions.add(i);
                    Log.d(TAG, "updateData: Обнаружено изменение taken для продукта '" + 
                          newProduct.getNomenclatureName() + "': " + 
                          oldProduct.getTaken() + " -> " + newProduct.getTaken());
                }
            }
        }
        
        // Проверяем изменения в ошибках
        if (errorUuids != null) {
            for (int i = 0; i < products.size() && i < newProducts.size(); i++) {
                String uuid = products.get(i).getNomenclatureUuid();
                boolean wasError = currentErrorUuids != null && currentErrorUuids.contains(uuid);
                boolean isError = errorUuids.contains(uuid);
                if (wasError != isError) {
                    changedPositions.add(i);
                    Log.d(TAG, "updateData: Обнаружено изменение статуса ошибки для продукта '" + 
                          products.get(i).getNomenclatureName() + "': " + 
                          wasError + " -> " + isError);
                }
            }
        }
        
        // Обновляем данные адаптера
        products.clear();
        products.addAll(newProducts);
        
        if (errorUuids != null) {
            currentErrorUuids.clear();
            currentErrorUuids.addAll(errorUuids);
        }
        

        if (productsChanged || forceUpdate) {
            Log.d(TAG, forceUpdate ? "Принудительное полное обновление списка" : "Полное обновление списка (изменились продукты)");
            notifyDataSetChanged();
        } else if (!changedPositions.isEmpty()) {
            Log.d(TAG, "Точечное обновление для " + changedPositions.size() + " позиций");
            for (int position : changedPositions) {
                notifyItemChanged(position);
            }
        } else {
            Log.d(TAG, "Обновление не требуется (данные не изменились)");
        }
    }

    /**
     * Создает анимацию тряски для view при ошибке валидации
     * @param view View для анимации
     * @param restoreFocusAfter нужно ли восстанавливать фокус после анимации
     */
    public void shakeView(final View view, final boolean restoreFocusAfter) {

        if (view == null || !view.isAttachedToWindow()) {
            Log.d(TAG, "shakeView: невозможно запустить анимацию, view недоступно");
            return;
        }
        
        if (isAnimatingView) {
            Log.d(TAG, "shakeView: невозможно запустить анимацию, уже выполняется другая анимация");
            return;
        }
        

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                isAnimatingView = true;
                Log.d(TAG, "Флаг isAnimatingView установлен в true");
                

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAnimatingView) {
                        Log.d(TAG, "Сработал таймаут сброса флага isAnimatingView");
                        isAnimatingView = false;
                    }
                }, 1000);
                

                final float originalX = view.getTranslationX();
                
                cancelCurrentAnimation();
                
                currentAnimator = ObjectAnimator.ofFloat(view, "translationX", 
                        0, -10f, 10f, -10f, 10f, -5f, 5f, -5f, 5f, 0);
                currentAnimator.setDuration(400);
                currentAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                
                currentAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {

                        isAnimatingView = false;
                        Log.d(TAG, "onAnimationEnd: флаг isAnimatingView сброшен");
                        

                        if (view.isAttachedToWindow()) {
                            view.setTranslationX(originalX);
                        }
                        

                        if (restoreFocusAfter && view instanceof EditText && view.isAttachedToWindow()) {
                            final EditText editText = (EditText) view;
                            

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (view.isAttachedToWindow()) {
                                    FocusManager.requestFocusChange(editText, true, 50, 2);
                                }
                            }, 100);
                        }
                    }
                    
                    @Override
                    public void onAnimationCancel(Animator animation) {

                        if (view.isAttachedToWindow()) {
                            view.setTranslationX(originalX);
                        }
                        isAnimatingView = false;
                        Log.d(TAG, "onAnimationCancel: флаг isAnimatingView сброшен");
                        currentAnimator = null;
                    }
                });
                

                if (view.getContext() != null) {
                    try {
                        android.os.VibrationEffect effect = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                            android.os.Vibrator vibrator = (android.os.Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null && vibrator.hasVibrator()) {
                                vibrator.vibrate(effect);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при создании вибрации: " + e.getMessage());
                    }
                }
                
                currentAnimator.start();
            } catch (Exception e) {

                isAnimatingView = false;
                currentAnimator = null;
                Log.e(TAG, "Ошибка при создании анимации тряски: " + e.getMessage());
            }
        });
    }
    

    
    /**
     * Отменяет текущую анимацию, если она выполняется
     */
    public void cancelCurrentAnimation() {
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
        }
        currentAnimator = null;
    }
    
    /**
     * Отменяет все анимации и запросы фокуса.
     * Может использоваться внешними компонентами при необходимости
     * прекратить все визуальные эффекты адаптера.
     */
    public void cancelAllAnimations() {
        cancelCurrentAnimation();
        isAnimatingView = false;
    }

    /**
     * Проверяет, выполняется ли в данный момент анимация
     * @return true если в данный момент выполняется анимация, false в противном случае
     */
    public boolean isAnimatingView() {
        return isAnimatingView;
    }

    public class InputinfoViewHolder extends RecyclerView.ViewHolder {
        TextView list_name, list_mesure, list_amount;
        TextView list_series;
        TextView sender_storage;
        TextView reserve_document;
        TextView cell_balance, series_balance, free_balance, total_balance;
        public EditText list_carried;
        View itemViewContainer;
        View seriesStorageContainer;
        View takeEditTextContainer;

        public InputinfoViewHolder(@NonNull View itemView) {
            super(itemView);
            list_name = itemView.findViewById(R.id.list_name);
            list_mesure = itemView.findViewById(R.id.list_mesure);
            list_amount = itemView.findViewById(R.id.list_amount);
            list_series = itemView.findViewById(R.id.list_series);
            sender_storage = itemView.findViewById(R.id.sender_storage);
            reserve_document = itemView.findViewById(R.id.reserve_document);
            cell_balance = itemView.findViewById(R.id.cell_balance);
            series_balance = itemView.findViewById(R.id.series_balance);
            free_balance = itemView.findViewById(R.id.free_balance);
            total_balance = itemView.findViewById(R.id.total_balance);
            list_carried = itemView.findViewById(R.id.carried);
            itemViewContainer = itemView.findViewById(R.id.item_container);
            seriesStorageContainer = itemView.findViewById(R.id.series_storage_container);
            takeEditTextContainer = itemView.findViewById(R.id.take_editText_container);
            
            try {
                list_carried.setShowSoftInputOnFocus(false);
            } catch (Exception e) {
                Log.w(TAG, "Ошибка при вызове setShowSoftInputOnFocus: " + e.getMessage());
            }
        }
        
        @SuppressLint("DefaultLocale")
        public void bind(final Product product, final Set<String> errorUuids) {
            list_name.setText(product.getNomenclatureName());
            

            if ("Комплектуется".equals(moveStatus)) {
                takeEditTextContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "Поле 'Взял' показано для статуса: " + moveStatus);
            } else {
                takeEditTextContainer.setVisibility(View.GONE);
                Log.d(TAG, "Поле 'Взял' скрыто для статуса: " + moveStatus);
            }
            

            list_carried.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            
            boolean hasSeriesName = product.getSeriesName() != null && !product.getSeriesName().isEmpty();
            boolean hasSenderStorage = product.getSenderStorageName() != null && !product.getSenderStorageName().isEmpty();
            boolean hasReserveDocument = product.getReserveDocumentName() != null && !product.getReserveDocumentName().isEmpty();
            
            // Серия
            TextView listSeriesLabel = itemView.findViewById(R.id.list_series_label);
                listSeriesLabel.setVisibility(View.VISIBLE);
                list_series.setText(product.getSeriesName().trim());
                list_series.setVisibility(View.VISIBLE);


            // Ячейка
            if (hasSenderStorage) {
                sender_storage.setText(product.getSenderStorageName().trim());
                sender_storage.setVisibility(View.VISIBLE);
            } else {
                sender_storage.setVisibility(View.GONE);
            }
            
            // Документ резерва
            if (hasReserveDocument) {
                String prefix = "Резерв: ";
                String documentName = product.getReserveDocumentName();
                
                android.text.SpannableString spannableString = new android.text.SpannableString(prefix + documentName);
                
                spannableString.setSpan(
                    new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    prefix.length(), 
                    prefix.length() + documentName.length(), 
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                reserve_document.setText(spannableString);
                reserve_document.setVisibility(View.VISIBLE);
            } else {
                reserve_document.setVisibility(View.GONE);
            }
            
            if (hasSeriesName || hasSenderStorage) {
                seriesStorageContainer.setVisibility(View.VISIBLE);
            } else {
                seriesStorageContainer.setVisibility(View.GONE);
            }
            
            list_amount.setText(String.format("%.1f", product.getQuantity()));
            list_mesure.setText(product.getUnitName());
            
            cell_balance.setText(String.format("%.1f", product.getFreeBalanceInCell()));
            series_balance.setText(String.format("%.1f", product.getFreeBalanceBySeries()));
            free_balance.setText(String.format("%.1f", product.getFreeBalance()));
            total_balance.setText(String.format("%.1f", product.getTotalBalance()));
            

            double takenValue = product.getTaken();
            String productName = product.getNomenclatureName();
            
            Log.d(TAG, "BIND: Продукт '" + productName + "' имеет taken=" + takenValue);
            

            if (takenValue > 0) {
                String formattedValue = takenValue == Math.floor(takenValue) ? 
                    String.valueOf((int)takenValue) : 
                    String.valueOf(takenValue);
                

                String currentText = list_carried.getText().toString();
                if (!formattedValue.equals(currentText)) {
                    list_carried.setText(formattedValue);
                    Log.d(TAG, "BIND: Обновлен текст в EditText для продукта '" + productName + 
                          "' с '" + currentText + "' на '" + formattedValue + "'");
                } else {
                    Log.d(TAG, "BIND: Текст в EditText для продукта '" + productName + 
                          "' уже соответствует значению taken=" + takenValue);
                }
            } else {
                if (!list_carried.getText().toString().isEmpty()) {
                    list_carried.setText("");
                    Log.d(TAG, "BIND: Очищено поле EditText для продукта '" + 
                          productName + "' (taken=" + takenValue + " <= 0)");
                }
            }

            // Устанавливаем цвет фона в зависимости от наличия ошибки ИЛИ состояния фокуса
            if (errorUuids != null && errorUuids.contains(product.getProductLineId())) {
                list_carried.setBackgroundResource(R.drawable.edit_text_error);
            } else if (list_carried.hasFocus()){
                list_carried.setBackgroundResource(R.drawable.edit_text_focused);
            } else {
                list_carried.setBackgroundResource(R.drawable.edit_text_def);
            }
            

            int adapterPosition = getAdapterPosition();
            if (adapterPosition == selectedPosition) {
                itemViewContainer.setBackgroundResource(R.drawable.selected_item_border);
            } else {
                itemViewContainer.setBackgroundResource(R.drawable.rounded_container_background);
            }
            

            itemViewContainer.setOnClickListener(v -> {
                if (!selectionEnabled || readOnlyMode) return;
                
                int currentPos = getAdapterPosition();
                

                if (currentPos == selectedPosition) {
                    selectedPosition = RecyclerView.NO_POSITION;
                    notifyItemChanged(currentPos);
                    if (itemSelectionListener != null) {
                        itemSelectionListener.onItemDeselected();
                    }
                } 

                else {

                    int oldSelectedPosition = selectedPosition;
                    if (oldSelectedPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(oldSelectedPosition);
                    }
                    

                    selectedPosition = currentPos;
                    notifyItemChanged(selectedPosition);
                    
                    if (itemSelectionListener != null) {
                        itemSelectionListener.onItemSelected(currentPos, product);
                    }
                }
            });

            // Настройка EditText в зависимости от режима "только чтение"
            if (readOnlyMode) {
                // В режиме "только чтение" блокируем EditText
                list_carried.setEnabled(false);
                list_carried.setFocusable(false);
                list_carried.setFocusableInTouchMode(false);
                list_carried.setAlpha(0.6f);
                Log.d(TAG, "EditText заблокирован в режиме 'только чтение' для продукта: " + 
                      product.getNomenclatureName());
            } else {
                // В обычном режиме разрешаем редактирование
                list_carried.setEnabled(true);
                list_carried.setAlpha(1.0f);
                

                if (productsActivity != null) {
                    try {

                        list_carried.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                                                 android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                                                 android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                        list_carried.setFocusable(true);
                        list_carried.setFocusableInTouchMode(true);
                        list_carried.setShowSoftInputOnFocus(false);
                        list_carried.setCursorVisible(true);
                        list_carried.setLongClickable(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при настройке числового поля: " + e.getMessage());
                    }
                }
            }

            list_carried.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (v == null || readOnlyMode) return;
                    

                    EditText editText = (EditText) v;
                    

                    double carriedValue = 0;
                    String valueStr = list_carried.getText().toString();

                    if (!valueStr.isEmpty()) {
                        carriedValue = parseDoubleWithCommaSupport(valueStr);
                    }
                    

                    final double finalCarriedValue = carriedValue;
                    final Product currentProduct = product;
                    final int currentPosition = getAdapterPosition();
                    
                    if (hasFocus) {

                        

                        editText.setCursorVisible(true);
                        

                        if (currentErrorUuids != null && currentErrorUuids.contains(product.getProductLineId())) {
                             editText.setBackgroundResource(R.drawable.edit_text_error);
                        } else {
                            editText.setBackgroundResource(R.drawable.edit_text_focused);
                        }
                        
                        String currentValue = editText.getText().toString();

                        if (!currentValue.equals("0") && !currentValue.isEmpty()) {
                            editText.setSelection(currentValue.length());
                        } else {
                            editText.setText("");
                        }
                        

                        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        }
                        
                    } else {
                        if (currentErrorUuids != null && currentErrorUuids.contains(product.getProductLineId())) {
                            editText.setBackgroundResource(R.drawable.edit_text_error);
                        } else {
                            editText.setBackgroundResource(R.drawable.edit_text_def);
                        }
                        

                        final double originalTakenValue = product.getTaken();
                        final boolean valueChanged = originalTakenValue != finalCarriedValue;
                        

                        View currentFocus = productsActivity != null ? productsActivity.getCurrentFocus() : null;
                        boolean focusMovedToOtherEditTextInRecyclerView = 
                            currentFocus != null && 
                            currentFocus instanceof EditText && 
                            currentFocus != editText && 
                            recyclerView != null &&
                            isViewInsideViewGroup(recyclerView, currentFocus);
                        

                        if (dataChangedListener != null && valueChanged) {
                            Log.d(TAG, "Значение изменено для " + currentProduct.getNomenclatureName() + 
                                  ": " + originalTakenValue + " -> " + finalCarriedValue);
                            

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                                if (!focusMovedToOtherEditTextInRecyclerView || 
                                    (currentFocus != null && currentFocus.hasFocus())) {
                                    
                                    Log.d(TAG, "Подтверждение измененных данных при потере фокуса для " + 
                                          currentProduct.getNomenclatureName() + ": " + finalCarriedValue);
                                          
                                    dataChangedListener.onProductDataConfirmed(
                                        currentProduct.getProductLineId(),
                                        finalCarriedValue,
                                        currentPosition,
                                        false,
                                        false
                                    );
                                }
                            }, 100);
                        } 

                        else if (dataChangedListener != null && !focusMovedToOtherEditTextInRecyclerView) {
                            Log.d(TAG, "Подтверждение неизмененных данных при потере фокуса для " + 
                                  currentProduct.getNomenclatureName() + ": " + finalCarriedValue);
                                  
                            dataChangedListener.onProductDataConfirmed(
                                currentProduct.getProductLineId(), 
                                finalCarriedValue, 
                                currentPosition, 
                                false,
                                false
                            );
                        }
                    }
                }
            });
            
            // Добавляем обработчик нажатия Enter
            list_carried.setOnEditorActionListener((v, actionId, event) -> {
                if (readOnlyMode) return false;
                
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    

                    list_carried.setCursorVisible(true);
                    
                    String valueStr = list_carried.getText().toString();
                    double carriedValue = 0;
                    
                    if (!valueStr.isEmpty()) {
                        carriedValue = parseDoubleWithCommaSupport(valueStr);
                    }
                    

                    final int currentPosition = getAdapterPosition();
                    

                    final double finalCarriedValue = carriedValue;
                    

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (list_carried.isAttachedToWindow()) {
                            list_carried.setCursorVisible(true);
                        }
                        

                        if (dataChangedListener != null) {
                            Log.d(TAG, "Enter нажат, передаем значение " + finalCarriedValue + 
                                  " для " + product.getNomenclatureName() + " на позиции " + currentPosition);
                                  
                            dataChangedListener.onProductDataConfirmed(
                                product.getProductLineId(), 
                                finalCarriedValue, 
                                currentPosition, 
                                false,
                                true
                            );
                        }
                        
                        // Дополнительная проверка видимости курсора после обработки
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (list_carried.isAttachedToWindow() && list_carried.hasFocus()) {
                                list_carried.setCursorVisible(true);
                            }
                        }, 100);
                    }, 10);
                    
                    return true;
                }
                
                return false;
            });
        }
    }


    
    @NonNull
    @Override
    public InputinfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_products, parent, false);
        return new InputinfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InputinfoViewHolder holder, int position) {
        if (products != null && position < products.size()) {
            Product product = products.get(position);
            holder.bind(product, currentErrorUuids);
            
            // Дополнительная проверка режима "только чтение" после привязки данных
            if (readOnlyMode && holder.list_carried != null) {
                // Принудительно применяем режим "только чтение" к EditText
                holder.list_carried.setEnabled(false);
                holder.list_carried.setFocusable(false);
                holder.list_carried.setFocusableInTouchMode(false);
                holder.list_carried.setAlpha(0.6f);
                Log.d(TAG, "Принудительно применен режим 'только чтение' в onBindViewHolder для позиции " + position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    private boolean isViewInsideViewGroup(ViewGroup parent, View view) {
        if (parent == null || view == null) {
            return false;
        }
        
        // Проверяем является ли parent родителем view или любым предком
        ViewParent currentParent = view.getParent();
        while (currentParent != null) {
            if (currentParent == parent) {
                return true;
            }
            currentParent = currentParent.getParent();
        }
        
        return false;
    }
}
