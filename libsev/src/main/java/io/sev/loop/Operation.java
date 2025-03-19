package io.sev.loop;

import java.lang.foreign.MemorySegment;

import io.sev.util.unix.Macros;

public abstract class Operation {

    public Op op = null;

    protected Operation(Op op) {
        this.op = op;
    }

    public static class Nop extends Operation {
        public Nop() {
            super(Op.NOP);
        }
    }

    private static class FdOperation<T extends FdOperation<T>> extends Operation {
        public int fd;
        protected FdOperation(Op op, int fd) {
            super(op);
            this.fd = fd;
        }
        protected FdOperation(Op op) {
            super(op);
        }
        public T fd(int fd) {
            this.fd = fd;
            return (T) this;
        }
    }

    public static class Accept extends FdOperation<Accept> {
        public MemorySegment addr = MemorySegment.NULL;
        public MemorySegment addrLen = MemorySegment.NULL;
        public int flags = 0;
        public boolean multishot = false;
        public Accept(int fd, MemorySegment addr, MemorySegment addrLen, int flags, boolean multishot) {
            super(Op.ACCEPT, fd);
            this.addr = addr; this.addrLen = addrLen; this.flags = flags; this.multishot = multishot;
        }
        public Accept() {
            super(Op.ACCEPT);
        }
        public Accept addr(MemorySegment addr) {
            this.addr = addr;
            return this;
        }
        public Accept addrLen(MemorySegment addrLen) {
            this.addrLen = addrLen;
            return this;
        }
        public Accept flags(int flags) {
            this.flags = flags;
            return this;
        }
        public Accept multishot(boolean multishot) {
            this.multishot = multishot;
            return this;
        }
    }

    public static class Connect extends FdOperation<Connect> {
        public MemorySegment addr;
        public int addrLen;
        public Connect(int fd, MemorySegment addr, int addrLen) {
            super(Op.CONNECT, fd);
            this.addr = addr; this.addrLen = addrLen;
        }
        public Connect() {
            super(Op.CONNECT);
        }
        public Connect addr(MemorySegment addr) {
            this.addr = addr;
            return this;
        }
        public Connect addrLen(int addrLen) {
            this.addrLen = addrLen;
            return this;
        }
    }

    public static class Close extends FdOperation<Close> {
        public Close(int fd) {
            super(Op.CLOSE, fd);
        }
        public Close() {
            super(Op.CLOSE);
        }
    }

    public static class Shutdown extends FdOperation<Shutdown> {
        public int how = Macros.SHUT_RDWR;
        public Shutdown(int fd, int how) {
            super(Op.SHUTDOWN, fd);
            this.how = how;
        }
        public Shutdown() {
            super(Op.SHUTDOWN);
        }
        public Shutdown how(int how) {
            this.how = how;
            return this;
        }
    }

    public static class Read extends FdOperation<Read> {
        public MemorySegment buf;
        public int nBytes;
        public long offset = 0L;
        public Read(int fd, MemorySegment buf, int nBytes, long offset) {
            super(Op.READ, fd);
            this.buf = buf; this.nBytes = nBytes; this.offset = offset;
        }
        public Read() {
            super(Op.READ);
        }
        public Read buf(MemorySegment buf) {
            this.buf = buf;
            return this;
        }
        public Read nBytes(int nBytes) {
            this.nBytes = nBytes;
            return this;
        }
        public Read offset(long offset) {
            this.offset = offset;
            return this;
        }
    }

    public static class Write extends FdOperation<Write> {
        public MemorySegment buf;
        public int nBytes;
        public long offset = 0L;
        public Write(int fd, MemorySegment buf, int nBytes, long offset) {
            super(Op.WRITE, fd);
            this.buf = buf; this.nBytes = nBytes; this.offset = offset;
        }
        public Write() {
            super(Op.WRITE);
        }
        public Write buf(MemorySegment buf) {
            this.buf = buf;
            return this;
        }
        public Write nBytes(int nBytes) {
            this.nBytes = nBytes;
            return this;
        }
        public Write offset(long offset) {
            this.offset = offset;
            return this;
        }
    }

    public static class Recv extends FdOperation<Recv> {
        public MemorySegment buf;
        public long len;
        public int flags = 0;
        public Recv(int fd, MemorySegment buf, long len, int flags) {
            super(Op.RECV, fd);
            this.buf = buf; this.len = len; this.flags = flags;
        }
        public Recv() {
            super(Op.RECV);
        }
        public Recv buf(MemorySegment buf) {
            this.buf = buf;
            return this;
        }
        public Recv len(long len) {
            this.len = len;
            return this;
        }
        public Recv flags(int flags) {
            this.flags = flags;
            return this;
        }
    }

    public static class Send extends FdOperation<Send> {
        public MemorySegment buf;
        public long len;
        public int flags = 0;
        public Send(int fd, MemorySegment buf, long len, int flags) {
            super(Op.SEND, fd);
            this.buf = buf; this.len = len; this.flags = flags;
        }
        public Send() {
            super(Op.SEND);
        }
        public Send buf(MemorySegment buf) {
            this.buf = buf;
            return this;
        }
        public Send len(long len) {
            this.len = len;
            return this;
        }
        public Send flags(int flags) {
            this.flags = flags;
            return this;
        }
    }

    public static class Poll extends FdOperation<Poll> {
        public int pollMask;
        public Poll(int fd, int pollMask) {
            super(Op.POLL, fd);
            this.pollMask = pollMask;
        }
        public Poll() {
            super(Op.POLL);
        }
        public Poll mask(int mask) {
            this.pollMask = mask;
            return this;
        }
    }

    public static class Timer extends Operation {
        public MemorySegment ts;
        public int count;
        public int flags;
        public Timer(MemorySegment ts, int count, int flags) {
            super(Op.TIMER);
            this.ts = ts; this.count = count; this.flags = flags;
        }
        public Timer() {
            super(Op.TIMER);
        }
        public Timer ts(MemorySegment ts) {
            this.ts = ts;
            return this;
        }
        public Timer count(int count) {
            this.count = count;
            return this;
        }
        public Timer flags(int flags) {
            this.flags = flags;
            return this;
        }
    }

    public static class Cancel extends Operation {
        public long userData;
        public Cancel(long userData) {
            super(Op.CANCEL);
            this.userData = userData;
        }
        public Cancel() {
            super(Op.CANCEL);
        }
        public Cancel userData(long userData) {
            this.userData = userData;
            return this;
        }
    }

    public static enum Op {
        ACCEPT,
        CONNECT,
        CLOSE,
        SHUTDOWN,
        READ,
        WRITE,
        RECV,
        SEND,
        POLL,
        TIMER,
        CANCEL,
        NOP
    }
}
