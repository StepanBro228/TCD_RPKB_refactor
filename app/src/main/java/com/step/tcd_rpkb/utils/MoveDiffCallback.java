package com.step.tcd_rpkb.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.step.tcd_rpkb.domain.model.MoveItem;

/**
 * Класс для эффективного обновления RecyclerView с помощью DiffUtil.ItemCallback
 * для использования с ListAdapter.
 */
public class MoveDiffCallback extends DiffUtil.ItemCallback<MoveItem> {
    @Override
    public boolean areItemsTheSame(@NonNull MoveItem oldItem, @NonNull MoveItem newItem) {
        // Проверяем, что это тот же элемент, сравнивая GUID
        // Добавляем проверку на null для ID на всякий случай
        if (oldItem.getMovementId() == null || newItem.getMovementId() == null) {
            return oldItem == newItem; // Сравнение по ссылке, если ID null
        }
        return oldItem.getMovementId().equals(newItem.getMovementId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull MoveItem oldItem, @NonNull MoveItem newItem) {
        // Проверяем, что содержимое не изменилось
        // Сравниваем только те поля, которые отображаются в UI или влияют на его вид
        
        // Сначала основные строковые поля, которые скорее всего не null
        if (!compareStrings(oldItem.getDate(), newItem.getDate())) return false;
        if (!compareStrings(oldItem.getNumber(), newItem.getNumber())) return false;
        if (!compareStrings(oldItem.getSourceWarehouseName(), newItem.getSourceWarehouseName())) return false;
        if (!compareStrings(oldItem.getDestinationWarehouseName(), newItem.getDestinationWarehouseName())) return false;
        if (!compareStrings(oldItem.getProductName(), newItem.getProductName())) return false;
        if (!compareStrings(oldItem.getPriority(), newItem.getPriority())) return false;
        if (!compareStrings(oldItem.getComment(), newItem.getComment())) return false; // Добавлено сравнение комментария
        if (!compareStrings(oldItem.getSigningStatus(), newItem.getSigningStatus())) return false; // Добавлено сравнение статуса подписания

        // Числовые поля
        if (oldItem.getPositionsCount() != newItem.getPositionsCount()) return false;
        if (Double.compare(oldItem.getItemsCount(), newItem.getItemsCount()) != 0) return false; // Для double используем Double.compare

        // Имена (могут быть null)
        if (!compareStrings(oldItem.getResponsiblePersonName(), newItem.getResponsiblePersonName())) return false;
        if (!compareStrings(oldItem.getAssemblerName(), newItem.getAssemblerName())) return false;
        
        // Сравнение состояния CPS, если оно отображается или влияет на UI
        if (oldItem.isCps() != newItem.isCps()) return false;

        // Если все проверки пройдены, содержимое одинаково
        return true;
    }
    
    /**
     * Безопасное сравнение строк с учетом null
     */
    private boolean compareStrings(String str1, String str2) {
        if (str1 == null) return str2 == null;
        return str1.equals(str2);
    }
} 