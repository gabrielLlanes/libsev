package io.sev.loop;

import java.util.Queue;

import org.jctools.queues.atomic.MpscChunkedAtomicArrayQueue;
import org.jctools.util.Pow2;

import io.sev.queue.IntrusiveQueue;


public abstract class Loop<L extends Loop<L, T>, T extends Completion<L, T>> {

    protected long active = 0;

    protected Queue<T> unqueuedCompletions = new IntrusiveQueue<>();

    protected final Queue<T> queuedTasks = new MpscChunkedAtomicArrayQueue<>(Pow2.MAX_POW2);

    protected Loop() {
    }

    public abstract void runOnce();

    public abstract void runAll();

    public abstract void enqueue(T completion);

    public abstract void cancel(T completion, Callback<L, T> callback);
    
}
