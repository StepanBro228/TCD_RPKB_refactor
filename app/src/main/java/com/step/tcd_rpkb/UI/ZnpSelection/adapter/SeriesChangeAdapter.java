package com.step.tcd_rpkb.UI.ZnpSelection.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.domain.model.SeriesChangeItem;

import java.util.List;

/**
 * Адаптер для отображения списка продуктов для замены серии
 */
public class SeriesChangeAdapter extends RecyclerView.Adapter<SeriesChangeAdapter.SeriesChangeViewHolder> {
    
    private static final String TAG = "SeriesChangeAdapter";
    
    private List<SeriesChangeItem> seriesChangeItems;
    private OnSelectionChangeListener selectionChangeListener;
    
    /**
     * Интерфейс для обратного вызова при изменении выбора
     */
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int position, boolean isSelected);
    }
    
    /**
     * Конструктор адаптера
     */
    public SeriesChangeAdapter(List<SeriesChangeItem> seriesChangeItems) {
        this.seriesChangeItems = seriesChangeItems;
    }
    
    /**
     * Устанавливает слушатель изменения выбора
     */
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }
    
    /**
     * Обновляет данные в адаптере
     */
    public void updateData(List<SeriesChangeItem> newSeriesChangeItems) {
        Log.d(TAG, "Обновление данных адаптера. Новых элементов: " + (newSeriesChangeItems != null ? newSeriesChangeItems.size() : 0));
        this.seriesChangeItems = newSeriesChangeItems;
        notifyDataSetChanged();
    }
    
    /**
     * Очищает все выбранные элементы
     */
    public void clearSelection() {
        if (seriesChangeItems != null) {
            for (SeriesChangeItem item : seriesChangeItems) {
                item.setSelected(false);
            }
            notifyDataSetChanged();
        }
    }
    
    @NonNull
    @Override
    public SeriesChangeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_series_change, parent, false);
        return new SeriesChangeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SeriesChangeViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder для позиции: " + position);
        SeriesChangeItem item = seriesChangeItems.get(position);
        Log.d(TAG, "Привязка продукта: " + item.getProductLineId());
        holder.bind(item, position, selectionChangeListener);
    }
    
    @Override
    public int getItemCount() {
        int count = seriesChangeItems != null ? seriesChangeItems.size() : 0;
        Log.d(TAG, "getItemCount() возвращает: " + count);
        return count;
    }
    
    /**
     * ViewHolder для элемента продукта
     */
    static class SeriesChangeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSeriesName;
        private TextView tvZnpName;
        private TextView tvQuantityToProcure;
        private CheckBox checkBoxSelect;
        
        public SeriesChangeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeriesName = itemView.findViewById(R.id.tv_series_name);
            tvZnpName = itemView.findViewById(R.id.tv_znp_name);
            tvQuantityToProcure = itemView.findViewById(R.id.tv_quantity_to_procure);
            checkBoxSelect = itemView.findViewById(R.id.checkbox_select);
        }
        
        /**
         * Привязывает данные продукта к элементам ViewHolder
         */
        public void bind(SeriesChangeItem item, int position, OnSelectionChangeListener listener) {
            // Заполняем основные данные
            tvSeriesName.setText(item.getSeriesName() != null ? item.getSeriesName() : "");
            tvZnpName.setText(item.getZnpName() != null ? item.getZnpName() : "");
            
            // Форматируем количество
            if (item.getQuantityToProcure() == Math.floor(item.getQuantityToProcure())) {
                tvQuantityToProcure.setText(String.format("%.0f", item.getQuantityToProcure()));
            } else {
                tvQuantityToProcure.setText(String.format("%.2f", item.getQuantityToProcure()));
            }

            checkBoxSelect.setOnCheckedChangeListener(null);

            checkBoxSelect.setChecked(item.isSelected());

            checkBoxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    Log.d(TAG, "Изменение выбора для позиции " + position + ": " + isChecked);
                    listener.onSelectionChanged(position, isChecked);
                }
            });
            
            // Также обрабатываем нажатие на весь элемент для удобства
            itemView.setOnClickListener(v -> {
                boolean newState = !checkBoxSelect.isChecked();
                checkBoxSelect.setChecked(newState);
                // Слушатель checkBoxSelect автоматически вызовется
            });
        }
    }
} 