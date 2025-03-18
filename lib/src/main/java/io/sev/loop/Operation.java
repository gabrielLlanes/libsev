package io.sev.loop;

import java.lang.foreign.MemorySegment;

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

    private static class FdOperation extends Operation {
        public int fd;
        protected FdOperation(Op op, int fd) {
            super(op);
            this.fd = fd;
        }
    }

    public static class Accept extends FdOperation {
        public MemorySegment addr = MemorySegment.NULL;
        public MemorySegment addrLen = MemorySegment.NULL;
        public int flags;
        public boolean multishot;
        public Accept(int fd, MemorySegment addr, MemorySegment addrLen, int flags, boolean multishot) {
            super(Op.ACCEPT, fd);
            this.addr = addr; this.addrLen = addrLen; this.flags = flags; this.multishot = multishot;
        }
    }

    public static class Connect extends FdOperation {
        public MemorySegment addr;
        public int addrLen;
        public Connect(int fd, MemorySegment addr, int addrLen) {
            super(Op.CONNECT, fd);
            this.addr = addr; this.addrLen = addrLen;
        }
    }

    public static class Close extends FdOperation {
        public Close(int fd) {
            super(Op.CLOSE, fd);
        }
    }

    public static class Shutdown extends FdOperation {
        public int how;
        public Shutdown(int fd, int how) {
            super(Op.SHUTDOWN, fd);
            this.how = how;
        }
    }

    public static class Read extends FdOperation {
        public MemorySegment buf;
        public int nBytes;
        public long offset;
        public Read(int fd, MemorySegment buf, int nBytes, long offset) {
            super(Op.READ, fd);
            this.buf = buf; this.nBytes = nBytes; this.offset = offset;
        }
    }

    public static class Write extends FdOperation {
        public MemorySegment buf;
        public int nBytes;
        public long offset;
        public Write(int fd, MemorySegment buf, int nBytes, long offset) {
            super(Op.WRITE, fd);
            this.buf = buf; this.nBytes = nBytes; this.offset = offset;
        }
    }

    public static class Recv extends FdOperation {
        public MemorySegment buf;
        public long len;
        public int flags;
        public Recv(int fd, MemorySegment buf, long len, int flags) {
            super(Op.RECV, fd);
            this.buf = buf; this.len = len; this.flags = flags;
        }
    }

    public static class Send extends FdOperation {
        public MemorySegment buf;
        public long len;
        public int flags;
        public Send(int fd, MemorySegment buf, long len, int flags) {
            super(Op.SEND, fd);
            this.buf = buf; this.len = len; this.flags = flags;
        }
    }

    public static class Poll extends FdOperation {
        public int pollMask;
        public Poll(int fd, int pollMask) {
            super(Op.POLL, fd);
            this.pollMask = pollMask;
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
    }

    public static class Cancel extends Operation {
        public long userData;
        public Cancel(long userData) {
            super(Op.CANCEL);
            this.userData = userData;
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
