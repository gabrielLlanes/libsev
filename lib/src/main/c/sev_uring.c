#include <stdlib.h>
#include <stddef.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <stdbool.h>
#include <time.h>

#include "liburing.h"

int sev_uring_queueInit(unsigned entries, struct io_uring *ring, unsigned flags) {
    int res = io_uring_queue_init(entries, ring, flags);
    return res;
}

struct io_uring_sqe *sev_uring_getSqe(struct io_uring *ring) {
    struct io_uring_sqe *sqe = io_uring_get_sqe(ring);
    return sqe;
}

void sev_uring_sqeSetData64(struct io_uring_sqe *sqe, long data) {
    io_uring_sqe_set_data64(sqe, data);
}

void sev_uring_sqeSetData(struct io_uring_sqe *sqe, void *data) {
    io_uring_sqe_set_data(sqe, data);
}

void sev_uring_sqeSetFlags(struct io_uring_sqe *sqe, unsigned flags) {
    io_uring_sqe_set_flags(sqe, flags);
}

void sev_uring_prepNop(struct io_uring_sqe *sqe) {
    io_uring_prep_nop(sqe);
}

void sev_uring_prepAccept(struct io_uring_sqe *sqe, int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags, bool multishot) {
    if(multishot) {
        io_uring_prep_multishot_accept(sqe, sockfd, addr, addrlen, flags);
    } else {
        io_uring_prep_accept(sqe, sockfd, addr, addrlen, flags);
    }
}

void sev_uring_prepConnect(struct io_uring_sqe *sqe, int fd, const struct sockaddr *addr, socklen_t addrlen) {
    io_uring_prep_connect(sqe, fd, addr, addrlen);
}

void sev_uring_prepClose(struct io_uring_sqe *sqe, int fd) {
    io_uring_prep_close(sqe, fd);
}

void sev_uring_prepShutdown(struct io_uring_sqe *sqe, int fd, int how) {
    io_uring_prep_shutdown(sqe, fd, how);
}

void sev_uring_prepRead(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset) {
    io_uring_prep_read(sqe, fd, buf, nbytes, offset);
}

void sev_uring_prepWrite(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset) {
    io_uring_prep_write(sqe, fd, buf, nbytes, offset);
}

void sev_uring_prepRecv(struct io_uring_sqe *sqe , int sockfd, void *buf, size_t len, int flags) {
    io_uring_prep_recv(sqe, sockfd, buf, len, flags);
}

void sev_uring_prepSend(struct io_uring_sqe *sqe , int sockfd, void *buf, size_t len, int flags) {
    io_uring_prep_send(sqe, sockfd, buf, len, flags);
}

void sev_uring_prepTimeout(struct io_uring_sqe *sqe, struct __kernel_timespec *ts, unsigned count, unsigned flags) {
    io_uring_prep_timeout(sqe, ts, count, flags);
}

void sev_uring_prepPollAdd(struct io_uring_sqe *sqe, int fd, unsigned poll_mask) {
    io_uring_prep_poll_add(sqe, fd, poll_mask);
}

void sev_uring_prepCancel(struct io_uring_sqe *sqe, void *user_data, int flags) {
    io_uring_prep_cancel(sqe, user_data, flags);
}

int sev_uring_submit(struct io_uring *ring) {
    return io_uring_submit(ring);
}

int sev_uring_submitAndWait(struct io_uring *ring, unsigned wait_nr) {
    return io_uring_submit_and_wait(ring, wait_nr);
}

static uint32_t minOfInts(uint32_t a, uint32_t b) {
    return a < b ? a : b;
}

/*

cq ring:
0                        n-1
|......head......tail......|

needs wrapping:

|......tail......head......|

*/

static uint32_t copy_cqes_ready(struct io_uring *ring, struct io_uring_cqe *cqes, uint32_t cqes_len) {
    uint32_t ready = io_uring_cq_ready(ring);
    uint32_t count = minOfInts(ready, cqes_len);
    uint32_t head = *(ring->cq.khead) & ring->cq.ring_mask;

    uint32_t n = minOfInts(ring->cq.ring_entries - head, count);
    struct io_uring_cqe *cqring_head_ptr = ring->cq.cqes + head;
    memcpy(cqes, cqring_head_ptr, n * sizeof(struct io_uring_cqe));
    if(count > n) {
        uint32_t extra = count - n;
        struct io_uring_cqe *cqring_array_ptr = ring->cq.cqes;
        struct io_uring_cqe *cqes_remaining = cqes + n;
        memcpy(cqes_remaining, cqring_array_ptr, extra * sizeof(struct io_uring_cqe));
    }
    io_uring_cq_advance(ring, count);
    return count;
}

static int cq_ring_needs_flush(struct io_uring *ring) {
    return IO_URING_READ_ONCE(*ring->sq.kflags) & (IORING_SQ_CQ_OVERFLOW);
}

//inspired by implementation of zig implementation in std.os.linux.IoUring.cope_cqes()
int sev_uring_copyCqes(struct io_uring *ring, struct io_uring_cqe *cqes, uint32_t cqes_len, uint32_t wait_nr) {
    uint32_t count = copy_cqes_ready(ring, cqes, cqes_len);
    if(count > 0) {
        return count;
    }
    if(cq_ring_needs_flush(ring) || wait_nr > 0) {
        int enter = io_uring_enter(ring->ring_fd, 0, wait_nr, IORING_ENTER_GETEVENTS, NULL);
        if(enter < 0) {
            return enter;
        } else {
            return copy_cqes_ready(ring, cqes, cqes_len);
        }
    }
    return 0;
}

void sev_uring_queueExit(struct io_uring *ring) {
    io_uring_queue_exit(ring);
}