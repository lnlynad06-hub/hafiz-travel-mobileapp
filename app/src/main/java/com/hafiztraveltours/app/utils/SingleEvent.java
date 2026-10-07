package com.hafiztraveltours.app.utils;

/**
 * One-shot LiveData payload (consumed once — safe across rotation).
 */
public final class SingleEvent<T> {

    private final T content;
    private boolean consumed;

    public SingleEvent(T content) {
        this.content = content;
    }

    /** Returns the content on first call, null afterwards. */
    public T consume() {
        if (consumed) return null;
        consumed = true;
        return content;
    }

    /** Peeks without consuming (for debugging only). */
    public T peek() {
        return content;
    }
}
