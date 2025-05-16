package com.step.tcd_rpkb.UI.main.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.usecase.GetCredentialsUseCase;
import com.step.tcd_rpkb.domain.usecase.GetOnlineModeUseCase;
import com.step.tcd_rpkb.domain.usecase.SaveCredentialsUseCase;
import com.step.tcd_rpkb.domain.usecase.SetOnlineModeUseCase;
import com.step.tcd_rpkb.domain.usecase.CheckServerAvailabilityUseCase;
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

    private final MutableLiveData<Boolean> _isOnlineMode = new MutableLiveData<>();
    public LiveData<Boolean> isOnlineMode = _isOnlineMode;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _serverStatusText = new MutableLiveData<>();
    public LiveData<String> serverStatusText = _serverStatusText;

    private final MutableLiveData<Integer> _serverStatusTextColor = new MutableLiveData<>();
    public LiveData<Integer> serverStatusTextColor = _serverStatusTextColor; // Для цвета текста статуса

    // --- LiveData для MainActivity (уже было) ---
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    // --- UseCases ---
    private final GetCredentialsUseCase getCredentialsUseCase;
    private final GetOnlineModeUseCase getOnlineModeUseCase;
    private final SaveCredentialsUseCase saveCredentialsUseCase;
    private final SetOnlineModeUseCase setOnlineModeUseCase;
    private final CheckServerAvailabilityUseCase checkServerAvailabilityUseCase;

    @Inject
    public MainViewModel(
            GetCredentialsUseCase getCredentialsUseCase,
            GetOnlineModeUseCase getOnlineModeUseCase,
            SaveCredentialsUseCase saveCredentialsUseCase,
            SetOnlineModeUseCase setOnlineModeUseCase,
            CheckServerAvailabilityUseCase checkServerAvailabilityUseCase
    ) {
        this.getCredentialsUseCase = getCredentialsUseCase;
        this.getOnlineModeUseCase = getOnlineModeUseCase;
        this.saveCredentialsUseCase = saveCredentialsUseCase;
        this.setOnlineModeUseCase = setOnlineModeUseCase;
        this.checkServerAvailabilityUseCase = checkServerAvailabilityUseCase;
        
        loadInitialSettings();
    }

    public void loadInitialSettings() {
        Credentials credentials = getCredentialsUseCase.execute();
        _username.setValue(credentials.getUsername());
        _password.setValue(credentials.getPassword());
        _isOnlineMode.setValue(getOnlineModeUseCase.execute());
    }

    public void checkServerAvailability(String currentUsername, String currentPassword) {
        if (currentUsername.isEmpty() || currentPassword.isEmpty()) {
            _toastMessage.setValue("Введите логин и пароль, чтобы они сохранились для проверки сервера");
            _serverStatusText.setValue("Введите логин и пароль");
            return;
        }
        saveCredentialsUseCase.execute(new Credentials(currentUsername, currentPassword));

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

    public void handleLoginResult(String username, String password, boolean onlineMode) {
        saveCredentialsUseCase.execute(new Credentials(username, password));
        setOnlineModeUseCase.execute(onlineMode);

        // Обновляем LiveData, которые слушает диалог (если он еще открыт и слушает)
        _username.setValue(username);
        _password.setValue(password);
        _isOnlineMode.setValue(onlineMode);

        String message = onlineMode ? "Настройки сохранены: Онлайн режим" : "Настройки сохранены: Оффлайн режим";
        _toastMessage.setValue(message); 
    }

    public void onOnlineModeChanged(boolean isOnline) {
        _isOnlineMode.setValue(isOnline);
        // Можно также сразу сохранять изменение режима, если это требуется по логике
        // setOnlineModeUseCase.execute(isOnline);
        // Но обычно это делается при явном сохранении всего диалога
    }
} 