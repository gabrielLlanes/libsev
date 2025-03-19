package io.sev.loop.uring;

import static io.sev.socket.Socket.*;
import static io.sev.util.unix.Macros.*;
import static java.lang.foreign.ValueLayout.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.sev.Native;
import io.sev.loop.Callback;
import io.sev.loop.Operation;
import io.sev.util.inet.InetUtil;

public class UringLoopTest {

    private static MemorySegment localhost;

    private static SegmentAllocator allocator = Native.callocator();

    @BeforeAll
    public static void beforeAll() {
        try {
            localhost = InetUtil.sockAddrInet4((Inet4Address) InetAddress.getByName("127.0.0.1"), 8096, allocator);
        } catch(IOException ex) {

        }
    }

    @Test
    public void uringLoopTest() throws Throwable {

        int serverFd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setSockOpt(serverFd, SOL_SOCKET, SO_REUSEADDR, 1);
        setSockOpt(serverFd, SOL_SOCKET, SO_REUSEPORT, 1);
        bind(serverFd, localhost, SOCKADDR_IN_SIZE);
        listen(serverFd, 1);

        int clientFd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        AtomicInteger acceptedFd = new AtomicInteger(-1);

        UringLoop loop = UringLoop.init();

        AtomicBoolean accepted = new AtomicBoolean(false);
        Operation acceptOperation = new Operation.Accept().fd(serverFd);
        Integer acceptContext = 99;
        Callback<UringLoop, UringCompletion> acceptCallback = (context, uringLoop, completion, result) -> {
            assertSame(acceptContext, context);
            assertSame(loop, uringLoop);
            assertTrue(result > 0);
            System.out.println("ACCEPTED FD " + result);
            acceptedFd.set(result);
            assertEquals(Operation.Op.ACCEPT, completion.operation.op);
            accepted.set(true);
            return false;
        };
        UringCompletion acceptCompletion  = new UringCompletion()
                                                    .operation(acceptOperation)
                                                    .context(acceptContext)
                                                    .callback(acceptCallback);
        
        AtomicBoolean connected = new AtomicBoolean(false);
        Operation connectOperation = new Operation.Connect()
                                                .fd(clientFd)
                                                .addr(localhost)
                                                .addrLen(SOCKADDR_IN_SIZE);
        Integer connectContext = 100;
        Callback<UringLoop, UringCompletion> connectCallback = (context, uringLoop, completion, result) -> {
            assertSame(connectContext, context);
            assertSame(loop, uringLoop);
            assertEquals(0, result);
            assertEquals(Operation.Op.CONNECT, completion.operation.op);
            connected.set(true);
            return false;
        };
        UringCompletion connectCompletion = new UringCompletion()
                                                .operation(connectOperation)
                                                .context(connectContext)
                                                .callback(connectCallback);

        loop.enqueue(acceptCompletion);
        loop.enqueue(connectCompletion);
        loop.runForNs(50_000_000L);

        assertTrue(accepted.get());
        assertTrue(connected.get());

        byte[] sendBytes = new byte[] {5,6,7,8,9,0,1,2,3,4};
        MemorySegment sendSegment = allocator.allocate(10L);
        sendSegment.copyFrom(MemorySegment.ofArray(sendBytes));

        Operation sendOperation = new Operation.Send()
                                        .fd(clientFd)
                                        .buf(sendSegment)
                                        .len(10L);
        AtomicBoolean sent = new AtomicBoolean(false);
        Callback<UringLoop, UringCompletion> sendCallback = (context, uringLoop, completion, result) -> {
            assertEquals(10, result);
            assertSame(loop, uringLoop);
            sent.set(true);
            return false;
        };
        UringCompletion sendCompletion = new UringCompletion()
                                            .operation(sendOperation)
                                            .callback(sendCallback);

        MemorySegment recvSegment = allocator.allocate(10L);
        Operation recvOperation = new Operation.Recv()
                                        .fd(acceptedFd.get())
                                        .buf(recvSegment)
                                        .len(10L);
        AtomicBoolean received = new AtomicBoolean(false);
        Callback<UringLoop, UringCompletion> recvCallback = (context, uringLoop, completion, result) -> {
            assertEquals(10, result);
            assertSame(loop, uringLoop);
            assertArrayEquals(sendBytes, recvSegment.toArray(JAVA_BYTE));
            System.out.println("received bytes: " + Arrays.toString(recvSegment.toArray(JAVA_BYTE)));
            received.set(true);
            return false;
        };
        UringCompletion recvCompletion = new UringCompletion()
                                            .operation(recvOperation)
                                            .callback(recvCallback);

        loop.enqueue(sendCompletion);
        loop.enqueue(recvCompletion);
        loop.runAll();

        assertTrue(sent.get());
        assertTrue(received.get());

        closeStrict(serverFd);
        closeStrict(clientFd);
        closeStrict(acceptedFd.get());

        AtomicLong nopsFinished = new AtomicLong(0L);
        Operation nopOperation = new Operation.Nop();
        Callback<UringLoop, UringCompletion> nopCallback = (context, uringLoop, completion, result) -> {
            assertEquals(0, result);
            assertSame(loop, uringLoop);
            if(nopsFinished.incrementAndGet() == 1000L) {
                return false;
            } 
            return true;
        };
        UringCompletion nopCompletion = new UringCompletion()
                                            .operation(nopOperation)
                                            .callback(nopCallback);

        loop.enqueue(nopCompletion);
        loop.runAll();
        assertEquals(1000L, nopsFinished.get());

        loop.deinit();
        System.out.println("URING LOOP TEST");
    }
    
}
