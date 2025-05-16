package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import javax.inject.Inject;

public class GetOnlineModeUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public GetOnlineModeUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public boolean execute() {
        return userSettingsRepository.isOnlineMode();
    }
} 