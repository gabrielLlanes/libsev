#include <stdlib.h>
#include <stddef.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "liburing.h"
#include "sev_jni.h"
#include "sev_socket.h"
#include "sev_buffer.h"

#define ILLEGALARGEXCEPTION_CLASSNAME "java/lang/IllegalArgumentException"
#define IOEXCEPTION_CLASSNAME "java/io/IOException"
#define SEV_URING_CLASSNAME "io/sev/uring/IOUring"
#define IOURINGEXCEPTION_CLASSNAME "io/sev/uring/IOUringException"
#define SEV_JNI_VERSION JNI_VERSION_1_6

static jclass ILLEGALARGEXCEPTION_CLASS = NULL;

static jclass IOEXCEPTION_CLASS = NULL;

static jclass IOURINGEXCEPTION_CLASS = NULL;

static jlong sev_uring_queueInit0
    (JNIEnv *env, jclass clazz, jint entries, jint flags) {
        struct io_uring *ring = (struct io_uring *) malloc(sizeof(struct io_uring));
        int res = io_uring_queue_init((unsigned int) entries, ring, (unsigned int) flags);
        if(res < 0) {
            (*env)->ThrowNew(env, IOURINGEXCEPTION_CLASS, strerror(-res));
            return -1;
        }
        return (jlong) ring;
    }

static jlong sev_uring_getSqe0
    (JNIEnv *env, jclass clazz, jlong ringAddress) {
        struct io_uring *ring = (struct io_uring *) ringAddress;
        struct io_uring_sqe *sqe = io_uring_get_sqe(ring);
        return (jlong) sqe;
    }

static void sev_uring_setSqeData0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jlong userData, jboolean isAddress) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        if(isAddress) {
            io_uring_sqe_set_data(sqe, (void *) userData);
        } else {
            io_uring_sqe_set_data64(sqe, (unsigned long long) userData);
        }
    }

static void sev_uring_prepNop0
    (JNIEnv *env, jclass clazz, jlong sqeAddress) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_nop(sqe);
    }

static void sev_uring_prepAccept0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong addr,
            jlong addrLen, jint flags, jboolean multishot) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        if(multishot) {
            io_uring_prep_multishot_accept(sqe, fd, 
                (struct sockaddr *) addr,
                (socklen_t *) addrLen, flags);
        } else {
            io_uring_prep_accept(sqe, fd, 
                (struct sockaddr *) addr,
                (socklen_t *) addrLen, flags);
        }
    }

//TODO: Do proper memory management (for example, allocate the address memory in java)    
static void sev_uring_prepConnect0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong addr) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        struct sockaddr_in *sockAddr = (struct sockaddr_in *) addr;
        io_uring_prep_connect(sqe, fd, (const struct sockaddr *) sockAddr, sizeof(struct sockaddr_in));
    }

static void sev_uring_prepClose0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_close(sqe, fd);
    }

static void sev_uring_prepRead0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong bufAddress, jint nBytes, jlong offset) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_read(sqe, fd, (void *) bufAddress, nBytes, (unsigned long long) offset);
    }

static void sev_uring_prepRecv0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong bufAddress, jlong bufLen, jint flags) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_recv(sqe, fd, (void *) bufAddress, bufLen, flags);
    }

static void sev_uring_prepSend0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong bufAddress, jlong bufLen, jint flags) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_send(sqe, fd, (const void *) bufAddress, bufLen, flags);
    }

static jint sev_uring_submit0
    (JNIEnv *env, jclass clazz, jlong ringAddress) {
        return io_uring_submit((struct io_uring *) ringAddress);
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

// inspired by Zig implementation std.os.linux.IoUring.copy_cqes()
static jint copy_cqes(struct io_uring *ring, struct io_uring_cqe *cqes, uint32_t cqes_len, uint32_t wait_nr) {
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

static jint sev_uring_copyCqes0
    (JNIEnv *env, jclass clazz, jlong ringAddress, jlong cqesAddress, jint cqesLen, jint waitNr) {
        struct io_uring *ring = (struct io_uring *) ringAddress;
        struct io_uring_cqe *cqes = (struct io_uring_cqe *) cqesAddress;
        int res = copy_cqes(ring, cqes, cqesLen, waitNr);
        return res;
    }

static jint sev_uring_enter0
    (JNIEnv *env, jclass clazz, jlong ringAddress, jint toSubmit, jint minComplete, jint flags) {
        struct io_uring *ring = (struct io_uring *) ringAddress;
        return io_uring_enter(ring->ring_fd, toSubmit, minComplete, flags, NULL);
    }

static const JNINativeMethod method_table[] = {
    {"queueInit0", "(II)J", sev_uring_queueInit0},
    {"getSqe0", "(J)J", sev_uring_getSqe0},
    {"setSqeData0", "(JJZ)V", sev_uring_setSqeData0},
    {"prepNop0", "(J)V", sev_uring_prepNop0},
    {"prepAccept0", "(JIJJIZ)V", sev_uring_prepAccept0},
    {"prepConnect0", "(JIJ)V", sev_uring_prepConnect0},
    {"prepClose0", "(JI)V", sev_uring_prepClose0},
    {"prepRead0", "(JIJIJ)V", sev_uring_prepRead0},
    {"prepRecv0", "(JIJJI)V", sev_uring_prepRecv0},
    {"prepSend0", "(JIJJI)V", sev_uring_prepSend0},
    {"submit0", "(J)I", sev_uring_submit0},
    {"copyCqes0", "(JJII)I", sev_uring_copyCqes0},
    {"enter0", "(JIII)I", sev_uring_enter0}
};

static const int method_table_size = 13;

static jint sev_uring_JNI_OnLoad(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, SEV_URING_CLASSNAME);
    int res = (*env)->RegisterNatives(env, clazz, method_table, method_table_size);
    return res;
}

JNIEXPORT jint JNICALL JNI_OnLoad
    (JavaVM *vm, void *reserved) {
        JNIEnv *env = NULL;
        if((*vm)->GetEnv(vm, (void **) &env, SEV_JNI_VERSION) != JNI_OK) {
            return JNI_ERR;
        }
        if(sev_socket_JNI_OnLoad(env) != JNI_OK) {
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Unable to register native socket methods");
        }
        if(sev_buffer_JNI_OnLoad(env) != JNI_OK) {
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Unable to register native buffer util methods");
        }
        if(sev_uring_JNI_OnLoad(env) != JNI_OK) {
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Unable to register native liburing methods");
        }
        ILLEGALARGEXCEPTION_CLASS = (*env)->FindClass(env, ILLEGALARGEXCEPTION_CLASSNAME);
        IOEXCEPTION_CLASS = (*env)->FindClass(env, IOEXCEPTION_CLASSNAME);
        IOURINGEXCEPTION_CLASS = (*env)->FindClass(env, IOURINGEXCEPTION_CLASSNAME);
        //(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Test Exception");
        //return JNI_ERR;
        return SEV_JNI_VERSION;
    }