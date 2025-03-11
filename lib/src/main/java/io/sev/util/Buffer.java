package io.sev.util;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.sev.socket.Socket;
import io.sev.uring.IOUring;

public class Buffer {

    private static final int SOCKADDR_IN_SIZE = 16;

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
        return this.memory;
    }

    public int capacityCqe() {
        return this.capacity / IOUring.CQE_SIZE;
    }

    public static Buffer initInet4(Inet4Address address, int port) {
        Buffer b = new Buffer(SOCKADDR_IN_SIZE);
        ByteBuffer buf = b.memory();
        buf.putShort(0, Socket.AF_INET);
        buf.put(2, (byte) (port >> 8));
        buf.put(3, (byte) port);
        buf.put(4, address.getAddress());
        return b;
    }

    
}
