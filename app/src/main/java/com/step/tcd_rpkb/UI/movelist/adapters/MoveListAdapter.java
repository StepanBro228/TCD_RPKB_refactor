package com.step.tcd_rpkb.UI.movelist.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.step.tcd_rpkb.domain.model.MoveItem;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.utils.MoveDiffCallback;
import com.step.tcd_rpkb.views.CustomCheckBox;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MoveListAdapter extends ListAdapter<MoveItem, MoveListAdapter.MoveViewHolder> {

    // Интерфейс для обработки кликов по элементу
    public interface OnMoveItemClickListener {
        void onItemClicked(MoveItem moveItem);
    }

    private OnMoveItemClickListener itemClickListener;

    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    
    private Set<String> selectedItems = new HashSet<>();
    
    private static final ThreadLocal<SimpleDateFormat> inputDateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()));
    private static final ThreadLocal<SimpleDateFormat> outputDateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()));
    
    private static final SparseArray<Integer> priorityColors = new SparseArray<>(4);
    
    static {
        priorityColors.put(0, Color.parseColor("#8B0000"));
        priorityColors.put(1, Color.parseColor("#FF6347"));
        priorityColors.put(2, Color.YELLOW);
        priorityColors.put(3, Color.GREEN);
    }

    public MoveListAdapter(Context context) {
        super(new MoveDiffCallback());
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int position) {
        MoveItem item = getItem(position);
        return item != null ? item.hashCode() : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public MoveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movelist, parent, false);
        return new MoveViewHolder(view);
    }

    public void setOnMoveItemClickListener(OnMoveItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull MoveViewHolder holder, int position) {
        MoveItem moveItem = getItem(position);
        
        if (moveItem == null) {
            return;
        }

        try {
            Date date = inputDateFormat.get().parse(moveItem.getDate());
            if (date != null) {
                holder.tvDate.setText(outputDateFormat.get().format(date));
            } else {
                holder.tvDate.setText(moveItem.getDate());
            }
        } catch (ParseException e) {
            holder.tvDate.setText(moveItem.getDate());
        }
        
        holder.tvNumber.setText(moveItem.getNumber());
        holder.tvFromWarehouse.setText(moveItem.getSourceWarehouseName());
        holder.tvToWarehouse.setText(moveItem.getDestinationWarehouseName());
        
        String productName = moveItem.getProductName();
        if (productName != null && !productName.isEmpty()) {
            holder.tvProduct.setText(productName);
        } else {
            holder.tvProduct.setText(moveItem.getComment());
        }
        
        holder.tvPositionCount.setText(String.valueOf(moveItem.getPositionsCount()));
        holder.tvItemsCount.setText(decimalFormat.format(moveItem.getItemsCount()));
        
        String responsibleName = moveItem.getResponsiblePersonName();
        String assemblerName = moveItem.getAssemblerName();
        
        boolean isKomplektuetsa = "Комплектуется".equals(moveItem.getSigningStatus());
        
        if (isKomplektuetsa && assemblerName != null && !assemblerName.isEmpty()) {
            holder.tvAssembler.setText(createFormattedText("Комплектовщик: ", assemblerName));
            holder.tvAssembler.setVisibility(View.VISIBLE);
            
            if (responsibleName != null && !responsibleName.isEmpty()) {
                holder.tvResponsible.setText(createFormattedText("Ответственный: ", responsibleName));
                holder.tvResponsible.setVisibility(View.VISIBLE);
            } else {
                holder.tvResponsible.setVisibility(View.GONE);
            }
        } else {
            holder.tvAssembler.setVisibility(View.GONE);
            
            if (responsibleName != null && !responsibleName.isEmpty()) {
                holder.tvResponsible.setText(createFormattedText("Ответственный: ", responsibleName));
                holder.tvResponsible.setVisibility(View.VISIBLE);
            } else if (assemblerName != null && !assemblerName.isEmpty()) {
                holder.tvResponsible.setText(createFormattedText("Комплектовщик: ", assemblerName));
                holder.tvResponsible.setVisibility(View.VISIBLE);
            } else {
                holder.tvResponsible.setVisibility(View.GONE);
            }
        }
        
        setPriorityColor(holder, moveItem.getPriority());
        
        // Устанавливаем состояние checkbox "Проведен"
        setCompletedCheckbox(holder, moveItem.isCompleted(), moveItem.getPriority());
        
        holder.itemView.setOnClickListener(v -> {
            if (!containerClickEnabled) {
                return;
            }
            if (itemClickListener != null) {
                itemClickListener.onItemClicked(moveItem);
            }
        });
        
        final String itemId = moveItem.getMovementId();
        holder.checkboxMove.setOnCheckedChangeListener(null);
        boolean isSelected = selectedItems.contains(itemId);
        holder.checkboxMove.setChecked(isSelected);
        holder.checkboxMove.setOnCheckedChangeListener((checkBox, isChecked) -> {
            if (isChecked) {
                selectedItems.add(itemId);
            } else {
                selectedItems.remove(itemId);
            }
            notifySelectionChangeListener();
        });
    }

    private void setPriorityColor(MoveViewHolder holder, String priority) {
        holder.priorityIndicator.setVisibility(View.VISIBLE);
        if ("Неотложный".equals(priority)) {
            holder.priorityIndicator.setBackgroundColor(priorityColors.get(0));
        } else if ("Высокий".equals(priority)) {
            holder.priorityIndicator.setBackgroundColor(priorityColors.get(1));
        } else if ("Средний".equals(priority)) {
            holder.priorityIndicator.setBackgroundColor(priorityColors.get(2));
        } else {
            holder.priorityIndicator.setBackgroundColor(priorityColors.get(3));
        }
    }

    private void setCompletedCheckbox(MoveViewHolder holder, boolean isCompleted, String priority) {
        // Устанавливаем состояние checkbox
        holder.checkboxCompleted.setChecked(isCompleted);
        
        // Получаем цвет приоритета
        int priorityColor;
        if ("Неотложный".equals(priority)) {
            priorityColor = priorityColors.get(0);
        } else if ("Высокий".equals(priority)) {
            priorityColor = priorityColors.get(1);
        } else if ("Средний".equals(priority)) {
            priorityColor = priorityColors.get(2);
        } else {
            priorityColor = priorityColors.get(3);
        }

        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        backgroundDrawable.setCornerRadius(4f);
        backgroundDrawable.setColor(priorityColor);
        

        holder.checkboxCompleted.setBackground(backgroundDrawable);
        
        // Если перемещение проведено, показываем зеленую галочку
        if (isCompleted) {
            holder.checkboxCompleted.setText("✓");
            holder.checkboxCompleted.setTextColor(Color.parseColor("#277d1d")); // Ярко-зеленый неоновый цвет
            holder.checkboxCompleted.setTextSize(22f);// Ещё больший размер

            holder.checkboxCompleted.setTypeface(null, android.graphics.Typeface.BOLD); // Жирный шрифт
            // Добавляем более яркую тень для неонового эффекта
            holder.checkboxCompleted.setShadowLayer(4f, 0f, 0f, Color.parseColor("#00FF00"));
        } else {
            holder.checkboxCompleted.setText("");
            holder.checkboxCompleted.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT); // Убираем тень
        }
    }

    public List<MoveItem> getSelectedItems() {
        List<MoveItem> selected = new ArrayList<>();
        List<MoveItem> currentList = getCurrentList();
        for (MoveItem item : currentList) {
            if (item != null && selectedItems.contains(item.getMovementId())) {
                selected.add(item);
            }
        }
        return selected;
    }

    public void clearSelection() {
        if (selectedItems.isEmpty()) {
            return;
        }
        List<Integer> positionsToUpdate = new ArrayList<>();
        List<MoveItem> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            MoveItem item = currentList.get(i);
            if (item != null && selectedItems.contains(item.getMovementId())) {
                positionsToUpdate.add(i);
            }
        }
        selectedItems.clear();
        for (int position : positionsToUpdate) {
            notifyItemChanged(position);
        }
        notifySelectionChangeListener();
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }
    
    private OnSelectionChangeListener selectionChangeListener;
    
    public void setSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }
    
    private void notifySelectionChangeListener() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedItems.size());
        }
    }

    /**
     * Создает SpannableStringBuilder с обычным префиксом и жирным именем
     * @param prefix обычный текст (например, "Комплектовщик: ")
     * @param name жирный текст (ФИО)
     * @return отформатированный SpannableStringBuilder
     */
    private SpannableStringBuilder createFormattedText(String prefix, String name) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(prefix);
        int start = builder.length();
        builder.append(name);
        int end = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    public void setContainerClickEnabled(boolean enabled) {
        containerClickEnabled = enabled;
    }
    
    private boolean containerClickEnabled = true;

    static class MoveViewHolder extends RecyclerView.ViewHolder {
        View priorityIndicator;
        TextView tvDate;
        TextView tvNumber;
        TextView tvFromWarehouse;
        TextView tvToWarehouse;
        TextView tvProduct;
        TextView tvPositionCount;
        TextView tvItemsCount;
        TextView tvAssembler;
        TextView tvResponsible;
        CustomCheckBox checkboxMove;
        CheckBox checkboxCompleted;

        public MoveViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvFromWarehouse = itemView.findViewById(R.id.tvFromWarehouse);
            tvToWarehouse = itemView.findViewById(R.id.tvToWarehouse);
            tvProduct = itemView.findViewById(R.id.tvProduct);
            tvPositionCount = itemView.findViewById(R.id.tvPositionCount);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            tvAssembler = itemView.findViewById(R.id.tvAssembler);
            tvResponsible = itemView.findViewById(R.id.tvResponsible);
            checkboxMove = itemView.findViewById(R.id.checkboxMove);
            checkboxCompleted = itemView.findViewById(R.id.checkboxCompleted);
        }
    }
} 