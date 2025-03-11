package io.sev.uring;

import java.io.IOException;

import io.sev.Native;

public class IoUring {

    private static final int DEFAULT_ENTRIES = 1024;

    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("could not load Native");
        }
    }

    private final long ringAddress;

    private IoUring(int entries, int flags) throws IOException {
        ringAddress = queueInit0(entries, flags);
    }

    public IoUring init(int entries, int flags) throws IOException {
        return new IoUring(entries, flags);
    }

    public IoUring init() throws IOException {
        return new IoUring(DEFAULT_ENTRIES, 0);
    }

    /*io operations:
     * nop
     * accept
     * connect
     * close
     * read
     * write
     * recv
     * send
     * recvmsg
     * sendmsg
     * shutdown
     * timeout
     */


    private static native long queueInit0(int entries, int flags) throws IOException;

    private static native long getSqe0(long ringAddress);

    private static native void setSqeData0(long sqeAddress, long userData, boolean isAddress);

    private static native void prepNop0(long sqeAddress);

    private static native void prepAccept0(long sqeAddress, int fd, long addr, long addrLen, int flags, boolean multishot);

    private static native void prepConnect0(long sqeAddress, int fd, long addr, int addrLen);

    private static native void prepClose0(long sqeAddress, int fd);

    private static native void prepRead0(long sqeAddress, int fd, long bufAddress, int nBytes, long offset);

    private static native void prepRecv0(long sqeAddress, int fd, long bufAddress, long bufLen, int flags);

    private static native void prepSend0(long sqeAddress, int fd, long bufAddress, long bufLen, int flags);

    private static native int submit0(long ringAddress);

}
