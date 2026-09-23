#!/bin/bash
set -euo pipefail
if [[ $# -lt 3 || ( "$1" != 21 && "$1" != 22 && "$1" != 25 ) ]]; then
    echo "Usage: $0 {21|22|25} /path/to/patched/source /path/to/boot-jdk/Contents/Home [configure options...]" >&2
    exit 2
fi
version=$1
source_dir=$(cd "$2" && pwd)
boot_jdk=$(cd "$3" && pwd)
shift 3
[[ $(uname -s) == Darwin && $(uname -m) == x86_64 ]] || {
    echo "Use an Intel macOS build host. The result targets Intel Mavericks." >&2; exit 1;
}
[[ -f "$source_dir/src/hotspot/os/bsd/legacy_macos_clock.hpp" ]] || {
    echo "Apply the Mavericks patches first." >&2; exit 1;
}
actual=$(awk -F= '/^DEFAULT_VERSION_FEATURE=/ { print $2 }' "$source_dir/make/conf/version-numbers.conf")
[[ "$actual" == "$version" ]] || { echo "Source version mismatch" >&2; exit 1; }
[[ -x "$boot_jdk/bin/javac" ]] || { echo "Bootstrap path must contain bin/javac" >&2; exit 1; }
# Keep configure output under the source tree, even when invoked elsewhere.
cd "$source_dir"
bash configure \
    --with-boot-jdk="$boot_jdk" \
    --with-conf-name=mavericks \
    --with-debug-level=release \
    --with-native-debug-symbols=none \
    --disable-precompiled-headers \
    --disable-warnings-as-errors \
    --with-jvm-features=-zgc \
    --with-vendor-name=jdk-macos-legacy \
    --with-vendor-version-string=mavericks-experimental \
    --without-version-pre --without-version-opt \
    "$@"
make images JOBS="${JOBS:-4}"
echo "Build finished. Audit build/mavericks/images/jdk before transferring it to Mavericks."
