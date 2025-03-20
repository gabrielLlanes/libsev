package io.sev;

import static io.sev.util.unix.UnixException.unixException;
import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import io.sev.util.unix.UnixException;

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

    private static Object invokeWithArgumentsUnchecked(MethodHandle handle, Object... args) {
        Object result = null;
        try {
            result = handle.invokeWithArguments(args);
            return result;
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void invokeWithArgumentsVoidUnchecked(MethodHandle handle, Object... args) {
        try {
            handle.invokeWithArguments(args);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static long calloc(long nmemb, long size) {
        return (long) invokeWithArgumentsUnchecked(callocHandle, nmemb, size);
    }

    public static MemorySegment callocSegment(long nmemb, long size) {
        long address = calloc(nmemb, size);
        if(address == 0L) return MemorySegment.NULL;
        return MemorySegment.ofAddress(address).reinterpret(nmemb * size);
    }

    public static void free(long address) {
        invokeWithArgumentsVoidUnchecked(freeHandle, address);
    }

    public static void free(MemorySegment segment) {
        invokeWithArgumentsVoidUnchecked(freeHandle, segment.address());
    }

    public static void clockGetTime(int clockid, MemorySegment tp) throws UnixException {
        int res = (int) invokeWithArgumentsUnchecked(clockGetTimeHandle, clockid, tp);
        if(res < 0) {
            unixException(res);
        }
    }

    public static SegmentAllocator callocator() {
        if(callocator == null) {
            callocator = (byteSize, byteAlignment) -> {
                return callocSegment(1, byteSize);
            };
        }
        return callocator;
    }

}
