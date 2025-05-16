package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.model.User;
import com.step.tcd_rpkb.domain.repository.UserRepository;

import javax.inject.Inject;

public class GetUserUseCase {
    private final UserRepository userRepository;

    @Inject
    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute() {
        return userRepository.getUser();
    }
} 