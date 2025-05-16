package com.step.tcd_rpkb.domain.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T data);
    void onError(Exception exception);
}
