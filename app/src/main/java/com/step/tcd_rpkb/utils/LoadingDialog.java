package com.step.tcd_rpkb.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.step.tcd_rpkb.R;

/**
 * Диалог для отображения процесса загрузки
 */
public class LoadingDialog {
    private Dialog dialog;
    private TextView tvMessage;
    private Context context;
    
    /**
     * Создает новый диалог загрузки
     * 
     * @param context контекст приложения
     */
    public LoadingDialog(Context context) {
        this.context = context;
        try {
            dialog = new Dialog(context);
            
            // Проверяем, что контекст активен
            if (context instanceof android.app.Activity && 
                !((android.app.Activity) context).isFinishing() &&
                !((android.app.Activity) context).isDestroyed()) {
                
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_loading);
                dialog.setCancelable(false);
                
                // Устанавливаем прозрачный фон для окна диалога
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    
                    // Устанавливаем макет на всю ширину экрана с правильным отображением
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.copyFrom(dialog.getWindow().getAttributes());
                    layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    dialog.getWindow().setAttributes(layoutParams);
                }
                
                tvMessage = dialog.findViewById(R.id.tvLoadingMessage);
            } else {
                Log.e("LoadingDialog", "Невозможно создать диалог: контекст не активен");
            }
        } catch (Exception e) {
            Log.e("LoadingDialog", "Ошибка при создании диалога: " + e.getMessage(), e);
        }
    }
    
    /**
     * Показывает диалог загрузки с сообщением по умолчанию
     */
    public void show() {
        show("Загрузка...");
    }
    
    /**
     * Показывает диалог загрузки с заданным сообщением
     * 
     * @param message сообщение для отображения
     */
    public void show(String message) {
        try {
            if (dialog != null && tvMessage != null) {
                // Проверяем, что контекст по-прежнему активен
                if (context instanceof android.app.Activity && 
                    !((android.app.Activity) context).isFinishing() &&
                    !((android.app.Activity) context).isDestroyed()) {
                    
                    tvMessage.setText(message);
                    
                    if (!dialog.isShowing()) {
                        dialog.show();
                        Log.d("LoadingDialog", "Диалог показан: " + message);
                    }
                } else {
                    Log.e("LoadingDialog", "Невозможно показать диалог: контекст не активен");
                }
            } else {
                Log.e("LoadingDialog", "Невозможно показать диалог: dialog или tvMessage равны null");
            }
        } catch (Exception e) {
            Log.e("LoadingDialog", "Ошибка при показе диалога: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновляет сообщение в диалоге
     * 
     * @param message новое сообщение
     */
    public void updateMessage(String message) {
        try {
            if (dialog != null && dialog.isShowing() && tvMessage != null) {
                // Проверяем, что контекст по-прежнему активен
                if (context instanceof android.app.Activity && 
                    !((android.app.Activity) context).isFinishing() &&
                    !((android.app.Activity) context).isDestroyed()) {
                    
                    tvMessage.setText(message);
                    Log.d("LoadingDialog", "Сообщение обновлено: " + message);
                }
            }
        } catch (Exception e) {
            Log.e("LoadingDialog", "Ошибка при обновлении сообщения: " + e.getMessage(), e);
        }
    }
    
    /**
     * Скрывает диалог загрузки
     */
    public void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                // Проверяем, что контекст по-прежнему активен
                if (context instanceof android.app.Activity && 
                    !((android.app.Activity) context).isFinishing() &&
                    !((android.app.Activity) context).isDestroyed()) {
                    
                    dialog.dismiss();
                    Log.d("LoadingDialog", "Диалог скрыт");
                }
            }
        } catch (Exception e) {
            Log.e("LoadingDialog", "Ошибка при скрытии диалога: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, отображается ли диалог
     * 
     * @return true если диалог отображается
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
} 