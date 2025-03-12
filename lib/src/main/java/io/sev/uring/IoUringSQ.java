package io.sev.uring;

import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;

public class IoUringSQ {

    private static final ValueLayout KHEAD = ADDRESS.withName("khead");

    private static final ValueLayout KTAIL = ADDRESS.withName("ktail");

    private static final ValueLayout KRING_MASK = ADDRESS.withName("kring_mask");

    private static final ValueLayout KRING_ENTRIES = ADDRESS.withName("kring_entries");

    private static final ValueLayout KFLAGS = ADDRESS.withName("kflags");

    private static final ValueLayout KDROPPED = ADDRESS.withName("kdropped");

    private static final ValueLayout ARRAY = ADDRESS.withName("array");

    private static final ValueLayout SQES = ADDRESS.withName("sqes");

    private static final ValueLayout SQE_HEAD = JAVA_INT.withName("sqe_head");

    private static final ValueLayout SQE_TAIL = JAVA_INT.withName("sqe_tail");

    private static final ValueLayout RING_SZ = JAVA_LONG.withName("ring_sz");

    private static final ValueLayout RING_PTR = ADDRESS.withName("ring_ptr");

    private static final ValueLayout RING_MASK = JAVA_INT.withName("ring_mask");

    private static final ValueLayout RING_ENTRIES = JAVA_INT.withName("ring_entries");

    private static final SequenceLayout PAD = sequenceLayout(2, JAVA_INT);
    
    static final StructLayout IO_URING_SQ_LAYOUT = structLayout(KHEAD, KTAIL, KRING_MASK,
                                                                KRING_ENTRIES, KFLAGS, KDROPPED,
                                                                ARRAY, SQES, SQE_HEAD,
                                                                SQE_TAIL, RING_SZ, RING_PTR,
                                                                RING_MASK, RING_ENTRIES, PAD);
}
