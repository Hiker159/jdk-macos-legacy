/* SPDX-License-Identifier: GPL-2.0-only */
#include "legacy_macos_clock.hpp"
#include <assert.h>
#include <unistd.h>
int main() {
    timespec first, second;
    assert(legacy_macos_clock_gettime(CLOCK_REALTIME, &first) == 0);
    timeval wall;
    assert(gettimeofday(&wall, nullptr) == 0);
    assert(wall.tv_sec >= first.tv_sec && wall.tv_sec - first.tv_sec <= 1);
    assert(first.tv_nsec >= 0 && first.tv_nsec < 1000000000);
    assert(legacy_macos_clock_gettime(CLOCK_MONOTONIC, &first) == 0);
    usleep(2000);
    assert(legacy_macos_clock_gettime(CLOCK_MONOTONIC, &second) == 0);
    assert(second.tv_sec > first.tv_sec ||
           (second.tv_sec == first.tv_sec && second.tv_nsec > first.tv_nsec));
    assert(legacy_macos_clock_gettime((clockid_t)-123, &second) == -1);
    assert(errno == EINVAL);
}
