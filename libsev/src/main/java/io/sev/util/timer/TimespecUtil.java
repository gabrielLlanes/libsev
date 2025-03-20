package io.sev.util.timer;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static io.sev.Native.*;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import io.sev.util.unix.Macros;
import io.sev.util.unix.UnixException;

public class TimespecUtil {

    public static final MemoryLayout TIMESPEC_LAYOUT = structLayout(JAVA_LONG, JAVA_LONG);

    public static MemorySegment timespec(long tv_sec, long tv_nsec, SegmentAllocator allocator) {
        MemorySegment timespec = allocator.allocate(TIMESPEC_LAYOUT);
        timespec.setAtIndex(JAVA_LONG, 0, tv_sec);
        timespec.setAtIndex(JAVA_LONG, 1, tv_nsec);
        return timespec;
    }

    public static MemorySegment timespecNext(long ns, SegmentAllocator allocator) {
        try {
            MemorySegment currentTs = callocSegment(1, 16);
            clockGetTime(Macros.CLOCK_MONOTONIC, currentTs);
            MemorySegment nextTs = timespec(getTvSec(currentTs), getTvNsec(currentTs) + ns, allocator);
            free(currentTs);
            return nextTs;
        } catch(UnixException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static long getTvSec(MemorySegment ts) {
        return ts.getAtIndex(JAVA_LONG, 0);
    }

    public static long getTvNsec(MemorySegment ts) {
        return ts.getAtIndex(JAVA_LONG, 1);
    }
    
}
