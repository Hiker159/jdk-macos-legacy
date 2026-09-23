/* SPDX-License-Identifier: GPL-2.0-only
 * Test-only dyld interposer: simulate missing post-Mavericks file APIs on a
 * modern host. Load only for the port's smoke test, never ship inside the JDK.
 */
#include <dlfcn.h>
#include <string.h>
#include <unistd.h>
static void* legacy_test_dlsym(void* handle, const char* name) {
    static const char* missing[] = {"futimens", "utimensat", "fchmodat", "openat",
        "fstatat", "fdopendir", "fstatat$INODE64", "fdopendir$INODE64", "unlinkat", "renameat", "clonefile"};
    for (unsigned int i = 0; i < sizeof(missing) / sizeof(missing[0]); i++) {
        if (strcmp(name, missing[i]) == 0) {
            const char prefix[] = "Simulated missing API: ";
            write(2, prefix, sizeof(prefix) - 1);
            write(2, name, strlen(name));
            write(2, "\n", 1);
            return NULL;
        }
    }
    // dyld excludes calls from the interposing image itself from interposition.
    return dlsym(handle, name);
}
__attribute__((used)) static struct {
    const void* replacement;
    const void* replacee;
} interpose[] __attribute__((section("__DATA,__interpose"))) = {
    {(const void*)legacy_test_dlsym, (const void*)dlsym}
};
