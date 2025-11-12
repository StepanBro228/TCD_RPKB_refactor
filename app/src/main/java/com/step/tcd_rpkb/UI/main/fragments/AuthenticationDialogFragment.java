package com.step.tcd_rpkb.UI.main.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.step.tcd_rpkb.R;
import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.usecase.AuthenticateUserUseCase;
import com.step.tcd_rpkb.domain.usecase.GetUserUseCase;
import com.step.tcd_rpkb.domain.usecase.GetUserInfoByGuidUseCase;
import com.step.tcd_rpkb.domain.usecase.IsUserAuthenticatedUseCase;
import com.step.tcd_rpkb.domain.model.AuthenticationResult;
import com.step.tcd_rpkb.domain.model.UserInfoResponse;
import com.step.tcd_rpkb.domain.repository.RepositoryCallback;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Диалог для авторизации пользователя через QR-код и пароль
 */
@AndroidEntryPoint
public class AuthenticationDialogFragment extends DialogFragment {
    
    // UI состояния
    private LinearLayout layoutScanQR;
    private LinearLayout layoutLogin;
    private LinearLayout layoutUserInfo;
    
    // Элементы для сканирования
    private ProgressBar progressBarScan;
    
    // Элементы для логина
    private TextInputEditText etAuthLogin;
    private TextInputEditText etAuthPassword;
    private MaterialButton btnLogin;
    
    // Элементы для информации о пользователе
    private TextView tvUserFullName;
    private TextView tvUserRole;
    private MaterialButton btnLogout;
    
    // Общие элементы
    private MaterialButton btnClose;
    private MaterialButton btnManual;
    private MaterialButton btnCloseUserInfo;
    
    // Use cases
    @Inject
    AuthenticateUserUseCase authenticateUserUseCase;
    
    @Inject
    GetUserUseCase getUserUseCase;
    
    @Inject
    IsUserAuthenticatedUseCase isUserAuthenticatedUseCase;
    
    @Inject
    GetUserInfoByGuidUseCase getUserInfoByGuidUseCase;
    
    // Временное хранение информации о пользователе из QR-кода
    private UserInfoResponse scannedUserInfo;
    
    // Receiver для обработки данных сканера
    private BroadcastReceiver barcodeDataReceiver;
    

    public interface AuthenticationListener {
        void onAuthenticationChanged(boolean isAuthenticated);
    }
    
    private AuthenticationListener authenticationListener;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            authenticationListener = (AuthenticationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " должен реализовать AuthenticationListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.dialog_authentication, null);
        
        bindViews(rootView);
        setupListeners();
        setupTouchListener(rootView);
        initializeBarcodeReceiver();
        updateUIState();
        
        builder.setView(rootView)
               .setTitle("Авторизация");
        
        AlertDialog dialog = builder.create();
        
        // Настраиваем размер диалога
        setupDialogSize(dialog);
        
        return dialog;
    }
    
    private void bindViews(View rootView) {
        // Контейнеры состояний
        layoutScanQR = rootView.findViewById(R.id.layoutScanQR);
        layoutLogin = rootView.findViewById(R.id.layoutLogin);
        layoutUserInfo = rootView.findViewById(R.id.layoutUserInfo);
        
        // Элементы сканирования
        progressBarScan = rootView.findViewById(R.id.progressBarScan);
        
        // Элементы логина
        etAuthLogin = rootView.findViewById(R.id.etAuthLogin);
        etAuthPassword = rootView.findViewById(R.id.etAuthPassword);
        btnLogin = rootView.findViewById(R.id.btnLogin);
        
        // Элементы информации о пользователе
        tvUserFullName = rootView.findViewById(R.id.tvUserFullName);
        tvUserRole = rootView.findViewById(R.id.tvUserRole);
        btnLogout = rootView.findViewById(R.id.btnLogout);
        
        // Общие элементы
        btnClose = rootView.findViewById(R.id.btnClose);
        btnManual = rootView.findViewById(R.id.btnManual);
        btnCloseUserInfo = rootView.findViewById(R.id.btnCloseUserInfo);
    }
    
    private void setupListeners() {
        // Кнопка "Войти"
        btnLogin.setOnClickListener(v -> performLogin());
        
        // Кнопка "Выйти из учетной записи"
        btnLogout.setOnClickListener(v -> performLogout());
        
        // Кнопка "Закрыть"
        btnClose.setOnClickListener(v -> dismiss());
        
        // Кнопка "Закрыть окно" в состоянии информации о пользователе
        btnCloseUserInfo.setOnClickListener(v -> dismiss());
        
        // Кнопка "Вручную"
        btnManual.setOnClickListener(v -> showManualLoginState());
        

        etAuthLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        

        etAuthPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Обработчик Enter в поле логина - переход к полю пароля
        etAuthLogin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || 
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && 
                 event.getAction() == KeyEvent.ACTION_DOWN)) {
                
                // Передаем фокус полю пароля
                etAuthPassword.requestFocus();
                return true;
            }
            return false;
        });
        
        // Обработчик Enter в поле пароля
        etAuthPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && 
                 event.getAction() == KeyEvent.ACTION_DOWN)) {
                
                // Проверяем, заполнены ли поля и кнопка "Войти" активна
                if (btnLogin.isEnabled()) {
                    hideKeyboard();
                    performLogin();
                }
                return true;
            }
            return false;
        });
    }
    
    private void initializeBarcodeReceiver() {
        barcodeDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("AuthenticationDialog", "Получен Broadcast от сканера: " + action);
                
                String barcodeData = extractBarcodeData(intent, action);
                
                if (barcodeData != null && !barcodeData.isEmpty()) {
                    Log.i("AuthenticationDialog", "Успешно получены данные сканирования: " + barcodeData);
                    processBarcodeData(barcodeData);
                } else {
                    Log.w("AuthenticationDialog", "Данные сканирования не получены или пусты для action: " + action);
                }
            }
        };
    }
    
    private String extractBarcodeData(Intent intent, String action) {
        String barcodeData = null;
        
        if ("com.example.scannerapp.ACTION_BARCODE_DATA".equals(action)) {
            barcodeData = intent.getStringExtra("data");
        } else if ("android.intent.action.SCAN".equals(action) || "com.google.zxing.client.android.SCAN".equals(action)) {
            barcodeData = intent.getStringExtra("SCAN_RESULT");
        } else if ("com.symbol.datawedge.api.ACTION".equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey("com.symbol.datawedge.data_string")) {
                    barcodeData = extras.getString("com.symbol.datawedge.data_string");
                } else if (extras.containsKey("data")) {
                    barcodeData = extras.getString("data");
                }
            }
        } else if ("com.scanner.broadcast".equals(action)) {
            barcodeData = intent.getStringExtra("data");
        } else if ("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST".equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey("EXTRA_BARCODE_DECODING_DATA")) {
                    barcodeData = extras.getString("EXTRA_BARCODE_DECODING_DATA");
                } else if (extras.containsKey("barcode_data")) {
                    barcodeData = extras.getString("barcode_data");
                } else if (extras.containsKey("data")) {
                    barcodeData = extras.getString("data");
                }
            }
        }
        
        return barcodeData != null ? barcodeData.trim() : null;
    }
    
    private void processBarcodeData(String barcodeData) {
        progressBarScan.setVisibility(View.VISIBLE);
        
        try {
            // Парсим QR код в формате: СП|Справочники|Пользователи|c6852e59-9997-11e9-bb93-001dd8b71c23
            String[] parts = barcodeData.split("\\|");
            if (parts.length >= 4 && "СП".equals(parts[0]) && "Справочники".equals(parts[1]) && "Пользователи".equals(parts[2])) {
                String userGuid = parts[3].trim(); // Убираем лишние пробелы и символы перевода строки
                
                // Запрашиваем информацию о пользователе с сервера
                getUserInfoByGuidUseCase.execute(userGuid, new RepositoryCallback<UserInfoResponse>() {
                    @Override
                    public void onSuccess(UserInfoResponse userInfo) {
                        progressBarScan.setVisibility(View.GONE);
                        
                        // Сохраняем информацию о пользователе для последующего использования
                        scannedUserInfo = userInfo;
                        
                        // Устанавливаем логин из ответа сервера
                        etAuthLogin.setText(userInfo.getName());
                        etAuthLogin.setEnabled(false); // Блокируем редактирование логина
                        
                        // Переключаемся на состояние ввода пароля
                        showLoginState();
                        
                        // Устанавливаем фокус на поле пароля
                        etAuthPassword.requestFocus();
                        
                        // Показываем информацию о пользователе
                        String message = "QR-код отсканирован\nПользователь: " + userInfo.getFullName();
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        progressBarScan.setVisibility(View.GONE);
                        Log.e("AuthenticationDialog", "Ошибка при получении информации о пользователе: " + e.getMessage());
                        Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                
            } else {
                progressBarScan.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Неверный формат QR-кода", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            progressBarScan.setVisibility(View.GONE);
            Log.e("AuthenticationDialog", "Ошибка при обработке QR-кода: " + e.getMessage());
            Toast.makeText(requireContext(), "Ошибка при обработке QR-кода", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void performLogin() {

        hideKeyboard();
        
        String login = etAuthLogin.getText().toString().trim();
        String password = etAuthPassword.getText().toString().trim();
        
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        

        btnLogin.setEnabled(false);
        btnLogin.setText("Авторизация...");
        
        // Выполняем авторизацию через новый UseCase с информацией о пользователе
        authenticateUserUseCase.execute(login, password, scannedUserInfo, new RepositoryCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {

                btnLogin.setEnabled(true);
                btnLogin.setText("Войти");
                
                if (result.isSuccess()) {
                    // Успешная авторизация

                    if (authenticationListener != null) {
                        authenticationListener.onAuthenticationChanged(true);
                    }
                    

                    Toast.makeText(requireContext(), "Успешная авторизация", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    // Ошибка авторизации
                    Toast.makeText(requireContext(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onError(Exception e) {

                btnLogin.setEnabled(true);
                btnLogin.setText("Войти");
                
                Log.e("AuthenticationDialog", "Ошибка при авторизации: " + e.getMessage());
                Toast.makeText(requireContext(), "Ошибка при авторизации: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void performLogout() {
        try {
            // Выходим из учетной записи
            authenticateUserUseCase.logout();
            

            if (authenticationListener != null) {
                authenticationListener.onAuthenticationChanged(false);
            }
            

            etAuthLogin.setText("");
            etAuthPassword.setText("");
            
            // Очищаем сохраненную информацию о пользователе
            scannedUserInfo = null;
            

            updateUIState();
            
            Toast.makeText(requireContext(), "Вы вышли из учетной записи", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AuthenticationDialog", "Ошибка при выходе из учетной записи: " + e.getMessage());
            Toast.makeText(requireContext(), "Ошибка при выходе", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUIState() {
        boolean isAuthenticated = isUserAuthenticatedUseCase.execute();
        
        if (isAuthenticated) {
            showUserInfoState();
        } else {
            showScanQRState();
        }
    }
    
    private void showScanQRState() {
        layoutScanQR.setVisibility(View.VISIBLE);
        layoutLogin.setVisibility(View.GONE);
        layoutUserInfo.setVisibility(View.GONE);
        
        // Показываем кнопку "Вручную" только в состоянии сканирования QR
        btnManual.setVisibility(View.VISIBLE);

        btnClose.setVisibility(View.VISIBLE);
    }
    
    private void showLoginState() {
        layoutScanQR.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.VISIBLE);
        layoutUserInfo.setVisibility(View.GONE);
        updateLoginButtonState();
        

        btnManual.setVisibility(View.GONE);
        
        // Показываем общую кнопку "Закрыть окно"
        btnClose.setVisibility(View.VISIBLE);
    }
    
    private void showUserInfoState() {
        layoutScanQR.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.GONE);
        layoutUserInfo.setVisibility(View.VISIBLE);
        
        // Заполняем информацию о пользователе
        User user = getUserUseCase.execute();
        if (user != null) {
            tvUserFullName.setText(user.getFullName());
            tvUserRole.setText(user.getRole());
        }
        

        btnManual.setVisibility(View.GONE);
        btnClose.setVisibility(View.GONE);
    }
    
    private void updateLoginButtonState() {
        String login = etAuthLogin.getText().toString().trim();
        String password = etAuthPassword.getText().toString().trim();
        btnLogin.setEnabled(!login.isEmpty() && !password.isEmpty());
    }
    
    /**
     * Переключает диалог в состояние ручного ввода логина и пароля с пустыми полями
     */
    private void showManualLoginState() {

        etAuthLogin.setText("");
        etAuthPassword.setText("");
        
        // Очищаем сохраненную информацию о пользователе
        scannedUserInfo = null;
        
        // Включаем поле логина для ручного ввода
        etAuthLogin.setEnabled(true);
        
        // Переключаемся на состояние ввода логина
        showLoginState();
        

        etAuthLogin.requestFocus();
        
        Toast.makeText(requireContext(), "Введите логин и пароль вручную", Toast.LENGTH_SHORT).show();
    }
     
     /**
      * Настраивает обработчик касаний для скрытия клавиатуры при касании вне полей ввода
      */
     private void setupTouchListener(View rootView) {
         rootView.setOnTouchListener((v, event) -> {
             if (event.getAction() == MotionEvent.ACTION_DOWN) {
                 // Получаем View, который имеет фокус
                 View focusedView = getDialog() != null ? getDialog().getCurrentFocus() : null;
                 
                 if (focusedView instanceof EditText) {
                     // Проверяем, произошло ли касание вне поля ввода
                     if (!isTouchInsideView(event, focusedView)) {
                         hideKeyboard();
                         focusedView.clearFocus();
                     }
                 }
             }
             return false;
         });
     }
     
     /**
      * Проверяет, произошло ли касание внутри указанного View
      */
     private boolean isTouchInsideView(MotionEvent event, View view) {
         if (view == null) return false;
         
         int[] location = new int[2];
         view.getLocationOnScreen(location);
         int x = location[0];
         int y = location[1];
         int width = view.getWidth();
         int height = view.getHeight();
         
         return event.getRawX() >= x && event.getRawX() <= (x + width) &&
                event.getRawY() >= y && event.getRawY() <= (y + height);
     }
     
     /**
      * Скрывает клавиатуру
      */
     private void hideKeyboard() {
         if (getContext() != null) {
             InputMethodManager imm = 
                 (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
             
             if (imm != null && getDialog() != null) {
                 View currentFocus = getDialog().getCurrentFocus();
                 if (currentFocus != null) {
                     imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                 }
             }
         }
     }
     
     /**
      * Настраивает размер диалога для лучшего отображения с клавиатурой
      */
     private void setupDialogSize(AlertDialog dialog) {
         dialog.setOnShowListener(dialogInterface -> {
             if (dialog.getWindow() != null) {
                 WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                 

                 DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                 int screenHeight = displayMetrics.heightPixels;
                 int dialogHeight = (int) (screenHeight * 0.95);
                 
                 layoutParams.height = dialogHeight;
                 layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                 

                 layoutParams.gravity = Gravity.CENTER;
                 
                 dialog.getWindow().setAttributes(layoutParams);
                 

                 dialog.getWindow().setSoftInputMode(
                     WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                     WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                 );
             }
         });
     }
     
     @Override
    public void onResume() {
        super.onResume();
        registerBarcodeReceiver();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterBarcodeReceiver();
    }
    
    private void registerBarcodeReceiver() {
        if (barcodeDataReceiver != null && getContext() != null) {
            IntentFilter scannerFilter = new IntentFilter("com.example.scannerapp.ACTION_BARCODE_DATA");
            scannerFilter.addAction("android.intent.action.SCAN");
            scannerFilter.addAction("com.google.zxing.client.android.SCAN");
            scannerFilter.addAction("com.symbol.datawedge.api.ACTION");
            scannerFilter.addAction("com.scanner.broadcast");
            scannerFilter.addAction("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(barcodeDataReceiver, scannerFilter, Context.RECEIVER_EXPORTED);
            } else {
                getContext().registerReceiver(barcodeDataReceiver, scannerFilter);
            }
            
            Log.i("AuthenticationDialog", "BroadcastReceiver зарегистрирован для сканера");
        }
    }
    
    private void unregisterBarcodeReceiver() {
        if (barcodeDataReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(barcodeDataReceiver);
                Log.i("AuthenticationDialog", "BroadcastReceiver отменен");
            } catch (IllegalArgumentException e) {
                Log.e("AuthenticationDialog", "Ошибка при отписке barcodeDataReceiver: " + e.getMessage());
            }
        }
    }
} 