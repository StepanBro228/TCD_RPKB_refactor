package com.step.tcd_rpkb.UI.main.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.step.tcd_rpkb.LC_menu;
import com.step.tcd_rpkb.LVC_menu;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.SP_menu;
import com.step.tcd_rpkb.base.BaseFullscreenActivity;
import com.step.tcd_rpkb.UI.main.fragments.LoginDialogFragment;
import com.step.tcd_rpkb.UI.main.fragments.AuthenticationDialogFragment;
import com.step.tcd_rpkb.UI.main.viewmodel.MainViewModel;
import com.step.tcd_rpkb.domain.usecase.IsUserAuthenticatedUseCase;
import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends BaseFullscreenActivity implements 
        LoginDialogFragment.LoginDialogListener,
        AuthenticationDialogFragment.AuthenticationListener {
    
    private TextView testView;
    private MainViewModel mainViewModel;
    
    // UI элементы
    private View mainButtonsLayout;
    private com.google.android.material.button.MaterialButton authButtonCenter;
    private com.google.android.material.floatingactionbutton.FloatingActionButton authButtonCorner;
    private com.google.android.material.floatingactionbutton.FloatingActionButton settingsButton;
    
    // Use cases
    @Inject
    IsUserAuthenticatedUseCase isUserAuthenticatedUseCase;

    public void goTolc(View view){
        Intent intent = new Intent(this, LC_menu.class);
        startActivity(intent);
    }
    public void goTolvk(View view){
        Intent intent = new Intent(this, LVC_menu.class);
        startActivity(intent);
    }
    public void goTosp(View view) {
        Intent intent = new Intent(this, SP_menu.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Инициализируем UI элементы
        initViews();
        setupListeners();
        
        // Устанавливаем начальное состояние UI
        updateUIBasedOnAuthState();
        
        // Наблюдаем за LiveData для отображения Toast сообщений
        mainViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void initViews() {
        testView = findViewById(R.id.textView1);
        mainButtonsLayout = findViewById(R.id.linearLayout);
        settingsButton = findViewById(R.id.settings_button);
        authButtonCenter = findViewById(R.id.auth_button_center);
        authButtonCorner = findViewById(R.id.auth_button_corner);
    }
    
    private void setupListeners() {
        // Кнопка настроек
        settingsButton.setOnClickListener(v -> showLoginDialog());
        
        // Кнопки авторизации (обе ведут к одному диалогу)
        authButtonCenter.setOnClickListener(v -> showAuthenticationDialog());
        authButtonCorner.setOnClickListener(v -> showAuthenticationDialog());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUIBasedOnAuthState();
    }
    

    
    /**
     * Показывает диалог настроек подключения
     */
    private void showLoginDialog() {
        LoginDialogFragment dialog = new LoginDialogFragment();
        dialog.show(getSupportFragmentManager(), "LoginDialogFragment");
    }
    
    @Override
    public void onLoginDialogPositiveClick(String username, String password, String deviceNum, boolean onlineMode, String URL) {
        // Делегируем обработку результата в ViewModel
        mainViewModel.handleLoginResult(username, password, deviceNum, onlineMode, URL);
    }
    
    /**
     * Показывает диалог авторизации
     */
    private void showAuthenticationDialog() {
        AuthenticationDialogFragment dialog = new AuthenticationDialogFragment();
        dialog.show(getSupportFragmentManager(), "AuthenticationDialogFragment");
    }
    
    /**
     * Обновляет видимость UI элементов в зависимости от статуса авторизации
     */
    private void updateUIBasedOnAuthState() {
        boolean isAuthenticated = isUserAuthenticatedUseCase.execute();
        
        if (isAuthenticated) {
            // Пользователь авторизован
            
            // Показываем основные кнопки меню
            mainButtonsLayout.setVisibility(View.VISIBLE);
            
            // Показываем кнопку настроек
            settingsButton.setVisibility(View.VISIBLE);
            
            // Показываем маленькую кнопку авторизации в углу
            authButtonCorner.setVisibility(View.VISIBLE);
            
            // Скрываем большую кнопку авторизации по центру
            authButtonCenter.setVisibility(View.GONE);
            
        } else {
            // Пользователь не авторизован
            
            // Скрываем основные кнопки меню
//            mainButtonsLayout.setVisibility(View.GONE);
            
            // Скрываем кнопку настроек

            
            // Скрываем маленькую кнопку авторизации в углу
            authButtonCorner.setVisibility(View.GONE);
            
            // Показываем большую кнопку авторизации по центру
            authButtonCenter.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onAuthenticationChanged(boolean isAuthenticated) {
        // Обновляем UI при изменении статуса авторизации
        updateUIBasedOnAuthState();
    }
}
