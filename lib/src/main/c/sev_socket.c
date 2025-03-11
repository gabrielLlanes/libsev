#include <stdlib.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

#include "sev_socket.h"
#include "sev_jni.h"

#define SEV_SOCKET_CLASSNAME "io/sev/socket/Socket"

int sev_socket(int domain, int type, int protocol) {
    return socket(domain, type, protocol);
}

int sev_socket_initAddr(JNIEnv *env, jbyteArray address, jint port,
    struct sockaddr_in *addr, socklen_t *addrSize) {
    int len = (*env)->GetArrayLength(env, address);
    if(len > 16) return -1;

    jbyte addrBytes[16];
    (*env)->GetByteArrayRegion(env, address, 0, len, addrBytes);

    *addrSize = sizeof(struct sockaddr_in);
    addr->sin_family = AF_INET;
    addr->sin_port = htons((uint16_t) port);
    memcpy(&(addr->sin_addr.s_addr), addrBytes, sizeof(addr->sin_addr.s_addr));

    return 0;
}

static jint sev_socket_newStreamSocketFd
    (JNIEnv *env, jclass clazz, jboolean sockCloExec, jboolean sockNonblock) {
        int type = SOCK_STREAM;
        if(sockCloExec) type |= SOCK_CLOEXEC;
        if(sockNonblock) type |= SOCK_NONBLOCK; 
        int sock = sev_socket(AF_INET, type, 0);
        if(sock == -1) return -errno;
        return sock;
    }

static jint sev_socket_bind0
    (JNIEnv *env, jclass clazz, jint fd, jbyteArray address, jint port) {
        struct sockaddr_in addr;
        socklen_t addrSize;
        if(sev_socket_initAddr(env, address, port, &addr, &addrSize) == -1) {
            return -1;
        }

        if(bind(fd, (const struct sockaddr *) &addr, addrSize) == -1) {
            return -errno;
        }
        return 0;
    }

static jint sev_socket_listen0
    (JNIEnv *env, jclass clazz, jint fd, jint backlog) {
        if(listen(fd, backlog) == -1) return -errno;
        return 0;
    }

static jint sev_socket_connect0
    (JNIEnv *env, jclass clazz, jint fd, jbyteArray address, jint port) {
        struct sockaddr_in addr;
        socklen_t addrSize;
        if(sev_socket_initAddr(env, address, port, &addr, &addrSize) == -1) {
            return -1;
        }

        int res;
        do {
            res = connect(fd, (const struct sockaddr *) &addr, addrSize);
        } while(res == -1 && errno == EINTR);

        if(res == -1) return -errno;
        return 0;
    }

static jint sev_socket_shutdown0
    (JNIEnv *env, jclass clazz, jint fd, jboolean read, jboolean write) {
        int mode;
        if(read && write) mode = SHUT_RDWR;
        else if(read) mode = SHUT_RD;
        else if(write) mode = SHUT_WR;
        else return -EINVAL;
        if(shutdown(fd, mode) == -1) return -errno;
        return 0;
    }

static jint sev_socket_close0
    (JNIEnv *env, jclass clazz, jint fd) {
        int res = close(fd);
        if(res == -1) {
            if(errno != EINTR) {
                return -errno;
            }
        }
        return 0;
    }

static jint sev_socket_getSockOpt0
    (JNIEnv *env, jclass clazz, jint fd, jint level, jint optname) {
        jint optval;
        socklen_t len = sizeof(optval);
        printf("%d %d\n", level, optname);
        int res = getsockopt(fd, level, optname, &optval, &len);
        if(res == -1) return -errno;
        return optval;
    }

static jint sev_socket_setSockOpt0
    (JNIEnv *env, jclass clazz, jint fd, jint level, jint optname, jint optval) {
        int res = setsockopt(fd, level, optname, &optval, sizeof(optval));
        if(res == -1) return -errno;
        return 0;
    }

static const JNINativeMethod method_table[] = {
    {"newStreamSocketFd", "(ZZ)I", sev_socket_newStreamSocketFd},
    {"bind0", "(I[BI)I", sev_socket_bind0},
    {"listen0", "(II)I", sev_socket_listen0},
    {"connect0", "(I[BI)I", sev_socket_connect0},
    {"shutdown0", "(IZZ)I", sev_socket_shutdown0},
    {"close0", "(I)I", sev_socket_close0},
    {"getSockOpt0", "(III)I", sev_socket_getSockOpt0},
    {"setSockOpt0", "(IIII)I", sev_socket_setSockOpt0}
};

static const jint method_table_size = 8;

jint sev_socket_JNI_OnLoad(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, SEV_SOCKET_CLASSNAME);
    int res = (*env)->RegisterNatives(env, clazz, method_table, method_table_size);
    return res;
}
