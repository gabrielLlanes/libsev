package io.sev.uring;

import java.net.Inet4Address;

import io.sev.Native;
import io.sev.util.Buffer;

/*
 * Provides wrapper methods around liburing
 */
public class IOUring {

    private static final int DEFAULT_ENTRIES = 1024;

    private static final long NULL = 0L;

    public static final int CQE_SIZE = 128;

    public static final int IORING_ENTER_GETEVENTS = 1;

    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("could not load Native");
        }
    }

    private final long ringAddress;

    private IOUring(int entries, int flags) throws IOUringException {
        ringAddress = queueInit0(entries, flags);
    }

    public static IOUring init(int entries, int flags) throws IOUringException {
        return new IOUring(entries, flags);
    }

    public static IOUring init() throws IOUringException {
        return init(DEFAULT_ENTRIES, 0);
    }

    public long ringAddress() {
        return ringAddress;
    }

    public long getSqe() throws IOUringException {
        long res = getSqe0(ringAddress);
        if(res == 0) {
            throw new IOUringException();
        }
        return res;
    }

    public static void setSqeData(long sqeAddress, long userData, boolean isAddress) {
        setSqeData0(sqeAddress, userData, isAddress);
    }

    public static void prepAccept(long sqeAddress, int fd, int flags, boolean multishot) {
        //for now, only do 0 for both addr and addrLen i.e. do not care about the peer address information
        prepAccept(sqeAddress, fd, NULL, NULL, flags, multishot);
    }

    private static void prepAccept(long sqeAddress, int fd, long addr, long addrLen, int flags, boolean multishot) {
        //for now, only do 0 for both addr and addrLen i.e. do not care about the peer address information
        prepAccept0(sqeAddress, fd, addr, addrLen, flags, multishot);
    }

    public static void prepConnect(long sqeAddress, int fd, Buffer socketAddress) {
        // byte[] addressBytes = addr.getAddress();
        // prepConnect0(sqeAddress, fd, addressBytes, port);
        prepConnect0(sqeAddress, fd, socketAddress.address());
    }

    public int submit() {
        return submit0(ringAddress);
    }

    public int copyCqes(Buffer buf, int waitNr) {
        return copyCqes0(ringAddress, buf.address(), buf.capacityCqe(), waitNr);
    }

    public int enter(int toSubmit, int minComplete, int flags) {
        return enter0(ringAddress, toSubmit, minComplete, flags);
    }

    /*implement io operations:
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


    private static native long queueInit0(int entries, int flags) throws IOUringException;

    private static native long getSqe0(long ringAddress);

    private static native void setSqeData0(long sqeAddress, long userData, boolean isAddress);

    private static native void prepNop0(long sqeAddress);

    private static native void prepAccept0(long sqeAddress, int fd, long addr, long addrLen, int flags, boolean multishot);

    private static native void prepConnect0(long sqeAddress, int fd, long addr);

    //private static native void prepConnect0(long sqeAddress, int fd, byte[] address, int port);

    private static native void prepClose0(long sqeAddress, int fd);

    private static native void prepRead0(long sqeAddress, int fd, long bufAddress, int nBytes, long offset);

    private static native void prepRecv0(long sqeAddress, int fd, long bufAddress, long bufLen, int flags);

    private static native void prepSend0(long sqeAddress, int fd, long bufAddress, long bufLen, int flags);

    private static native int submit0(long ringAddress);

    private static native int copyCqes0(long ringAddress, long cqesAddress, int cqesLen, int waitNr);

    private static native int enter0(long ringAddress, int toSubmit, int minComplete, int flags);

}
