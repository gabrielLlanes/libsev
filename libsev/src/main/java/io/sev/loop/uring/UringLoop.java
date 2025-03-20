package io.sev.loop.uring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import io.sev.Native;
import io.sev.loop.Callback;
import io.sev.loop.Loop;
import io.sev.loop.Operation;
import io.sev.queue.IntrusiveQueue;
import io.sev.uring.IoUring;
import io.sev.util.unix.Macros;
import io.sev.util.unix.UnixException;
import io.sev.util.value.BooleanWrapper;
import io.sev.util.value.LongWrapper;

import static io.sev.uring.IoUringCQE.*;
import static io.sev.util.timer.TimespecUtil.*;
import static io.sev.util.unix.Macros.*;
import static io.sev.Native.*;

public class UringLoop extends Loop<UringLoop, UringCompletion> {

    private static final SegmentAllocator callocator = Native.callocator();

    private static final int NCQES = 128;

    private final IoUring ring;

    private final Map<Long, UringCompletion> inUring = new HashMap<>();

    private final MemorySegment cqes = callocator.allocate(IO_URING_CQE_LAYOUT, NCQES);

    private UringLoop() throws UnixException {
            ring = IoUring.init(callocator);
    }

    public static UringLoop init() throws UnixException {
        return new UringLoop();
    }

    public void deinit() {
        free(cqes);
        ring.queueExit();
        free(ring.ringAddress());
    }

    @Override
    public void runAll() {
        try {
            while(active > 0 || !unqueuedCompletions.isEmpty()) {
                flush(1, null, null);
            }
        } catch(UnixException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void runOnce() {
        try {
            if(active > 0 || !unqueuedCompletions.isEmpty()) {
                flush(1, null, null);
            }
        } catch(UnixException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void enqueue(UringCompletion completion) {
        long sqe = ring.getSqe();
        if(sqe == 0L) {
            //no space in submission queue, put in unqueued for now
            unqueuedCompletions.offer(completion);
            return;
        }
        completion.prep(sqe);
        while(inUring.containsKey(completion.id)) {
            completion.rollId();
        }
        inUring.put(completion.id, completion);
        active++;
    }

    @Override
    public void cancel(UringCompletion completion, Callback<UringLoop, UringCompletion> callback) {
        Operation cancelOperation = new Operation.Cancel(completion.id);
        UringCompletion cancelCompletion = UringCompletion.of(cancelOperation, null, callback);
        enqueue(cancelCompletion);
    }

    @Override
    public void timer(long ns, Object context,
            Callback<UringLoop, UringCompletion> callback) {
        MemorySegment nextTs = timespecNext(ns, callocator);
        Operation timerOperation = new Operation.Timer()
                                .ts(nextTs)
                                .count(0)
                                .flags(IORING_TIMEOUT_ABS);
        Callback<UringLoop, UringCompletion> timerCallback = (ctx, loop, completion, result) -> {
            free(nextTs);
            callback.invoke(ctx, loop, completion, result);
            return false;
        };
        UringCompletion timerCompletion = new UringCompletion()
                                            .operation(timerOperation)
                                            .context(context)
                                            .callback(timerCallback);
        enqueue(timerCompletion);
    }

    public void runForNs(long ns) {
        try {
            MemorySegment timeoutTs = timespecNext(ns, callocator);

            LongWrapper timeouts = LongWrapper.of(0L);
            BooleanWrapper etime = BooleanWrapper.of(false);

            long timeoutSqe = ring.getSqe();
                if(timeoutSqe == 0L) {
                    flushSubmissions(0, timeouts, etime);
                    timeoutSqe = ring.getSqe();
                    if(timeoutSqe == 0L) {
                        throw new RuntimeException();
                    }
                }
            IoUring.prepTimeout(timeoutSqe, timeoutTs, 0, IORING_TIMEOUT_ABS);
            IoUring.sqeSetData64(timeoutSqe, 0L);
            timeouts.increment();

            while(!etime.value()) {
                flush(1, timeouts, etime);
            }

            while(timeouts.value() > 0L) {
                flushCompletions(0, timeouts, etime);
            }
            free(timeoutTs);
        } catch(UnixException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void enqueueUnqueued() {
        Queue<UringCompletion> copy = this.unqueuedCompletions;
        this.unqueuedCompletions = new IntrusiveQueue<>();
        UringCompletion curr = null;
        while((curr = copy.poll()) != null) {
            enqueue(curr);
        }
    }

    private void flush(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws UnixException {
        flushSubmissions(waitNr, timeouts, etime);
        flushCompletions(0, timeouts, etime);
        enqueueUnqueued();
    }
    
    private void flushCompletions(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws UnixException {
        int waitRemaining = waitNr;
        while(true) {
            int completed = 0;
            try {
                completed = ring.copyCqes(cqes, NCQES, waitRemaining);
            } catch (UnixException ex) {
                if(ex.errno() == EINTR) {
                    continue;
                } else {
                    throw ex;
                }
            }
            if(completed > waitRemaining) {
                waitRemaining = 0;
            } else {
                waitRemaining -= completed;
            }
            for(int i = 0; i < completed; i++) {
                long userData = getUserData(cqes, i);
                int result = getResult(cqes, i);
                if(userData == 0L) {
                    System.out.println("timeout cqe detected");
                    if(timeouts != null) {
                        timeouts.decrement();
                    }
                    if(result == -ETIME) {
                        System.out.println("timeout completed with ETIME");
                        if(etime != null) {
                            etime.set(true);
                        }
                    }
                    continue;
                }
                UringCompletion completion = inUring.remove(userData);
                boolean enqueueAgain = completion.complete(this, result);
                active--;
                if(enqueueAgain && completion.operation.op != Operation.Op.CANCEL) {
                    enqueue(completion);
                } else {
                    //free(userData);
                }
            }
            if(completed < NCQES) {
                break;
            }
        }
    }

    private void flushSubmissions(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws UnixException {
        while(true) {
            try {
                ring.submitAndWait(waitNr);
            } catch(UnixException ex) {
                int errno = ex.errno();
                if(errno == EINTR) {
                    continue;
                } else {
                    throw ex;
                }
            }
            break;
        }
    }
    
}
