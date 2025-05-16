package com.step.tcd_rpkb.domain.usecase;

import com.step.tcd_rpkb.domain.repository.ServerAvailabilityCallback;
import com.step.tcd_rpkb.domain.repository.ServerAvailabilityRepository;

import javax.inject.Inject;

public class CheckServerAvailabilityUseCase {
    private final ServerAvailabilityRepository repository;

    @Inject
    public CheckServerAvailabilityUseCase(ServerAvailabilityRepository repository) {
        this.repository = repository;
    }

    public void execute(ServerAvailabilityCallback callback) {
        repository.checkServerAvailability(callback);
    }
} 