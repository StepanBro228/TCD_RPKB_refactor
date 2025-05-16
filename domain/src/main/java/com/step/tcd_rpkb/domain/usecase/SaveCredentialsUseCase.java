package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.Credentials;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import javax.inject.Inject;

public class SaveCredentialsUseCase {

    private final UserSettingsRepository userSettingsRepository;

    @Inject
    public SaveCredentialsUseCase(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public void execute(Credentials credentials) {
        userSettingsRepository.saveCredentials(credentials);
    }

    // Если нужно принимать имя пользователя и пароль отдельно:
    // public void execute(String username, String password) {
    //    userSettingsRepository.saveCredentials(new Credentials(username, password));
    // }
} 