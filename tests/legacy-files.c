/* SPDX-License-Identifier: GPL-2.0-only */
#include "legacy_macos_fs.h"
#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
int main(void) {
    char dir[] = "/tmp/legacy-files-XXXXXX";
    assert(mkdtemp(dir));
    char file[256], link[256];
    snprintf(file, sizeof(file), "%s/file", dir);
    snprintf(link, sizeof(link), "%s/link", dir);
    int fd = open(file, O_CREAT | O_RDWR, 0600);
    assert(fd >= 0);
    struct timespec times[2] = {{1600000000, 123456789}, {1600000100, 987654321}};
    assert(legacy_macos_futimens(fd, times) == 0);
    struct stat data;
    assert(fstat(fd, &data) == 0);
    assert(data.st_mtimespec.tv_sec == times[1].tv_sec);
    assert(data.st_mtimespec.tv_nsec == 987654000);
    assert(symlink(file, link) == 0);
    times[1].tv_sec += 100;
    assert(legacy_macos_utimensat(AT_FDCWD, link, times, AT_SYMLINK_NOFOLLOW) == 0);
    assert(lstat(link, &data) == 0);
    assert(data.st_mtimespec.tv_sec == times[1].tv_sec);
    assert(fstat(fd, &data) == 0);
    assert(data.st_mtimespec.tv_sec == times[1].tv_sec - 100);
    assert(legacy_macos_utimensat(AT_FDCWD, link, times, 0) == 0);
    assert(fstat(fd, &data) == 0);
    assert(data.st_mtimespec.tv_sec == times[1].tv_sec);
    assert(legacy_macos_fchmodat(AT_FDCWD, file, 0640, 0) == 0);
    assert(fstat(fd, &data) == 0 && (data.st_mode & 0777) == 0640);
    assert(legacy_macos_utimensat(fd, "relative", times, 0) == -1 && errno == ENOTSUP);
    assert(legacy_macos_fchmodat(fd, "relative", 0600, 0) == -1 && errno == ENOTSUP);
    assert(legacy_macos_utimensat(AT_FDCWD, file, times, 0x40000000) == -1 && errno == EINVAL);
    times[1].tv_nsec = -1;
    assert(legacy_macos_futimens(fd, times) == -1 && errno == EINVAL);
    times[1].tv_nsec = 1000000000L;
    assert(legacy_macos_futimens(fd, times) == -1 && errno == EINVAL);
    assert(close(fd) == 0);
    assert(unlink(link) == 0 && unlink(file) == 0 && rmdir(dir) == 0);
    puts("PASS: timestamp precision, symlink follow/nofollow, permissions, invalid inputs, directory-relative refusal");
}
