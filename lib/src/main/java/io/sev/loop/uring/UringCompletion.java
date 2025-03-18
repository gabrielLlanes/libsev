package io.sev.loop.uring;

import io.sev.Native;
import io.sev.loop.Completion;
import io.sev.loop.Operation;
import io.sev.uring.IoUring;

public final class UringCompletion extends Completion<UringLoop, UringCompletion> {

    long addressId;

    public UringCompletion(Operation operation, Object context, UringCallback callback) {
        super(operation, context, callback);
    }

    public static UringCompletion of(Operation operation, Object context, UringCallback callback) {
        return new UringCompletion(operation, context, callback);
    }

    void prep(long sqe) throws Throwable {
        try {
            addressId = Native.calloc(1L, 1L);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
        IoUring.sqeSetData64(sqe, addressId);
        switch(operation.op) {
            case ACCEPT:
                Operation.Accept acceptOperation = (Operation.Accept) operation;
                IoUring.prepAccept(sqe, acceptOperation.fd, acceptOperation.addr, acceptOperation.addrLen, acceptOperation.flags, acceptOperation.multishot);
                break;
            case CANCEL:
                Operation.Cancel cancelOperation = (Operation.Cancel) operation;
                IoUring.prepCancel(sqe, cancelOperation.userData, 0);
                break;
            case CLOSE:
                Operation.Close closeOperation = (Operation.Close) operation;
                IoUring.prepClose(sqe, closeOperation.fd);
                break;
            case CONNECT:
                Operation.Connect connectOperation = (Operation.Connect) operation;
                IoUring.prepConnect(sqe, connectOperation.fd, connectOperation.addr, connectOperation.addrLen);
                break;
            case POLL:
                Operation.Poll pollOperation = (Operation.Poll) operation;
                IoUring.prepPollAdd(sqe, pollOperation.fd, pollOperation.pollMask);
                break;
            case READ:
                Operation.Read readOperation = (Operation.Read) operation;
                IoUring.prepRead(sqe, readOperation.fd, readOperation.buf, readOperation.nBytes, readOperation.offset);
                break;
            case RECV:
                Operation.Recv recvOperation = (Operation.Recv) operation;
                IoUring.prepRecv(sqe, recvOperation.fd, recvOperation.buf, recvOperation.len, recvOperation.flags);
                break;
            case SEND:
                Operation.Send sendOperation = (Operation.Send) operation;
                IoUring.prepSend(sqe, sendOperation.fd, sendOperation.buf, sendOperation.len, sendOperation.flags);
                break;
            case SHUTDOWN:
                Operation.Shutdown shutdownOperation = (Operation.Shutdown) operation;
                IoUring.prepShutdown(sqe, shutdownOperation.fd, shutdownOperation.how);
                break;
            case TIMER:
                Operation.Timer timerOperation = (Operation.Timer) operation;
                IoUring.prepTimeout(sqe, timerOperation.ts, timerOperation.count, timerOperation.flags);
                break;
            case WRITE:
                Operation.Write writeOperation = (Operation.Write) operation;
                IoUring.prepWrite(sqe, writeOperation.fd, writeOperation.buf, writeOperation.nBytes, writeOperation.offset);
                break;
            case NOP:
                IoUring.prepNop(sqe);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    final boolean complete(int res) {
        return callback.invoke(context, this, res);
    }
}
