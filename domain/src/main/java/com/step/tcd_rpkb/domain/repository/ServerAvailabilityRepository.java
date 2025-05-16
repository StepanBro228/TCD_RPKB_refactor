package com.step.tcd_rpkb.domain.repository;
 
public interface ServerAvailabilityRepository {
    void checkServerAvailability(ServerAvailabilityCallback callback);
} 