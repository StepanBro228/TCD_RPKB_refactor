package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import javax.inject.Inject;

public class SetOnlineModeUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public SetOnlineModeUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void execute(boolean isOnline) {
        userSettingsRepository.setOnlineMode(isOnline);
    }
} 