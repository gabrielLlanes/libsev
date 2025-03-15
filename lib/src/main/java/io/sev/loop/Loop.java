package io.sev.loop;

import io.sev.queue.IntrusiveQueue;

public abstract class Loop<L extends Loop<L, T>, T extends Completion<L, T>> {

    protected long active = 0;

    protected IntrusiveQueue<T> unqueued = new IntrusiveQueue<>();

    protected Loop() {

    }

    public abstract void tick() throws Throwable;

    public abstract void enqueue(T completion);

    //public abstract void cancel(T completion);
    
}
