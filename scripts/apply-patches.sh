#!/bin/bash
set -euo pipefail
if [[ $# -ne 2 || ( "$1" != 21 && "$1" != 22 && "$1" != 25 ) ]]; then
    echo "Usage: $0 {21|22|25} /path/to/clean/openjdk-source" >&2
    exit 2
fi
version=$1
project=$(cd "$(dirname "$0")/.." && pwd)
source_dir=$(cd "$2" && pwd)
case "$version" in
    21) expected=21.0.3 ;;
    22) expected=22.0.2 ;;
    25) expected=25.0.4.1 ;;
esac
version_file="$source_dir/make/conf/version-numbers.conf"
[[ -f "$version_file" ]] || { echo "Not an OpenJDK source tree: $source_dir" >&2; exit 1; }
actual=$(awk -F= '/^DEFAULT_VERSION_FEATURE=/ { f=$2 } /^DEFAULT_VERSION_INTERIM=/ { i=$2 } /^DEFAULT_VERSION_UPDATE=/ { u=$2 } /^DEFAULT_VERSION_PATCH=/ { p=$2 } END { printf "%s.%s.%s", f, i, u; if (p+0 != 0) printf ".%s", p }' "$version_file")
[[ "$actual" == "$expected" ]] || {
    echo "Expected OpenJDK $expected, found $actual. Use the pinned release in docs/MAVERICKS.md." >&2
    exit 1
}
cd "$source_dir"
patch_file="$project/patches/$version/mavericks.patch"
# Check every hunk before modifying anything. Never silently accept offsets via patch(1).
git apply --check "$patch_file"
git apply "$patch_file"
echo "Applied Java $version Mavericks patches to $source_dir"
