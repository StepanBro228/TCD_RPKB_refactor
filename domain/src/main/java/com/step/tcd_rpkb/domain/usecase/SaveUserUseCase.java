package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.UserRepository;

import javax.inject.Inject;

public class SaveUserUseCase {
    private final UserRepository userRepository;

    @Inject
    public SaveUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(User user) {
        userRepository.saveUser(user);
    }
} 