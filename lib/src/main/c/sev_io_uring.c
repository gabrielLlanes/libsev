#include <stdlib.h>
#include <stddef.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>

#include "liburing.h"
#include "sev_jni.h"
#include "sev_socket.h"
#include "sev_buffer.h"

#define IOEXCEPTION_CLASSNAME "java/io/IOException"
#define SEV_URING_CLASSNAME "io/sev/uring/IoUring"
#define SEV_JNI_VERSION JNI_VERSION_1_6

static jclass IOEXCEPTION_CLASS = NULL;

static jlong sev_uring_queueInit0
    (JNIEnv *env, jclass clazz, jint entries, jint flags) {
        struct io_uring *ring = (struct io_uring *) malloc(sizeof(struct io_uring));
        int res = io_uring_queue_init((unsigned int) entries, ring, (unsigned int) flags);
        if(res < 0) {
            (*env)->ThrowNew(env, IOEXCEPTION_CLASS, strerror(-res));
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

static void sev_uring_prepConnect0
    (JNIEnv *env, jclass clazz, jlong sqeAddress, jint fd, jlong addr, jint addrLen) {
        struct io_uring_sqe *sqe = (struct io_uring_sqe *) sqeAddress;
        io_uring_prep_connect(sqe, fd, (const struct sockaddr *) addr, addrLen);
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

// static jint sev_uring_waitCqe0
//     (JNIEnv *env, jclass clazz, jlong ringAddress, jlong) {
        
//     }

static const JNINativeMethod method_table[] = {
    {"queueInit0", "(II)J", sev_uring_queueInit0},
    {"getSqe0", "(J)J", sev_uring_getSqe0},
    {"setSqeData0", "(JJZ)V", sev_uring_setSqeData0},
    {"prepNop0", "(J)V", sev_uring_prepNop0},
    {"prepAccept0", "(JIJJIZ)V", sev_uring_prepAccept0},
    {"prepConnect0", "(JIJI)V", sev_uring_prepConnect0},
    {"prepClose0", "(JI)V", sev_uring_prepClose0},
    {"prepRead0", "(JIJIJ)V", sev_uring_prepRead0},
    {"prepRecv0", "(JIJJI)V", sev_uring_prepRecv0},
    {"prepSend0", "(JIJJI)V", sev_uring_prepSend0},
    {"submit0", "(J)I", sev_uring_submit0}
};

static const int method_table_size = 11;

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
        IOEXCEPTION_CLASS = (*env)->FindClass(env, IOEXCEPTION_CLASSNAME);
        //(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Test Exception");
        //return JNI_ERR;
        return SEV_JNI_VERSION;
    }