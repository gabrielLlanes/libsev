package io.sev.util.timer;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public class TimespecUtil {

    public static final MemoryLayout TIMESPEC_LAYOUT = structLayout(JAVA_LONG, JAVA_LONG);

    public static MemorySegment timespec(long tv_sec, long tv_nsec, SegmentAllocator allocator) {
        MemorySegment timespec = allocator.allocate(TIMESPEC_LAYOUT);
        timespec.setAtIndex(JAVA_LONG, 0, tv_sec);
        timespec.setAtIndex(JAVA_LONG, 1, tv_nsec);
        return timespec;
    }

    public static long getTvSec(MemorySegment ts) {
        return ts.getAtIndex(JAVA_LONG, 0);
    }

    public static long getTvNsec(MemorySegment ts) {
        return ts.getAtIndex(JAVA_LONG, 1);
    }
    
}
