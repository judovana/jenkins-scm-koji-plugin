package org.fakekoji.functional;

import java.util.function.Function;

public class Result<V, E> {
    private final V value;
    private final E error;

    private Result(V value, E error) {
        this.value = value;
        this.error = error;
    }

    public static <T, U> Result<T, U> ok(T value) {
        return new Result<>(value, null);
    }

    public static <T, U> Result<T, U> err(U error) {
        return new Result<>(null, error);
    }

    public <T> Result<T, E> flatMap(Function<V, Result<T, E>> mapper) {
        if (isError()) {
            return Result.err(error);
        }
        return mapper.apply(value);
    }

    public <T> Result<T, E> map(Function<V, T> mapper) {
        if (isError()) {
            return Result.err(error);
        }
        return Result.ok(mapper.apply(value));
    }

    public boolean isOk() {
        return !isError();
    }

    public boolean isError() {
        return this.error != null;
    }

    public V getValue() {
        return this.value;
    }

    public E getError() {
        return this.error;
    }
}
