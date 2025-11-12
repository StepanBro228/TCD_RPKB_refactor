package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.UserRepository;

import javax.inject.Inject;

public class IsUserAuthenticatedUseCase {
    private final UserRepository userRepository;

    @Inject
    public IsUserAuthenticatedUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean execute() {
        User user = userRepository.getUser();
        // Если пользователь существует, то он авторизован
        // UserRepository теперь возвращает null для неавторизованных пользователей
        return user != null;
    }
} 