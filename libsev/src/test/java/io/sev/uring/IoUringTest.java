package io.sev.uring;

import static io.sev.socket.Socket.*;
import static io.sev.util.unix.Macros.*;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static io.sev.uring.IoUringCQE.*;
import static java.lang.foreign.MemoryLayout.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.sev.util.inet.InetUtil;
import io.sev.util.timer.TimespecUtil;
import io.sev.util.unix.Macros;
import io.sev.util.unix.UnixException;

@TestInstance(Lifecycle.PER_CLASS)
public class IoUringTest {

    private static Arena arena;

    private static MemorySegment localhost;

    private static final SequenceLayout cqeArrayLayout = sequenceLayout(128, IO_URING_CQE_LAYOUT);

    @BeforeAll
    public static void beforeAll() {
        arena = Arena.ofConfined();
        try {
            localhost = InetUtil.sockAddrInet4((Inet4Address) InetAddress.getByName("127.0.0.1"), 9033, arena);
        } catch(IOException ex) {

        }
    }

    @Test
    public void uringSocketTest() throws UnixException, InterruptedException {
        IoUring ring = IoUring.init(arena);
        assertTrue(ring.ringAddress() > 0);

        long sqe = ring.getSqe();
        assertNotEquals(0L, sqe);

        int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        setSockOpt(fd, SOL_SOCKET, SO_REUSEADDR, 1);
        setSockOpt(fd, SOL_SOCKET, SO_REUSEPORT, 1);
        bind(fd, localhost, SOCKADDR_IN_SIZE);
        listen(fd, 1);

        IoUring.prepAccept(sqe, fd, MemorySegment.NULL, MemorySegment.NULL, 0, false);
        IoUring.sqeSetData64(sqe, 100L);

        int submitted = ring.submit();
        assertEquals(1, submitted);

        Thread.sleep(5);

        long sqeConnect = ring.getSqe();
        int clientFd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        IoUring.prepConnect(sqeConnect, clientFd, localhost, SOCKADDR_IN_SIZE);
        IoUring.sqeSetData64(sqeConnect, 101L);

        int submittedAgain = ring.submit();
        assertEquals(1, submittedAgain);

        MemorySegment cqes = arena.allocate(cqeArrayLayout);

        int copied = 0;
        int acceptFd = 0;
        while(copied < 2) {
            int currCopied = ring.copyCqes(cqes, 128, 1);
            assertTrue(currCopied > 0);
            copied += currCopied;
            for(int i = 0; i < currCopied; i++) {
                long ud = (long) getUserData(cqes, i);
                if(ud == 100L) {
                    acceptFd = (int) getResult(cqes, i);
                    System.out.println("GOT ACCEPT FD: " + acceptFd);
                }
            }
        }

        assertEquals(2, copied);


        byte[] sendBytes = new byte[] {1,2,3,4,5};
        MemorySegment sendSegment = arena.allocate(5);
        for(int i = 0; i < 5; i++) {
            sendSegment.set(JAVA_BYTE, i, sendBytes[i]);
        }
        long sendSqe = ring.getSqe();
        IoUring.prepSend(sendSqe, acceptFd, sendSegment, 5, 0);

        MemorySegment recvSegment = arena.allocate(5);
        long recvSqe = ring.getSqe();
        IoUring.prepRecv(recvSqe, clientFd, recvSegment, 5, 0);

        ring.submit();

        int copied2 = 0;
        while(copied2 < 2) {
            copied2 += ring.copyCqes(cqes, 128, 1);
        }
        assertArrayEquals(sendBytes, recvSegment.toArray(JAVA_BYTE));
        System.out.println("recv: " + Arrays.toString(recvSegment.toArray(JAVA_BYTE)));

        byte[] writeBytes = new byte[] {5,4,3,2,1};
        MemorySegment writeSegment = arena.allocate(5);
        for(int i = 0; i < 5; i++) {
            writeSegment.set(JAVA_BYTE, i, writeBytes[i]);
        }
        long writeSqe = ring.getSqe();
        IoUring.prepWrite(writeSqe, acceptFd, writeSegment, 5, 0);

        MemorySegment readSegment = arena.allocate(5);
        long readSqe = ring.getSqe();
        IoUring.prepRead(readSqe, clientFd, readSegment, 5, 0);

        ring.submit();

        int copied3 = 0;
        while(copied3 < 2) {
            copied3 += ring.copyCqes(cqes, 128, 1);
        }

        assertArrayEquals(writeBytes, readSegment.toArray(JAVA_BYTE));
        System.out.println("read: " + Arrays.toString(readSegment.toArray(JAVA_BYTE)));

        long shutdownSqe = ring.getSqe();
        IoUring.prepShutdown(shutdownSqe, clientFd, SHUT_RDWR);

        long closeSqe = ring.getSqe();
        IoUring.prepClose(closeSqe, fd);

        long closeAcceptSqe = ring.getSqe();
        IoUring.prepClose(closeAcceptSqe, acceptFd);

        long closeClientSqe = ring.getSqe();
        IoUring.prepClose(closeClientSqe, clientFd);

        assertEquals(4, ring.submit());

        int copied4 = 0;
        while(copied4 < 4) {
            int currCopied = ring.copyCqes(cqes, 128, 1);
            copied4 += currCopied;
            for(int i = 0; i < currCopied; i++) {
                assertEquals(0, getResult(cqes, i));
            }
        }
        assertEquals(4, copied4);
        // closeStrict(fd);
        // closeStrict(clientFd);

        ring.queueExit();
    }

    @Test
    public void uringOneHundredNopTest() throws UnixException, InterruptedException {
        IoUring ring = IoUring.init(arena);
        for(int i = 1; i <= 100; i++) {
            long sqe = ring.getSqe();
            IoUring.sqeSetData64(sqe, (long) i);
            IoUring.sqeSetFlags(sqe, Macros.IOSQE_IO_LINK);
            assertNotEquals(0L, sqe);
            IoUring.prepNop(sqe);
        }
        int submitted = ring.submit();
        assertEquals(100, submitted);
        
        Thread.sleep(10);

        MemorySegment cqes = arena.allocate(cqeArrayLayout);
        int copied = ring.copyCqes(cqes, 128, 100);

        assertEquals(100, copied);

        for(int i = 1; i <= 100; i++) {
            assertEquals((long) i, getUserData(cqes, i-1));
        }
        ring.queueExit();
        System.out.println("100 NOP TEST");
    }

    @Test
    public void uringTimeoutTest() throws UnixException {
        IoUring ring = IoUring.init(arena);
        long sqe = ring.getSqe();
        IoUring.sqeSetData64(sqe, 42L);
        MemorySegment timespec = TimespecUtil.timespec(0L, 10_000_000L, arena);
        IoUring.prepTimeout(sqe, timespec, 0, 0);
        assertEquals(1, ring.submit());
        MemorySegment cqes = arena.allocate(cqeArrayLayout);
        assertEquals(1, ring.copyCqes(cqes, 128, 1));
        assertEquals(42L, getUserData(cqes, 0));
        assertEquals(-ETIME, getResult(cqes, 0));
        ring.queueExit();
        System.out.println("0.1 SEC TIMEOUT TEST");
    }

    @AfterAll
    public static void afterAll() {
        arena.close();
    }

}