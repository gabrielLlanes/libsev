package io.sev.util.uuid;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.UUID;

public class UUIDUtil {
    
    public static final MemoryLayout UUID_LAYOUT = sequenceLayout(16, JAVA_BYTE);

    // warning: makes an allocation that must be manually freed with Native.free()
    public static MemorySegment randomUUID(SegmentAllocator allocator) {
        UUID uuid = UUID.randomUUID();
        long lsb = uuid.getLeastSignificantBits();
        long msb = uuid.getMostSignificantBits();
        MemorySegment uuidSegment = allocator.allocate(JAVA_LONG, 2L);
        for(int i = 0; i < 8; i++) {
            uuidSegment.setAtIndex(JAVA_BYTE, (long) i, (byte) (lsb >> (i * 8)));
        }
        for(int i = 0; i < 8; i++) {
            uuidSegment.setAtIndex(JAVA_BYTE, (long) i + 8L, (byte) (msb >> (i * 8)));
        }
        return uuidSegment;
    }

}
