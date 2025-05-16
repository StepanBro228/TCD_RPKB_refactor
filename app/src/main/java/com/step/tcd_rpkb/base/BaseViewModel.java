package com.step.tcd_rpkb.base;

import androidx.lifecycle.ViewModel;

/**
 * Базовый класс для всех ViewModel в приложении.
 * Можно добавить общую логику или LiveData здесь в будущем.
 */
public class BaseViewModel extends ViewModel {
    // Например, можно добавить LiveData для отображения общего индикатора загрузки или ошибок
    // protected final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    // public final LiveData<Boolean> isLoading = _isLoading;

    // protected final MutableLiveData<Event<String>> _errorMessage = new MutableLiveData<>();
    // public final LiveData<Event<String>> errorMessage = _errorMessage;

    @Override
    protected void onCleared() {
        super.onCleared();
        // Освобождение ресурсов, если необходимо
    }
} 