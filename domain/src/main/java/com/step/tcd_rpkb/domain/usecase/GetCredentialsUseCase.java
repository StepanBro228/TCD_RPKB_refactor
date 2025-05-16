package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import javax.inject.Inject;

public class GetCredentialsUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public GetCredentialsUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public Credentials execute() {
        return userSettingsRepository.getCredentials();
    }
} 