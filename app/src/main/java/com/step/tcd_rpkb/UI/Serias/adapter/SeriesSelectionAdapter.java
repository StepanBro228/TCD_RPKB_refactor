package com.step.tcd_rpkb.UI.Serias.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.domain.model.SeriesItem;

import java.util.List;

/**
 * Адаптер для отображения списка серий в RecyclerView
 */
public class SeriesSelectionAdapter extends RecyclerView.Adapter<SeriesSelectionAdapter.SeriesViewHolder> {

    private List<SeriesItem> seriesItems;
    private OnSeriesItemClickListener listener;

    /**
     * Интерфейс для обратного вызова при клике на серию
     */
    public interface OnSeriesItemClickListener {
        void onSeriesItemClick(SeriesItem seriesItem, int position);
    }

    /**
     * Конструктор адаптера
     * @param seriesItems список серий для отображения
     */
    public SeriesSelectionAdapter(List<SeriesItem> seriesItems) {
        this.seriesItems = seriesItems;
    }

    /**
     * Устанавливает слушатель клика на серию
     * @param listener слушатель
     */
    public void setOnSeriesItemClickListener(OnSeriesItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Обновляет данные в адаптере
     * @param newSeriesItems новый список серий
     */
    public void updateData(List<SeriesItem> newSeriesItems) {
        this.seriesItems = newSeriesItems;
        notifyDataSetChanged();
    }

    /**
     * Обновляет элемент в указанной позиции
     * @param position позиция элемента
     * @param seriesItem обновленный элемент
     */
    public void updateItem(int position, SeriesItem seriesItem) {
        if (position >= 0 && position < seriesItems.size()) {
            seriesItems.set(position, seriesItem);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_series_selection, parent, false);
        return new SeriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        SeriesItem item = seriesItems.get(position);
        holder.bind(item, position, listener);
    }

    @Override
    public int getItemCount() {
        return seriesItems != null ? seriesItems.size() : 0;
    }

    /**
     * ViewHolder для серии
     */
    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSeriesName;
        private TextView tvExpiryDate;
        private TextView tvFreeBalance;
        private TextView tvReservedByOthers;
        private TextView tvAllocatedQuantity;

        public SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSeriesName = itemView.findViewById(R.id.tv_series_name);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            tvFreeBalance = itemView.findViewById(R.id.tv_free_balance);
            tvReservedByOthers = itemView.findViewById(R.id.tv_reserved_by_others);
            tvAllocatedQuantity = itemView.findViewById(R.id.tv_allocated_quantity);
        }

        /**
         * Привязывает данные серии к элементам ViewHolder
         * @param item элемент серии
         * @param position позиция элемента
         * @param listener слушатель клика на серию
         */
        public void bind(SeriesItem item, int position, OnSeriesItemClickListener listener) {
            tvSeriesName.setText(item.getSeriesName());
            tvExpiryDate.setText(item.getExpiryDate());
            tvFreeBalance.setText(String.format("%.2f", item.getFreeBalance()));
            tvReservedByOthers.setText(String.format("%.2f", item.getReservedByOthers()));
            
            // Устанавливаем количество в документе
            double documentQuantity = item.getDocumentQuantity();
            if (documentQuantity > 0) {
                // Форматируем значение в зависимости от того, является ли оно целым числом
                String formattedQuantity;
                if (documentQuantity == Math.floor(documentQuantity)) {
                    formattedQuantity = String.format("%.0f", documentQuantity);
                } else {
                    formattedQuantity = String.format("%.2f", documentQuantity);
                }
                tvAllocatedQuantity.setText(formattedQuantity);
                tvAllocatedQuantity.setTextColor(itemView.getContext().getColor(R.color.colorPrimary));
            } else {
                tvAllocatedQuantity.setText("0");
                tvAllocatedQuantity.setTextColor(itemView.getContext().getColor(R.color.colorTextSecondary));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSeriesItemClick(item, position);
                }
            });
        }
    }
} 