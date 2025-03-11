package io.sev.util;

import java.nio.ByteBuffer;

import io.sev.Native;

public class BufferUtil {

    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("could not load Native");
        }
    }

    public static long memoryAddress(ByteBuffer buffer) {
        return memoryAddress0(buffer);
    }

    private static native long memoryAddress0(ByteBuffer buffer);
    
}
