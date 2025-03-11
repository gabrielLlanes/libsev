package io.sev.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Buffer {

    private final ByteBuffer memory;

    private final int capacity;

    private final long address;
    
    public Buffer(int capacity) {
        this.capacity = capacity;
        this.memory = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        if(!this.memory.isDirect()) throw new OutOfMemoryError("unable to allocate direct");
        this.address = BufferUtil.memoryAddress(this.memory);
    }

    public long address() {
        return this.address;
    }

    public int capacity() {
        return this.capacity;
    }

    public ByteBuffer memory() {
        return this.memory();
    }

    
}
