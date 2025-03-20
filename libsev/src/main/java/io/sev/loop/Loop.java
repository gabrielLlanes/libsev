package io.sev.loop;

import io.sev.queue.IntrusiveQueue;


public abstract class Loop<L extends Loop<L, T>, T extends Completion<L, T>> {

    protected long active = 0;

    protected IntrusiveQueue<T> unqueuedCompletions = new IntrusiveQueue<>();

    protected Loop() {
    }

    public abstract void runOnce();

    public abstract void runAll();

    public abstract void enqueue(T completion);

    public abstract void cancel(T completion, Callback<L, T> callback);

    public abstract void timer(long ns, Object context, Callback<L, T> callback);
    
}
