package com.step.tcd_rpkb.base;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Базовый класс активности с поддержкой полноэкранного режима
 */
public class BaseFullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Включаем полноэкранный режим при возобновлении активности
        enableFullScreen();
    }

    /**
     * Включает полноэкранный режим, скрывая строку состояния и панель навигации
     * Безопасно обрабатывает случаи, когда окно или его компоненты могут быть null
     */
    protected void enableFullScreen() {
        if (getWindow() == null) return;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // API уровень на устройстве RUGLINE RT41 = 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Конкретная обработка для Android 11 (API 30)
            final WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                controller.hide(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Для Android 10 (API 29) и ниже
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            
            // Скрываем навигационную панель
            View decorView = getWindow().getDecorView();
            if (decorView != null) {
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOptions);
            }
        }
    }
} 