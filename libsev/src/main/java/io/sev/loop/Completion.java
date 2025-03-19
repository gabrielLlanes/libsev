package io.sev.loop;

import io.sev.queue.IntrusiveQueue;

public abstract class Completion<L extends Loop<L, T>, T extends Completion<L, T>> extends IntrusiveQueue.Element<T> {
    
    public final Operation operation;

    public final Object context;

    public final Callback<L, T> callback;

    protected Completion(Operation operation, Object context, Callback<L, T> callback) {
        this.operation = operation;
        this.context = context;
        this.callback = callback;
    }
}
