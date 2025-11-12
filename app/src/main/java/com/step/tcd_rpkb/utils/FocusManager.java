package com.step.tcd_rpkb.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;

/**
 * Централизованный менеджер фокуса для предотвращения конфликтов
 * между разными частями пользовательского интерфейса.
 */
public class FocusManager {
    private static final String TAG = "FocusManager";
    private static boolean isFocusChangeInProgress = false;
    private static Handler focusHandler = new Handler(Looper.getMainLooper());
    private static Runnable currentFocusRunnable = null;

    /**
     * Запрашивает фокус для EditText с контролем конфликтов.
     * @param target целевой EditText
     * @param setSelection нужно ли устанавливать курсор в конец текста
     * @param delay задержка в мс перед установкой фокуса
     * @param priority приоритет запроса (выше = важнее)
     * @return true если запрос принят, false если отклонен
     */
    public static synchronized boolean requestFocusChange(
            EditText target,
            boolean setSelection,
            long delay,
            int priority) {

        if (target == null || !target.isAttachedToWindow()) {
            Log.d(TAG, "Запрос фокуса отклонен: target null или не прикреплен к окну");
            return false;
        }

        // Отменяем предыдущий запрос
        cancelPendingFocusRequests();
        
        // Создаем новый запрос
        isFocusChangeInProgress = true;
        
        currentFocusRunnable = () -> {
            try {
                if (target.isAttachedToWindow()) {
                    target.requestFocus();
                    if (setSelection) {
                        target.setSelection(target.getText().length());
                    }
                    Log.d(TAG, "Фокус успешно установлен");
                } else {
                    Log.d(TAG, "Не удалось установить фокус: target больше не прикреплен к окну");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке фокуса: " + e.getMessage());
            } finally {
                isFocusChangeInProgress = false;
                currentFocusRunnable = null;
            }
        };
        
        focusHandler.postDelayed(currentFocusRunnable, delay);
        Log.d(TAG, "Запрос фокуса принят с приоритетом " + priority + ", задержка " + delay + "мс");
        
        return true;
    }
    
    /**
     * Запрашивает фокус с приоритетом по умолчанию (1).
     */
    public static synchronized boolean requestFocusChange(
            EditText target,
            boolean setSelection,
            long delay) {
        return requestFocusChange(target, setSelection, delay, 1);
    }

    /**
     * Отменяет все ожидающие запросы фокуса.
     */
    public static void cancelPendingFocusRequests() {
        if (currentFocusRunnable != null) {
            focusHandler.removeCallbacks(currentFocusRunnable);
            Log.d(TAG, "Отменены ожидающие запросы фокуса");
        }
        currentFocusRunnable = null;
        isFocusChangeInProgress = false;
    }
    

} 