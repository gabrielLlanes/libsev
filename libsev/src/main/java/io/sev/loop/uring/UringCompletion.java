package io.sev.loop.uring;

import java.util.Random;

import io.sev.loop.Callback;
import io.sev.loop.Completion;
import io.sev.loop.Operation;
import io.sev.uring.IoUring;

public final class UringCompletion extends Completion<UringLoop, UringCompletion> {

    private static final Random random = new Random();

    volatile long id = random.nextLong();

    public UringCompletion(Operation operation, Object context, Callback<UringLoop, UringCompletion> callback) {
        super(operation, context, callback);
    }

    public UringCompletion() {
    }

    public void rollId() {
        id = random.nextLong();
    }

    public static UringCompletion of(Operation operation, Object context, Callback<UringLoop, UringCompletion> callback) {
        return new UringCompletion(operation, context, callback);
    }

    void prep(long sqe) {
        IoUring.sqeSetData64(sqe, id);
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

    final boolean complete(UringLoop loop, int res) {
        return callback.invoke(context, loop, this, res);
    }
}
