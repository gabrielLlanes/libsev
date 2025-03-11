package io.sev.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BufferTest {
    @Test
    public void addressTest() {
        Buffer buf = new Buffer(1024);
        long address = buf.address();
        assertTrue(address > 0);
        System.out.println("Native Address: " + Long.toHexString(address));
        System.out.println("BUFFER TEST");
    }
}
