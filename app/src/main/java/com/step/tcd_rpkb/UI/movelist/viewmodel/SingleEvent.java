package com.step.tcd_rpkb.UI.movelist.viewmodel;

// Вспомогательный класс для событий, которые должны обрабатываться один раз
public class SingleEvent<T> {
    private T content;
    private boolean hasBeenHandled = false;

    public SingleEvent(T content) {
        if (content == null) {
            // throw new IllegalArgumentException("null values in SingleEvent are not supported.");
            // В данном контексте, если content это, например, сообщение об ошибке,
            // null может быть легитимным значением для "нет ошибки/сообщения".
            // Однако для событий навигации null обычно не используется.
            // Если предполагается, что content не может быть null, раскомментировать throw.
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
