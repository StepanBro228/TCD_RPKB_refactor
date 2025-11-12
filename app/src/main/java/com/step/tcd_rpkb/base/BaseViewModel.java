package com.step.tcd_rpkb.base;

import androidx.lifecycle.ViewModel;

/**
 * Базовый класс для всех ViewModel в приложении.
 * Можно добавить общую логику или LiveData здесь в будущем.
 */
public class BaseViewModel extends ViewModel {

    @Override
    protected void onCleared() {
        super.onCleared();

    }
} 