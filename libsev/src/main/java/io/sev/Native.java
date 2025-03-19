package io.sev;

import static io.sev.util.unix.UnixException.unixException;
import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public class Native {

    private static final MethodHandle callocHandle;

    private static final MethodHandle freeHandle;

    private static final MethodHandle clockGetTimeHandle;

    private static SegmentAllocator callocator = null;

    static {
        System.loadLibrary("sev");
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        
        MemorySegment callocSegment = lookup.findOrThrow("sev_calloc");
        FunctionDescriptor callocDescriptor = FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG);
        callocHandle = linker.downcallHandle(callocSegment, callocDescriptor);

        MemorySegment freeSegment = lookup.findOrThrow("sev_free");
        FunctionDescriptor freeDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG);
        freeHandle = linker.downcallHandle(freeSegment, freeDescriptor);

        MemorySegment clockGetTimeSegment = lookup.findOrThrow("sev_clockGetTime");
        FunctionDescriptor clockGetTimeDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
        clockGetTimeHandle = linker.downcallHandle(clockGetTimeSegment, clockGetTimeDescriptor);
    }

    public static long calloc(long nmemb, long size) throws Throwable {
        return (long) callocHandle.invokeExact(nmemb, size);
    }

    public static MemorySegment callocSegment(long nmemb, long size) throws Throwable {
        long address = (long) callocHandle.invokeExact(nmemb, size);
        if(address == 0L) return MemorySegment.NULL;
        return MemorySegment.ofAddress(address).reinterpret(nmemb * size);
    }

    public static void free(long address) throws Throwable {
        freeHandle.invokeExact(address);
    }

    public static void free(MemorySegment segment) throws Throwable {
        freeHandle.invokeExact(segment.address());
    }

    public static void clockGetTime(int clockid, MemorySegment tp) throws Throwable {
        int res = (int) clockGetTimeHandle.invokeExact(clockid, tp);
        if(res < 0) {
            unixException(res);
        }
    }

    public static SegmentAllocator callocator() {
        if(callocator == null) {
            callocator = (byteSize, byteAlignment) -> {
                try {
                    return callocSegment(1, byteSize);
                } catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            };
        }
        return callocator;
    }

}
