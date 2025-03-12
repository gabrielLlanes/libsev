#ifndef SEV_URING_H_
#define SEV_URING_H_

#include <stddef.h>
#include <stdint.h>
#include <sys/socket.h>
#include <stdbool.h>
#include <time.h>

#include "liburing.h"

int sev_uring_queueInit(unsigned entries, struct io_uring *ring, unsigned flags);

struct io_uring_sqe *sev_uring_getSqe(struct io_uring *ring);

void sev_uring_sqeSetData64(struct io_uring_sqe *sqe, long data);

void sev_uring_sqeSetData(struct io_uring_sqe *sqe, void *data);

void sev_uring_sqeSetFlags(struct io_uring_sqe *sqe, unsigned flags);

void sev_uring_prepNop(struct io_uring_sqe *sqe);

void sev_uring_prepAccept(struct io_uring_sqe *sqe, int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags, bool multishot);

void sev_uring_prepConnect(struct io_uring_sqe *sqe, int fd, const struct sockaddr *addr, socklen_t addrlen);

void sev_uring_prepClose(struct io_uring_sqe *sqe, int fd);

void sev_uring_prepShutdown(struct io_uring_sqe *sqe, int fd, int how);

void sev_uring_prepRead(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset);

void sev_uring_prepWrite(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, unsigned long long offset);

void sev_uring_prepRecv(struct io_uring_sqe *sqe , int sockfd, void *buf, size_t len, int flags);

void sev_uring_prepSend(struct io_uring_sqe *sqe , int sockfd, void *buf, size_t len, int flags);

void sev_uring_prepTimeout(struct io_uring_sqe *sqe, struct __kernel_timespec *ts, unsigned count, unsigned flags);

int sev_uring_submit(struct io_uring *ring);

int sev_uring_copyCqes(struct io_uring *ring, struct io_uring_cqe *cqes, uint32_t cqes_len, uint32_t wait_nr);

void sev_uring_queueExit(struct io_uring *ring);

#endif