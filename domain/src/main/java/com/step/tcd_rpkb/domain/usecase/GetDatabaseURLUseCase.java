package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import javax.inject.Inject;

public class GetDatabaseURLUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public GetDatabaseURLUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public String execute() {
        return userSettingsRepository.getDatabaseURL();
    }
} 