#define _GNU_SOURCE

#include <time.h>
#include <errno.h>
#include <stdlib.h>

long sev_calloc(long nmemb, long size) {
    return (long) calloc(nmemb, size);
}

void sev_free(long ptr_long) {
    free((void *) ptr_long);
}

int sev_clockGetTime(clockid_t clockid, struct timespec *tp) {
    int res = clock_gettime(clockid, tp);
    if(res == -1) {
        return -errno;
    }
    return 0;
}