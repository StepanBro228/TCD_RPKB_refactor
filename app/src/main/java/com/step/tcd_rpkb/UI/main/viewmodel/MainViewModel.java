package com.step.tcd_rpkb.UI.main.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.usecase.GetCredentialsUseCase;
import com.step.tcd_rpkb.domain.usecase.GetOnlineModeUseCase;
import com.step.tcd_rpkb.domain.usecase.SaveCredentialsUseCase;
import com.step.tcd_rpkb.domain.usecase.SetOnlineModeUseCase;
import com.step.tcd_rpkb.domain.usecase.CheckServerAvailabilityUseCase;
import com.step.tcd_rpkb.domain.usecase.GetDatabaseURLUseCase;
import com.step.tcd_rpkb.domain.usecase.SetDatabaseURLUseCase;
import com.step.tcd_rpkb.domain.repository.ServerAvailabilityCallback;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {

    // --- LiveData для LoginDialogFragment ---
    private final MutableLiveData<String> _username = new MutableLiveData<>();
    public LiveData<String> username = _username;

    private final MutableLiveData<String> _password = new MutableLiveData<>();
    public LiveData<String> password = _password;
    private final MutableLiveData<String> _deviceNum = new MutableLiveData<>();
    public LiveData<String> deviceNum = _deviceNum;

    private final MutableLiveData<Boolean> _isOnlineMode = new MutableLiveData<>();
    public LiveData<Boolean> isOnlineMode = _isOnlineMode;

    private final MutableLiveData<String> _databaseURL = new MutableLiveData<>();
    public LiveData<String> databaseURL = _databaseURL;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _serverStatusText = new MutableLiveData<>();
    public LiveData<String> serverStatusText = _serverStatusText;

    private final MutableLiveData<Integer> _serverStatusTextColor = new MutableLiveData<>();
    public LiveData<Integer> serverStatusTextColor = _serverStatusTextColor; // Для цвета текста статуса


    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    // --- UseCases ---
    private final GetCredentialsUseCase getCredentialsUseCase;
    private final GetOnlineModeUseCase getOnlineModeUseCase;
    private final SaveCredentialsUseCase saveCredentialsUseCase;
    private final SetOnlineModeUseCase setOnlineModeUseCase;
    private final CheckServerAvailabilityUseCase checkServerAvailabilityUseCase;
    private final GetDatabaseURLUseCase getDatabaseURLUseCase;
    private final SetDatabaseURLUseCase setDatabaseURLUseCase;

    @Inject
    public MainViewModel(
            GetCredentialsUseCase getCredentialsUseCase,
            GetOnlineModeUseCase getOnlineModeUseCase,
            SaveCredentialsUseCase saveCredentialsUseCase,
            SetOnlineModeUseCase setOnlineModeUseCase,
            CheckServerAvailabilityUseCase checkServerAvailabilityUseCase,
            GetDatabaseURLUseCase getDatabaseURLUseCase,
            SetDatabaseURLUseCase setDatabaseURLUseCase
    ) {
        this.getCredentialsUseCase = getCredentialsUseCase;
        this.getOnlineModeUseCase = getOnlineModeUseCase;
        this.saveCredentialsUseCase = saveCredentialsUseCase;
        this.setOnlineModeUseCase = setOnlineModeUseCase;
        this.checkServerAvailabilityUseCase = checkServerAvailabilityUseCase;
        this.getDatabaseURLUseCase = getDatabaseURLUseCase;
        this.setDatabaseURLUseCase = setDatabaseURLUseCase;
        
        loadInitialSettings();
    }

    public void loadInitialSettings() {
        Credentials credentials = getCredentialsUseCase.execute();
        _username.setValue(credentials.getUsername());
        _password.setValue(credentials.getPassword());
        _deviceNum.setValue(credentials.getDeviceNum());
        _isOnlineMode.setValue(getOnlineModeUseCase.execute());
        _databaseURL.setValue(getDatabaseURLUseCase.execute());
    }

    public void checkServerAvailability(String currentUsername, String currentPassword, String currentDeviceNum) {
        if (currentUsername.isEmpty() || currentPassword.isEmpty()) {
            _toastMessage.setValue("Введите логин и пароль, чтобы они сохранились для проверки сервера");
            _serverStatusText.setValue("Введите логин и пароль");
            return;
        }
        saveCredentialsUseCase.execute(new Credentials(currentUsername, currentPassword, currentDeviceNum));

        _isLoading.setValue(true);
        _serverStatusText.setValue("Проверка доступности сервера...");

        checkServerAvailabilityUseCase.execute(new ServerAvailabilityCallback() {
            @Override
            public void onResult(boolean isAvailable) {
                _isLoading.setValue(false);
                if (isAvailable) {
                    _serverStatusText.setValue("Сервер доступен (ViewModel)");
                } else {
                    _serverStatusText.setValue("Сервер недоступен (ViewModel)");
                }
            }
        });
    }

    public void handleLoginResult(String username, String password, String deviceNum, boolean onlineMode, String DatabaseURL) {
        Log.d("MainViewModel", "Сохранение настроек: режим=" + (onlineMode ? "онлайн" : "оффлайн") + 
              ", база=" + DatabaseURL);
        
        saveCredentialsUseCase.execute(new Credentials(username, password, deviceNum));
        setOnlineModeUseCase.execute(onlineMode);
        setDatabaseURLUseCase.execute(DatabaseURL);

        // Обновляем LiveData, которые слушает диалог (если он еще открыт и слушает)
        _username.setValue(username);
        _password.setValue(password);
        _isOnlineMode.setValue(onlineMode);
        _databaseURL.setValue(DatabaseURL);


        String modeType = onlineMode ? "онлайн режим" : "оффлайн режим";
        String message = String.format("Настройки сохранены: %s (%s)", modeType, DatabaseURL);
        _toastMessage.setValue(message); 
        
        Log.d("MainViewModel", "Настройки успешно сохранены");
    }

    public void onOnlineModeChanged(boolean isOnline) {
        _isOnlineMode.setValue(isOnline);

    }


} 