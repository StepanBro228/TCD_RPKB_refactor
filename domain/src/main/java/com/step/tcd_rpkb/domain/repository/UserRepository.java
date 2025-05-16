package com.step.tcd_rpkb.domain.repository;

import com.step.tcd_rpkb.domain.model.User;

public interface UserRepository {
    void saveUser(User user);
    User getUser(); // Может возвращать null, если пользователь не сохранен
} 