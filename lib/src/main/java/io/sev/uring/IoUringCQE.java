package io.sev.uring;

import java.lang.foreign.StructLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.MemoryLayout.structLayout;

public class IoUringCQE {

    static final StructLayout IO_URING_CQE_LAYOUT = structLayout(JAVA_LONG, JAVA_INT, JAVA_INT);

    public static final int CQE_SIZE = 16;

    public static long getUserData(MemorySegment cqes, long index) {
        return (long) cqes.getAtIndex(JAVA_LONG, index * 2);
    }

    public static int getResult(MemorySegment cqes, long index) {
        return (int) cqes.getAtIndex(JAVA_INT, index * 4 + 2);
    }

    public static int getFlags(MemorySegment cqes, long index) {
        return (int) cqes.getAtIndex(JAVA_INT, index * 4 + 3);
    }
    
}
