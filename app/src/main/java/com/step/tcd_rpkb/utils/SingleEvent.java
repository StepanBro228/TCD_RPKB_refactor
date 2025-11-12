package com.step.tcd_rpkb.utils;

// Вспомогательный класс для событий, которые должны обрабатываться один раз
public class SingleEvent<T> {
    private T content;
    private boolean hasBeenHandled = false;

    public SingleEvent(T content) {
        if (content == null) {

        }
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        } else {
            hasBeenHandled = true;
            return content;
        }
    }

    @SuppressWarnings("unused")
    public boolean hasBeenHandled() {
        return hasBeenHandled;
    }

    public T peekContent() { // Посмотреть содержимое без пометки как обработанное
        return content;
    }
} 