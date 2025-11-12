package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import javax.inject.Inject;

public class SetDatabaseURLUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public SetDatabaseURLUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void execute(String URL) {
        userSettingsRepository.setDatabaseURL(URL);
    }
} 