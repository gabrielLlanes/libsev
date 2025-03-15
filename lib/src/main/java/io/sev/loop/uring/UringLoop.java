package io.sev.loop.uring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.HashMap;
import java.util.Map;

import io.sev.Native;
import io.sev.loop.Loop;
import io.sev.queue.IntrusiveQueue;
import io.sev.uring.IoUring;
import io.sev.util.errors.UnixException;
import io.sev.util.value.BooleanWrapper;
import io.sev.util.value.LongWrapper;

import static io.sev.util.errors.UnixException.*;
import static io.sev.uring.IoUringCQE.*;
import static io.sev.util.timer.TimespecUtil.*;
import static io.sev.uring.IoUring.*;
import static io.sev.Native.*;

public class UringLoop extends Loop<UringLoop, UringCompletion> {

    private final IoUring ring;

    private static final SegmentAllocator callocator = Native.callocator();

    private IntrusiveQueue<UringCompletion> completed = new IntrusiveQueue<>();

    private Map<Long, UringCompletion> inUring = new HashMap<>();

    private UringLoop() {
        try {
            ring = IoUring.init(callocator);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static UringLoop init() {
        return new UringLoop();
    }

    public void deinit() {
        try {
            free(cqes);
            ring.queueExit();
            free(ring.memorySegment());
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static final int NCQES = 128;

    private final MemorySegment cqes = callocator.allocate(IO_URING_CQE_LAYOUT, NCQES);

    @Override
    public void tick() throws Throwable {

    }

    public void runForNs(long nanoseconds) {
        try {
            MemorySegment currentTs = callocator.allocate(TIMESPEC_LAYOUT);
            clockGetTime(CLOCK_MONOTONIC, currentTs);
            MemorySegment timeoutTs = timespec(getTvSec(currentTs), getTvNsec(currentTs) + nanoseconds, callocator);

            LongWrapper timeouts = LongWrapper.of(0L);
            BooleanWrapper etime = BooleanWrapper.of(false);
            while(!etime.value()) {
                MemorySegment timeoutSqe = ring.getSqe();
                if(timeoutSqe.equals(MemorySegment.NULL)) {
                    flushSubmissions(0, timeouts, etime);
                    timeoutSqe = ring.getSqe();
                    if(timeoutSqe.equals(MemorySegment.NULL)) {
                        throw new RuntimeException();
                    }
                }
                IoUring.prepTimeout(timeoutSqe, timeoutTs, 0, IORING_TIMEOUT_ABS);
                IoUring.sqeSetData64(timeoutSqe, 0L);
                timeouts.increment();

                flush(1, timeouts, etime);
            }

            while(timeouts.value() > 0L) {
                flushCompletions(0, timeouts, etime);
            }
            free(currentTs);
            free(timeoutTs);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    
    @Override
    public void enqueue(UringCompletion completion) {
        try {
            MemorySegment sqe = ring.getSqe();
            if(sqe.equals(MemorySegment.NULL)) {
                //no space in submission queue, put in unqueued for now
                unqueued.offer(completion);
                return;
            }
            completion.prep(sqe);
            inUring.put(completion.addressId, completion);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void flush(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws Throwable {
        flushSubmissions(waitNr, timeouts, etime);
        flushCompletions(0, timeouts, etime);

        IntrusiveQueue<UringCompletion> copy = this.unqueued;
        this.unqueued = new IntrusiveQueue<>();
        UringCompletion curr = null;
        while((curr = copy.poll()) != null) {
            enqueue(curr);
        }

        while((curr = completed.poll()) != null) {
            free(curr.addressId);
            if(curr.complete()) {
                enqueue(curr);
            }
        }
    }
    
    private void flushCompletions(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws Throwable {
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
            if(completed > waitRemaining) waitRemaining = 0;
            else waitRemaining -= completed;
            for(int i = 0; i < completed; i++) {
                long userData = getUserData(cqes, i);
                int result = getResult(cqes, i);
                if(userData == 0L) {
                    timeouts.decrement();
                    if(result == -ETIME) {
                        etime.set(true);
                    }
                    continue;
                }
                UringCompletion completion = inUring.remove(userData);
                completion.res = result;
                this.completed.offer(completion);
            }
            if(completed < NCQES) break;
        }
    }

    private void flushSubmissions(int waitNr, LongWrapper timeouts, BooleanWrapper etime) throws Throwable {
        while(true) {
            try {
                ring.submitAndWait(waitNr);
            } catch(UnixException ex) {
                int errno = ex.errno();
                if(errno == EINTR) continue;
                else if(errno == EBUSY || errno == EAGAIN) {
                    flushCompletions(1, timeouts, etime);
                    continue;
                } else {
                    throw ex;
                }
            }
            break;
        }
    }
    
}
