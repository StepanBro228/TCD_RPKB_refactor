package com.step.tcd_rpkb.utils;

/**
 * Используется как обертка для данных, которые представляют собой событие.
 */
public class Event<T> {

    private T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        if (content == null) {
            throw new IllegalArgumentException("null values in Event are not allowed.");
        }
        this.content = content;
    }

    /**
     * Возвращает контент и предотвращает его повторное использование.
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        } else {
            hasBeenHandled = true;
            return content;
        }
    }

    /**
     * Возвращает контент, даже если он уже был обработан.
     */
    public T peekContent() {
        return content;
    }

    public boolean hasBeenHandled() {
        return hasBeenHandled;
    }
} 