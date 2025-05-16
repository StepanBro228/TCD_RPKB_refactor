package com.step.tcd_rpkb.data.datasources;

public interface DataSourceCallback<T> {
    void onSuccess(T data);
    void onError(Exception exception);
} 