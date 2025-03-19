package io.sev.loop;

import io.sev.queue.IntrusiveQueue;

public abstract class Completion<L extends Loop<L, T>, T extends Completion<L, T>> extends IntrusiveQueue.Element<T> {
    
    public Operation operation;

    public Object context = null;

    public Callback<L, T> callback;

    protected Completion(Operation operation, Object context, Callback<L, T> callback) {
        this.operation = operation;
        this.context = context;
        this.callback = callback;
    }

    public T operation(Operation operation) {
        this.operation = operation;
        return (T) this;
    }

    public T context(Object context) {
        this.context = context;
        return (T) this;
    }

    public T callback(Callback<L, T> callback) {
        this.callback = callback;
        return (T) this;
    }

    protected Completion() {

    }
}
