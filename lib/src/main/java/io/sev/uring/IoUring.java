package io.sev.uring;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import io.sev.Native;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import static io.sev.util.errors.UnixException.unixException;

public class IoUring {

    private static final int DEFAULT_ENTRIES = 512;

    //change as needed
    private static final int STRUCT_IO_URING_SIZE = 216;

    public static final int IORING_SETUP_SQPOLL = 2;
    public static final int IOSQE_IO_LINK = 4;

    private static final MethodHandle queueInitHandle;

    private static final MethodHandle getSqeHandle;

    private static final MethodHandle sqeSetData64Handle;

    private static final MethodHandle sqeSetDataHandle;

    private static final MethodHandle sqeSetFlagsHandle;

    private static final MethodHandle prepNopHandle;

    private static final MethodHandle prepAcceptHandle;

    private static final MethodHandle prepConnectHandle;

    private static final MethodHandle prepCloseHandle;

    private static final MethodHandle prepShutdownHandle;

    private static final MethodHandle prepReadHandle;

    private static final MethodHandle prepWriteHandle;

    private static final MethodHandle prepRecvHandle;

    private static final MethodHandle prepSendHandle;

    private static final MethodHandle prepTimeoutHandle;

    private static final MethodHandle prepPollAddHandle;

    private static final MethodHandle prepCancel64Handle;

    private static final MethodHandle prepCancelHandle;

    private static final MethodHandle submitHandle;

    private static final MethodHandle submitAndWaitHandle;

    private static final MethodHandle copyCqesHandle;

    private static final MethodHandle queueExitHandle;

    static {
        try {
            Class.forName(Native.class.getName());
        } catch(ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        MemorySegment queueInitSegment = lookup.findOrThrow("sev_uring_queueInit");
        FunctionDescriptor queueInitDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT);
        queueInitHandle = linker.downcallHandle(queueInitSegment, queueInitDescriptor);

        MemorySegment getSqeSegment = lookup.findOrThrow("sev_uring_getSqe");
        FunctionDescriptor getSqeDescriptor = FunctionDescriptor.of(JAVA_LONG, JAVA_LONG);
        getSqeHandle = linker.downcallHandle(getSqeSegment, getSqeDescriptor);

        MemorySegment sqeSetData64Segment = lookup.findOrThrow("sev_uring_sqeSetData64");
        FunctionDescriptor sqeSetData64Descriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG);
        sqeSetData64Handle = linker.downcallHandle(sqeSetData64Segment, sqeSetData64Descriptor);

        MemorySegment sqeSetDataSegment = lookup.findOrThrow("sev_uring_sqeSetData");
        FunctionDescriptor sqeSetDataDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG);
        sqeSetDataHandle = linker.downcallHandle(sqeSetDataSegment, sqeSetDataDescriptor);

        MemorySegment sqeSetFlagsSegment = lookup.findOrThrow("sev_uring_sqeSetFlags");
        FunctionDescriptor sqeSetFlagsDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT);
        sqeSetFlagsHandle = linker.downcallHandle(sqeSetFlagsSegment, sqeSetFlagsDescriptor);

        MemorySegment prepNopSegment = lookup.findOrThrow("sev_uring_prepNop");
        FunctionDescriptor prepNopDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG);
        prepNopHandle = linker.downcallHandle(prepNopSegment, prepNopDescriptor);

        MemorySegment prepAcceptSegment = lookup.findOrThrow("sev_uring_prepAccept");
        FunctionDescriptor prepAcceptDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_BOOLEAN);
        prepAcceptHandle = linker.downcallHandle(prepAcceptSegment, prepAcceptDescriptor);

        MemorySegment prepConnectSegment = lookup.findOrThrow("sev_uring_prepConnect");
        FunctionDescriptor prepConnectDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT);
        prepConnectHandle = linker.downcallHandle(prepConnectSegment, prepConnectDescriptor);

        MemorySegment prepCloseSegment = lookup.findOrThrow("sev_uring_prepClose");
        FunctionDescriptor prepCloseDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT);
        prepCloseHandle = linker.downcallHandle(prepCloseSegment, prepCloseDescriptor);

        MemorySegment prepShutdownSegment = lookup.findOrThrow("sev_uring_prepShutdown");
        FunctionDescriptor prepShutdownDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT);
        prepShutdownHandle = linker.downcallHandle(prepShutdownSegment, prepShutdownDescriptor);

        MemorySegment prepReadSegment = lookup.findOrThrow("sev_uring_prepRead");
        FunctionDescriptor prepReadDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG);
        prepReadHandle = linker.downcallHandle(prepReadSegment, prepReadDescriptor);

        MemorySegment prepWriteSegment = lookup.findOrThrow("sev_uring_prepWrite");
        FunctionDescriptor prepWriteDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG);
        prepWriteHandle = linker.downcallHandle(prepWriteSegment, prepWriteDescriptor);

        MemorySegment prepRecvSegment = lookup.findOrThrow("sev_uring_prepRecv");
        FunctionDescriptor prepRecvDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT);
        prepRecvHandle = linker.downcallHandle(prepRecvSegment, prepRecvDescriptor);

        MemorySegment prepSendSegment = lookup.findOrThrow("sev_uring_prepSend");
        FunctionDescriptor prepSendDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT);
        prepSendHandle = linker.downcallHandle(prepSendSegment, prepSendDescriptor);
        
        MemorySegment prepTimeoutSegment = lookup.findOrThrow("sev_uring_prepTimeout");
        FunctionDescriptor prepTimeoutDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT);
        prepTimeoutHandle = linker.downcallHandle(prepTimeoutSegment, prepTimeoutDescriptor);

        MemorySegment prepPollAddSegment = lookup.findOrThrow("sev_uring_prepPollAdd");
        FunctionDescriptor prepPollAddDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT);
        prepPollAddHandle = linker.downcallHandle(prepPollAddSegment, prepPollAddDescriptor);

        MemorySegment prepCancel64Segment = lookup.findOrThrow("sev_uring_prepCancel64");
        FunctionDescriptor prepCancel64Descriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT);
        prepCancel64Handle = linker.downcallHandle(prepCancel64Segment, prepCancel64Descriptor);

        MemorySegment prepCancelSegment = lookup.findOrThrow("sev_uring_prepCancel");
        FunctionDescriptor prepCancelDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT);
        prepCancelHandle = linker.downcallHandle(prepCancelSegment, prepCancelDescriptor);

        MemorySegment submitSegment = lookup.findOrThrow("sev_uring_submit");
        FunctionDescriptor submitDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_LONG);
        submitHandle = linker.downcallHandle(submitSegment, submitDescriptor);

        MemorySegment submitAndWaitSegment = lookup.findOrThrow("sev_uring_submitAndWait");
        FunctionDescriptor submitAndWaitDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT);
        submitAndWaitHandle = linker.downcallHandle(submitAndWaitSegment, submitAndWaitDescriptor);

        MemorySegment copyCqesSegment = lookup.findOrThrow("sev_uring_copyCqes");
        FunctionDescriptor copyCqesDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT);
        copyCqesHandle = linker.downcallHandle(copyCqesSegment, copyCqesDescriptor);

        MemorySegment queueExitSegment = lookup.findOrThrow("sev_uring_queueExit");
        FunctionDescriptor queueExitDescriptor = FunctionDescriptor.ofVoid(JAVA_LONG);
        queueExitHandle = linker.downcallHandle(queueExitSegment, queueExitDescriptor);
    }
    
    private final long ringAddress;

    private IoUring(int entries, SegmentAllocator allocator) throws Throwable {
        MemorySegment ring = allocator.allocate(STRUCT_IO_URING_SIZE);
        this.ringAddress = ring.address();
        init(entries, ring.address(), 0);
    }

    public long ringAddress() {
        return ringAddress;
    }

    private static void init(int entries, long ringAddress, int flags) throws Throwable {
        int res = (int) queueInitHandle.invokeExact(entries, ringAddress, flags);
        if(res < 0) {
            unixException(res);
        }
    }

    public static IoUring init(SegmentAllocator allocator) throws Throwable {
        return new IoUring(DEFAULT_ENTRIES, allocator);
    }

    public static void sqeSetData64(long sqe, long data) throws Throwable {
        sqeSetData64Handle.invokeExact(sqe, data);
    }

    public static void sqeSetData(long sqe, long data) throws Throwable {
        sqeSetDataHandle.invokeExact(sqe, data);
    }

    public static void sqeSetFlags(long sqe, int flags) throws Throwable {
        sqeSetFlagsHandle.invokeExact(sqe, flags);
    }

    public static void prepNop(long sqe) throws Throwable {
        prepNopHandle.invokeExact(sqe);
    }

    public static void prepAccept(long sqe, int sockfd, MemorySegment addr, MemorySegment addrlen, int flags, boolean multishot) throws Throwable {
        prepAcceptHandle.invokeExact(sqe, sockfd, addr, addrlen, flags, multishot);
    }

    public static void prepConnect(long sqe, int sockfd, MemorySegment addr, int addrlen) throws Throwable {
        prepConnectHandle.invokeExact(sqe, sockfd, addr, addrlen);
    }

    public static void prepClose(long sqe, int fd) throws Throwable {
        prepCloseHandle.invokeExact(sqe, fd);
    }

    public static void prepShutdown(long sqe, int sockfd, int how) throws Throwable {
        prepShutdownHandle.invokeExact(sqe, sockfd, how);
    }

    public static void prepRead(long sqe, int fd, MemorySegment buf, int nbytes, long offset) throws Throwable {
        prepReadHandle.invokeExact(sqe, fd, buf, nbytes, offset);
    }

    public static void prepWrite(long sqe, int fd, MemorySegment buf, int nbytes, long offset) throws Throwable {
        prepWriteHandle.invokeExact(sqe, fd, buf, nbytes, offset);
    }

    public static void prepRecv(long sqe, int sockfd, MemorySegment buf, long len, int flags) throws Throwable {
        prepRecvHandle.invokeExact(sqe, sockfd, buf, len, flags);
    }

    public static void prepSend(long sqe, int sockfd, MemorySegment buf, long len, int flags) throws Throwable {
        prepSendHandle.invokeExact(sqe, sockfd, buf, len, flags);
    }

    public static final int IORING_TIMEOUT_ABS = 1;

    public static void prepTimeout(long sqe, MemorySegment ts, int count, int flags) throws Throwable {
        prepTimeoutHandle.invokeExact(sqe, ts, count, flags);
    }

    public static void prepPollAdd(long sqe, int fd, int poll_mask) throws Throwable {
        prepPollAddHandle.invokeExact(sqe, fd, poll_mask);
    }

    public static void prepCancel64(long sqe, long user_data, int flags) throws Throwable {
        prepCancel64Handle.invokeExact(sqe, user_data, flags);
    }

    public static void prepCancel(long sqe, long user_data, int flags) throws Throwable {
        prepCancelHandle.invokeExact(sqe, user_data, flags);
    }

    public long getSqe() throws Throwable {
        return (long) getSqeHandle.invokeExact(ringAddress);
    }

    public int submit() throws Throwable {
        int res = (int) submitHandle.invokeExact(ringAddress);
        if(res < 0) {
            unixException(res);
        }
        return res;
    }

    public int submitAndWait(int wait_nr) throws Throwable {
        int res = (int) submitAndWaitHandle.invokeExact(ringAddress, wait_nr);
        if(res < 0) {
            unixException(res);
        }
        return res;
    }

    public int copyCqes(MemorySegment cqes, int cqeslen, int waitnr) throws Throwable {
        int res = (int) copyCqesHandle.invokeExact(ringAddress, cqes, cqeslen, waitnr);
        if(res < 0) {
            unixException(res);
        }
        return res;
    }

    public void queueExit() throws Throwable {
        queueExitHandle.invokeExact(ringAddress);
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

     public enum OpenStatus {
        OPEN, CLOSED
     }

}
